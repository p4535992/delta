package ee.webmedia.alfresco.workflow.bootstrap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * Reorder not parallel workflow tasks, where current task ordering does not follow the rule:
 * Teostatud/Lõpetamata -> Teostamisel/Peatatud -> Uus (see cl task 214260).
 * Enabled with conf parameter updater.reorderNotParallelWorkflowTasks.enabled.
 * NB! It should be validated that this updater's algorithm is suitable for repairing data
 * before executing it.
 * 
 * @author Riina Tens
 */
public class ReorderNotParallelWorkflowTasks extends AbstractNodeUpdater {

    private WorkflowDbService workflowDbService;
    private WorkflowService workflowService;

    @Override
    public boolean isContinueWithNextBatchAfterError() {
        return true;
    }

    @Override
    protected Set<NodeRef> loadNodesFromRepo() throws Exception {
        return workflowDbService.getWorkflowsWithWrongTaskOrder();
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        // not used
        return null;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        if (Boolean.TRUE.equals(nodeService.getProperty(nodeRef, WorkflowCommonModel.Props.PARALLEL_TASKS))) {
            return new String[] { "parallel workflow, not changed" };
        }
        CompoundWorkflow compoundWorkflow = workflowService.getCompoundWorkflow(generalService.getPrimaryParent(nodeRef).getNodeRef());
        Workflow workflow = null;
        for (Workflow wf : compoundWorkflow.getWorkflows()) {
            if (nodeRef.equals(wf.getNodeRef())) {
                workflow = wf;
                break;
            }
        }
        if (workflow == null) {
            return new String[] { "workflow not found" };
        }
        List<Task> finishedTasks = new ArrayList<Task>();
        List<Task> inProgressTasks = new ArrayList<Task>();
        List<Task> newTasks = new ArrayList<Task>();
        for (Task task : workflow.getTasks()) {
            if (task.isStatus(Status.NEW)) {
                newTasks.add(task);
            } else if (task.isStatus(Status.IN_PROGRESS, Status.STOPPED)) {
                inProgressTasks.add(task);
            } else {
                finishedTasks.add(task);
            }
        }
        int i = updateTaskIndexes(finishedTasks, 0);
        i = updateTaskIndexes(inProgressTasks, i);
        i = updateTaskIndexes(newTasks, i);
        return null;
    }

    private int updateTaskIndexes(List<Task> tasks, int i) {
        for (Task task : tasks) {
            task.setTaskIndexInWorkflow(i++);
            workflowDbService.updateTaskEntry(task, null);
        }
        return i;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setWorkflowDbService(WorkflowDbService workflowDbService) {
        this.workflowDbService = workflowDbService;
    }

}
