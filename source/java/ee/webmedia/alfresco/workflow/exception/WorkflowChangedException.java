package ee.webmedia.alfresco.workflow.exception;

<<<<<<< HEAD
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

=======
import ee.webmedia.alfresco.workflow.service.BaseWorkflowObject;

public class WorkflowChangedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final BaseWorkflowObject baseWorkflowObject;

    public WorkflowChangedException(String message, BaseWorkflowObject baseWorkflowObject) {
        super(message);
        this.baseWorkflowObject = baseWorkflowObject;
    }

    public String getShortMessage() {
        return super.getMessage() + (baseWorkflowObject != null ? (";nodeRef=" + baseWorkflowObject.getNodeRef()) : "");
    }

    @Override
    public String getMessage() {
        return super.getMessage() + "\n" + baseWorkflowObject;
    }
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}
