package ee.webmedia.alfresco.workflow.service.type;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEvent;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListenerWithModifications;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventQueue;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventType;
import ee.webmedia.alfresco.workflow.service.event.WorkflowModifications;

/**
 * @author Riina Tens
 */
public class GroupAssignmnetWorkflowType extends BaseWorkflowType implements WorkflowEventListenerWithModifications {

    @Override
    public void handle(WorkflowEvent event, WorkflowModifications workflowService, WorkflowEventQueue queue) {
        if (!(event.getObject() instanceof Task)) {
            return;
        }
        Task task = (Task) event.getObject();
        List<NodeRef> tasksFinishedByGroupTask = queue.getTasksFinishedByGroupTask();
        NodeRef initiatingGroupTask = queue.getInitiatingGroupTask();
        if (initiatingGroupTask == null) {
            return;
        }
        List<Task> workflowTasks = task.getParent().getTasks();
        boolean isSameWorkflow = false;
        for (Task groupAssignmentTask : workflowTasks) {
            if (initiatingGroupTask.equals(groupAssignmentTask.getNodeRef())) {
                isSameWorkflow = true;
                break;
            }
        }
        if (!isSameWorkflow) {
            return;
        }
        if (event.getType() == WorkflowEventType.STATUS_CHANGED && task.isStatus(Status.FINISHED) && !tasksFinishedByGroupTask.contains(task.getNodeRef())) {
            for (Task groupAssignmentTask : workflowTasks) {
                if (groupAssignmentTask.isStatus(Status.IN_PROGRESS)) {
                    groupAssignmentTask.setComment(MessageUtil.getMessage("task_comment_finished_by_group_task"));
                    workflowService.setTaskFinished(queue, groupAssignmentTask);
                    tasksFinishedByGroupTask.add(groupAssignmentTask.getNodeRef());
                }
            }
        }
    }
}
