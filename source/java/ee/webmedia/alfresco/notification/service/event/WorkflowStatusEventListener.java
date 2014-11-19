package ee.webmedia.alfresco.notification.service.event;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.context.FacesContext;

import ee.webmedia.alfresco.workflow.service.WorkflowConstantsBean;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.Pair;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.notification.model.NotificationCache;
import ee.webmedia.alfresco.notification.service.NotificationService;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.privilege.service.PrivilegeUtil;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageUtil;
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

    private final Set<Task> tasksToFinish = new HashSet<Task>();

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
        final List<NodeRef> groupAssignmentTasksFinishedAutomatically = queue.getParameter(WorkflowQueueParameter.TASKS_FINISHED_BY_GROUP_TASK);
        // Send notifications in background in separate thread.
        // TODO: implement correctly - save information about emails to send to repo to avoid loss of data, cl task 189285.
        // TODO: same for permissions.
        generalService.runOnBackground(new RunAsWork<Void>() {

            @Override
            public Void doWork() throws Exception {
                return WorkflowStatusEventListener.this.doWork(events, initiatingTask, sendNotifications, groupAssignmentTasksFinishedAutomatically);
            }

        }, "workflowPermissionsAndNotifications", false);

        UserService userService = BeanHelper.getUserService();
        final String userId = userService.getCurrentUserName();
        final String userName = userService.getUserFullName();
        final List<WorkflowEvent> eventsToLog = new ArrayList<>(queue.getEvents());
        generalService.runOnBackground(new RunAsWork<Void>() {
            @Override
            public Void doWork() throws Exception {
                return WorkflowStatusEventListener.this.logWorkflowEvents(eventsToLog, userId, userName);
            }
        }, "workflowLogEntries", true);
    }

    private Void logWorkflowEvents(List<WorkflowEvent> eventsToLog, String userId, String userName) {
        LogService logService = BeanHelper.getLogService();
        WorkflowConstantsBean workflowConstantsBean = BeanHelper.getWorkflowConstantsBean();
        for (WorkflowEvent event : eventsToLog) {
            BaseWorkflowObject object = event.getObject();
            if (!(object instanceof Task)) {
                continue;
            }
            Task task = (Task) object;
            if (WorkflowEventType.STATUS_CHANGED.equals(event.getType()) && Status.NEW.equals(event.getOriginalStatus()) && task.isStatus(Status.IN_PROGRESS)) {
                NodeRef taskRef = object.getNodeRef();
                logService.addLogEntry(LogEntry.create(LogObject.TASK, userId, userName, taskRef, "applog_task_assigned",
                        task.getOwnerName(), workflowConstantsBean.getTaskTypeName(task.getType())));
            }
        }
        return null;
    }

    private Void doWork(final List<WorkflowEvent> events, final Task initiatingTask, final boolean sendNotifications, final List<NodeRef> groupAssignmentTasksFinishedAutomatically) {
        RetryingTransactionHelper txHelper = transactionService.getRetryingTransactionHelper();
        try {
            final Map<NodeRef, Map<String, Set<Privilege>>> permissions = getPermissions(events);
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
            try {
                final Map<NodeRef, List<Map<QName, Serializable>>> docSendInfos = txHelper.doInTransaction(
                        new RetryingTransactionCallback<Map<NodeRef, List<Map<QName, Serializable>>>>() {
                            @Override
                            public Map<NodeRef, List<Map<QName, Serializable>>> execute() throws Throwable {
                                return processEventQueue(events, initiatingTask, groupAssignmentTasksFinishedAutomatically);
                            }
                        }, false, true);
                if (!docSendInfos.isEmpty()) {
                    txHelper.doInTransaction(new RetryingTransactionCallback<Void>() {
                        @Override
                        public Void execute() throws Throwable {
                            SendOutService sendOutService = BeanHelper.getSendOutService();
                            for (Entry<NodeRef, List<Map<QName, Serializable>>> entry : docSendInfos.entrySet()) {
                                NodeRef docRef = entry.getKey();
                                for (Map<QName, Serializable> props : entry.getValue()) {
                                    sendOutService.addSendinfo(docRef, props, false);
                                }
                                sendOutService.updateSearchableSendInfo(docRef);
                            }
                            return null;
                        }
                    }, false, true);
                }
            } catch (Exception e) {
                LOG.error("Error sending notifications or updating documents", e);
            }
        }
        if (!tasksToFinish.isEmpty()) {
            try {
                for (final Task task : tasksToFinish) {
                    task.setAction(Task.Action.FINISH);
                }
                final CompoundWorkflow compoundWorkflow = tasksToFinish.iterator().next().getParent().getParent();
                txHelper.doInTransaction(new RetryingTransactionCallback<Void>() {
                    @Override
                    public Void execute() throws Throwable {
                        workflowService.saveCompoundWorkflow(compoundWorkflow);
                        return null;
                    }
                }, false, true);
            } finally {
                tasksToFinish.clear();
            }
        }
        return null;
    }

    private Map<NodeRef, Map<String, Set<Privilege>>> getPermissions(final List<WorkflowEvent> events) {
        final Map<NodeRef, Map<String, Set<Privilege>>> permissions = new HashMap<NodeRef, Map<String, Set<Privilege>>>();
        for (WorkflowEvent event : events) {
            BaseWorkflowObject object = event.getObject();
            if (object instanceof Task) {
                Task task = (Task) object;
                CompoundWorkflow compoundWorkflow = task.getParent().getParent();
                if (!compoundWorkflow.isDocumentWorkflow()) {
                    continue;
                }
                String taskOwnerId = task.getOwnerId();
                if (StringUtils.isNotBlank(taskOwnerId) && event.getType().equals(WorkflowEventType.STATUS_CHANGED) && task.isStatus(Status.IN_PROGRESS)) {
                    Workflow workflow = task.getParent();
                    NodeRef docRef = workflow.getParent().getParent();
                    Set<Privilege> requiredPrivileges = PrivilegeUtil.getRequiredPrivsForInprogressTask(task, docRef, fileService, false);
                    if (!requiredPrivileges.isEmpty()) {
                        Map<String, Set<Privilege>> permissionsByDocRef = permissions.get(docRef);
                        if (permissionsByDocRef == null) {
                            permissionsByDocRef = new HashMap<String, Set<Privilege>>();
                            permissions.put(docRef, permissionsByDocRef);
                        }
                        Set<Privilege> permissionsByTaskOwnerId = permissionsByDocRef.get(taskOwnerId);
                        if (permissionsByTaskOwnerId == null) {
                            permissionsByTaskOwnerId = new HashSet<Privilege>();
                            permissionsByDocRef.put(taskOwnerId, permissionsByTaskOwnerId);
                        }
                        permissionsByTaskOwnerId.addAll(requiredPrivileges);
                    }
                }
            }
        }
        return permissions;
    }

    private void setPermissions(final Map<NodeRef, Map<String, Set<Privilege>>> permissions) {
        int count = 0;
        for (Iterator<Entry<NodeRef, Map<String, Set<Privilege>>>> i = permissions.entrySet().iterator(); i.hasNext();) {
            Entry<NodeRef, Map<String, Set<Privilege>>> entry = i.next();
            NodeRef docRef = entry.getKey();
            Map<String, Set<Privilege>> permissionsByDocRef = entry.getValue();
            for (Iterator<Entry<String, Set<Privilege>>> j = permissionsByDocRef.entrySet().iterator(); j.hasNext();) {
                Entry<String, Set<Privilege>> entry2 = j.next();
                Set<Privilege> permissionsByTaskOwnerId = entry2.getValue();
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

    private Map<NodeRef, List<Map<QName, Serializable>>> processEventQueue(final List<WorkflowEvent> events, final Task initiatingTask,
            List<NodeRef> groupAssignmentTasksFinishedAutomatically) {
        Map<NodeRef, List<Map<QName, Serializable>>> docSendInfos = new HashMap<NodeRef, List<Map<QName, Serializable>>>();
        NotificationCache notificationCache = new NotificationCache();
        ListIterator<WorkflowEvent> iterator1 = events.listIterator(events.size());
        Set<NodeRef> nonTasksRefs = new HashSet<>();
        while (iterator1.hasPrevious()) {
            BaseWorkflowObject object = iterator1.previous().getObject();
            if (!(object instanceof Task)) {
                nonTasksRefs.add(object.getNodeRef());
            }
        }

        Map<NodeRef, Node> props = BeanHelper.getBulkLoadNodeService().loadNodes(new ArrayList<>(nonTasksRefs), null);
        notificationCache.getCachedProps().putAll(props);
        ListIterator<WorkflowEvent> iterator = events.listIterator(events.size());
        while (iterator.hasPrevious()) {
            WorkflowEvent event = iterator.previous();
            processEvent(event, initiatingTask, groupAssignmentTasksFinishedAutomatically, docSendInfos, notificationCache);
            iterator.remove();
        }
        return docSendInfos;
    }

    private void processEvent(WorkflowEvent event, Task initiatingTask, List<NodeRef> groupAssignmentTasksFinishedAutomatically,
            Map<NodeRef, List<Map<QName, Serializable>>> docSendInfos, NotificationCache notificationCache) {
        BaseWorkflowObject object = event.getObject();
        if (object instanceof CompoundWorkflow) {
            handleCompoundWorkflowNotifications(event, notificationCache);
        } else if (object instanceof Workflow) {
            handleWorkflowNotifications(event, notificationCache);
        } else if (object instanceof Task) {
            Pair<NodeRef, List<Map<QName, Serializable>>> docRefAndSendInfoProps =
                    handleTaskNotifications(event, groupAssignmentTasksFinishedAutomatically, initiatingTask, notificationCache);
            if (docRefAndSendInfoProps != null) {
                NodeRef nodeRef = docRefAndSendInfoProps.getFirst();
                List<Map<QName, Serializable>> props = docRefAndSendInfoProps.getSecond();
                if (docSendInfos.containsKey(nodeRef)) {
                    docSendInfos.get(nodeRef).addAll(props);
                } else {
                    docSendInfos.put(nodeRef, props);
                }
            }
        }
    }

    private void refreshMenuTaskCount() {
        // Let's assume that this never gets called from a job, and there is an existing context :)
        // TODO: this is not needed for mDelta
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            MenuBean menuBean = (MenuBean) FacesHelper.getManagedBean(context, MenuBean.BEAN_NAME);
            menuBean.processTaskItems();
        }
    }

    private Pair<NodeRef, List<Map<QName, Serializable>>> handleTaskNotifications(WorkflowEvent event, List<NodeRef> groupAssignmentTasksFinishedAutomatically,
            Task orderAssignmentFinishTriggeringTask, NotificationCache notificationCache) {
        final Task task = (Task) event.getObject();
        Pair<NodeRef, List<Map<QName, Serializable>>> docRefAndSendInfoProps = null;
        if (event.getType().equals(WorkflowEventType.STATUS_CHANGED)) {
            if (!task.isStatus(Status.UNFINISHED)) {
                docRefAndSendInfoProps = BeanHelper.getDvkService().sendTaskNotificationDocument(task, notificationCache);
                boolean sentOverDvk = docRefAndSendInfoProps != null;
                if (!sentOverDvk) {
                    boolean isGroupAssignmentTaskFinishedAutomatically = groupAssignmentTasksFinishedAutomatically != null
                            && groupAssignmentTasksFinishedAutomatically.contains(task.getNodeRef());
                    docRefAndSendInfoProps = notificationService
                            .notifyTaskEvent(task, isGroupAssignmentTaskFinishedAutomatically, orderAssignmentFinishTriggeringTask, sentOverDvk, notificationCache);
                }
                if (docRefAndSendInfoProps != null && task.isType(WorkflowSpecificModel.Types.INFORMATION_TASK) && task.isStatus(Status.IN_PROGRESS)
                        && task.getOwnerId() == null) {
                    tasksToFinish.add(task);
                }
            } else {
                boolean cancelledManually = event.getExtras() != null && event.getExtras().contains(WorkflowQueueParameter.WORKFLOW_CANCELLED_MANUALLY);
                notificationService.notifyTaskUnfinishedEvent(task, cancelledManually, notificationCache);
            }
        }
        return docRefAndSendInfoProps;
    }

    private void handleWorkflowNotifications(WorkflowEvent event, NotificationCache notificationCache) {
        Workflow workflow = (Workflow) event.getObject();
        if (event.getType().equals(WorkflowEventType.STATUS_CHANGED)) {
            notificationService.notifyWorkflowEvent(workflow, event.getType(), notificationCache);
        } else if (event.getType().equals(WorkflowEventType.WORKFLOW_STARTED_AUTOMATICALLY)) {
            notificationService.notifyWorkflowEvent(workflow, event.getType(), notificationCache);
        } else if (event.getType().equals(WorkflowEventType.WORKFLOW_STOPPED_AUTOMATICALLY)) {
            notificationService.notifyCompoundWorkflowStoppedAutomatically(workflow, notificationCache);
        }

    }

    private void handleCompoundWorkflowNotifications(WorkflowEvent event, NotificationCache notificationCache) {
        CompoundWorkflow compoundWorkflow = (CompoundWorkflow) event.getObject();
        if (!WorkflowEventType.STATUS_CHANGED.equals(event.getType()) || compoundWorkflow.isDocumentWorkflow()) {
            return;
        }
        notificationService.notifyCompoundWorkflowEvent(event, notificationCache);
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
