package ee.webmedia.alfresco.document.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Checks document's searchableSendMode property and if needed updates it to correct value
 * Used to remove sequences of document copying bug (CL task 131723)
 * 
 * @author Riina Tens
 */
public class SearchableSendModeUpdater extends AbstractNodeUpdater {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(SearchableSendModeUpdater.class);

    private SendOutService sendOutService;
    private String searchableSendModeUpdateBeginDate;

    @Override
    protected void executeInternal() throws Throwable {
        if (StringUtils.isBlank(searchableSendModeUpdateBeginDate)) {
            log.debug("Skipping searchableSendMode update, begin date is blank");
            return;
        }
        super.executeInternal();
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        Date liveDate = dateFormat.parse(searchableSendModeUpdateBeginDate);

        List<String> queryParts = new ArrayList<String>(2);
        queryParts.add(SearchUtil.generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE));
        queryParts.add(SearchUtil.generateDatePropertyRangeQuery(liveDate, null, ContentModel.PROP_CREATED));
        String query = SearchUtil.joinQueryPartsAnd(queryParts);
        List<ResultSet> result = new ArrayList<ResultSet>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own modifier and modified values
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {

        Map<QName, Serializable> origProps = nodeService.getProperties(nodeRef);
        @SuppressWarnings("unchecked")
        List<String> searchableSendMode = (List<String>) origProps.get(DocumentCommonModel.Props.SEARCHABLE_SEND_MODE);
        List<ChildAssociationRef> sendInfos = null;
        boolean sendModeUpdated = false;
        if (searchableSendMode != null && searchableSendMode.size() > 0) {
            sendInfos = nodeService.getChildAssocs(nodeRef, RegexQNamePattern.MATCH_ALL, DocumentCommonModel.Assocs.SEND_INFO);
            if (sendInfos == null || sendInfos.size() == 0 || sendInfos.size() != searchableSendMode.size()) {
                Map<QName, Serializable> setProps = new HashMap<QName, Serializable>();
                setProps.put(DocumentCommonModel.Props.SEARCHABLE_SEND_MODE, sendOutService.buildSearchableSendMode(nodeRef));
                setProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
                setProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
                nodeService.addProperties(nodeRef, setProps);
                sendModeUpdated = true;
            }
        }

        return new String[] { String.valueOf(sendModeUpdated),
                            searchableSendMode == null ? "0" : String.valueOf(searchableSendMode.size()),
                            sendInfos == null ? "0" : String.valueOf(sendInfos.size()) };
    }

    public void setSendOutService(SendOutService sendOutService) {
        this.sendOutService = sendOutService;
    }

    public void setSearchableSendModeUpdateBeginDate(String searchableSendModeUpdateBeginDate) {
        this.searchableSendModeUpdateBeginDate = searchableSendModeUpdateBeginDate;
    }

}
