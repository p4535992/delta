package ee.webmedia.alfresco.workflow.service.event;

/**
 * @author Riina Tens
 */
public interface WorkflowMultiEventListener {

    void handleMultipleEvents(WorkflowEventQueue queue);

}
