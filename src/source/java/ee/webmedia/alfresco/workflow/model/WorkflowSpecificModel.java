package ee.webmedia.alfresco.workflow.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Alar Kvell
 */
public interface WorkflowSpecificModel {
    String URI = "http://alfresco.webmedia.ee/model/workflow/specific/1.0";
    String PREFIX = "wfs:";

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
        /** Allkirjastamiseks */
        QName SIGNATURE_WORKFLOW = QName.createQName(URI, "signatureWorkflow");

        QName ASSIGNMENT_TASK = QName.createQName(URI, "assignmentTask");
        QName INFORMATION_TASK = QName.createQName(URI, "informationTask");
        QName OPINION_TASK = QName.createQName(URI, "opinionTask");
        QName REVIEW_TASK = QName.createQName(URI, "reviewTask");
        QName SIGNATURE_TASK = QName.createQName(URI, "signatureTask");
    }

    interface Aspects {
        QName COMMENT = QName.createQName(URI, "comment");
        QName RESOLUTION = QName.createQName(URI, "resolution");
        QName RESPONSIBLE = QName.createQName(URI, "responsible");
        QName SEARCHABLE = QName.createQName(URI, "searchable");
    }

    interface Props {
        QName DUE_DATE = QName.createQName(URI, "dueDate");
        QName DESCRIPTION = QName.createQName(URI, "description");
        QName RESOLUTION = QName.createQName(URI, "resolution");
        QName FILE = QName.createQName(URI, "file");
        QName ACTIVE = QName.createQName(URI, "active");
        QName COMMENT = QName.createQName(URI, "comment");
        QName WORKFLOW_RESOLUTION = QName.createQName(URI, "workflowResolution");
        QName COMPLETED_OVERDUE = QName.createQName(URI, "completedOverdue");
        QName TEMP_OUTCOME = QName.createQName(URI, "tempOutcome");
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

}