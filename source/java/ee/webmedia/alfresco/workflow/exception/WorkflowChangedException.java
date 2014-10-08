package ee.webmedia.alfresco.workflow.exception;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
import ee.webmedia.alfresco.workflow.service.BaseWorkflowObject;

>>>>>>> develop-5.1
public class WorkflowChangedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private ErrorCause errorCause;
<<<<<<< HEAD
=======
    private final BaseWorkflowObject baseWorkflowObject;
>>>>>>> develop-5.1

    public enum ErrorCause {
        INDEPENDENT_WORKFLOW_REGISTRATION_NO_DOCUMENTS,
        INDEPENDENT_WORKFLOW_SIGNATURE_NO_DOCUMENTS
    }

<<<<<<< HEAD
    public WorkflowChangedException(String message) {
        super(message);
    }

    public WorkflowChangedException(String message, ErrorCause errorCause) {
        super(message);
        this.errorCause = errorCause;
    }
=======
    public WorkflowChangedException(String message,  BaseWorkflowObject baseWorkflowObject, ErrorCause errorCause) {
        super(message);
        this.baseWorkflowObject = baseWorkflowObject;
        this.errorCause = errorCause;
    }
    
    public WorkflowChangedException(String message,  BaseWorkflowObject baseWorkflowObject) {
        this(message, baseWorkflowObject, null);
    }
>>>>>>> develop-5.1

    public ErrorCause getErrorCause() {
        return errorCause;
    }

<<<<<<< HEAD
=======
    public String getShortMessage() {
        return super.getMessage() + (baseWorkflowObject != null ? (";nodeRef=" + baseWorkflowObject.getNodeRef()) : "");
    }

    @Override
    public String getMessage() {
        return super.getMessage() + "\n" + baseWorkflowObject;
    }

>>>>>>> develop-5.1
}
