package ee.webmedia.alfresco.workflow.model;

import org.alfresco.service.namespace.QName;

public interface WorkflowCommonModel {
    String URI = "http://alfresco.webmedia.ee/model/workflow/common/1.0";
    String PREFIX = "wfc:";

    interface Repo {
        String WORKFLOWS_PARENT = "/";
        String WORKFLOWS_SPACE = WORKFLOWS_PARENT + PREFIX + "compoundWorkflowDefinitions";
        String INDEPENDENT_WORKFLOWS_SPACE = WORKFLOWS_PARENT + PREFIX + "independentCompoundWorkflows";
    }

    interface Types {
        QName COMPOUND_WORKFLOW_DEFINITIONS_ROOT = QName.createQName(URI, "compoundWorkflowDefinitions");
        QName INDEPENDENT_COMPOUND_WORKFLOWS_ROOT = QName.createQName(URI, "independentCompoundWorkflows");
        QName COMPOUND_WORKFLOW_DEFINITION = QName.createQName(URI, "compoundWorkflowDefinition");
        QName COMPOUND_WORKFLOW = QName.createQName(URI, "compoundWorkflow");
        QName WORKFLOW = QName.createQName(URI, "workflow");
        /** No new nodes of this type should be created. Nodes for this type are still created in memory. */
        @Deprecated
        QName TASK = QName.createQName(URI, "task");
        /** Deprecated: no new nodes of this type should be created */
        @Deprecated
        QName DUE_DATE_HISTORY = QName.createQName(URI, "dueDateHistory");
        QName RELATED_URL = QName.createQName(URI, "relatedUrl");
        QName COMMENT = QName.createQName(URI, "comment");
    }

    interface Assocs {
        QName COMPOUND_WORKFLOW_DEFINITION = QName.createQName(URI, "compoundWorkflowDefinition");
        QName COMPOUND_WORKFLOW = QName.createQName(URI, "compoundWorkflow");
        QName WORKFLOW = QName.createQName(URI, "workflow");
        /** Deprecated: new tasks must be saved only to delta_task db table and not to repo nodes */
        @Deprecated
        QName TASK = QName.createQName(URI, "task");
        QName FAVORITE = QName.createQName(URI, "favorite");
        QName RELATED_URL = QName.createQName(URI, "relatedUrl");
    }

    interface Aspects {
        QName COMPOUND_WORKFLOW_CONTAINER = QName.createQName(URI, "compoundWorkflowContainer");
    }

    interface Props {
        // Common fields
        /**
         * All values for this property are defined in Enum {@link Status}
         */
        QName STATUS = QName.createQName(URI, "status");
        QName CREATOR_NAME = QName.createQName(URI, "creatorName");
        QName CREATED_DATE_TIME = QName.createQName(URI, "createdDateTime");
        QName STARTED_DATE_TIME = QName.createQName(URI, "startedDateTime");
        QName STOPPED_DATE_TIME = QName.createQName(URI, "stoppedDateTime");
        QName FINISHED_DATE_TIME = QName.createQName(URI, "finishedDateTime");
        QName OWNER_ID = QName.createQName(URI, "ownerId"); // username ehk isikukood
        QName OWNER_NAME = QName.createQName(URI, "ownerName");
        QName PREVIOUS_OWNER_ID = QName.createQName(URI, "previousOwnerId"); // username ehk isikukood
        QName PROCEDURE_ID = QName.createQName(URI, "procedureId");

        // Compound workflow definition
        QName NAME = QName.createQName(URI, "name");
        QName DOCUMENT_TYPES = QName.createQName(URI, "documentTypes");
        /** userId whom this compoundWorkflowDefinition is visible (if null, then visible for all users) */
        QName USER_ID = QName.createQName(URI, "userId");
        QName TYPE = QName.createQName(URI, "type");
        QName CASE_FILE_TYPES = QName.createQName(URI, "caseFileTypes");
        QName TITLE = QName.createQName(URI, "title");
        /**
         * Deprecated - comments are moved to delta_compound_workflow_comment table, this property can be removed when old data has been updated.
         */
        @Deprecated
        QName COMMENT = QName.createQName(URI, "comment");
        QName MAIN_DOCUMENT = QName.createQName(URI, "mainDocument");
        QName DOCUMENTS_TO_SIGN = QName.createQName(URI, "documentsToSign");

        // Workflow
        QName PARALLEL_TASKS = QName.createQName(URI, "parallelTasks");
        QName STOP_ON_FINISH = QName.createQName(URI, "stopOnFinish");
        /** can workflow be deleted? */
        QName MANDATORY = QName.createQName(URI, "mandatory");

        // Task
        QName OWNER_EMAIL = QName.createQName(URI, "ownerEmail");
        QName OWNER_GROUP = QName.createQName(URI, "ownerGroup");
        QName OWNER_SUBSTITUTE_NAME = QName.createQName(URI, "ownerSubstituteName");
        QName OUTCOME = QName.createQName(URI, "outcome");
        QName DOCUMENT_TYPE = QName.createQName(URI, "documentType");
        QName COMPLETED_DATE_TIME = QName.createQName(URI, "completedDateTime");
        QName OWNER_ORGANIZATION_NAME = QName.createQName(URI, "ownerOrganizationName");
        QName OWNER_JOB_TITLE = QName.createQName(URI, "ownerJobTitle");
        QName VIEWED_BY_OWNER = QName.createQName(URI, "viewedByOwner");

        // Due date history
        QName PREVIOUS_DUE_DATE = QName.createQName(URI, "previousDueDate");
        QName CHANGE_REASON = QName.createQName(URI, "changeReason");

        // Related url
        QName URL = QName.createQName(URI, "url");
        QName URL_COMMENT = QName.createQName(URI, "urlComment");
        QName URL_CREATOR_NAME = QName.createQName(URI, "urlCreatorName");
        QName CREATED = QName.createQName(URI, "created");
        QName URL_MODIFIER_NAME = QName.createQName(URI, "urlModifierName");
        QName MODIFIED = QName.createQName(URI, "modified");

    }

}
