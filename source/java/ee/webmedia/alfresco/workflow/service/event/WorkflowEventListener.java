package ee.webmedia.alfresco.workflow.service.event;

public interface WorkflowEventListener {

    void handle(WorkflowEvent event, WorkflowEventQueue queue);

}