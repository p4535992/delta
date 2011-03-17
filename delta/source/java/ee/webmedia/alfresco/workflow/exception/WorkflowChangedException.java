package ee.webmedia.alfresco.workflow.exception;

/**
 * @author Alar Kvell
 */
public class WorkflowChangedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public WorkflowChangedException(String message) {
        super(message);
    }

}
