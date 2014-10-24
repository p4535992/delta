package ee.webmedia.alfresco.document.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.search.DbSearchUtil;
import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.common.service.CreateObjectCallback;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.sendout.model.DocumentSendInfo;
import ee.webmedia.alfresco.document.sendout.model.SendInfo;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Duplicates {@link ee.webmedia.alfresco.document.model.DocumentCommonModel.Props#SEND_INFO_SEND_MODE},
 * {@link ee.webmedia.alfresco.document.model.DocumentCommonModel.Props#SEND_INFO_RECIPIENT} ,
 * {@link ee.webmedia.alfresco.document.model.DocumentCommonModel.Props#SEND_INFO_SEND_DATE_TIME} and
 * {@link ee.webmedia.alfresco.document.model.DocumentCommonModel.Props#SEND_INFO_RESOLUTION} properties to parent document. This enables us to search documents based on these
 * properties.
 */
public class SearchableSendInfoUpdater extends AbstractNodeUpdater {

    private static final Set<QName> MODIFIER_PROPS = new HashSet<>(Arrays.asList(ContentModel.PROP_MODIFIER, ContentModel.PROP_MODIFIED));

    private BulkLoadNodeService bulkLoadNodeService;
    private Map<Long, QName> propertyTypes;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(DocumentCommonModel.Types.DOCUMENT);
        List<ResultSet> resultSets = new ArrayList<>();
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected List<String[]> processNodes(final List<NodeRef> batchList) throws Exception, InterruptedException {
        final List<String[]> batchInfos = new ArrayList<>(batchList.size());
        Map<NodeRef, List<SendInfo>> searchableSendInfos = bulkLoadNodeService.loadChildNodes(batchList, null, DocumentCommonModel.Types.SEND_INFO, propertyTypes,
                new CreateObjectCallback<SendInfo>() {

            @Override
            public SendInfo create(NodeRef nodeRef, Map<QName, Serializable> properties) {
                return new DocumentSendInfo(properties);
            }
        });
        Map<NodeRef, Map<QName, Serializable>> documentSearchableSendInfos = new HashMap<>();
        for (Map.Entry<NodeRef, List<SendInfo>> entry : searchableSendInfos.entrySet()) {
            List<SendInfo> searchableSendInfoProps = entry.getValue();
            if (searchableSendInfoProps.isEmpty()) {
                continue;
            }
            documentSearchableSendInfos.put(entry.getKey(), DbSearchUtil.buildSearchableSendInfos(searchableSendInfoProps));
        }
        Map<NodeRef, Node> documentProps = bulkLoadNodeService.loadNodes(batchList, MODIFIER_PROPS, propertyTypes);
        for (NodeRef docRef : batchList) {
            if (!documentProps.containsKey(docRef)) {
                batchInfos.add(new String[] { "nodeDoesNotExist" });
                continue;
            }
            if (!documentSearchableSendInfos.containsKey(docRef)) {
                batchInfos.add(new String[] { "no sendinfos found" });
                continue;
            }
            Map<QName, Serializable> searchableSendInfoProps = documentSearchableSendInfos.get(docRef);
            searchableSendInfoProps.putAll(RepoUtil.toQNameProperties(documentProps.get(docRef).getProperties()));
            nodeService.addProperties(docRef, searchableSendInfoProps);
            batchInfos.add(new String[] { "searchablePropsAdded" });
        }
        return batchInfos;
    }

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own modifier and modified values
    }

    public void setBulkLoadNodeService(BulkLoadNodeService bulkLoadNodeService) {
        this.bulkLoadNodeService = bulkLoadNodeService;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        throw new RuntimeException("Method not implemented!");
    }
}
