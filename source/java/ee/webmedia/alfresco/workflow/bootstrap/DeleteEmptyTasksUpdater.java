package ee.webmedia.alfresco.workflow.bootstrap;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * Delete tasks where ownerName and dueDate is null (see cl task 214260 for reasons how such tasks could be created)
 * and launch regular saving process on containing compound workflow.
 * 
 * @author Riina Tens
 */
public class DeleteEmptyTasksUpdater extends AbstractNodeUpdater {

    private WorkflowDbService workflowDbService;
    private WorkflowService workflowService;
    private final Set<NodeRef> processedCompoundWorkflows = new HashSet<NodeRef>();

    @Override
    public boolean isContinueWithNextBatchAfterError() {
        return true;
    }

    @Override
    protected Set<NodeRef> loadNodesFromRepo() throws Exception {
        return workflowDbService.getAllWorkflowsWithEmptyTasks();
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        // not used
        return null;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        NodeRef compoundWorkflowRef = generalService.getPrimaryParent(nodeRef).getNodeRef();
        if (processedCompoundWorkflows.contains(compoundWorkflowRef)) {
            return new String[] { "already processed" };
        }
        processedCompoundWorkflows.add(compoundWorkflowRef);
        CompoundWorkflow compoundWorkflow = workflowService.getCompoundWorkflow(compoundWorkflowRef);
        int tasksDeleted = 0;
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            for (Task task : workflow.getTasks()) {
                if (StringUtils.isBlank(task.getOwnerName()) && task.getDueDate() == null) {
                    workflowDbService.deleteTask(task.getNodeRef());
                    tasksDeleted++;
                }
            }
        }
        if (tasksDeleted == 0) {
            return new String[] { "no empty tasks" };
        }
        compoundWorkflow = workflowService.getCompoundWorkflow(compoundWorkflowRef);
        workflowService.saveCompoundWorkflow(compoundWorkflow);
        return new String[] { tasksDeleted + " tasks deleted" };
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setWorkflowDbService(WorkflowDbService workflowDbService) {
        this.workflowDbService = workflowDbService;
    }
}
