package ee.webmedia.alfresco.workflow.bootstrap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.WorkflowConstantsBean;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;

/**
 * Delete invalid (actually deleted) tasks with non-existing workflow refs.
 */
public class LogAndDeleteNotExistingWorkflowTasks extends AbstractNodeUpdater {

    private WorkflowDbService workflowDbService;
    private WorkflowConstantsBean workflowConstantsBean;

    @Override
    protected boolean usePreviousInputState() {
        return false;
    }

    @Override
    protected boolean processOnlyExistingNodeRefs() {
        return false;
    }

    @Override
    protected Set<NodeRef> loadNodesFromRepo() throws Exception {
        return workflowDbService.getAllWorflowNodeRefs();
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        // not used
        return null;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        if (!nodeService.exists(nodeRef)) {
            List<Task> tasksToDelete = workflowDbService.getWorkflowTasks(nodeRef, null, null, workflowConstantsBean.getTaskPrefixedQNames(), null, null, false);
            List<String> taskData = new ArrayList<String>();
            for (Task task : tasksToDelete) {
                taskData.add(task.getNode().toString());
            }
            workflowDbService.deleteWorkflowTasks(nodeRef);
            return taskData.toArray(new String[taskData.size()]);
        }
        return null;
    }

    public void setWorkflowDbService(WorkflowDbService workflowDbService) {
        this.workflowDbService = workflowDbService;
    }

    public WorkflowConstantsBean getWorkflowConstantsBean() {
        return workflowConstantsBean;
    }

    public void setWorkflowConstantsBean(WorkflowConstantsBean workflowConstantsBean) {
        this.workflowConstantsBean = workflowConstantsBean;
    }

}
