package ee.webmedia.alfresco.workflow.service.event;

public enum WorkflowEventType {
    STATUS_CHANGED, // Create and delete events are not supported
    WORKFLOW_STARTED_AUTOMATICALLY,
    WORKFLOW_CHECK_EXTERNAL_REVIEW, // used to send one event if multiple workflows and/or tasks are changed
    CREATED,
    UPDATED
}
