package ee.webmedia.alfresco.notification.service.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.web.app.servlet.FacesHelper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.notification.service.NotificationService;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.privilege.service.PrivilegeUtil;
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
    private static org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(WorkflowStatusEventListener.class);

    private static final int NR_OF_PERMISSIONS_IN_TRANSACTION = 30;

    private NotificationService notificationService;
    private WorkflowService workflowService;
    private GeneralService generalService;
    private PrivilegeService privilegeService;
    private FileService fileService;
    private TransactionService transactionService;

    @Override
    public void afterPropertiesSet() throws Exception {
        workflowService.registerMultiEventListener(this);
    }

    @Override
    public void handleMultipleEvents(WorkflowEventQueue queue) {
        final boolean sendNotifications = !Boolean.TRUE.equals(queue
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

        // Send notifications in background in separate thread.
        // TODO: Riina - implement correctly - save information about emails to send to repo to avoid loss of data, cl task 189285.
        // TODO: Alar - same for permissions.
        generalService.runOnBackground(new RunAsWork<Void>() {

            @Override
            public Void doWork() throws Exception {
                return WorkflowStatusEventListener.this.doWork(events, initiatingTask, sendNotifications);
            }

        }, "workflowPermissionsAndNotifications", false);
    }

    private Void doWork(final List<WorkflowEvent> events, final Task initiatingTask, final boolean sendNotifications) {
        RetryingTransactionHelper txHelper = transactionService.getRetryingTransactionHelper();
        try {
            final Map<NodeRef, Map<String, Set<String>>> permissions = getPermissions(events);
            while (!permissions.isEmpty()) {
                txHelper.doInTransaction(new RetryingTransactionCallback<Void>() {
                    @Override
                    public Void execute() throws Throwable {
                        setPermissions(permissions);
                        return null;
                    }

                }, false, true);
            }
        } catch (Exception e) {
            LOG.error("Error setting permissions to document, continuing with notification e-mails", e);
        }
        if (sendNotifications) {
            txHelper.doInTransaction(new RetryingTransactionCallback<Void>() {
                @Override
                public Void execute() throws Throwable {
                    handleNotifications(events, initiatingTask);
                    return null;
                }
            }, true, true);
        }
        return null;
    }

    private Map<NodeRef, Map<String, Set<String>>> getPermissions(final List<WorkflowEvent> events) {
        final Map<NodeRef, Map<String, Set<String>>> permissions = new HashMap<NodeRef, Map<String, Set<String>>>();
        for (WorkflowEvent event : events) {
            BaseWorkflowObject object = event.getObject();
            if (object instanceof Task) {
                Task task = (Task) object;
                String taskOwnerId = task.getOwnerId();
                if (StringUtils.isNotBlank(taskOwnerId) && event.getType().equals(WorkflowEventType.STATUS_CHANGED) && task.isStatus(Status.IN_PROGRESS)) {
                    Workflow workflow = task.getParent();
                    NodeRef docRef = workflow.getParent().getParent();
                    Set<String> requiredPrivileges = PrivilegeUtil.getRequiredPrivsForInprogressTask(task, docRef, fileService);
                    if (!requiredPrivileges.isEmpty()) {
                        Map<String, Set<String>> permissionsByDocRef = permissions.get(docRef);
                        if (permissionsByDocRef == null) {
                            permissionsByDocRef = new HashMap<String, Set<String>>();
                            permissions.put(docRef, permissionsByDocRef);
                        }
                        Set<String> permissionsByTaskOwnerId = permissionsByDocRef.get(taskOwnerId);
                        if (permissionsByTaskOwnerId == null) {
                            permissionsByTaskOwnerId = new HashSet<String>();
                            permissionsByDocRef.put(taskOwnerId, permissionsByTaskOwnerId);
                        }
                        permissionsByTaskOwnerId.addAll(requiredPrivileges);
                    }
                }
            }
        }
        return permissions;
    }

    private void setPermissions(final Map<NodeRef, Map<String, Set<String>>> permissions) {
        int count = 0;
        for (Iterator<Entry<NodeRef, Map<String, Set<String>>>> i = permissions.entrySet().iterator(); i.hasNext();) {
            Entry<NodeRef, Map<String, Set<String>>> entry = i.next();
            NodeRef docRef = entry.getKey();
            Map<String, Set<String>> permissionsByDocRef = entry.getValue();
            for (Iterator<Entry<String, Set<String>>> j = permissionsByDocRef.entrySet().iterator(); j.hasNext();) {
                Entry<String, Set<String>> entry2 = j.next();
                Set<String> permissionsByTaskOwnerId = entry2.getValue();
                privilegeService.setPermissions(docRef, entry2.getKey(), permissionsByTaskOwnerId);
                j.remove();
                count += permissionsByTaskOwnerId.size();
                if (count >= NR_OF_PERMISSIONS_IN_TRANSACTION) {
                    return;
                }
            }
            if (permissionsByDocRef.isEmpty()) {
                i.remove();
            }
        }
        return;
    }

    private void handleNotifications(final List<WorkflowEvent> events, final Task initiatingTask) {
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
                boolean cancelledManually = event.getExtras() != null && event.getExtras().contains(WorkflowQueueParameter.WORKFLOW_CANCELLED_MANUALLY);
                notificationService.notifyTaskUnfinishedEvent(task, cancelledManually);
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

    public void setPrivilegeService(PrivilegeService privilegeService) {
        this.privilegeService = privilegeService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // END: getters/setters

}
