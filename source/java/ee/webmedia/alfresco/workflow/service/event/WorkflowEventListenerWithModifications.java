package ee.webmedia.alfresco.workflow.service.event;

public interface WorkflowEventListenerWithModifications {

    void handle(WorkflowEvent event, WorkflowModifications workflowService, WorkflowEventQueue queue);

}
