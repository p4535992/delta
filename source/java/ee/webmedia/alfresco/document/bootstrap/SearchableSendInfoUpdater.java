package ee.webmedia.alfresco.document.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Duplicates {@link ee.webmedia.alfresco.document.model.DocumentCommonModel.Props#SEND_INFO_SEND_MODE},
 * {@link ee.webmedia.alfresco.document.model.DocumentCommonModel.Props#SEND_INFO_RECIPIENT} ,
 * {@link ee.webmedia.alfresco.document.model.DocumentCommonModel.Props#SEND_INFO_SEND_DATE_TIME} and
 * {@link ee.webmedia.alfresco.document.model.DocumentCommonModel.Props#SEND_INFO_RESOLUTION} properties to parent document. This enables us to search documents based on these
 * properties.
 */
public class SearchableSendInfoUpdater extends AbstractNodeUpdater {

    private final Set<NodeRef> processedDocs = new HashSet<NodeRef>();
    private SendOutService sendOutService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(DocumentCommonModel.Types.SEND_INFO);
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef sendInfoNodeRef) throws Exception {
        NodeRef documentRef = nodeService.getPrimaryParent(sendInfoNodeRef).getParentRef();
        if (processedDocs.contains(documentRef)) {
            return new String[] { "documentAlreadyProcessed", documentRef.toString() };
        }

        Map<QName, Serializable> origProps = nodeService.getProperties(documentRef);
        Map<QName, Serializable> searchableSendInfoProps = sendOutService.buildSearchableSendInfo(documentRef);
        searchableSendInfoProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
        searchableSendInfoProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
        nodeService.addProperties(documentRef, searchableSendInfoProps);
        processedDocs.add(documentRef);

        return new String[] { "searchablePropsAdded", documentRef.toString() };
    }

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own modifier and modified values
    }

    public void setSendOutService(SendOutService sendOutService) {
        this.sendOutService = sendOutService;
    }
}
