package ee.webmedia.alfresco.dvk.service;

import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.Task;

public class ReviewTaskException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    Task task; // conflicting task
    String dvkId = null; // dvkId from incoming dvk call
    ExceptionType type;
    String attemptedStatus = null;

    public static enum ExceptionType {
        VERSION_CONFLICT,
        PARSING_EXCEPTION,
        ORIGINAL_TASK_NOT_FOUND,
        TASK_OVERWRITE_WRONG_STATUS,
        EXTERNAL_REVIEW_DVK_CAPABILITY_ERROR,
        REVIEW_DVK_CAPABILITY_ERROR
    }

    public ReviewTaskException(ExceptionType type) {
        this.type = type;
    }

    public ReviewTaskException(ExceptionType type, Task task, String dvkId) {
        this.task = task;
        this.dvkId = dvkId;
        this.type = type;
    }

    public ReviewTaskException(ExceptionType type, Task task, String dvkId, String attemptedStatus) {
        this.task = task;
        this.dvkId = dvkId;
        this.type = type;
        this.attemptedStatus = attemptedStatus;
    }

    public ReviewTaskException(ExceptionType type, String message) {
        super(message);
        this.type = type;
    }

    public ReviewTaskException(ExceptionType exceptionType, RuntimeException e) {
        super(e);
        type = exceptionType;
    }

    public Task getTask() {
        return task;
    }

    public String getDvkId() {
        return dvkId;
    }

    public ExceptionType getExceptionType() {
        return type;
    }

    boolean isType(ExceptionType type) {
        return this.type.equals(type);
    }

    @Override
    public String getMessage() {
        if (isType(ExceptionType.VERSION_CONFLICT)) {
            return "Recieved dvkId=" + dvkId + " <= existing tasks's recievedDvkId="
                    + task.getProp(WorkflowSpecificModel.Props.ORIGINAL_DVK_ID) + "; task=" + task;
        } else if (isType(ExceptionType.TASK_OVERWRITE_WRONG_STATUS)) {
            return "Recieved task or local task is in wrong status for finishing, originalDvkId = " + dvkId + ", attemptedStatus=" + attemptedStatus + ", local task = " + task;
        } else if (isType(ExceptionType.ORIGINAL_TASK_NOT_FOUND)) {
            return "Recieved task has no corresponding task with originalDvkId=" + dvkId;
        } else {
            return super.getMessage();
        }
    }

}
