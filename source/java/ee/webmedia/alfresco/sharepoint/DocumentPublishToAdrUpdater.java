package ee.webmedia.alfresco.sharepoint;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.adr.service.AdrService;
import ee.webmedia.alfresco.classificator.enums.PublishToAdr;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.SearchUtil;

public class DocumentPublishToAdrUpdater extends AbstractNodeUpdater {

    private AdrService adrService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateAndNotQuery(SearchUtil.joinQueryPartsAnd(
                    SearchUtil.generateTypeQuery(DocumentCommonModel.Types.DOCUMENT),
                    SearchUtil.generateStringExactQuery("Rahvusvaheline õigusabi § 35 lg 1 p 3", DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON)
                ),
                SearchUtil.generateStringExactQuery(PublishToAdr.NOT_TO_ADR.getValueName(), DocumentDynamicModel.Props.PUBLISH_TO_ADR));
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        resultSets.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        resultSets.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Map<QName, Serializable> props = nodeService.getProperties(nodeRef);
        String accessRestrictionReason = (String) props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON);
        String oldPublishToAdr = (String) props.get(DocumentDynamicModel.Props.PUBLISH_TO_ADR);
        String newPublishToAdr = oldPublishToAdr;
        Boolean setValue = Boolean.FALSE;
        if ("Rahvusvaheline õigusabi § 35 lg 1 p 3".equals(StringUtils.strip(accessRestrictionReason))
                && !PublishToAdr.NOT_TO_ADR.getValueName().equals(oldPublishToAdr)) {
            newPublishToAdr = PublishToAdr.NOT_TO_ADR.getValueName();
            nodeService.setProperty(nodeRef, DocumentDynamicModel.Props.PUBLISH_TO_ADR, newPublishToAdr);
            adrService.addDeletedDocument(nodeRef);
            setValue = Boolean.TRUE;
        }
        Date regDateTime = (Date) props.get(DocumentCommonModel.Props.REG_DATE_TIME);
        return new String[] {
                setValue.toString(),
                accessRestrictionReason,
                oldPublishToAdr,
                newPublishToAdr,
                (String) props.get(DocumentCommonModel.Props.DOC_NAME),
                (String) props.get(DocumentCommonModel.Props.REG_NUMBER),
                regDateTime == null ? null : regDateTime.toString() };
    }

    public void setAdrService(AdrService adrService) {
        this.adrService = adrService;
    }

}
