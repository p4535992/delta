package ee.webmedia.alfresco.workflow.service.type;

import ee.webmedia.alfresco.notification.service.NotificationService;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEvent;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListenerWithModifications;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventQueue;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventType;
import ee.webmedia.alfresco.workflow.service.event.WorkflowModifications;

public class InformationWorkflowType extends BaseWorkflowType implements WorkflowEventListenerWithModifications {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(InformationWorkflowType.class);

    private NotificationService notificationService;

    @Override
    public void handle(WorkflowEvent event, WorkflowModifications workflowService, WorkflowEventQueue queue) {
        if (event.getType() == WorkflowEventType.STATUS_CHANGED && event.getObject() instanceof Task
                && WorkflowUtil.isStatus(event.getObject(), Status.IN_PROGRESS)) {

            Task task = (Task) event.getObject();
            if (task.getOwnerId() == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Received event STATUS_CHANGED to IN_PROGRESS, finishing task");
                }
                // Finish task only if sending e-mail succeeds
                if (notificationService.processOutgoingInformationTask(task)) {
                    workflowService.setTaskFinished(queue, task);
                }
            }
        }
    }

    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

}
