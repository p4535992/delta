package ee.webmedia.alfresco.workflow.service.event;

/**
 * @author Alar Kvell
 */
public interface WorkflowEventListenerWithModifications {

    void handle(WorkflowEvent event, WorkflowModifications workflowService, WorkflowEventQueue queue);

}
