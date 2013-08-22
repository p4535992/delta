package ee.webmedia.alfresco.workflow.service.event;

/**
 * @author Alar Kvell
 */
public interface WorkflowEventListener {

    void handle(WorkflowEvent event, WorkflowEventQueue queue);

}
