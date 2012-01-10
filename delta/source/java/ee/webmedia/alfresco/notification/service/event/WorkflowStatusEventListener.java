package ee.webmedia.alfresco.notification.service.event;

import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.web.app.servlet.FacesHelper;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.notification.service.NotificationService;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.BaseWorkflowObject;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEvent;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventQueue;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventQueue.WorkflowQueueParameter;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventType;
import ee.webmedia.alfresco.workflow.service.event.WorkflowMultiEventListener;

public class WorkflowStatusEventListener implements WorkflowMultiEventListener, InitializingBean {

    private NotificationService notificationService;
    private WorkflowService workflowService;
    private GeneralService generalService;

    @Override
    public void afterPropertiesSet() throws Exception {
        workflowService.registerMultiEventListener(this);
    }

    @Override
    public void handleMultipleEvents(WorkflowEventQueue queue) {
        boolean sendNotifications = !Boolean.TRUE.equals(queue
                .getParameter(WorkflowQueueParameter.TRIGGERED_BY_FINISHING_EXTERNAL_REVIEW_TASK_ON_CURRENT_SYSTEM));
        for (WorkflowEvent event : queue.getEvents()) {
            BaseWorkflowObject object = event.getObject();
            if (object instanceof Task && ((Task) object).getOwnerId() != null && ((Task) object).getOwnerId().equals(AuthenticationUtil.getRunAsUser())) {
                refreshMenuTaskCount();
                break;
            }
        }
        final List<WorkflowEvent> events = new ArrayList<WorkflowEvent>();
        events.addAll(queue.getEvents());
        final Task initiatingTask = queue.getParameter(WorkflowQueueParameter.ORDER_ASSIGNMENT_FINISH_TRIGGERING_TASK);
        if (sendNotifications) {
            // Send notifications in background in separate thread.
            // TODO: Riina - implement correctly - save information about emails to send to repo to avoid loss of data, cl task 189285.
            generalService.runOnBackground(new RunAsWork<Void>() {

                @Override
                public Void doWork() throws Exception {
                    return WorkflowStatusEventListener.this.notify(events, initiatingTask);
                }

            }, "WorkflowEmailSender");
        }
    }

    private Void notify(final List<WorkflowEvent> events, final Task initiatingTask) {
        for (WorkflowEvent event : events) {
            BaseWorkflowObject object = event.getObject();
            if (object instanceof CompoundWorkflow) {
                handleCompoundWorkflowNotifications(event);
            }
            if (object instanceof Workflow) {
                handleWorkflowNotifications(event);
            }
            if (object instanceof Task) {
                Task task = (Task) event.getObject();
                if (task.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK) && task.isStatus(Status.FINISHED)) {
                    if (initiatingTask == null || initiatingTask.getNodeRef().equals(task.getNodeRef())
                            || !initiatingTask.getParent().getNodeRef().equals(task.getParent().getNodeRef())
                            || !Boolean.TRUE.equals(task.getProp(WorkflowSpecificModel.Props.SEND_ORDER_ASSIGNMENT_COMPLETED_EMAIL))) {
                        continue;
                    }
                }
                handleTaskNotifications(event);
            }
        }
        return null;
    }

    public boolean getSendNotifications(WorkflowEvent event, WorkflowEventQueue queue) {
        boolean sendNotifications = !Boolean.TRUE.equals(queue
                .getParameter(WorkflowQueueParameter.TRIGGERED_BY_FINISHING_EXTERNAL_REVIEW_TASK_ON_CURRENT_SYSTEM));
        if (sendNotifications && event.getObject() instanceof Task) {
            Task task = (Task) event.getObject();
            if (task.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK) && task.isStatus(Status.FINISHED)) {
                Task initiatingTask = queue.getParameter(WorkflowQueueParameter.ORDER_ASSIGNMENT_FINISH_TRIGGERING_TASK);
                if (initiatingTask == null || initiatingTask.getNodeRef().equals(task.getNodeRef())
                        || !initiatingTask.getParent().getNodeRef().equals(task.getParent().getNodeRef())
                        || !Boolean.TRUE.equals(task.getProp(WorkflowSpecificModel.Props.SEND_ORDER_ASSIGNMENT_COMPLETED_EMAIL))) {
                    sendNotifications = false;
                }
            }
        }

        return sendNotifications;
    }

    private void refreshMenuTaskCount() {
        // Let's assume that this never gets called from a job, and there is an existing context :)
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            MenuBean menuBean = (MenuBean) FacesHelper.getManagedBean(context, MenuBean.BEAN_NAME);
            menuBean.processTaskItems();
        }
    }

    private void handleTaskNotifications(WorkflowEvent event) {

        Task task = (Task) event.getObject();
        if (event.getType().equals(WorkflowEventType.STATUS_CHANGED)) {
            if (!task.isStatus(Status.UNFINISHED)) {
                notificationService.notifyTaskEvent(task);
            } else {
                notificationService.notifyTaskUnfinishedEvent(task);
            }
        }

    }

    private void handleWorkflowNotifications(WorkflowEvent event) {
        Workflow workflow = (Workflow) event.getObject();
        if (event.getType().equals(WorkflowEventType.STATUS_CHANGED)) {
            notificationService.notifyWorkflowEvent(workflow, event.getType());
        } else if (event.getType().equals(WorkflowEventType.WORKFLOW_STARTED_AUTOMATICALLY)) {
            notificationService.notifyWorkflowEvent(workflow, event.getType());
        }

    }

    private void handleCompoundWorkflowNotifications(WorkflowEvent event) {

    }

    // START: getters/setters

    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    // END: getters/setters

}
