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
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * Insert tasks' multivalued properties into corresponding tables.
 */
public class TaskAssociatedDataTableInsertBootstrap2 extends AbstractNodeUpdater {

    private WorkflowDbService workflowDbService;
    private WorkflowService workflowService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.TASK);
        List<ResultSet> result = new ArrayList<ResultSet>(6);
        for (StoreRef storeRef : generalService.getAllWithArchivalsStoreRefs()) {
            result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        String[] taskExistsError = TaskAssociatedDataTableInsertBootstrap.checkTaskExists(nodeRef, log, nodeService, workflowService, workflowDbService);
        if (taskExistsError != null) {
            return taskExistsError;
        }
        Map<QName, Serializable> newProps = new HashMap<QName, Serializable>();
        newProps.put(WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_OWNER_ORGANIZATION_NAME,
                nodeService.getProperty(nodeRef, WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_OWNER_ORGANIZATION_NAME));
        workflowDbService.updateTaskProperties(nodeRef, newProps);
        return new String[] {};
    }

    public void setWorkflowDbService(WorkflowDbService workflowDbService) {
        this.workflowDbService = workflowDbService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

}
