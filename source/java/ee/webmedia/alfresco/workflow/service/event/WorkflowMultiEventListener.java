package ee.webmedia.alfresco.workflow.service.event;

public interface WorkflowMultiEventListener {

    void handleMultipleEvents(WorkflowEventQueue queue);

}
