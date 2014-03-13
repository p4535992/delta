package ee.webmedia.alfresco.workflow.exception;

import ee.webmedia.alfresco.workflow.service.BaseWorkflowObject;

public class WorkflowChangedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private ErrorCause errorCause;
    private final BaseWorkflowObject baseWorkflowObject;

    public enum ErrorCause {
        INDEPENDENT_WORKFLOW_REGISTRATION_NO_DOCUMENTS,
        INDEPENDENT_WORKFLOW_SIGNATURE_NO_DOCUMENTS
    }

    public WorkflowChangedException(String message,  BaseWorkflowObject baseWorkflowObject, ErrorCause errorCause) {
        super(message);
        this.baseWorkflowObject = baseWorkflowObject;
        this.errorCause = errorCause;
    }
    
    public WorkflowChangedException(String message,  BaseWorkflowObject baseWorkflowObject) {
        this(message, baseWorkflowObject, null);
    }

    public ErrorCause getErrorCause() {
        return errorCause;
    }

    public String getShortMessage() {
        return super.getMessage() + (baseWorkflowObject != null ? (";nodeRef=" + baseWorkflowObject.getNodeRef()) : "");
    }

    @Override
    public String getMessage() {
        return super.getMessage() + "\n" + baseWorkflowObject;
    }

}
