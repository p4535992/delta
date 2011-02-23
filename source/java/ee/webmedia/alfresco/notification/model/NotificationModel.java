package ee.webmedia.alfresco.notification.model;

import org.alfresco.service.namespace.QName;

public interface NotificationModel {
    String URI = "http://alfresco.webmedia.ee/model/notification/1.0";
    String NAMESPACE_PREFIX = "ntf:";
    
    interface Repo {
        final static String NOTIFICATIONS_PARENT = "/";
        final static String NOTIFICATIONS_SPACE = NOTIFICATIONS_PARENT + NAMESPACE_PREFIX + Types.NOTIFICATIONS_ROOT.getLocalName();
    }
    
    interface Types {
        QName NOTIFICATIONS_ROOT = QName.createQName(URI, "genNotifications");
        QName GENERAL_NOTIFICATION = QName.createQName(URI, "generalNotification");
    }
    
    interface Prop {
        QName CREATOR_NAME = QName.createQName(URI, "creatorName");
        QName CREATED_DATE_TIME = QName.createQName(URI, "createdDateTime");
        QName MESSAGE = QName.createQName(URI, "message");
        QName ACTIVE = QName.createQName(URI, "active");
    }
    
    interface Assoc {
        QName GENERAL_NOTIFICATION = QName.createQName(URI, "generalNotification");
    }
    
    interface NotificationType {
        QName TASK_NEW_TASK_NOTIFICATION = QName.createQName(URI, "newTaskNotification");
        QName TASK_CANCELLED_TASK_NOTIFICATION = QName.createQName(URI, "cancelledTaskNotification");
        QName TASK_SIGNATURE_TASK_COMPLETED = QName.createQName(URI, "signatureTaskCompleted");
        QName TASK_OPINION_TASK_COMPLETED = QName.createQName(URI, "opinionTaskCompleted");
        QName TASK_REVIEW_TASK_COMPLETED = QName.createQName(URI, "reviewTaskCompleted");
        QName TASK_INFORMATION_TASK_COMPLETED = QName.createQName(URI, "informationTaskCompleted");
        QName TASK_ASSIGNMENT_TASK_COMPLETED_BY_CO_RESPONSIBLE = QName.createQName(URI, "assignmentTaskCompletedByCoResponsible");
        QName TASK_ASSIGNMENT_TASK_COMPLETED_BY_RESPONSIBLE = QName.createQName(URI, "assignmentTaskCompletedByResponsible");
        QName TASK_REVIEW_TASK_COMPLETED_NOT_ACCEPTED = QName.createQName(URI, "reviewTaskCompletedNotAccepted");
        QName TASK_REVIEW_TASK_COMPLETED_WITH_REMARKS = QName.createQName(URI, "reviewTaskCompletedWithRemarks");
        QName WORKFLOW_WORKFLOW_COMPLETED = QName.createQName(URI, "workflowCompleted");
        QName WORKFLOW_NEW_WORKFLOW_STARTED = QName.createQName(URI, "newWorkflowStarted");
        
        QName DISCUSSION_INVITATION = QName.createQName(URI, "discussionInvitation");
        
        QName TASK_DUE_DATE_APPROACHING = QName.createQName(URI, "dueDateApproaching");
        QName TASK_DUE_DATE_EXCEEDED = QName.createQName(URI, "dueDateExceeded");
        QName VOLUME_DISPOSITION_DATE = QName.createQName(URI, "volumeDispositionDate");
        QName ACCESS_RESTRICTION_END_DATE = QName.createQName(URI, "accessRestrictionEndDate");
    }

}
