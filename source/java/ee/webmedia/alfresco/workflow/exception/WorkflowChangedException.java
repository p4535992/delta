package ee.webmedia.alfresco.workflow.exception;

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
}
