package ee.webmedia.alfresco.workflow.model;

import java.util.Arrays;
import java.util.List;

import org.alfresco.service.namespace.QName;

/**
 * @author Alar Kvell
 */
public interface WorkflowSpecificModel {
    String URI = "http://alfresco.webmedia.ee/model/workflow/specific/1.0";
    String PREFIX = "wfs:";

    interface Repo {
        String WORKFLOWS_PARENT = "/";
        String LINKED_REVIEW_TASKS_SPACE = WORKFLOWS_PARENT + PREFIX + "linkedReviewTasks";
    }

    /** Workflows that can be started in parallel when they are consecutively in the compoundWorkflow (workflow status can be {@link Status#IN_PROGRESS}) */
    public QName[] CAN_START_PARALLEL = new QName[] { WorkflowSpecificModel.Types.OPINION_WORKFLOW
            , WorkflowSpecificModel.Types.INFORMATION_WORKFLOW, WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW
            , WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW };
    public List<QName> RESPONSIBLE_TASK_WORKFLOW_TYPES = Arrays.asList(Types.ASSIGNMENT_WORKFLOW, Types.ORDER_ASSIGNMENT_WORKFLOW);

    interface Types {
        QName LINKED_REVIEW_TASKS_ROOT = QName.createQName(URI, "linkedReviewTasks");

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
        /** Grupitäitmiseks */
        QName GROUP_ASSIGNMENT_WORKFLOW = QName.createQName(URI, "groupAssignmentWorkflow");

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
        /** Kooskõlastamine teisest asutusest */
        QName LINKED_REVIEW_TASK = QName.createQName(URI, "linkedReviewTask");
        /** Allkirjastamiseks */
        QName SIGNATURE_TASK = QName.createQName(URI, "signatureTask");
        /** Kinnitamiseks */
        QName CONFIRMATION_TASK = QName.createQName(URI, "confirmationTask");
        /** Tähtaja pikendamiseks */
        QName DUE_DATE_EXTENSION_TASK = QName.createQName(URI, "dueDateExtensionTask");
        /** Grupitäitmiseks */
        QName GROUP_ASSIGNMENT_TASK = QName.createQName(URI, "groupAssignmentTask");
    }

    interface Assocs {

        /**
         * DEPRECATED: do not create new assocs of that type; linkedReviewTasks should be saved in delta_task table only.
         */
        @Deprecated
        QName LINKED_REVIEW_TASK = QName.createQName(URI, "linkedReviewTask");
        /**
         * DEPRECATED: do not create new assocs of that type; taskDueDateExtension info should be saved in delta_task_due_date_extension_assoc table only.
         * task -> dueDateExtensionTask task
         */
        @Deprecated
        QName TASK_DUE_DATE_EXTENSION = QName.createQName(URI, "taskDueDateExtension");
        /**
         * DEPRECATED: do not create new assocs of that type; taskDueDateHistory info should be saved in delta_task_due_date_history table only.
         * task -> dueDateExtensionTaskHistory record
         */
        @Deprecated
        QName TASK_DUE_DATE_EXTENSION_HISTORY = QName.createQName(URI, "taskDueDateExtensionHistory");
    }

    interface Aspects {
        QName COMMENT = QName.createQName(URI, "comment");
        QName RESOLUTION = QName.createQName(URI, "resolution");
        QName RESPONSIBLE = QName.createQName(URI, "responsible");
        QName SEARCHABLE = QName.createQName(URI, "searchable");
        QName TEMP_OUTCOME = QName.createQName(URI, "tempOutcome");
        QName TASK_DUE_DATE_EXTENSION_CONTAINER = QName.createQName(URI, "taskDueDateExtensionContainer");
        QName COMMON_TASK = QName.createQName(URI, "commonTask");
        QName INSTITUTION = QName.createQName(URI, "institution");
        QName CREATOR_INSTITUTION = QName.createQName(URI, "creatorInstitution");
        QName CREATOR_INSTITUTION_CODE = QName.createQName(URI, "creatorInstitutionCode");
        QName RECIEVED_DVK_ID = QName.createQName(URI, "recievedDvkId");
        QName SENT_DVK_DATA = QName.createQName(URI, "sentDvkData");
        QName SEARCHABLE_COMPOUND_WORKFLOW_TITLE_AND_COMMENT = QName.createQName(URI, "searchableCompoundWorkflowTitleAndComment");
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
        QName CREATOR_INSTITUTION_NAME = QName.createQName(URI, "creatorInstitutionName");
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
        /** Null value should always be treated as TRUE */
        QName SEND_ORDER_ASSIGNMENT_COMPLETED_EMAIL = QName.createQName(URI, "sendOrderAssignmentCompletedEmail");
        QName FILE_VERSIONS = QName.createQName(URI, "fileVersions");
        QName SIGNING_TYPE = QName.createQName(URI, "signingType");
        // properties duplicated from compound workflow for task search
        QName COMPOUND_WORKFLOW_TITLE = QName.createQName(URI, "compoundWorkflowTitle");
        /**
         * Deprecated - compound workflow comments are not used in search any more; comments are moved to delta_compound_workflow_comment table
         */
        @Deprecated
        QName COMPOUND_WORKFLOW_COMMENT = QName.createQName(URI, "compoundWorkflowComment");
        QName SEARCHABLE_COMPOUND_WORKFLOW_TYPE = QName.createQName(URI, "searchableCompoundWorkflowType");
        QName SEARCHABLE_COMPOUND_WORKFLOW_OWNER_NAME = QName.createQName(URI, "searchableCompoundWorkflowOwnerName");
        QName SEARCHABLE_COMPOUND_WORKFLOW_OWNER_ORGANIZATION_NAME = QName.createQName(URI, "searchableCompoundWorkflowOwnerOrganizationName");
        QName SEARCHABLE_COMPOUND_WORKFLOW_OWNER_JOB_TITLE = QName.createQName(URI, "searchableCompoundWorkflowOwnerJobTitle");
        QName SEARCHABLE_COMPOUND_WORKFLOW_CREATED_DATE_TIME = QName.createQName(URI, "searchableCompoundWorkflowCreatedDateTime");
        QName SEARCHABLE_COMPOUND_WORKFLOW_STARTED_DATE_TIME = QName.createQName(URI, "searchableCompoundWorkflowStartedDateTime");
        QName SEARCHABLE_COMPOUND_WORKFLOW_STOPPED_DATE_TIME = QName.createQName(URI, "searchableCompoundWorkflowStoppedDateTime");
        QName SEARCHABLE_COMPOUND_WORKFLOW_FINISHED_DATE_TIME = QName.createQName(URI, "searchableCompoundWorkflowFinishedDateTime");
        QName SEARCHABLE_COMPOUND_WORKFLOW_STATUS = QName.createQName(URI, "searchableCompoundWorkflowStatus");

        QName ORIGINAL_NODEREF_ID = QName.createQName(URI, "originalNoderefId");
        QName ORIGINAL_TASK_OBJECT_URL = QName.createQName(URI, "originalTaskObjectUrl");

        QName COMPOUND_WORKFLOW_ID = QName.createQName(URI, "compoundWorkflowId");
    }

    enum SignatureTaskOutcome {
        NOT_SIGNED,
        SIGNED_IDCARD,
        SIGNED_MOBILEID;

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
