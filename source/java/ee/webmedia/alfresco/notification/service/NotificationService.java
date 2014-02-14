package ee.webmedia.alfresco.notification.service;

import java.util.Date;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.document.sendout.model.SendInfo;
import ee.webmedia.alfresco.notification.model.GeneralNotification;
import ee.webmedia.alfresco.substitute.model.Substitute;
import ee.webmedia.alfresco.user.model.Authority;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEvent;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventType;

/**
 * @author Kaarel Jõgeva
 */
public interface NotificationService {

    public static String BEAN_NAME = "NotificationService";

    public void notifyTaskEvent(Task task);

    public void notifyTaskEvent(Task task, boolean isGroupAssignmentTaskFinishedAutomatically, Task orderAssignmentFinishTriggeringTask);

    public void notifyWorkflowEvent(Workflow workflow, WorkflowEventType eventType);

    public void notifyCompoundWorkflowEvent(WorkflowEvent compoundWorkflowEvent);

    public void notifySubstitutionEvent(Substitute substitute);

    public void addMissingConfigurations(Node userPreferencesNode);

    public void saveConfigurationChanges(Node userPreferencesNode);

    public boolean processOutgoingInformationTask(Task task);

    public int processTaskDueDateNotificationsIfWorkingDay(Date firingDate);

    public int processVolumeDispositionDateNotifications();

    public int processAccessRestrictionEndDateNotifications();

    public int processContractDueDateNotifications();

    public void processTaskDueDateNotifications(ActionEvent event);

    public void processVolumeDispositionDateNotifications(ActionEvent event);

    public void processAccessRestrictionEndDateNotifications(ActionEvent event);

    public void processContractDueDateNotifications(ActionEvent event);

    public GeneralNotification getGeneralNotificationByNodeRef(NodeRef nodeRef);

    public List<GeneralNotification> getGeneralNotifications();

    public void updateGeneralNotification(Node notification);

    public Node generalNotificationAsNode(GeneralNotification notification);

    public List<GeneralNotification> getActiveGeneralNotifications();

    public int getUpdateCount();

    public void notifyTaskUnfinishedEvent(Task task, boolean manuallyCancelled);

    String generateTemplateContent(QName notificationType, Task task);

    void notifyExternalReviewError(Task task);

    void notifyExternalReviewError(String notificationContent);

    void processAccessRestrictionChangedNotification(DocumentDynamic document, List<String> emails);

    List<QName> getAllNotificationProps();

    void notifyCompoundWorkflowStoppedAutomatically(Workflow workflow);

    void sendForInformationNotification(List<Authority> authorities, Node docNode, String emailTemplate, String subject, String content);

    void addNotificationAssocForCurrentUser(NodeRef targetNodeRef, QName assocQName, QName aspectQName);

    void removeNotificationAssocForCurrentUser(NodeRef targetNodeRef, QName assocQName, QName aspectQName);

    public boolean isNotificationAssocExists(NodeRef userRef, NodeRef nodeRef, QName assocType);

    Pair<List<String>, List<SendInfo>> getExistingAndMissingEmails(List<SendInfo> sendInfos);

    public void addUserSpecificNotification(String userKey, String notification);

    public List<String> getUserSpecificNotification(String userKey);

    public void deleteUserSpecificNotification(String userKey);

    boolean isSubstitutionTaskEndDateRestricted();

}
