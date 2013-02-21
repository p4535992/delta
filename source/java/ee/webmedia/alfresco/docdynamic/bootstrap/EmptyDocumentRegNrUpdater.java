package ee.webmedia.alfresco.docdynamic.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyNotNullQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyNullQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * Updater that fill document registration number with dash if it empty but document is registered.
 * 
 * @author Kaarel Jõgeva
 */
public class EmptyDocumentRegNrUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = joinQueryPartsAnd(
                generatePropertyNullQuery(DocumentCommonModel.Props.REG_NUMBER)
                , generatePropertyNotNullQuery(DocumentCommonModel.Props.REG_DATE_TIME)
                );
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        resultSets.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        resultSets.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Map<QName, Serializable> origProps = nodeService.getProperties(nodeRef);

        String regNr = (String) origProps.get(DocumentCommonModel.Props.REG_NUMBER);
        Date regDateTime = (Date) origProps.get(DocumentCommonModel.Props.REG_DATE_TIME);
        if (isNotBlank(regNr) || regDateTime == null) {
            return new String[] { "docRegNrNotUpdated", regNr, regDateTime == null ? "null" : regDateTime.toString() };
        }

        String newRegNr = "-";
        nodeService.setProperty(nodeRef, DocumentCommonModel.Props.REG_NUMBER, newRegNr);

        return new String[] { "docRegNrUpdated", newRegNr, regDateTime.toString() };
    }

}
