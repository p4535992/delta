package ee.webmedia.alfresco.workflow.bootstrap;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.bootstrap.AbstractParallelNodeUpdater;
import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 * Update the {@link WorkflowCommonModel.Props#MAIN_DOCUMENT} property value if the referenced document has been archived
 */
public class FixIndependentCompoundWorkflowMainDocumentProperty extends AbstractParallelNodeUpdater {

    private static ThreadLocal<Map<NodeRef, NodeRef>> cwfRefToMainDocRef = new ThreadLocal<Map<NodeRef, NodeRef>>() {
        @Override
        protected java.util.Map<NodeRef, NodeRef> initialValue() {
            return new HashMap<>();
        }
    };

    private BulkLoadNodeService bulkLoadNodeService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.joinQueryPartsAnd(SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW),
                SearchUtil.generatePropertyExactQuery(WorkflowCommonModel.Props.TYPE, CompoundWorkflowType.INDEPENDENT_WORKFLOW.name()),
                SearchUtil.generatePropertyNotNullQuery(WorkflowCommonModel.Props.MAIN_DOCUMENT));
        return Arrays.asList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
    }

    @Override
    protected String[] updateNode(NodeRef cwfRef) throws Exception {
        NodeRef mainDocRef = cwfRefToMainDocRef.get().get(cwfRef);
        if (mainDocRef == null) {
            return null; // No main doc
        }
        if (nodeService.exists(mainDocRef)) {
            return null; // Correct reference
        }
        NodeRef ref = generalService.getExistingNodeRefAllStores(mainDocRef.getId());
        nodeService.setProperty(cwfRef, WorkflowCommonModel.Props.MAIN_DOCUMENT, ref);
        return new String[] { mainDocRef.toString(), (ref != null) ? ref.toString() : "null" };
    }

    @Override
    protected void doBeforeBatchUpdate(List<NodeRef> batchList) {
        Map<NodeRef, NodeRef> map = cwfRefToMainDocRef.get();
        map.clear();
        Map<NodeRef, Node> nodes = bulkLoadNodeService.loadNodes(batchList, Collections.singleton(WorkflowCommonModel.Props.MAIN_DOCUMENT));
        for (Map.Entry<NodeRef, Node> entry : nodes.entrySet()) {
            NodeRef docRef = (NodeRef) entry.getValue().getProperties().get(WorkflowCommonModel.Props.MAIN_DOCUMENT);
            map.put(entry.getKey(), docRef);
        }
    }

    @Override
    protected void executeUpdater() throws Exception {
        super.executeUpdater();
        cwfRefToMainDocRef = null;
    }

    public void setBulkLoadNodeService(BulkLoadNodeService bulkLoadNodeService) {
        this.bulkLoadNodeService = bulkLoadNodeService;
    }

}
