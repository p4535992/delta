package ee.webmedia.alfresco.workflow.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;

/**
 * Fill store_id field by parent workflow (or linkedReviewRoot) storeRef value
 * 
 * @author Riina Tens
 */
public class FixTaskStoreIdFromWorkflow extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.WORKFLOW, WorkflowSpecificModel.Types.LINKED_REVIEW_TASKS_ROOT);
        List<ResultSet> result = new ArrayList<ResultSet>(6);
        for (StoreRef storeRef : generalService.getAllWithArchivalsStoreRefs()) {
            result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        WorkflowDbService workflowDbService = BeanHelper.getWorkflowDbService();
        boolean isLinkedReviewRoot = WorkflowSpecificModel.Types.LINKED_REVIEW_TASKS_ROOT.equals(nodeService.getType(nodeRef));
        workflowDbService.updateWorkflowTasksStore(isLinkedReviewRoot ? null : nodeRef, nodeRef.getStoreRef());
        if (!isLinkedReviewRoot) {
            Map<QName, Serializable> newProps = new HashMap<QName, Serializable>();
            newProps.put(WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_OWNER_ORGANIZATION_NAME,
                    nodeService.getProperty(nodeRef, WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME));
            workflowDbService.updateWorkflowTaskProperties(nodeRef, newProps);
        }
        return new String[] {};
    }

}
