package ee.webmedia.alfresco.notification.service.event;

import javax.faces.context.FacesContext;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.web.app.servlet.FacesHelper;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.notification.service.NotificationService;
import ee.webmedia.alfresco.workflow.service.BaseWorkflowObject;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEvent;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListener;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventType;

public class WorkflowStatusEventListener implements WorkflowEventListener, InitializingBean {

    private NotificationService notificationService;
    private WorkflowService workflowService;
    
    @Override
    public void afterPropertiesSet() throws Exception {
        workflowService.registerEventListener(this);
    }

    @Override
    public void handle(WorkflowEvent event) {
        final BaseWorkflowObject object = event.getObject();

        if (object instanceof CompoundWorkflow)
            handleCompoundWorkflowNotifications(event);

        if (object instanceof Workflow)
            handleWorkflowNotifications(event);

        if (object instanceof Task) {
            handleTaskNotifications(event);
            refreshMenuTaskCount(event);
        }

    }

    private void refreshMenuTaskCount(WorkflowEvent event) {
        if(((Task)event.getObject()).getOwnerId() != null && ((Task)event.getObject()).getOwnerId().equals(AuthenticationUtil.getRunAsUser())) {
            // Let's assume that this never gets called from a job, and there is an existing context :)
            FacesContext context = FacesContext.getCurrentInstance();
            if(context != null) {
                MenuBean menuBean = (MenuBean) FacesHelper.getManagedBean(context, MenuBean.BEAN_NAME);
                menuBean.processTaskItems();
            }
        }
    }

    private void handleTaskNotifications(WorkflowEvent event) {

        Task task = (Task) event.getObject();
        if (event.getType().equals(WorkflowEventType.STATUS_CHANGED)) {
            notificationService.notifyTaskEvent(task);
        }
        
    }

    private void handleWorkflowNotifications(WorkflowEvent event) {
        Workflow workflow = (Workflow) event.getObject();
        if(event.getType().equals(WorkflowEventType.STATUS_CHANGED)) {
            notificationService.notifyWorkflowEvent(workflow, event.getType());
        } else if(event.getType().equals(WorkflowEventType.WORKFLOW_STARTED_AUTOMATICALLY)) {
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

    // END: getters/setters

}
