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
        QName TASK_EXTERNAL_REVIEW_TASK_COMPLETED = QName.createQName(URI, "externalReviewTaskCompleted");
        QName TASK_EXTERNAL_REVIEW_TASK_COMPLETED_NOT_ACCEPTED = QName.createQName(URI, "externalReviewTaskCompletedNotAccepted");
        QName TASK_NEW_TASK_NOTIFICATION = QName.createQName(URI, "newTaskNotification");
        QName TASK_CANCELLED_TASK_NOTIFICATION = QName.createQName(URI, "cancelledTaskNotification");
        QName TASK_SIGNATURE_TASK_COMPLETED = QName.createQName(URI, "signatureTaskCompleted");
        QName TASK_SIGNATURE_TASK_COMPLETED_ORDERED = QName.createQName(URI, "signatureTaskCompletedOrdered");
        QName TASK_OPINION_TASK_COMPLETED = QName.createQName(URI, "opinionTaskCompleted");
        QName TASK_OPINION_TASK_COMPLETED_ORDERED = QName.createQName(URI, "opinionTaskCompletedOrdered");
        QName TASK_REVIEW_TASK_COMPLETED = QName.createQName(URI, "reviewTaskCompleted");
        QName TASK_REVIEW_TASK_COMPLETED_ORDERED = QName.createQName(URI, "reviewTaskCompletedOrdered");
        QName TASK_INFORMATION_TASK_COMPLETED = QName.createQName(URI, "informationTaskCompleted");
        QName TASK_INFORMATION_TASK_COMPLETED_ORDERED = QName.createQName(URI, "informationTaskCompletedOrdered");
        QName TASK_ASSIGNMENT_TASK_COMPLETED_BY_CO_RESPONSIBLE = QName.createQName(URI, "assignmentTaskCompletedByCoResponsible");
        QName TASK_ASSIGNMENT_TASK_COMPLETED_BY_CO_RESPONSIBLE_ORDERED = QName.createQName(URI, "assignmentTaskCompletedByCoResponsibleOrdered");
        QName TASK_ASSIGNMENT_TASK_COMPLETED_BY_RESPONSIBLE = QName.createQName(URI, "assignmentTaskCompletedByResponsible");
        QName TASK_ASSIGNMENT_TASK_COMPLETED_BY_RESPONSIBLE_ORDERED = QName.createQName(URI, "assignmentTaskCompletedByResponsibleOrdered");
        QName TASK_REVIEW_TASK_COMPLETED_NOT_ACCEPTED = QName.createQName(URI, "reviewTaskCompletedNotAccepted");
        QName TASK_REVIEW_TASK_COMPLETED_NOT_ACCEPTED_ORDERED = QName.createQName(URI, "reviewTaskCompletedNotAcceptedOrdered");
        QName TASK_REVIEW_TASK_COMPLETED_WITH_REMARKS = QName.createQName(URI, "reviewTaskCompletedWithRemarks");
        QName TASK_REVIEW_TASK_COMPLETED_WITH_REMARKS_ORDERED = QName.createQName(URI, "reviewTaskCompletedWithRemarksOrdered");
        QName PARRALEL_WORKFLOW_COMPLETED = QName.createQName(URI, "parralelWorkflowCompleted");
        QName WORKFLOW_WORKFLOW_COMPLETED = QName.createQName(URI, "workflowCompleted");
        QName WORKFLOW_NEW_WORKFLOW_STARTED = QName.createQName(URI, "newWorkflowStarted");
        QName TASK_ORDER_ASSIGNMENT_WORKFLOW_COMPLETED = QName.createQName(URI, "orderAssignmentWorkflowCompleted");
        QName TASK_ORDER_ASSIGNMENT_TASK_COMPLETED = QName.createQName(URI, "orderAssignmentTaskCompleted");
        QName TASK_ORDER_ASSIGNMENT_TASK_COMPLETED_ORDERED = QName.createQName(URI, "orderAssignmentTaskCompletedOrdered");
        QName TASK_CONFIRMATION_TASK_COMPLETED = QName.createQName(URI, "confirmationTaskCompleted");
        QName TASK_CONFIRMATION_TASK_COMPLETED_ORDERED = QName.createQName(URI, "confirmationTaskCompletedOrdered");
        QName TASK_CONFIRMATION_TASK_COMPLETED_NOT_ACCEPTED = QName.createQName(URI, "confirmationTaskCompletedNotAccepted");
        QName TASK_CONFIRMATION_TASK_COMPLETED_NOT_ACCEPTED_ORDERED = QName.createQName(URI, "confirmationTaskCompletedNotAcceptedOrdered");
        QName TASK_DUE_DATE_EXTENSION_TASK_COMPLETED = QName.createQName(URI, "taskDueDateExtensionTaskCompleted");
        QName TASK_DUE_DATE_EXTENSION_TASK_COMPLETED_ORDERED = QName.createQName(URI, "taskDueDateExtensionTaskCompletedOrdered");
        QName TASK_DUE_DATE_EXTENSION_TASK_COMPLETED_NOT_ACCEPTED = QName.createQName(URI, "taskDueDateExtensionTaskCompletedNotAccepted");
        QName TASK_DUE_DATE_EXTENSION_TASK_COMPLETED_NOT_ACCEPTED_ORDERED = QName.createQName(URI, "taskDueDateExtensionTaskCompletedNotAcceptedOrdered");
        QName TASK_REVIEWED_DOCUMENT_REVIEWED = QName.createQName(URI, "reviewedDocumentReviewed");
        QName TASK_REVIEWED_DOCUMENT_REVIEWED_NOT_ACCEPTED = QName.createQName(URI, "reviewedDocumentReviewedNotAccepted");
        QName TASK_REVIEWED_DOCUMENT_REVIEWED_WITH_REMARKS = QName.createQName(URI, "reviewedDocumentReviewedWithRemarks");
        QName TASK_REVIEWED_DOCUMENT_CONFIRMED = QName.createQName(URI, "reviewedDocumentConfirmed");
        QName TASK_REVIEWED_DOCUMENT_CONFIRMED_NOT_ACCEPTED = QName.createQName(URI, "reviewedDocumentConfirmedNotAccepted");
        QName WORKFLOW_REGISTRATION_STOPPED_NO_DOCUMENTS = QName.createQName(URI, "workflowRegistrationStoppedNoDocuments");
        QName WORKFLOW_SIGNATURE_STOPPED_NO_DOCUMENTS = QName.createQName(URI, "workflowSignatureStoppedNoDocuments");
        QName COMPOUND_WORKFLOW_STOPPED = QName.createQName(URI, "compoundWorkflowStopped");
        QName COMPOUND_WORKFLOW_CONTINUED = QName.createQName(URI, "compoundWorkflowContinued");
        QName COMPOUND_WORKFLOW_REOPENED = QName.createQName(URI, "compoundWorkflowReopened");
        QName COMPOUND_WORKFLOW_FINISHED = QName.createQName(URI, "compoundWorkflowFinished");

        QName EXTERNAL_REVIEW_WORKFLOW_RECIEVING_ERROR = QName.createQName(URI, "externalReviewWorkflowRecievingError");
        QName EXTERNAL_REVIEW_WORKFLOW_SERIES_ERROR = QName.createQName(URI, "externalReviewWorkflowSeriesError");
        QName EXTERNAL_REVIEW_WORKFLOW_OWNER_ERROR = QName.createQName(URI, "externalReviewWorkflowOwnerError");

        QName DISCUSSION_INVITATION = QName.createQName(URI, "discussionInvitation");
        QName SUBSTITUTION = QName.createQName(URI, "substitution");

        QName TASK_DUE_DATE_APPROACHING = QName.createQName(URI, "dueDateApproaching");
        QName TASK_DUE_DATE_EXCEEDED = QName.createQName(URI, "dueDateExceeded");
        QName VOLUME_DISPOSITION_DATE = QName.createQName(URI, "volumeDispositionDate");
        QName CONTRACT_DUE_DATE = QName.createQName(URI, "contractDueDate");
        QName ACCESS_RESTRICTION_END_DATE = QName.createQName(URI, "accessRestrictionEndDate");
        QName ACCESS_RESTRICTION_REASON_CHANGED = QName.createQName(URI, "accessRestrictionReasonChanged");

        QName DELEGATED_TASK_COMPLETED = QName.createQName(URI, "delegatedTaskCompleted");
        QName REVIEW_DOCUMENT_SIGNED = QName.createQName(URI, "reviewDocumentSigned");
        QName REVIEW_DOCUMENT_NOT_SIGNED = QName.createQName(URI, "reviewDocumentNotSigned");
        QName TASK_CANCELLED = QName.createQName(URI, "taskCancelled");
        QName TASK_CANCELLED_ORDERED = QName.createQName(URI, "taskCancelledOrdered");
        QName GROUP_ASSIGNMENT_TASK_COMPLETED_BY_OTHERS = QName.createQName(URI, "groupAssignmentTaskCompletedByOthers");
        QName GROUP_ASSIGNMENT_TASK_COMPLETED_ORDERED = QName.createQName(URI, "groupAssignmentTaskCompletedOrdered");
        QName DOCUMENT_SEND_FOR_INFORMATION = QName.createQName(URI, "documentSendForInformation");
        QName EMAIL_ATTACHMENT_SIZE_LIMIT_EXCEEDED = QName.createQName(URI, "emailAttachmentSizeLimitExceeded");
        
        QName MY_FILE_MODIFIED = QName.createQName(URI, "myFileModified");
    }

}
