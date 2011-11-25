package ee.webmedia.alfresco.workflow.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Alar Kvell
 */
public interface WorkflowSpecificModel {
    String URI = "http://alfresco.webmedia.ee/model/workflow/specific/1.0";
    String PREFIX = "wfs:";

    /** Workflows that can be started in parallel when they are consecutively in the compoundWorkflow (workflow status can be {@link Status#IN_PROGRESS}) */
    public QName[] CAN_START_PARALLEL = new QName[] { WorkflowSpecificModel.Types.OPINION_WORKFLOW
            , WorkflowSpecificModel.Types.INFORMATION_WORKFLOW, WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW
            , WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW };

    interface Types {
        /** Täitmiseks */
        QName ASSIGNMENT_WORKFLOW = QName.createQName(URI, "assignmentWorkflow");
        /** Registreeri */
        QName DOC_REGISTRATION_WORKFLOW = QName.createQName(URI, "docRegistrationWorkflow");
        /** Teadmiseks */
        QName INFORMATION_WORKFLOW = QName.createQName(URI, "informationWorkflow");
        /** Arvamuse andmiseks */
        QName OPINION_WORKFLOW = QName.createQName(URI, "opinionWorkflow");
        /** Kooskõlastamiseks */
        QName REVIEW_WORKFLOW = QName.createQName(URI, "reviewWorkflow");
        /** Asutuseüleseks kooskõlastamiseks */
        QName EXTERNAL_REVIEW_WORKFLOW = QName.createQName(URI, "externalReviewWorkflow");
        /** Allkirjastamiseks */
        QName SIGNATURE_WORKFLOW = QName.createQName(URI, "signatureWorkflow");
        /** Otsuse täitmiseks */
        QName ORDER_ASSIGNMENT_WORKFLOW = QName.createQName(URI, "orderAssignmentWorkflow");
        /** Kinnitamiseks */
        QName CONFIRMATION_WORKFLOW = QName.createQName(URI, "confirmationWorkflow");
        /** Tähtaja pikendamiseks */
        QName DUE_DATE_EXTENSION_WORKFLOW = QName.createQName(URI, "dueDateExtensionWorkflow");

        /** Täitmiseks */
        QName ASSIGNMENT_TASK = QName.createQName(URI, "assignmentTask");
        /** Otsuse täitmiseks */
        QName ORDER_ASSIGNMENT_TASK = QName.createQName(URI, "orderAssignmentTask");
        /** Teadmiseks */
        QName INFORMATION_TASK = QName.createQName(URI, "informationTask");
        /** Arvamuse andmiseks */
        QName OPINION_TASK = QName.createQName(URI, "opinionTask");
        /** Kooskõlastamiseks */
        QName REVIEW_TASK = QName.createQName(URI, "reviewTask");
        QName EXTERNAL_REVIEW_TASK = QName.createQName(URI, "externalReviewTask");
        /** Allkirjastamiseks */
        QName SIGNATURE_TASK = QName.createQName(URI, "signatureTask");
        /** Kinnitamiseks */
        QName CONFIRMATION_TASK = QName.createQName(URI, "confirmationTask");
        /** Tähtaja pikendamiseks */
        QName DUE_DATE_EXTENSION_TASK = QName.createQName(URI, "dueDateExtensionTask");
    }

    interface Assocs {
        /** task -> dueDateExtensionTask task */
        QName TASK_DUE_DATE_EXTENSION = QName.createQName(URI, "taskDueDateExtension");
        /** task -> dueDateExtensionTaskHistory record */
        QName TASK_DUE_DATE_EXTENSION_HISTORY = QName.createQName(URI, "taskDueDateExtensionHistory");
    }

    interface Aspects {
        QName COMMENT = QName.createQName(URI, "comment");
        QName RESOLUTION = QName.createQName(URI, "resolution");
        QName RESPONSIBLE = QName.createQName(URI, "responsible");
        QName SEARCHABLE = QName.createQName(URI, "searchable");
        QName TEMP_OUTCOME = QName.createQName(URI, "tempOutcome");
        QName TASK_DUE_DATE_EXTENSION_CONTAINER = QName.createQName(URI, "taskDueDateExtensionContainer");
    }

    interface Props {
        QName DUE_DATE = QName.createQName(URI, "dueDate");
        QName DUE_DATE_DAYS = QName.createQName(URI, "dueDateDays");
        QName IS_DUE_DATE_WORKING_DAYS = QName.createQName(URI, "isDueDateDaysWorkingDays");
        QName DESCRIPTION = QName.createQName(URI, "description");
        QName RESOLUTION = QName.createQName(URI, "resolution");
        QName ACTIVE = QName.createQName(URI, "active");
        QName COMMENT = QName.createQName(URI, "comment");
        QName WORKFLOW_RESOLUTION = QName.createQName(URI, "workflowResolution");
        QName COMPLETED_OVERDUE = QName.createQName(URI, "completedOverdue");
        QName TEMP_OUTCOME = QName.createQName(URI, "tempOutcome");
        QName INSTITUTION_NAME = QName.createQName(URI, "institutionName");
        QName CREATOR_INSTITUTION_CODE = QName.createQName(URI, "creatorInstitutionCode");
        QName INSTITUTION_CODE = QName.createQName(URI, "institutionCode");
        QName SENT_DVK_ID = QName.createQName(URI, "sentDvkId");
        QName RECIEVED_DVK_ID = QName.createQName(URI, "recievedDvkId");
        QName ORIGINAL_DVK_ID = QName.createQName(URI, "originalDvkId");
        QName SEND_STATUS = QName.createQName(URI, "sendStatus");
        QName SEND_DATE_TIME = QName.createQName(URI, "sendDateTime");
        QName CATEGORY = QName.createQName(URI, "category");
        QName CREATOR_ID = QName.createQName(URI, "creatorId");
        QName CREATOR_EMAIL = QName.createQName(URI, "creatorEmail");
        QName PROPOSED_DUE_DATE = QName.createQName(URI, "proposedDueDate");
        QName CONFIRMED_DUE_DATE = QName.createQName(URI, "confirmedDueDate");
        QName SEND_ORDER_ASSIGNMENT_COMPLETED_EMAIL = QName.createQName(URI, "sendOrderAssignmentCompletedEmail");
    }

    enum SignatureTaskOutcome {
        NOT_SIGNED,
        SIGNED;

        public static SignatureTaskOutcome of(int index) {
            if (index < 0 || index >= values().length) {
                return null;
            }
            return values()[index];
        }

        public boolean equals(int index) {
            return this == of(index);
        }

    }

    enum ReviewTaskOutcome {
        CONFIRMED,
        CONFIRMED_WITH_REMARKS,
        NOT_CONFIRMED;

        public static ReviewTaskOutcome of(int index) {
            if (index < 0 || index >= values().length) {
                return null;
            }
            return values()[index];
        }

        public boolean equals(int index) {
            return this == of(index);
        }
    }

    enum ExternalReviewTaskOutcome {
        CONFIRMED,
        NOT_CONFIRMED;

        public static ExternalReviewTaskOutcome of(int index) {
            if (index < 0 || index >= values().length) {
                return null;
            }
            return values()[index];
        }

        public boolean equals(int index) {
            return this == of(index);
        }
    }

    enum ConfirmationTaskOutcome {
        ACCEPTED,
        NOT_ACCEPTED;

        public static ConfirmationTaskOutcome of(int index) {
            if (index < 0 || index >= values().length) {
                return null;
            }
            return values()[index];
        }

        public boolean equals(int index) {
            return this == of(index);
        }

    }

    enum DueDateExtensionTaskOutcome {
        ACCEPTED,
        NOT_ACCEPTED;

        public static DueDateExtensionTaskOutcome of(int index) {
            if (index < 0 || index >= values().length) {
                return null;
            }
            return values()[index];
        }

        public boolean equals(int index) {
            return this == of(index);
        }

    }

}
