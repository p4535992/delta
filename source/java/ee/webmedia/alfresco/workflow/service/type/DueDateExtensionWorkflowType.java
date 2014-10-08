package ee.webmedia.alfresco.workflow.service.type;

import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEvent;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListenerWithModifications;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventQueue;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventType;
import ee.webmedia.alfresco.workflow.service.event.WorkflowModifications;

public class DueDateExtensionWorkflowType extends BaseWorkflowType implements WorkflowEventListenerWithModifications {

    public static final int DUE_DATE_EXTENSION_OUTCOME_NOT_ACCEPTED = 1;
    private static final int DUE_DATE_EXTENSION_OUTCOME_ACCEPTED = 0;

    @Override
    public void handle(WorkflowEvent event, WorkflowModifications workflowService, WorkflowEventQueue queue) {
        if (event.getType() == WorkflowEventType.STATUS_CHANGED && event.getObject() instanceof Task
                && event.getObject().isStatus(Status.FINISHED)) {

            Task task = (Task) event.getObject();
            int outcomeIndex = task.getOutcomeIndex();
            if (outcomeIndex == DUE_DATE_EXTENSION_OUTCOME_ACCEPTED) {
                workflowService.changeInitiatingTaskDueDate(task, queue);
            } else if (outcomeIndex == DUE_DATE_EXTENSION_OUTCOME_NOT_ACCEPTED) {
                workflowService.rejectDueDateExtension(task);
            }
        }
    }

}
