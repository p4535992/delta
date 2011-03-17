package ee.webmedia.alfresco.notification.service;

import java.util.List;

import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.notification.model.GeneralNotification;
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

    public void addMissingConfigurations(Node userPreferencesNode);

    public void saveConfigurationChanges(Node userPreferencesNode);

    public boolean processOutgoingInformationTask(Task task);

    public int processTaskDueDateNotifications();

    public int processVolumeDispositionDateNotifications();

    public int processAccessRestrictionEndDateNotifications();

    public void processTaskDueDateNotifications(ActionEvent event);

    public void processVolumeDispositionDateNotifications(ActionEvent event);

    public void processAccessRestrictionEndDateNotifications(ActionEvent event);

    public GeneralNotification getGeneralNotificationByNodeRef(NodeRef nodeRef);

    public List<GeneralNotification> getGeneralNotifications();
    
    public void updateGeneralNotification(Node notification);

    public Node generalNotificationAsNode(GeneralNotification notification);

    public List<GeneralNotification> getActiveGeneralNotifications();
    
    public int getUpdateCount();
    
    public void notifyTaskUnfinishedEvent(Task task);

}
