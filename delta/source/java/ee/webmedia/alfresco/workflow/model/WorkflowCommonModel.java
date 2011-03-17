package ee.webmedia.alfresco.workflow.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Alar Kvell
 */
public interface WorkflowCommonModel {
    String URI = "http://alfresco.webmedia.ee/model/workflow/common/1.0";
    String PREFIX = "wfc:";

    interface Repo {
        String WORKFLOWS_PARENT = "/";
        String WORKFLOWS_SPACE = WORKFLOWS_PARENT + PREFIX + "compoundWorkflowDefinitions";
    }

    interface Types {
        QName COMPOUND_WORKFLOW_DEFINITIONS_ROOT = QName.createQName(URI, "compoundWorkflowDefinitions");
        QName COMPOUND_WORKFLOW_DEFINITION = QName.createQName(URI, "compoundWorkflowDefinition");
        QName COMPOUND_WORKFLOW = QName.createQName(URI, "compoundWorkflow");
        QName WORKFLOW = QName.createQName(URI, "workflow");
        QName TASK = QName.createQName(URI, "task");

        QName DELEGATION_HISTORY = QName.createQName(URI, "delegationHistory");
    }

    interface Assocs {
        QName COMPOUND_WORKFLOW_DEFINITION = QName.createQName(URI, "compoundWorkflowDefinition");
        QName COMPOUND_WORKFLOW = QName.createQName(URI, "compoundWorkflow");
        QName WORKFLOW = QName.createQName(URI, "workflow");
        QName TASK = QName.createQName(URI, "task");
        QName DELEGATION_HISTORY = QName.createQName(URI, "delegationHistory");
    }

    interface Props {
        // Common fields
        /**
         * All values for this property are defined in Enum {@link Status}
         */
        QName STATUS = QName.createQName(URI, "status");
        QName CREATOR_NAME = QName.createQName(URI, "creatorName");
        QName STARTED_DATE_TIME = QName.createQName(URI, "startedDateTime");
        QName STOPPED_DATE_TIME = QName.createQName(URI, "stoppedDateTime");
        QName OWNER_ID = QName.createQName(URI, "ownerId"); // username ehk isikukood
        QName OWNER_NAME = QName.createQName(URI, "ownerName");

        // Compound workflow definition
        QName NAME = QName.createQName(URI, "name");
        QName DOCUMENT_TYPES = QName.createQName(URI, "documentTypes");

        // Workflow
        QName PARALLEL_TASKS = QName.createQName(URI, "parallelTasks");
        QName STOP_ON_FINISH = QName.createQName(URI, "stopOnFinish");

        // Task
        QName OWNER_EMAIL = QName.createQName(URI, "ownerEmail");
        QName OUTCOME = QName.createQName(URI, "outcome");
        QName COMPLETED_DATE_TIME = QName.createQName(URI, "completedDateTime");
        QName OWNER_ORGANIZATION_NAME = QName.createQName(URI, "ownerOrganizationName");
        QName OWNER_JOB_TITLE = QName.createQName(URI, "ownerJobTitle");

        QName DELEG_HIST_CREATOR_NAME = QName.createQName(URI, "delegHistCreatorName");
        QName DELEG_HIST_OWNER_NAME = QName.createQName(URI, "delegHistOwnerName");
        QName DELEG_HIST_RESOLUTION = QName.createQName(URI, "delegHistResolution");
        QName DELEG_HIST_CO_ASSIGNMENT_TASKS = QName.createQName(URI, "delegHistCoAssignmentTasks");
        QName DELEG_HIST_DUE_DATE = QName.createQName(URI, "delegHistDueDate");
    }

}
