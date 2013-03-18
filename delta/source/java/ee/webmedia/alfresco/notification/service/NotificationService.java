package ee.webmedia.alfresco.notification.service;

import java.util.Date;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.document.sendout.model.SendInfo;
import ee.webmedia.alfresco.notification.model.GeneralNotification;
import ee.webmedia.alfresco.substitute.model.Substitute;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventType;

/**
 * @author Kaarel Jõgeva
 */
public interface NotificationService {

    public static String BEAN_NAME = "NotificationService";

    public void notifyTaskEvent(Task task);

    public void notifyWorkflowEvent(Workflow workflow, WorkflowEventType eventType);

    public void notifyCompoundWorkflowEvent(CompoundWorkflow compoundWorkflowEvent);

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

    List<SendInfo> processAccessRestrictionChangedNotification(DocumentDynamic document, List<SendInfo> sendInfos, boolean ignoreMissingEmails);

    List<QName> getAllNotificationProps();


}
