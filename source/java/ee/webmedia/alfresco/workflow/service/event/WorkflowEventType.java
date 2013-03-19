package ee.webmedia.alfresco.workflow.service.event;

/**
 * @author Alar Kvell
 */
public enum WorkflowEventType {
    STATUS_CHANGED, // Create and delete events are not supported
    WORKFLOW_STARTED_AUTOMATICALLY,
    /**
     * Fired when independent workflow is trying to start automatically registering or signature workflow,
     * but doesn't have any associated documents, so compound workflow is stopped instead
     */
    WORKFLOW_STOPPED_AUTOMATICALLY,
    WORKFLOW_CHECK_EXTERNAL_REVIEW, // used to send one event if multiple workflows and/or tasks are changed
    CREATED,
    /** NB! Currently this event is not queued for tasks (due to performance issues) */
    UPDATED
}
