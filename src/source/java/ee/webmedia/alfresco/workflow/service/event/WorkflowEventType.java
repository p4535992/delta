package ee.webmedia.alfresco.workflow.service.event;

/**
 * @author Alar Kvell
 */
public enum WorkflowEventType {
    STATUS_CHANGED, // Create and delete events are not supported
    WORKFLOW_STARTED_AUTOMATICALLY,
    CREATED,
    UPDATED
}
