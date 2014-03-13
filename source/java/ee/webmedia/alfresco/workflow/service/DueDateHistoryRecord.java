package ee.webmedia.alfresco.workflow.service;

import java.io.Serializable;
import java.util.Date;

import org.alfresco.service.cmr.repository.NodeRef;

public class DueDateHistoryRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private String taskId;
    private Date previousDate;
    private String changeReason;
    private String extensionTaskId;
    private final NodeRef extensionWorkflowNodeRef;

    public DueDateHistoryRecord(String taskId, String changeReason, Date previousDate, String extensionTaskId, NodeRef extensionWorkflowNodeRef) {
        this.taskId = taskId;
        this.previousDate = previousDate;
        this.changeReason = changeReason;
        this.extensionTaskId = extensionTaskId;
        this.extensionWorkflowNodeRef = extensionWorkflowNodeRef;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Date getPreviousDate() {
        return previousDate;
    }

    public void setPreviousDate(Date previousDate) {
        this.previousDate = previousDate;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public String getExtensionTaskId() {
        return extensionTaskId;
    }

    public void setExtensionTaskId(String extensionTaskId) {
        this.extensionTaskId = extensionTaskId;
    }

    public NodeRef getExtensionWorkflowNodeRef() {
        return extensionWorkflowNodeRef;
    }

}
