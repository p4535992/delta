package ee.webmedia.alfresco.notification.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.event.ActionEvent;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.email.service.EmailException;
import ee.webmedia.alfresco.email.service.EmailService;
import ee.webmedia.alfresco.notification.model.GeneralNotification;
import ee.webmedia.alfresco.notification.model.Notification;
import ee.webmedia.alfresco.notification.model.NotificationModel;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.template.service.DocumentTemplateNotFoundException;
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

    private static final String ZIP_FILENAME = I18NUtil.getMessage("notification_zip_filename");
    private static final String ZIP_SIZE_TOO_LARGE = I18NUtil.getMessage("notification_zip_size_too_large");
    private static final String NOTIFICATION_PREFIX = "notification_";
    private static final String TEMPLATE_SUFFIX = "_template";
    private static final String SUBJECT_SUFFIX = "_subject";
    private EmailService emailService;
    private NodeService nodeService;
    private GeneralService generalService;
    private ParametersService parametersService;
    private UserService userService;
    private DocumentTemplateService templateService;
    private FileFolderService fileFolderService;
    private DocumentSearchService documentSearchService;
    private AuthorityService authorityService;
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
    public void notifyCompoundWorkflowEvent(CompoundWorkflow compoundWorkflowEvent) {
        // the future is bright!
    }

    @Override
    public void notifyWorkflowEvent(Workflow workflow, WorkflowEventType eventType) {
        Notification notification = processNotification(workflow, new Notification(), eventType);
        if (notification == null) { // no need for sending out emails
            return;
        }

        NodeRef docRef = workflow.getParent().getParent();

        LinkedHashMap<String, NodeRef> templateDataNodeRefs = new LinkedHashMap<String, NodeRef>();
        templateDataNodeRefs.put("default", docRef);
        templateDataNodeRefs.put("workflow", workflow.getNode().getNodeRef());
        templateDataNodeRefs.put("compoundWorkflow", workflow.getParent().getNode().getNodeRef());

        try {
            sendNotification(notification, docRef, templateDataNodeRefs);
        } catch (EmailException e) {
            log.error("Workflow event notification e-mail sending failed, ignoring and continueing", e);
        }
    }

    @Override
    public void notifyTaskEvent(Task task) {

        Notification notification = processNotification(task, new Notification());
        if (notification == null) { // no need for sending out emails
            return;
        }

        NodeRef docRef = task.getParent().getParent().getParent();
        try {
            sendNotification(notification, docRef, setupTemplateData(task));
        } catch (EmailException e) {
            log.error("Workflow task event notification e-mail sending failed, ignoring and continueing", e);
        }
    }

    @Override
    public boolean processOutgoingInformationTask(Task task) {
        Notification notification = new Notification();
        notification.setFailOnError(true);
        notification = processNewTask(task, notification);

        NodeRef docRef = task.getParent().getParent().getParent();
        try {
            sendNotification(notification, docRef, setupTemplateData(task));
        } catch (Exception e) {
            log.error("Workflow task event notification e-mail sending failed, ignoring and continueing", e);
            return false;
        }
        return true;
    }

    private void sendNotification(Notification notification, NodeRef docRef, LinkedHashMap<String, NodeRef> templateDataNodeRefs) throws EmailException {
        NodeRef systemTemplateByName = null;
        try {
            systemTemplateByName = templateService.getSystemTemplateByName(notification.getTemplateName());
        } catch (DocumentTemplateNotFoundException e) {
            return; // if the admins are lazy and we don't have a template, we don't have to send out notifications... :)
        }

        String content = templateService.getProcessedEmailTemplate(templateDataNodeRefs, systemTemplateByName);

        if (notification.isAttachFiles()) {
            long maxSize = parametersService.getLongParameter(Parameters.MAX_ATTACHED_FILE_SIZE);
            long zipSize = 0;
            List<FileInfo> files = fileFolderService.listFiles(docRef);
            List<String> fileRefs = new ArrayList<String>(files.size());

            for (FileInfo fi : files) {
                File file = new File(fi);
                zipSize += file.getSize();
                if (zipSize > maxSize) {
                    log.debug(ZIP_SIZE_TOO_LARGE);
                    throw new RuntimeException(ZIP_SIZE_TOO_LARGE);
                }
                fileRefs.add(file.getNodeRef().toString());
            }

            emailService.sendEmail(notification.getToEmails(), notification.getToNames(), notification.getSenderEmail(), notification.getSubject(), content,
                    true, docRef, fileRefs, true, ZIP_FILENAME);
        } else {
            emailService.sendEmail(notification.getToEmails(), notification.getToNames(), notification.getSenderEmail(), notification.getSubject(), content,
                    true, docRef, null, false, null);
        }
    }

    private Notification processNotification(Workflow workflow, Notification notification, WorkflowEventType eventType) {

        if (Status.FINISHED.equals(workflow.getStatus())) {
            return processFinishedWorkflow(workflow, notification);
        } else if (Status.IN_PROGRESS.equals(workflow.getStatus()) && eventType.equals(WorkflowEventType.WORKFLOW_STARTED_AUTOMATICALLY)) {
            return processNewWorkflow(workflow, notification, true);
        }

        return null;
    }

    private Notification processNewWorkflow(Workflow workflow, Notification notification, boolean automaticallyStarted) {
        final CompoundWorkflow compoundWorkflow = workflow.getParent();

        if (automaticallyStarted) {
            if (!isSubscribed(compoundWorkflow.getOwnerId(), NotificationModel.NotificationType.WORKFLOW_NEW_WORKFLOW_STARTED)) {
                return null;
            }
            notification = setupNotification(notification, NotificationModel.NotificationType.WORKFLOW_NEW_WORKFLOW_STARTED);
            notification.addRecipient(compoundWorkflow.getOwnerName(), userService.getUserEmail(compoundWorkflow.getOwnerId()));
        }
        return notification;
    }

    private Notification processFinishedWorkflow(Workflow workflow, Notification notification) {
        final CompoundWorkflow compoundWorkflow = workflow.getParent();

        if (!isSubscribed(compoundWorkflow.getOwnerId(), NotificationModel.NotificationType.WORKFLOW_WORKFLOW_COMPLETED)) {
            return null;
        }
        notification = setupNotification(notification, NotificationModel.NotificationType.WORKFLOW_WORKFLOW_COMPLETED);
        notification.addRecipient(compoundWorkflow.getOwnerName(), userService.getUserEmail(compoundWorkflow.getOwnerId()));

        return notification;
    }

    private Notification processNotification(Task task, Notification notification) {

        if (Status.IN_PROGRESS.equals(task.getStatus())) {
            notification = processNewTask(task, notification);
        } else if (Status.STOPPED.equals(task.getStatus())) {
            notification = processStoppedTask(task, notification);
        } else if (Status.FINISHED.equals(task.getStatus())) {
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

            else if (taskType.equals(WorkflowSpecificModel.Types.REVIEW_TASK)) {
                notification = processReviewTask(task, notification);
            }

            else if (taskType.equals(WorkflowSpecificModel.Types.INFORMATION_TASK)) {
                notification = processInformationTask(task, notification);
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
            notification.setTemplateName(getTemplate(NotificationModel.NotificationType.TASK_CANCELLED_TASK_NOTIFICATION, 1));
            notification.setSubject(getSubject(NotificationModel.NotificationType.TASK_CANCELLED_TASK_NOTIFICATION));
            notification.setSenderEmail(parametersService.getStringParameter(Parameters.DOC_SENDER_EMAIL));
        }

        notification.addRecipient(task.getOwnerName(), task.getOwnerEmail());
        return notification;
    }

    private Notification processNewTask(Task task, Notification notification) {
        if (StringUtils.isNotEmpty(task.getOwnerId())) {
            if (!isSubscribed(task.getOwnerId(), NotificationModel.NotificationType.TASK_NEW_TASK_NOTIFICATION)) {
                return null;
            }
            notification = setupNotification(notification, NotificationModel.NotificationType.TASK_NEW_TASK_NOTIFICATION);
        } else {
            notification.setTemplateName(getTemplate(NotificationModel.NotificationType.TASK_NEW_TASK_NOTIFICATION, 1));
            notification.setSubject(getSubject(NotificationModel.NotificationType.TASK_NEW_TASK_NOTIFICATION));
            notification.setSenderEmail(parametersService.getStringParameter(Parameters.DOC_SENDER_EMAIL));
            notification.setAttachFiles(true);
        }
        notification.addRecipient(task.getOwnerName(), task.getOwnerEmail());
        return notification;
    }

    /**
     * // informationTask -> informationTaskCompleted -> compound workflow ownerId
     */
    private Notification processInformationTask(Task task, Notification notification) {
        CompoundWorkflow compoundWorkflow = task.getParent().getParent();

        if (StringUtils.isNotBlank(compoundWorkflow.getOwnerId())) {
            if (!isSubscribed(compoundWorkflow.getOwnerId(), NotificationModel.NotificationType.TASK_INFORMATION_TASK_COMPLETED)) {
                return null;
            }
            notification.addRecipient(compoundWorkflow.getOwnerName(), userService.getUserEmail(compoundWorkflow.getOwnerId()));
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
                        return null;
                    }
                    notification.addRecipient(workflowTask.getOwnerName(), workflowTask.getOwnerEmail());
                }
                notification = setupNotification(notification, NotificationModel.NotificationType.TASK_REVIEW_TASK_COMPLETED_WITH_REMARKS);
            } else {
                for (Task workflowTask : workflow.getTasks()) {
                    if (StringUtils.isEmpty(workflowTask.getOwnerId()) || !Status.FINISHED.equals(workflowTask.getStatus())
                            || !isSubscribed(workflowTask.getOwnerId(), NotificationModel.NotificationType.TASK_REVIEW_TASK_COMPLETED_WITH_REMARKS)) {
                        return null;
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
                        return null;
                    }
                    notification = setupNotification(notification, NotificationModel.NotificationType.TASK_REVIEW_TASK_COMPLETED_NOT_ACCEPTED);
                }
            } else {
                for (Task workflowTask : workflow.getTasks()) {
                    if (StringUtils.isEmpty(workflowTask.getOwnerId()) || !Status.FINISHED.equals(workflowTask.getStatus())
                            || !isSubscribed(workflowTask.getOwnerId(), NotificationModel.NotificationType.TASK_REVIEW_TASK_COMPLETED_NOT_ACCEPTED)) {
                        return null;
                    }
                    notification = setupNotification(notification, NotificationModel.NotificationType.TASK_REVIEW_TASK_COMPLETED_NOT_ACCEPTED);
                }
            }
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

        if (StringUtils.isNotEmpty(task.getOwnerId()) && !task.getNode().hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE)) {
            for (Task workflowTask : workflow.getTasks()) {
                final Serializable active = nodeService.getProperty(workflowTask.getNode().getNodeRef(), WorkflowSpecificModel.Props.ACTIVE); // responsible
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
        notification.setSubject(getSubject(notificationType, version));
        notification.setTemplateName(getTemplate(notificationType, version));
        notification.setSenderEmail(parametersService.getStringParameter(Parameters.TASK_SENDER_EMAIL));
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
        notificationProps.add(NotificationModel.NotificationType.TASK_INFORMATION_TASK_COMPLETED);
        notificationProps.add(NotificationModel.NotificationType.TASK_NEW_TASK_NOTIFICATION);
        notificationProps.add(NotificationModel.NotificationType.TASK_OPINION_TASK_COMPLETED);
        notificationProps.add(NotificationModel.NotificationType.TASK_REVIEW_TASK_COMPLETED);
        notificationProps.add(NotificationModel.NotificationType.TASK_REVIEW_TASK_COMPLETED_NOT_ACCEPTED);
        notificationProps.add(NotificationModel.NotificationType.TASK_REVIEW_TASK_COMPLETED_WITH_REMARKS);
        notificationProps.add(NotificationModel.NotificationType.TASK_SIGNATURE_TASK_COMPLETED);
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
            NodeRef compoundWorkflowRef = (nodeService.getPrimaryParent(workflow.getNode().getNodeRef())).getParentRef();
            NodeRef docRef = (nodeService.getPrimaryParent(compoundWorkflowRef)).getParentRef();

            try {
                sendNotification(notification, docRef, setupTemplateData(task));
                sentMails++;
            } catch (Exception e) {
                log.debug("Workflow task event notification e-mail sending failed, ignoring and continueing", e);
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
        if(volumesDispositionedAfterDate.size() == 0) {
            return 0;
        }
        sendVolumesDispositionDateNotifications(volumesDispositionedAfterDate);

        return 0;
    }

    private int sendVolumesDispositionDateNotifications(List<Volume> volumesDispositionedAfterDate) {
        int sentMails = 0;
        Notification notification = setupNotification(new Notification(), NotificationModel.NotificationType.VOLUME_DISPOSITION_DATE);
        notification = addDocumentManagersAsRecipients(notification);
        
        if(notification.getToEmails() == null || notification.getToEmails().isEmpty()) {
            return sentMails; // no doc managers available
        }

        NodeRef systemTemplateByName = null;
        try {
            systemTemplateByName = templateService.getSystemTemplateByName(notification.getTemplateName());
        } catch (DocumentTemplateNotFoundException e) {
            return 0; // if the admins are lazy and we don't have a template, we don't have to send out notifications... :)
        }

        String content = templateService.getProcessedVolumeDispositionTemplate(volumesDispositionedAfterDate, systemTemplateByName);
        try {
            emailService.sendEmail(notification.getToEmails(), notification.getToNames(), notification.getSenderEmail(), notification.getSubject(), content,
                    true, null, null, false, null);

            sentMails = notification.getToEmails().size();
        } catch (EmailException e) {
            log.debug("Volume disposition date notification e-mail sending failed, ignoring and continuing", e);
        }
        return sentMails;
    }

    @Override
    public int processAccessRestrictionEndDateNotifications() {
        int accessRestrictionEndDateNotificationDays = parametersService.getLongParameter(Parameters.ACCESS_RESTRICTION_END_DATE_NOTIFICATION_DAYS).intValue();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, accessRestrictionEndDateNotificationDays);
        Date restrictionEndDate = cal.getTime();

        List<Document> documents = documentSearchService.searchAccessRestictionEndsAfterDate(restrictionEndDate);
        if(documents == null || documents.isEmpty()) {
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

        NodeRef systemTemplateByName = null;
        try {
            systemTemplateByName = templateService.getSystemTemplateByName(notification.getTemplateName());
        } catch (DocumentTemplateNotFoundException e) {
            return 0; // if the admins are lazy and we don't have a template, we don't have to send out notifications... :)
        }

        String content = "";
        String userFullName = "";
        String userEmail = "";
        DocumentAccessRestrictionEndDateComparator endDateComparator = new DocumentAccessRestrictionEndDateComparator();
        for (String userName : documentsByUser.keySet()) {
            userFullName = userService.getUserFullName(userName);
            userEmail = userService.getUserEmail(userName);
            notification.addRecipient(userFullName, userEmail);
            List<Document> userDocuments = documentsByUser.get(userName);
            Collections.sort(userDocuments, endDateComparator);
            content = templateService.getProcessedAccessRestrictionEndDateTemplate(userDocuments, systemTemplateByName);

            try {
                emailService.sendEmail(notification.getToEmails(), notification.getToNames(), notification.getSenderEmail(), notification.getSubject(),
                        content,
                        true, null, null, false, null);
            } catch (Exception e) {
                log.debug("Access restriction due date notification e-mail sending to " + userFullName + " (" + userName + ") <"
                        + userEmail + "> failed, ignoring and continuing", e);
            }
            notification.clearRecipients();
        }

        notification.clearRecipients();
        notification = setupNotification(notification, NotificationModel.NotificationType.ACCESS_RESTRICTION_END_DATE, 1);

        try {
            systemTemplateByName = templateService.getSystemTemplateByName(notification.getTemplateName());
        } catch (DocumentTemplateNotFoundException e) {
            return 0; // if the admins are lazy and we don't have a template, we don't have to send out notifications... :)
        }

        notification = addDocumentManagersAsRecipients(notification);
        DocumentRegNrComparator regNrComparator = new DocumentRegNrComparator();
        Collections.sort(documents, regNrComparator);
        content = templateService.getProcessedAccessRestrictionEndDateTemplate(documents, systemTemplateByName);

        try {
            emailService.sendEmail(notification.getToEmails(), notification.getToNames(), notification.getSenderEmail(), notification.getSubject(),
                    content, true, null, null, false, null);
        } catch (Exception e) {
            log.debug("Access restriction due date notification e-mail sending to document managers failed, ignoring and continuing", e);
        }

        return 0;
    }

    private LinkedHashMap<String, NodeRef> setupTemplateData(Task task) {
        LinkedHashMap<String, NodeRef> templateDataNodeRefs = new LinkedHashMap<String, NodeRef>();
        Workflow workflow = task.getParent();
        NodeRef compoundWorkflowRef = (nodeService.getPrimaryParent(workflow.getNode().getNodeRef())).getParentRef();
        NodeRef docRef = (nodeService.getPrimaryParent(compoundWorkflowRef)).getParentRef();
        templateDataNodeRefs.put("default", docRef);
        templateDataNodeRefs.put("task", task.getNode().getNodeRef());
        templateDataNodeRefs.put("workflow", workflow.getNode().getNodeRef());
        templateDataNodeRefs.put("compoundWorkflow", compoundWorkflowRef);

        return templateDataNodeRefs;
    }

    private Notification addDocumentManagersAsRecipients(Notification notification) {
        Set<String> documentManagers =
                authorityService.
                getContainedAuthorities(
                AuthorityType.USER,
                userService.getDocumentManagersGroup(), true);
        for (String documentManager : documentManagers) {
            String userName = authorityService.getShortName(documentManager);
            notification.addRecipient(userService.getUserFullName(userName), userService.getUserEmail(userName));
        }
        return notification;
    }

    private boolean isSubscribed(String userName, QName subscriptionType) {
        Serializable property = nodeService.getProperty(userService.getUsersPreferenceNodeRef(userName), subscriptionType);
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

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
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
            if (doc1.getRegNumber() != null && doc2.getRegNumber() != null)
                return doc1.getRegNumber().compareTo(doc2.getRegNumber());

            if (doc1.getRegNumber() != null && doc2.getRegNumber() == null)
                return -1;

            if (doc1.getRegNumber() == null && doc2.getRegNumber() != null)
                return 1;

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
        processTaskDueDateNotifications();

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
