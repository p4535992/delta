package ee.webmedia.alfresco.notification.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.event.ActionEvent;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.service.AddressbookService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.sendout.model.SendInfo;
import ee.webmedia.alfresco.email.service.EmailException;
import ee.webmedia.alfresco.email.service.EmailService;
import ee.webmedia.alfresco.notification.exception.EmailAttachmentSizeLimitException;
import ee.webmedia.alfresco.notification.model.GeneralNotification;
import ee.webmedia.alfresco.notification.model.Notification;
import ee.webmedia.alfresco.notification.model.NotificationModel;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.substitute.model.Substitute;
import ee.webmedia.alfresco.substitute.service.SubstituteService;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventType;

/**
 * @author Kaarel Jõgeva
 */
public class NotificationServiceImpl implements NotificationService {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(NotificationServiceImpl.class);

    private static final String NOTIFICATION_PREFIX = "notification_";
    private static final String TEMPLATE_SUFFIX = "_template";
    private static final String SUBJECT_SUFFIX = "_subject";
    private EmailService emailService;
    private NodeService nodeService;
    private GeneralService generalService;
    private ParametersService parametersService;
    private UserService userService;
    private DocumentTemplateService templateService;
    private FileService fileService;
    private DocumentSearchService documentSearchService;
    private SubstituteService substituteService;
    private AddressbookService addressbookService;
    private int updateCount = 0;

    private static BeanPropertyMapper<GeneralNotification> generalNotificationBeanPropertyMapper;
    static {
        generalNotificationBeanPropertyMapper = BeanPropertyMapper.newInstance(GeneralNotification.class);
    }

    @Override
    public List<GeneralNotification> getActiveGeneralNotifications() {
        List<GeneralNotification> notifications = new ArrayList<GeneralNotification>();
        for (GeneralNotification notification : getGeneralNotifications()) {
            if (notification.isActive()) {
                notifications.add(notification);
            }
        }
        return notifications;
    }

    @Override
    public List<GeneralNotification> getGeneralNotifications() {
        List<ChildAssociationRef> notificationNodeRefs = nodeService.getChildAssocs(getGeneralNotificationsRoot());
        List<GeneralNotification> notifications = new ArrayList<GeneralNotification>(notificationNodeRefs.size());
        for (ChildAssociationRef notificationRef : notificationNodeRefs) {
            GeneralNotification notification = generalNotificationBeanPropertyMapper.toObject(nodeService.getProperties(notificationRef.getChildRef()));
            notification.setNodeRef(notificationRef.getChildRef());
            notifications.add(notification);
        }

        return notifications;
    }

    @Override
    public GeneralNotification getGeneralNotificationByNodeRef(NodeRef nodeRef) {
        GeneralNotification notification = generalNotificationBeanPropertyMapper.toObject(nodeService.getProperties(nodeRef));
        notification.setNodeRef(nodeRef);
        return notification;
    }

    @Override
    public void updateGeneralNotification(Node notification) {
        NodeRef nodeRef = notification.getNodeRef();
        if (!nodeService.exists(nodeRef)) {
            ChildAssociationRef assocRef = nodeService.createNode(getGeneralNotificationsRoot(), NotificationModel.Assoc.GENERAL_NOTIFICATION,
                    NotificationModel.Assoc.GENERAL_NOTIFICATION, NotificationModel.Types.GENERAL_NOTIFICATION);
            nodeRef = assocRef.getChildRef();
        }
        generalService.setPropertiesIgnoringSystem(nodeRef, notification.getProperties());
        setUpdateCount(getUpdateCount() + 1);
    }

    @Override
    public Node generalNotificationAsNode(GeneralNotification notification) {
        if (nodeService.exists(notification.getNodeRef())) {
            return generalService.fetchNode(notification.getNodeRef());
        }

        TransientNode transientNode = new TransientNode(NotificationModel.Types.GENERAL_NOTIFICATION, NotificationModel.Assoc.GENERAL_NOTIFICATION.toString(),
                generalNotificationBeanPropertyMapper.toProperties(notification));
        transientNode.getProperties();
        return transientNode;
    }

    private NodeRef getGeneralNotificationsRoot() {
        return generalService.getNodeRef(NotificationModel.Repo.NOTIFICATIONS_SPACE);
    }

    @Override
    public void notifyExternalReviewError(Task task) {
        Notification notification = new Notification();
        setupNotification(notification, NotificationModel.NotificationType.EXTERNAL_REVIEW_WORKFLOW_RECIEVING_ERROR);
        addAdminGroupRecipients(notification);
        LinkedHashMap<String, NodeRef> templateDataNodeRefs = setupTemplateData(task);
        try {
            sendNotification(notification, null, templateDataNodeRefs);
        } catch (EmailException e) {
            log.error("Failed to send email notification " + notification, e);
        }
    }

    @Override
    public void notifyExternalReviewError(String notificationContent) {
        Notification notification = new Notification();
        setupNotification(notification, NotificationModel.NotificationType.EXTERNAL_REVIEW_WORKFLOW_RECIEVING_ERROR);
        addAdminGroupRecipients(notification);
        try {
            sendFilesAndContent(notification, null, notificationContent);
        } catch (EmailException e) {
            log.error("Failed to send email notification " + notification, e);
        }
    }

    private void addAdminGroupRecipients(Notification notification) {
        Set<String> adminAuthorities = userService.getUserNamesInGroup(UserService.ADMINISTRATORS_GROUP);
        for (String userName : adminAuthorities) {
            String userFullName = userService.getUserFullName(userName);
            if (userFullName == null) {
                // User does not exist
                continue;
            }
            notification.addRecipient(userFullName, userService.getUserEmail(userName));
        }
    }

    @Override
    public void notifyCompoundWorkflowEvent(CompoundWorkflow compoundWorkflowEvent) {
        // the future is bright!
    }

    @Override
    public void notifySubstitutionEvent(Substitute substitute) {
        Notification notification = setupNotification(new Notification(), NotificationModel.NotificationType.SUBSTITUTION);
        notification.setFailOnError(true); // XXX does not do anything at the moment
        String substituteId = substitute.getSubstituteId();
        if (log.isDebugEnabled()) {
            log.debug("Sending notification email to substitute: " + substituteId);
        }

        NodeRef personRef = userService.getPerson(substituteId);
        if (personRef == null) {
            if (log.isDebugEnabled()) {
                log.debug("Person '" + substituteId + "' not found, no notification email is sent");
            }
            return;
        }
        String toEmailAddress = (String) nodeService.getProperty(personRef, ContentModel.PROP_EMAIL);
        if (StringUtils.isEmpty(toEmailAddress)) {
            if (log.isDebugEnabled()) {
                log.debug("Person '" + substituteId + "' doesn't have email address defined, no notification email is sent");
            }
            return;
        }
        notification.addRecipient(substitute.getSubstituteName(), toEmailAddress);

        LinkedHashMap<String, NodeRef> nodeRefs = new LinkedHashMap<String, NodeRef>();
        nodeRefs.put(null, substitute.getNodeRef());

        try {
            sendNotification(notification, null, nodeRefs);
        } catch (EmailException e) {
            log.error("Substitution event notification e-mail sending failed, ignoring and continuing", e);
        }

        if (log.isDebugEnabled()) {
            log.debug("Notification email sent to person '" + substituteId + "' with email address '" + toEmailAddress + "'");
        }
    }

    @Override
    public void notifyWorkflowEvent(Workflow workflow, WorkflowEventType eventType) {
        List<Notification> notifications = processNotification(workflow, eventType);
        if (notifications == null) { // no need for sending out emails
            return;
        }

        NodeRef docRef = workflow.getParent().getParent();

        LinkedHashMap<String, NodeRef> templateDataNodeRefs = new LinkedHashMap<String, NodeRef>();
        templateDataNodeRefs.put(null, docRef);
        templateDataNodeRefs.put("workflow", workflow.getNodeRef());
        templateDataNodeRefs.put("compoundWorkflow", workflow.getParent().getNodeRef());

        for (Notification notification : notifications) {
            try {
                sendNotification(notification, docRef, templateDataNodeRefs);
            } catch (EmailException e) {
                log.error("Workflow event notification e-mail sending failed, ignoring and continuing", e);
            }
        }
    }

    @Override
    public void notifyTaskEvent(Task task) {
        Notification substitutionNotification = null;
        if (task.isStatus(Status.IN_PROGRESS)) {
            substitutionNotification = processSubstituteNewTask(task, new Notification());
        }
        Notification notification = processNotification(task, new Notification());
        NodeRef docRef = task.getParent().getParent().getParent();
        try {
            if (substitutionNotification != null) {
                sendNotification(substitutionNotification, docRef, setupTemplateData(task));
            }
            if (notification != null) {
                sendNotification(notification, docRef, setupTemplateData(task));
            }
        } catch (EmailException e) {
            log.error("Workflow task event notification e-mail sending failed, ignoring and continuing", e);
        }
    }

    @Override
    public void notifyTaskUnfinishedEvent(Task task) {
        Notification notification = processTaskUnfinishedNotification(task, new Notification());
        NodeRef docRef = task.getParent().getParent().getParent();
        if (notification != null) {
            try {
                sendNotification(notification, docRef, setupTemplateData(task));
            } catch (EmailException e) {
                log.error("Workflow task event notification e-mail sending failed, ignoring and continuing", e);
            }
        }
    }

    private Notification processTaskUnfinishedNotification(Task task, Notification notification) {
        if (StringUtils.isNotEmpty(task.getOwnerId())) {
            notification = setupNotification(notification, NotificationModel.NotificationType.TASK_ASSIGNMENT_TASK_COMPLETED_BY_RESPONSIBLE);
            notification.addRecipient(task.getOwnerName(), task.getOwnerEmail());
            return notification;
        }
        return null;
    }

    @Override
    public boolean processOutgoingInformationTask(Task task) {
        Notification notification = new Notification();
        Notification substituteNotification = new Notification();
        notification.setFailOnError(true);
        substituteNotification.setFailOnError(true);
        notification = processNewTask(task, notification);
        substituteNotification = processSubstituteNewTask(task, substituteNotification);
        NodeRef docRef = task.getParent().getParent().getParent();
        try {
            sendNotification(substituteNotification, docRef, setupTemplateData(task));
            sendNotification(notification, docRef, setupTemplateData(task));
        } catch (Exception e) {
            log.error("Workflow task event notification e-mail sending failed, ignoring and continuing", e);
            return false;
        }
        return true;
    }

    @Override
    public String generateTemplateContent(QName notificationType, Task task) {
        String templateName = getTemplate(notificationType, -1);
        NodeRef systemTemplateByName = templateService.getSystemTemplateByName(templateName);
        if (systemTemplateByName == null) {
            if (log.isDebugEnabled()) {
                log.debug("Workflow notification email template '" + templateName + "' not found, no notification email is sent");
            }
            return null; // if the admins are lazy and we don't have a template, we don't have to send out notifications... :)
        }
        getTemplate(notificationType, -1);
        LinkedHashMap<String, NodeRef> templateDataNodeRefs = setupTemplateData(task);
        return templateService.getProcessedEmailTemplate(templateDataNodeRefs, systemTemplateByName);
    }

    private void sendNotification(Notification notification, NodeRef docRef, LinkedHashMap<String, NodeRef> templateDataNodeRefs) throws EmailException {
        NodeRef systemTemplateByName = templateService.getSystemTemplateByName(notification.getTemplateName());
        if (systemTemplateByName == null) {
            if (log.isDebugEnabled()) {
                log.debug("Workflow notification email template '" + notification.getTemplateName() + "' not found, no notification email is sent");
            }
            return; // if the admins are lazy and we don't have a template, we don't have to send out notifications... :)
        }

        String content = templateService.getProcessedEmailTemplate(templateDataNodeRefs, systemTemplateByName);

        sendFilesAndContent(notification, docRef, content);
    }

    public void sendFilesAndContent(Notification notification, NodeRef docRef, String content) throws EmailException {
        if (notification.isAttachFiles()) {
            long maxSize = parametersService.getLongParameter(Parameters.MAX_ATTACHED_FILE_SIZE) * 1024 * 1024; // Parameter is MB
            long zipSize = 0;

            List<File> files = fileService.getAllActiveFiles(docRef);
            List<String> fileRefs = new ArrayList<String>(files.size());

            for (File file : files) {
                zipSize += file.getSize();
                if (zipSize > maxSize) {
                    String msg = "Files archive size exceeds limit configured with parameter!";
                    log.debug(msg);
                    throw new EmailAttachmentSizeLimitException(msg);
                }
                fileRefs.add(file.getNodeRef().toString());
            }

            String zipName = I18NUtil.getMessage("notification_zip_filename") + ".zip";
            sendEmail(notification, content, docRef, fileRefs, true, zipName);
        } else {
            sendEmail(notification, content, docRef);
        }
    }

    private List<Notification> processNotification(Workflow workflow, WorkflowEventType eventType) {

        if (workflow.isStatus(Status.FINISHED)) {
            return processFinishedWorkflow(workflow);
        } else if (workflow.isStatus(Status.IN_PROGRESS) && eventType.equals(WorkflowEventType.WORKFLOW_STARTED_AUTOMATICALLY)) {
            Notification newWorkflowNotification = processNewWorkflow(workflow, true);
            if (newWorkflowNotification != null) {
                return new ArrayList<Notification>(Arrays.asList(newWorkflowNotification));
            }
        }

        return null;
    }

    private Notification processNewWorkflow(Workflow workflow, boolean automaticallyStarted) {
        final CompoundWorkflow compoundWorkflow = workflow.getParent();
        Notification notification = null;
        if (automaticallyStarted) {
            if (isSubscribed(compoundWorkflow.getOwnerId(), NotificationModel.NotificationType.WORKFLOW_NEW_WORKFLOW_STARTED)) {
                notification = setupNotification(new Notification(), NotificationModel.NotificationType.WORKFLOW_NEW_WORKFLOW_STARTED);
                notification.addRecipient(compoundWorkflow.getOwnerName(), userService.getUserEmail(compoundWorkflow.getOwnerId()));
            }
        }
        return notification;
    }

    private List<Notification> processFinishedWorkflow(Workflow workflow) {
        final CompoundWorkflow compoundWorkflow = workflow.getParent();

        List<Notification> notifications = new ArrayList<Notification>();
        if (isSubscribed(compoundWorkflow.getOwnerId(), NotificationModel.NotificationType.WORKFLOW_WORKFLOW_COMPLETED)) {
            Notification notification = setupNotification(new Notification(), NotificationModel.NotificationType.WORKFLOW_WORKFLOW_COMPLETED);
            notification.addRecipient(compoundWorkflow.getOwnerName(), userService.getUserEmail(compoundWorkflow.getOwnerId()));
            notifications.add(notification);
        }
        if (workflow.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW)) {
            String ownerId = compoundWorkflow.getOwnerId();
            if (isSubscribed(ownerId, NotificationModel.NotificationType.TASK_ORDER_ASSIGNMENT_WORKFLOW_COMPLETED)) {
                Notification notification = setupNotification(new Notification(), NotificationModel.NotificationType.TASK_ORDER_ASSIGNMENT_WORKFLOW_COMPLETED);
                notification.addRecipient(compoundWorkflow.getOwnerName(), userService.getUserEmail(ownerId));
                notifications.add(notification);
            }
        }
        return notifications;

    }

    private Notification processNotification(Task task, Notification notification) {

        if (task.isStatus(Status.IN_PROGRESS)) {
            notification = processNewTask(task, notification);
        } else if (task.isStatus(Status.STOPPED)) {
            notification = processStoppedTask(task, notification);
        } else if (task.isStatus(Status.FINISHED)) {
            QName taskType = task.getNode().getType();

            if (taskType.equals(WorkflowSpecificModel.Types.SIGNATURE_TASK)) {
                notification = processSignatureTask(task, notification);
            }

            else if (taskType.equals(WorkflowSpecificModel.Types.OPINION_TASK)) {
                notification = processOpinionTask(task, notification);
            }

            else if (taskType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_TASK)) {
                notification = processAssignmentTask(task, notification);
            }

            else if (taskType.equals(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK)) {
                notification = processOrderAssignmentTask(task, notification);
            }

            else if (taskType.equals(WorkflowSpecificModel.Types.REVIEW_TASK)) {
                notification = processReviewTask(task, notification);
            }

            else if (taskType.equals(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK)) {
                notification = processExternalReviewTask(task, notification);
            }

            else if (taskType.equals(WorkflowSpecificModel.Types.INFORMATION_TASK)) {
                notification = processInformationTask(task, notification);
            } else if (taskType.equals(WorkflowSpecificModel.Types.CONFIRMATION_TASK)) {
                notification = processConfirmationTask(task, notification);
            } else if (taskType.equals(WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK)) {
                notification = processDueDateExtensionTask(task, notification);
            }

        }

        return notification;
    }

    private Notification processStoppedTask(Task task, Notification notification) {
        if (StringUtils.isNotEmpty(task.getOwnerId())) {
            if (!isSubscribed(task.getOwnerId(), NotificationModel.NotificationType.TASK_CANCELLED_TASK_NOTIFICATION)) {
                return null;
            }
            notification = setupNotification(notification, NotificationModel.NotificationType.TASK_CANCELLED_TASK_NOTIFICATION);
        } else {
            if (StringUtils.isEmpty(task.getInstitutionName())) {
                notification.setTemplateName(getTemplate(NotificationModel.NotificationType.TASK_CANCELLED_TASK_NOTIFICATION, 1));
                notification.setSubject(getSubject(NotificationModel.NotificationType.TASK_CANCELLED_TASK_NOTIFICATION));
                notification.setSenderEmail(parametersService.getStringParameter(Parameters.DOC_SENDER_EMAIL));
            }
        }

        notification.addRecipient(task.getOwnerName(), task.getOwnerEmail());
        return notification;
    }

    private Notification processNewTask(Task task, Notification notification) {
        if (StringUtils.isNotEmpty(task.getOwnerId())) {
            // Send to system user
            if (!isSubscribed(task.getOwnerId(), NotificationModel.NotificationType.TASK_NEW_TASK_NOTIFICATION)) {
                return null;
            }
            notification = setupNotification(notification, NotificationModel.NotificationType.TASK_NEW_TASK_NOTIFICATION);
            notification.addRecipient(task.getOwnerName(), task.getOwnerEmail());
            return notification;
        }

        if (StringUtils.isEmpty(task.getInstitutionName())) {
            // Send to third party
            notification = setupNotification(notification, NotificationModel.NotificationType.TASK_NEW_TASK_NOTIFICATION, 1);
            notification.setSenderEmail(parametersService.getStringParameter(Parameters.DOC_SENDER_EMAIL));
            notification.setAttachFiles(true);
            notification.addRecipient(task.getOwnerName(), task.getOwnerEmail());
            return notification;
        }
        return null;

    }

    private Notification processSubstituteNewTask(Task task, Notification notification) {
        if (StringUtils.isEmpty(task.getOwnerId())) {
            return null;
        }
        if (task.getDueDate() == null) {
            if (WorkflowSpecificModel.Types.INFORMATION_TASK.equals(task.getNode().getType())) {
                if (log.isDebugEnabled()) {
                    log.debug("Not sending new task notification to substitutes, because informationTask has no dueDate");
                }
            } else {
                log.error("Duedate is null for task: " + task);
            }
            return null;
        }
        Node taskOwnerUser = userService.getUser(task.getOwnerId());
        if (taskOwnerUser == null) {
            if (log.isDebugEnabled()) {
                log.debug("Not sending new task notification to substitutes, because task owner user does not exist");
            }
            return null;
        }
        List<Substitute> substitutes = substituteService.getSubstitutes(taskOwnerUser.getNodeRef());
        if (log.isDebugEnabled()) {
            log.debug("Found " + substitutes.size() + " substitutes for new task notification:\n  task=" + task + "\n  substitutes=" + substitutes);
        }
        if (substitutes.isEmpty()) {
            return null;
        }
        int daysForSubstitutionTasksCalc = parametersService.getLongParameter(Parameters.DAYS_FOR_SUBSTITUTION_TASKS_CALC).intValue();
        Calendar calendar = Calendar.getInstance();
        Date now = new Date();
        for (Substitute sub : substitutes) {
            calendar.setTime(sub.getSubstitutionEndDate());
            calendar.add(Calendar.DATE, daysForSubstitutionTasksCalc);
            Date substitutionStartDate = sub.getSubstitutionStartDate();
            Date substitutionEndDate = sub.getSubstitutionEndDate();
            boolean result = false;
            if (isSubstituting(substitutionStartDate, substitutionEndDate, now) && substitutionStartDate.before(task.getDueDate())
                                && calendar.getTime().after(task.getDueDate())) {
                notification.addRecipient(sub.getSubstituteName(), userService.getUserEmail(sub.getSubstituteId()));
                result = true;
            }
            if (log.isDebugEnabled()) {
                log.debug("Substitute=" + sub + ", daysForSubstitutionTasksCalc=" + daysForSubstitutionTasksCalc + ", calculated time = " + calendar.getTime() + ", result = "
                        + (result ? "" : "NOT ") + "added substitute to notification recipients");
            }
        }
        if (notification.getToNames() != null && notification.getToNames().size() > 0) {
            notification = setupNotification(notification, NotificationModel.NotificationType.TASK_NEW_TASK_NOTIFICATION, 2);
            if (log.isDebugEnabled()) {
                log.debug("Successfully prepared new task notification to substitutes: " + notification);
            }
            return notification;
        }
        if (log.isDebugEnabled()) {
            log.debug("Not sending new task notification to substitutes, because no suitable substitutes found");
        }
        return null;
    }

    private boolean isSubstituting(Date substitutionStartDate, Date substitutionEndDate, Date now) {
        return (substitutionStartDate == null || DateUtils.isSameDay(substitutionStartDate, now) || substitutionStartDate.before(now))
                && (substitutionEndDate == null || DateUtils.isSameDay(substitutionEndDate, now) || substitutionEndDate.after(now));
    }

    /**
     * // informationTask -> informationTaskCompleted -> compound workflow ownerId
     */
    private Notification processInformationTask(Task task, Notification notification) {
        CompoundWorkflow compoundWorkflow = task.getParent().getParent();

        final String ownerId = compoundWorkflow.getOwnerId();
        if (StringUtils.isNotBlank(ownerId)) {
            if (!isSubscribed(ownerId, NotificationModel.NotificationType.TASK_INFORMATION_TASK_COMPLETED)) {
                return null;
            }
            notification.addRecipient(compoundWorkflow.getOwnerName(), userService.getUserEmail(ownerId));
            notification = setupNotification(notification, NotificationModel.NotificationType.TASK_INFORMATION_TASK_COMPLETED);
            return notification;
        }

        return null;
    }

    /**
     * //if : reviewTask -> reviewTaskCompleted -> compound workflow ownerId
     * //else : reviewTask -> compound workflow ownerId
     * //if : reviewTask -> reviewTaskCompletedNotAccepted -> workflow block[parallelTasks == true] -> every task ownerId
     * //else : reviewTask -> reviewTaskCompletedNotAccepted -> workflow block[parallelTasks == false] -> every task[status == finished] ownerId
     * //if : reviewTask -> reviewTaskCompletedWithRemarks -> workflow block[parallelTasks == true] -> every task ownerId
     * //else : reviewTask -> reviewTaskCompletedWithRemarks -> workflow block[parallelTasks == false] -> every task[status == finished] ownerId
     */
    private Notification processReviewTask(Task task, Notification notification) {
        final Workflow workflow = task.getParent();
        final CompoundWorkflow compoundWorkflow = workflow.getParent();
        if (WorkflowSpecificModel.ReviewTaskOutcome.CONFIRMED.equals(task.getOutcomeIndex())) {
            if (StringUtils.isEmpty(compoundWorkflow.getOwnerId())
                    || !isSubscribed(compoundWorkflow.getOwnerId(), NotificationModel.NotificationType.TASK_REVIEW_TASK_COMPLETED)) {
                return null;
            }
            notification = setupNotification(notification, NotificationModel.NotificationType.TASK_REVIEW_TASK_COMPLETED);
            notification.addRecipient(compoundWorkflow.getOwnerName(), userService.getUserEmail(compoundWorkflow.getOwnerId()));
            return notification;
        }

        else if (WorkflowSpecificModel.ReviewTaskOutcome.CONFIRMED_WITH_REMARKS.equals(task.getOutcomeIndex())) {

            if (workflow.isParallelTasks()) {
                for (Task workflowTask : workflow.getTasks()) {
                    if (StringUtils.isEmpty(workflowTask.getOwnerId())
                            || !isSubscribed(workflowTask.getOwnerId(), NotificationModel.NotificationType.TASK_REVIEW_TASK_COMPLETED_WITH_REMARKS)) {
                        continue;
                    }
                    notification.addRecipient(workflowTask.getOwnerName(), workflowTask.getOwnerEmail());
                }
                notification = setupNotification(notification, NotificationModel.NotificationType.TASK_REVIEW_TASK_COMPLETED_WITH_REMARKS);
            } else {
                for (Task workflowTask : workflow.getTasks()) {
                    if (StringUtils.isEmpty(workflowTask.getOwnerId()) || !workflowTask.isStatus(Status.FINISHED)
                            || !isSubscribed(workflowTask.getOwnerId(), NotificationModel.NotificationType.TASK_REVIEW_TASK_COMPLETED_WITH_REMARKS)) {
                        continue;
                    }
                    notification.addRecipient(workflowTask.getOwnerName(), workflowTask.getOwnerEmail());
                }
                notification = setupNotification(notification, NotificationModel.NotificationType.TASK_REVIEW_TASK_COMPLETED_WITH_REMARKS);
            }
            notification.addRecipient(compoundWorkflow.getOwnerName(), userService.getUserEmail(compoundWorkflow.getOwnerId()));
            return notification;
        }

        else if (WorkflowSpecificModel.ReviewTaskOutcome.NOT_CONFIRMED.equals(task.getOutcomeIndex())) {

            if (workflow.isParallelTasks()) {
                for (Task workflowTask : workflow.getTasks()) {
                    if (StringUtils.isEmpty(workflowTask.getOwnerId())
                            || !isSubscribed(workflowTask.getOwnerId(), NotificationModel.NotificationType.TASK_REVIEW_TASK_COMPLETED_NOT_ACCEPTED)) {
                        continue;
                    }
                    notification.addRecipient(workflowTask.getOwnerName(), workflowTask.getOwnerEmail());
                }
                notification = setupNotification(notification, NotificationModel.NotificationType.TASK_REVIEW_TASK_COMPLETED_NOT_ACCEPTED);
            } else {
                for (Task workflowTask : workflow.getTasks()) {
                    if (StringUtils.isEmpty(workflowTask.getOwnerId()) || !workflowTask.isStatus(Status.FINISHED)
                            || !isSubscribed(workflowTask.getOwnerId(), NotificationModel.NotificationType.TASK_REVIEW_TASK_COMPLETED_NOT_ACCEPTED)) {
                        continue;
                    }
                    notification.addRecipient(workflowTask.getOwnerName(), workflowTask.getOwnerEmail());
                }
                notification = setupNotification(notification, NotificationModel.NotificationType.TASK_REVIEW_TASK_COMPLETED_NOT_ACCEPTED);
            }
            notification.addRecipient(compoundWorkflow.getOwnerName(), userService.getUserEmail(compoundWorkflow.getOwnerId()));
            return notification;
        }

        return null;
    }

    private Notification processExternalReviewTask(Task task, Notification notification) {
        final Workflow workflow = task.getParent();
        final CompoundWorkflow compoundWorkflow = workflow.getParent();
        if (WorkflowSpecificModel.ExternalReviewTaskOutcome.CONFIRMED.equals(task.getOutcomeIndex())) {
            if (StringUtils.isEmpty(compoundWorkflow.getOwnerId())
                    || !isSubscribed(compoundWorkflow.getOwnerId(), NotificationModel.NotificationType.TASK_REVIEW_TASK_COMPLETED)) {
                return null;
            }
            notification = setupNotification(notification, NotificationModel.NotificationType.TASK_EXTERNAL_REVIEW_TASK_COMPLETED);
            notification.addRecipient(compoundWorkflow.getOwnerName(), userService.getUserEmail(compoundWorkflow.getOwnerId()));
            return notification;
        } else if (WorkflowSpecificModel.ExternalReviewTaskOutcome.NOT_CONFIRMED.equals(task.getOutcomeIndex())) {
            notification = setupNotification(notification, NotificationModel.NotificationType.TASK_EXTERNAL_REVIEW_TASK_COMPLETED_NOT_ACCEPTED);
            notification.addRecipient(compoundWorkflow.getOwnerName(), userService.getUserEmail(compoundWorkflow.getOwnerId()));
            return notification;
        }
        return null;
    }

    /**
     * // assignmentTask -> assignmentTaskCompletedByCoResponsible-> workflow block -> task[aspect responsible: active == true] ownerId (Kas saab mitu aktiivset
     * täitjat olla? Ei)
     */
    private Notification processAssignmentTask(Task task, Notification notification) {
        Workflow workflow = task.getParent();

        // co-responsible finished task
        if (StringUtils.isNotEmpty(task.getOwnerId()) && !task.getNode().hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE)) {
            for (Task workflowTask : workflow.getTasks()) {
                final Serializable active = nodeService.getProperty(workflowTask.getNodeRef(), WorkflowSpecificModel.Props.ACTIVE); // responsible
                // aspect
                if (active != null && Boolean.valueOf(active.toString())) {
                    if (!isSubscribed(workflowTask.getOwnerId(), NotificationModel.NotificationType.TASK_ASSIGNMENT_TASK_COMPLETED_BY_CO_RESPONSIBLE)) {
                        return null;
                    }
                    notification = setupNotification(notification, NotificationModel.NotificationType.TASK_ASSIGNMENT_TASK_COMPLETED_BY_CO_RESPONSIBLE);
                    notification.addRecipient(workflowTask.getOwnerName(), workflowTask.getOwnerEmail());
                    return notification;
                }
            }
        }
        return null;
    }

    private Notification processOrderAssignmentWorkflow(Task task, Notification notification) {
        CompoundWorkflow compoundWorkflow = task.getParent().getParent();
        String ownerId = compoundWorkflow.getOwnerId();
        if (!isSubscribed(ownerId, NotificationModel.NotificationType.TASK_ORDER_ASSIGNMENT_WORKFLOW_COMPLETED)) {
            return null;
        }
        notification = setupNotification(notification, NotificationModel.NotificationType.TASK_ORDER_ASSIGNMENT_WORKFLOW_COMPLETED);
        notification.addRecipient(compoundWorkflow.getOwnerName(), userService.getUserEmail(ownerId));
        return notification;
    }

    private Notification processOrderAssignmentTask(Task task, Notification notification) {
        if (!Boolean.TRUE.equals(task.getProp(WorkflowSpecificModel.Props.SEND_ORDER_ASSIGNMENT_COMPLETED_EMAIL))) {
            return null;
        }
        notification = setupNotification(notification, NotificationModel.NotificationType.TASK_ORDER_ASSIGNMENT_TASK_COMPLETED);
        notification.addRecipient(task.getCreatorName(), task.getCreatorEmail());
        return notification;
    }

    private Notification processConfirmationTask(Task task, Notification notification) {
        CompoundWorkflow compoundWorkflow = task.getParent().getParent();
        String ownerId = compoundWorkflow.getOwnerId();
        if (WorkflowSpecificModel.ConfirmationTaskOutcome.ACCEPTED.equals(task.getOutcomeIndex())) {
            if (!isSubscribed(ownerId, NotificationModel.NotificationType.TASK_CONFIRMATION_TASK_COMPLETED)) {
                return null;
            }
            notification = setupNotification(notification, NotificationModel.NotificationType.TASK_CONFIRMATION_TASK_COMPLETED);
            notification.addRecipient(compoundWorkflow.getOwnerName(), userService.getUserEmail(ownerId));
            return notification;
        } else if (WorkflowSpecificModel.ConfirmationTaskOutcome.NOT_ACCEPTED.equals(task.getOutcomeIndex())) {
            if (!isSubscribed(ownerId, NotificationModel.NotificationType.TASK_CONFIRMATION_TASK_COMPLETED_NOT_ACCEPTED)) {
                return null;
            }
            notification = setupNotification(notification, NotificationModel.NotificationType.TASK_CONFIRMATION_TASK_COMPLETED_NOT_ACCEPTED);
            notification.addRecipient(compoundWorkflow.getOwnerName(), userService.getUserEmail(ownerId));
            return notification;
        }
        return null;
    }

    private Notification processDueDateExtensionTask(Task task, Notification notification) {
        String creatorId = task.getCreatorId();
        if (WorkflowSpecificModel.DueDateExtensionTaskOutcome.ACCEPTED.equals(task.getOutcomeIndex())) {
            if (!isSubscribed(creatorId, NotificationModel.NotificationType.TASK_DUE_DATE_EXTENSION_TASK_COMPLETED)) {
                return null;
            }
            notification = setupNotification(notification, NotificationModel.NotificationType.TASK_DUE_DATE_EXTENSION_TASK_COMPLETED);
            notification.addRecipient(task.getCreatorName(), task.getCreatorEmail());
            return notification;
        } else if (WorkflowSpecificModel.ConfirmationTaskOutcome.NOT_ACCEPTED.equals(task.getOutcomeIndex())) {
            if (!isSubscribed(creatorId, NotificationModel.NotificationType.TASK_DUE_DATE_EXTENSION_TASK_COMPLETED_NOT_ACCEPTED)) {
                return null;
            }
            notification = setupNotification(notification, NotificationModel.NotificationType.TASK_DUE_DATE_EXTENSION_TASK_COMPLETED_NOT_ACCEPTED);
            notification.addRecipient(task.getCreatorName(), task.getCreatorEmail());
            return notification;
        }
        return null;
    }

    private Notification processOpinionTask(Task task, Notification notification) {
        final CompoundWorkflow compoundWorkflow = task.getParent().getParent();
        if (StringUtils.isNotEmpty(task.getOwnerId())) {
            if (!isSubscribed(compoundWorkflow.getOwnerId(), NotificationModel.NotificationType.TASK_OPINION_TASK_COMPLETED)) {
                return null;
            }

            notification = setupNotification(notification, NotificationModel.NotificationType.TASK_OPINION_TASK_COMPLETED);
            notification.addRecipient(compoundWorkflow.getOwnerName(), userService.getUserEmail(compoundWorkflow.getOwnerId()));

            return notification;
        }

        return null;
    }

    private Notification processSignatureTask(Task task, Notification notification) {
        // if : signatureTask -> signatureTaskCompleted -> compound workflow ownerId
        // else : signatureTask -> compound workflow ownerId
        final CompoundWorkflow compoundWorkflow = task.getParent().getParent();

        if (WorkflowSpecificModel.SignatureTaskOutcome.SIGNED.equals(task.getOutcomeIndex()) && StringUtils.isNotEmpty(task.getOwnerId())) {
            if (!isSubscribed(compoundWorkflow.getOwnerId(), NotificationModel.NotificationType.TASK_SIGNATURE_TASK_COMPLETED)) {
                return null;
            }
            notification = setupNotification(notification, NotificationModel.NotificationType.TASK_SIGNATURE_TASK_COMPLETED);
            notification.addRecipient(compoundWorkflow.getOwnerName(), userService.getUserEmail(compoundWorkflow.getOwnerId()));

            return notification;
        }

        else if (WorkflowSpecificModel.SignatureTaskOutcome.NOT_SIGNED.equals(task.getOutcomeIndex())) {
            notification = setupNotification(notification, NotificationModel.NotificationType.TASK_SIGNATURE_TASK_COMPLETED, 1);
            notification.addRecipient(compoundWorkflow.getOwnerName(), userService.getUserEmail(compoundWorkflow.getOwnerId()));

            return notification;
        }

        return null;
    }

    /**
     * @param notification
     */
    private Notification setupNotification(Notification notification, QName notificationType, int version) {
        return setupNotification(notification, notificationType, version, Parameters.TASK_SENDER_EMAIL);
    }

    public Notification setupNotification(Notification notification, QName notificationType, int version, Parameters senderEmailParameter) {
        notification.setSubject(getSubject(notificationType, version));
        notification.setTemplateName(getTemplate(notificationType, version));
        notification.setSenderEmail(parametersService.getStringParameter(senderEmailParameter));
        return notification;
    }

    private Notification setupNotification(Notification notification, QName notificationType) {
        return setupNotification(notification, notificationType, -1);
    }

    @Override
    public void addMissingConfigurations(Node userPreferencesNode) {

        final NodeRef nodeRef = userPreferencesNode.getNodeRef();
        Map<QName, Serializable> props = nodeService.getProperties(nodeRef);
        List<QName> notificationProps = new ArrayList<QName>();
        notificationProps.add(NotificationModel.NotificationType.TASK_ASSIGNMENT_TASK_COMPLETED_BY_CO_RESPONSIBLE);
        notificationProps.add(NotificationModel.NotificationType.TASK_CANCELLED_TASK_NOTIFICATION);
        notificationProps.add(NotificationModel.NotificationType.TASK_EXTERNAL_REVIEW_TASK_COMPLETED);
        notificationProps.add(NotificationModel.NotificationType.TASK_EXTERNAL_REVIEW_TASK_COMPLETED_NOT_ACCEPTED);
        notificationProps.add(NotificationModel.NotificationType.TASK_INFORMATION_TASK_COMPLETED);
        notificationProps.add(NotificationModel.NotificationType.TASK_NEW_TASK_NOTIFICATION);
        notificationProps.add(NotificationModel.NotificationType.TASK_OPINION_TASK_COMPLETED);
        notificationProps.add(NotificationModel.NotificationType.TASK_REVIEW_TASK_COMPLETED);
        notificationProps.add(NotificationModel.NotificationType.TASK_REVIEW_TASK_COMPLETED_NOT_ACCEPTED);
        notificationProps.add(NotificationModel.NotificationType.TASK_REVIEW_TASK_COMPLETED_WITH_REMARKS);
        notificationProps.add(NotificationModel.NotificationType.TASK_SIGNATURE_TASK_COMPLETED);
        notificationProps.add(NotificationModel.NotificationType.TASK_ORDER_ASSIGNMENT_WORKFLOW_COMPLETED);
        notificationProps.add(NotificationModel.NotificationType.TASK_ORDER_ASSIGNMENT_TASK_COMPLETED);
        notificationProps.add(NotificationModel.NotificationType.TASK_CONFIRMATION_TASK_COMPLETED);
        notificationProps.add(NotificationModel.NotificationType.TASK_CONFIRMATION_TASK_COMPLETED_NOT_ACCEPTED);
        notificationProps.add(NotificationModel.NotificationType.TASK_DUE_DATE_EXTENSION_TASK_COMPLETED);
        notificationProps.add(NotificationModel.NotificationType.TASK_DUE_DATE_EXTENSION_TASK_COMPLETED_NOT_ACCEPTED);
        notificationProps.add(NotificationModel.NotificationType.WORKFLOW_NEW_WORKFLOW_STARTED);
        notificationProps.add(NotificationModel.NotificationType.WORKFLOW_WORKFLOW_COMPLETED);

        for (QName key : notificationProps) {
            if (!props.containsKey(key)) {
                nodeService.setProperty(nodeRef, key, Boolean.FALSE);
            }
        }
    }

    @Override
    public void saveConfigurationChanges(Node userPreferencesNode) {
        generalService.setPropertiesIgnoringSystem(userPreferencesNode.getNodeRef(), userPreferencesNode.getProperties());
    }

    @Override
    public int processTaskDueDateNotifications() {
        int taskDueDateNotifictionDays = parametersService.getLongParameter(Parameters.TASK_DUE_DATE_NOTIFICATION_DAYS).intValue();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, taskDueDateNotifictionDays);
        Date dueDate = cal.getTime();

        List<Task> tasksDueAfterDate = documentSearchService.searchTasksDueAfterDate(dueDate);
        List<Task> dueTasks = documentSearchService.searchTasksDueAfterDate(null);

        int approaching = sendTaskDueDateNotifications(tasksDueAfterDate, false);
        int exceeded = sendTaskDueDateNotifications(dueTasks, true);

        return approaching + exceeded;
    }

    /**
     * @param tasks
     */
    private int sendTaskDueDateNotifications(List<Task> tasks, boolean taskDue) {
        int sentMails = 0;
        for (Task task : tasks) {
            Notification notification = processDueDateNotification(new Notification(), taskDue);
            notification.addRecipient(task.getOwnerName(), task.getOwnerEmail());

            Workflow workflow = task.getParent();
            NodeRef compoundWorkflowRef = (nodeService.getPrimaryParent(workflow.getNodeRef())).getParentRef();
            NodeRef docRef = (nodeService.getPrimaryParent(compoundWorkflowRef)).getParentRef();

            try {
                sendNotification(notification, docRef, setupTemplateData(task));
                sentMails++;
            } catch (EmailException e) {
                log.error("Workflow task event notification e-mail sending failed, ignoring and continuing", e);
            }
        }
        return sentMails;
    }

    private Notification processDueDateNotification(Notification notification, boolean taskDue) {
        if (taskDue) {
            notification = setupNotification(notification, NotificationModel.NotificationType.TASK_DUE_DATE_EXCEEDED);
        } else {
            notification = setupNotification(notification, NotificationModel.NotificationType.TASK_DUE_DATE_APPROACHING);
        }

        return notification;
    }

    @Override
    public int processVolumeDispositionDateNotifications() {
        int volumeDispositionDateNotificationDays = parametersService.getLongParameter(Parameters.VOLUME_DISPOSITION_DATE_NOTIFICATION_DAYS).intValue();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, volumeDispositionDateNotificationDays);
        Date dispositionDate = cal.getTime();

        List<Volume> volumesDispositionedAfterDate = documentSearchService.searchVolumesDispositionedAfterDate(dispositionDate);
        if (log.isDebugEnabled()) {
            log.debug("Starting to process volume disposition date notifications:\n  volumeDispositionDateNotificationDays=" + volumeDispositionDateNotificationDays
                    + "\n  searching for volumes with disposition date between now and " + dispositionDate + "\n  found " + volumesDispositionedAfterDate + " volumes");
        }
        if (volumesDispositionedAfterDate.size() == 0) {
            return 0;
        }
        sendVolumesDispositionDateNotifications(volumesDispositionedAfterDate);

        return 0;
    }

    private int sendVolumesDispositionDateNotifications(List<Volume> volumesDispositionedAfterDate) {
        Notification notification = setupNotification(new Notification(), NotificationModel.NotificationType.VOLUME_DISPOSITION_DATE);
        notification = addDocumentManagersAsRecipients(notification);

        if (notification.getToEmails() == null || notification.getToEmails().isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Volume disposition date notification email not sent, no document managers found");
            }
            return 0; // no doc managers available
        }

        NodeRef systemTemplateByName = templateService.getSystemTemplateByName(notification.getTemplateName());
        if (systemTemplateByName == null) {
            if (log.isDebugEnabled()) {
                log.debug("Volume disposition date notification email template '" + notification.getTemplateName()
                        + "' not found, no notification email is sent");
            }
            return 0; // if the admins are lazy and we don't have a template, we don't have to send out notifications... :)
        }

        String content = templateService.getProcessedVolumeDispositionTemplate(volumesDispositionedAfterDate, systemTemplateByName);
        try {
            sendEmail(notification, content, null);
            return notification.getToEmails().size();
        } catch (EmailException e) {
            log.error("Volume disposition date notification e-mail sending failed, ignoring and continuing", e);
            return 0;
        }
    }

    @Override
    public int processAccessRestrictionEndDateNotifications() {
        int accessRestrictionEndDateNotificationDays = parametersService.getLongParameter(Parameters.ACCESS_RESTRICTION_END_DATE_NOTIFICATION_DAYS).intValue();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, accessRestrictionEndDateNotificationDays);
        Date restrictionEndDate = cal.getTime();

        List<Document> documents = documentSearchService.searchAccessRestictionEndsAfterDate(restrictionEndDate);
        if (documents == null || documents.isEmpty()) {
            return 0;
        }
        Map<String, List<Document>> documentsByUser = new HashMap<String, List<Document>>();

        for (Document document : documents) {
            if (documentsByUser.containsKey(document.getOwnerId())) {
                (documentsByUser.get(document.getOwnerId())).add(document);
            } else {
                ArrayList<Document> documentList = new ArrayList<Document>();
                documentList.add(document);
                documentsByUser.put(document.getOwnerId(), documentList);
            }
        }

        return sendAccessRestrictionEndDateNotifications(documents, documentsByUser);
    }

    private int sendAccessRestrictionEndDateNotifications(List<Document> documents, Map<String, List<Document>> documentsByUser) {

        Notification notification = setupNotification(new Notification(), NotificationModel.NotificationType.ACCESS_RESTRICTION_END_DATE);

        NodeRef systemTemplateByName = templateService.getSystemTemplateByName(notification.getTemplateName());
        if (systemTemplateByName == null) {
            if (log.isDebugEnabled()) {
                log.debug("Access restriction end date notification email template '" + notification.getTemplateName()
                        + "' not found, no notification email is sent");
            }
            return 0; // if the admins are lazy and we don't have a template, we don't have to send out notifications... :)
        }

        DocumentAccessRestrictionEndDateComparator endDateComparator = new DocumentAccessRestrictionEndDateComparator();
        for (Entry<String, List<Document>> entry : documentsByUser.entrySet()) {
            String userName = entry.getKey();
            String userFullName = userService.getUserFullName(userName);
            if (userFullName == null) {
                // User does not exist
                continue;
            }
            String userEmail = userService.getUserEmail(userName);
            notification.addRecipient(userFullName, userEmail);
            List<Document> userDocuments = entry.getValue();
            Collections.sort(userDocuments, endDateComparator);
            String content = templateService.getProcessedAccessRestrictionEndDateTemplate(userDocuments, systemTemplateByName);

            try {
                sendEmail(notification, content, null);
            } catch (EmailException e) {
                log.error("Access restriction due date notification e-mail sending to " + userFullName + " (" + userName + ") <"
                        + userEmail + "> failed, ignoring and continuing", e);
            }
            notification.clearRecipients();
        }

        notification.clearRecipients();
        notification = setupNotification(notification, NotificationModel.NotificationType.ACCESS_RESTRICTION_END_DATE, 1);

        systemTemplateByName = templateService.getSystemTemplateByName(notification.getTemplateName());
        if (systemTemplateByName == null) {
            if (log.isDebugEnabled()) {
                log.debug("Access restriction end date notification email template '" + notification.getTemplateName()
                        + "' not found, no notification email is sent");
            }
            return 0; // if the admins are lazy and we don't have a template, we don't have to send out notifications... :)
        }

        notification = addDocumentManagersAsRecipients(notification);
        DocumentRegNrComparator regNrComparator = new DocumentRegNrComparator();
        Collections.sort(documents, regNrComparator);
        String content = templateService.getProcessedAccessRestrictionEndDateTemplate(documents, systemTemplateByName);

        try {
            sendEmail(notification, content, null);
        } catch (EmailException e) {
            log.error("Access restriction due date notification e-mail sending to document managers failed, ignoring and continuing", e);
        }

        return 0;
    }

    @Override
    public void processAccessRestrictionChangedNotification(DocumentDynamic document, List<SendInfo> sendInfos) {
        List<String> recipientEmails = new ArrayList<String>();
        for (SendInfo sendInfo : sendInfos) {
            Map<String, Object> properties = sendInfo.getNode().getProperties();
            String recipientRegNr = (String) properties.get(DocumentCommonModel.Props.SEND_INFO_RECIPIENT_REG_NR);
            String recipientEmail = null;
            if (StringUtils.isNotBlank(recipientRegNr)) {
                List<Node> contacts = addressbookService.getContactsByRegNumber(recipientRegNr);
                if (contacts != null && contacts.size() > 0) {
                    recipientEmail = (String) contacts.get(0).getProperties().get(AddressbookModel.Props.EMAIL);
                }
            } else {
                String recipient = sendInfo.getRecipient();
                if (recipient != null && recipient.contains("(") && recipient.contains(")")) {
                    int emailStart = recipient.lastIndexOf("(");
                    int emailEnd = recipient.lastIndexOf(")");
                    if (emailEnd - emailStart > 1) {
                        // some characters exist between parentheses
                        recipientEmail = recipient.substring(emailStart + 1, emailEnd).trim();
                    }
                }
            }
            if (StringUtils.isNotBlank(recipientEmail)) {
                recipientEmails.add(recipientEmail);
            }
        }
        if (!recipientEmails.isEmpty()) {
            Notification notification = setupNotification(new Notification(), NotificationModel.NotificationType.ACCESS_RESTRICTION_REASON_CHANGED, -1, Parameters.DOC_SENDER_EMAIL);
            notification.setToEmails(recipientEmails);
            try {
                LinkedHashMap<String, NodeRef> templateDataNodeRefs = new LinkedHashMap<String, NodeRef>();
                NodeRef docRef = document.getNodeRef();
                templateDataNodeRefs.put(null, docRef);
                sendNotification(notification, docRef, templateDataNodeRefs);
            } catch (EmailException e) {
                log.error("Failed to send email notification " + notification, e);
            }
        }
    }

    private LinkedHashMap<String, NodeRef> setupTemplateData(Task task) {
        LinkedHashMap<String, NodeRef> templateDataNodeRefs = new LinkedHashMap<String, NodeRef>();
        Workflow workflow = task.getParent();
        NodeRef compoundWorkflowRef = (nodeService.getPrimaryParent(workflow.getNodeRef())).getParentRef();
        NodeRef docRef = (nodeService.getPrimaryParent(compoundWorkflowRef)).getParentRef();
        templateDataNodeRefs.put(null, docRef);
        templateDataNodeRefs.put("task", task.getNodeRef());
        templateDataNodeRefs.put("workflow", workflow.getNodeRef());
        templateDataNodeRefs.put("compoundWorkflow", compoundWorkflowRef);

        return templateDataNodeRefs;
    }

    private Notification addDocumentManagersAsRecipients(Notification notification) {
        Set<String> documentManagers = userService.getUserNamesInGroup(userService.getDocumentManagersGroup());
        // XXX: if no documentManagers set (could it happen in live environment?) then there will be exception when sending out notifications
        for (String userName : documentManagers) {
            String userFullName = userService.getUserFullName(userName);
            String userEmail = userService.getUserEmail(userName);
            if (userEmail == null) {
                // User does not exist
                continue;
            }
            notification.addRecipient(userFullName, userEmail);
        }
        return notification;
    }

    private boolean isSubscribed(String userName, QName subscriptionType) {
        NodeRef usersPreferenceRef = userService.getUsersPreferenceNodeRef(userName);
        if (usersPreferenceRef == null) {
            return false;
        }
        Serializable property = nodeService.getProperty(usersPreferenceRef, subscriptionType);
        if (property != null && Boolean.valueOf(property.toString())) {
            return true;
        }
        return false;
    }

    private String getSubject(QName notificationType, int version) {
        String messageKey = NOTIFICATION_PREFIX + notificationType.getLocalName() + SUBJECT_SUFFIX;
        if (version > -1) {
            messageKey += "_" + version;
        }
        String message = I18NUtil.getMessage(messageKey);

        return (message != null) ? message : messageKey;
    }

    private String getSubject(QName notificationType) {
        return getSubject(notificationType, -1);
    }

    private String getTemplate(QName notificationType, int version) {
        String messageKey = NOTIFICATION_PREFIX + notificationType.getLocalName() + TEMPLATE_SUFFIX;
        if (version > -1) {
            messageKey += "_" + version;
        }
        String message = I18NUtil.getMessage(messageKey);
        return (message != null) ? message : messageKey;
    }

    private void sendEmail(Notification notification, String content, NodeRef docRef) throws EmailException {
        sendEmail(notification, content, docRef, null, false, null);
    }

    private void sendEmail(Notification notification, String content, NodeRef docRef, List<String> fileRefs, boolean zipIt, String zipName)
            throws EmailException {
        if (log.isDebugEnabled()) {
            log.debug("Sending notification e-mail\nnotification=" + notification + "\ncontent=" + WmNode.toString(content) + "\ndocRef=" + docRef
                    + "\nfileRefs=" + WmNode.toString(fileRefs) + "\nzipIt=" + zipIt + "\nzipName=" + zipName);
        }

        // Remove recipients with blank e-mail address
        // So that, if there is at least one recipient with non-blank e-mail address, e-mail sending doesn't fail
        List<String> toEmails = new ArrayList<String>(notification.getToEmails());
        List<String> toNames = null;
        if (notification.getToNames() != null) {
            toNames = new ArrayList<String>();
            toNames.addAll(notification.getToNames());
        }
        for (int i = 0; i < toEmails.size();) {
            if (StringUtils.isBlank(toEmails.get(i))) {
                toEmails.remove(i);
                if (toNames != null) {
                    toNames.remove(i);
                }
            } else {
                i++;
            }
        }

        emailService.sendEmail(toEmails, toNames, notification.getSenderEmail() //
                , notification.getSubject(), content, true, docRef, fileRefs, zipIt, zipName);
    }

    // START: setters/getters

    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setTemplateService(DocumentTemplateService templateService) {
        this.templateService = templateService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    public void setSubstituteService(SubstituteService substituteService) {
        this.substituteService = substituteService;
    }

    public void setAddressbookService(AddressbookService addressbookService) {
        this.addressbookService = addressbookService;
    }

    // END: setters/getters

    private class DocumentAccessRestrictionEndDateComparator implements Comparator<Document> {

        public DocumentAccessRestrictionEndDateComparator() { /* synthetic fix */
        }

        @Override
        public int compare(Document doc1, Document doc2) {
            if (doc1.getAccessRestrictionEndDate() != null && doc2.getAccessRestrictionEndDate() != null) {
                if (doc1.getAccessRestrictionEndDate().before(doc2.getAccessRestrictionEndDate())) {
                    return -1;
                } else if (doc1.getAccessRestrictionEndDate().after(doc2.getAccessRestrictionEndDate())) {
                    return 1;
                }
            }
            return 0;
        }

    }

    private class DocumentRegNrComparator implements Comparator<Document> {

        public DocumentRegNrComparator() { /* synthetic fix */
        }

        @Override
        public int compare(Document doc1, Document doc2) {
            if (doc1.getRegNumber() != null && doc2.getRegNumber() != null) {
                return doc1.getRegNumber().compareTo(doc2.getRegNumber());
            }

            if (doc1.getRegNumber() != null && doc2.getRegNumber() == null) {
                return -1;
            }

            if (doc1.getRegNumber() == null && doc2.getRegNumber() != null) {
                return 1;
            }

            return 0;
        }

    }

    @Override
    public void processAccessRestrictionEndDateNotifications(ActionEvent event) {
        AuthenticationUtil.runAs(new RunAsWork<Integer>() {
            @Override
            public Integer doWork() throws Exception {
                return processAccessRestrictionEndDateNotifications();
            }
        }, AuthenticationUtil.getSystemUserName());

    }

    @Override
    public void processTaskDueDateNotifications(ActionEvent event) {
        AuthenticationUtil.runAs(new RunAsWork<Integer>() {
            @Override
            public Integer doWork() throws Exception {
                return processTaskDueDateNotifications();
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    @Override
    public void processVolumeDispositionDateNotifications(ActionEvent event) {

        AuthenticationUtil.runAs(new RunAsWork<Integer>() {
            @Override
            public Integer doWork() throws Exception {
                return processVolumeDispositionDateNotifications();
            }
        }, AuthenticationUtil.getSystemUserName());

    }

    @Override
    public int getUpdateCount() {
        return updateCount;
    }

    public void setUpdateCount(int updateCount) {
        this.updateCount = updateCount;
    }

}
