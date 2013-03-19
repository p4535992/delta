package ee.webmedia.alfresco.workflow.exception;

/**
 * @author Alar Kvell
 */
public class WorkflowChangedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private ErrorCause errorCause;

    public enum ErrorCause {
        INDEPENDENT_WORKFLOW_REGISTRATION_NO_DOCUMENTS,
        INDEPENDENT_WORKFLOW_SIGNATURE_NO_DOCUMENTS
    }

    public WorkflowChangedException(String message) {
        super(message);
    }

    public WorkflowChangedException(String message, ErrorCause errorCause) {
        super(message);
        this.errorCause = errorCause;
    }

    public ErrorCause getErrorCause() {
        return errorCause;
    }

}
