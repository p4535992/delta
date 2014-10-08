package ee.webmedia.alfresco.workflow.model;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import ee.webmedia.alfresco.common.web.BeanHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.context.MessageSource;
import org.springframework.web.util.HtmlUtils;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.workflow.service.DueDateHistoryRecord;
import ee.webmedia.alfresco.workflow.web.DueDateHistoryModalComponent;

public class WorkflowBlockItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("dd.MM.yyyy");
    private Integer rowNumber;
    private NodeRef taskNodeRef;
    private NodeRef workflowNodeRef;
    private NodeRef compoundWorkflowNodeRef;
    private String ownerName;
    private int indexInWorkflow = -1;
    private boolean raisedRights = false;
    private boolean separator = false;
    private boolean zebra = false;
    private boolean isGroupBlockItem = false;
    private Integer numberOfTasksInGroup;
    private String workflowGroupTasksUrl;
    private MessageSource messageSource;
    private Date startedDateTime;
    private Date completedDateTime;
    private Date dueDate;
    private String creatorName;
    private String taskType;
    private boolean isResponsible;
    private Date proposedDueDate;
    private String taskResolution;
    private String taskOutcome;
    private String ownerSubstituteName;
    private String taskComment;
    private String taskOwnerGroup;
    private String taskStatus;
    private List<DueDateHistoryRecord> dueDateHistoryRecords;

    private String workflowTypeMessage;
    private String workflowTypeCoResponsibleMessage;
    private String emptyTaskValueMessage;
    private String taskCompletedOutcomeMessage;
    private String substituteMessage;
    private String taskOutcomeMessage;
    private String taskOutcomeWithSubstituteNoteMessage;

    public WorkflowBlockItem(NodeRef compoundWorkflowNodeRef, NodeRef workflowNodeRef, NodeRef taskNodeRef, String ownerName, boolean isGroupBlockItem, boolean raisedRights) {
        this.compoundWorkflowNodeRef = compoundWorkflowNodeRef;
        this.workflowNodeRef = workflowNodeRef;
        this.taskNodeRef = taskNodeRef;
        this.ownerName = ownerName;
        this.isGroupBlockItem = isGroupBlockItem;
        this.raisedRights = raisedRights;
    }

    public Date getStartedDateTime() {
        return !isGroupBlockItem ? startedDateTime : null;
    }

    public Date getDueDate() {
        return !isGroupBlockItem ? dueDate : null;
    }

    public String getTaskCreatorName() {
        return !isGroupBlockItem ? creatorName : null;
    }

    public String getWorkflowType() {
        if (isCoResponsibleTask()) {
            if (workflowTypeCoResponsibleMessage == null) {
                workflowTypeCoResponsibleMessage = BeanHelper.getWorkflowConstantsBean().getAssignmentWorkflowCoOwnerMessage();
            }
            return workflowTypeCoResponsibleMessage;
        }
        if (workflowTypeMessage == null) {
            workflowTypeMessage = BeanHelper.getWorkflowConstantsBean().getWorkflowTypeNameByTask(taskType);
        }

        return workflowTypeMessage;
    }

    private boolean isCoResponsibleTask() {
        return WorkflowSpecificModel.Types.ASSIGNMENT_TASK.equals(taskType) && isResponsible;
    }

    public String getTaskOwnerName() {
        return ownerName;
    }

    public String getTaskResolution() {
        if (isGroupBlockItem) {
            return "";
        }
        if (WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK.getLocalName().equals(taskType)) {
            if (proposedDueDate == null && emptyTaskValueMessage == null) {
                emptyTaskValueMessage = BeanHelper.getWorkflowConstantsBean().getEmptyTaskValueMessage();
            }
            String taskDue = proposedDueDate != null ? DATE_FORMAT.format(proposedDueDate) : emptyTaskValueMessage;
            return MessageUtil.getMessage("task_due_date_extension_resolution", taskDue, taskResolution);
        }
        return taskResolution;
    }

    public Date getCompletedDateTime() {
        return completedDateTime;
    }

    public String getTaskOutcome() {
        if (isGroupBlockItem) {
            return "";
        }
        if (taskOutcomeMessage == null) {
            StringBuffer sb = new StringBuffer(200);
            if (StringUtils.isNotBlank(taskOutcome) && !isTaskCompletedOutcome()) {
                sb.append(HtmlUtils.htmlEscape(taskOutcome));
            }
            if (sb.length() > 0) {
                sb.append(" ");
            }
            if (getCompletedDateTime() != null) {
                sb.append(DateFormatUtils.format(getCompletedDateTime(), "dd.MM.yyyy"));
            }
            if (StringUtils.isNotBlank(getTaskComment())) {
                sb.append(": ").append(WebUtil.escapeHtmlExceptLinks(WebUtil.processLinks(getTaskComment())));
            }
            taskOutcomeMessage = sb.toString();
        }

        return taskOutcomeMessage;
    }

    private boolean isTaskCompletedOutcome() {
        return taskOutcome.equals(getTaskCompletedMessage());
    }

    private String getTaskCompletedMessage() {
        if (taskCompletedOutcomeMessage == null) {
            taskCompletedOutcomeMessage = MessageUtil.getMessage("task_completed_outcome");
        }
        return taskCompletedOutcomeMessage;
    }

    public String getCompoundWorkflowId() {
        return compoundWorkflowNodeRef.getId();
    }

    public String getTaskOutcomeWithSubstituteNote() {
        if (isGroupBlockItem) {
            return "";
        }

        if (taskOutcomeWithSubstituteNoteMessage == null) {
            taskOutcomeWithSubstituteNoteMessage = getTaskOutcome() + getSubstituteMessage();
        }

        return taskOutcomeWithSubstituteNoteMessage;
    }

    private String getSubstituteMessage() {
        if (substituteMessage == null) {
            if (StringUtils.isNotBlank(ownerSubstituteName)) {
                String message = " ";
                if (messageSource != null) {
                    message += messageSource.getMessage("workflow.task.substitute.summary", new String[] { ownerSubstituteName }, AppConstants.getDefaultLocale());
                } else {
                    message += MessageUtil.getMessage("task_substitute_summary", ownerSubstituteName);
                }
                substituteMessage = HtmlUtils.htmlEscape(message);
            } else {
                substituteMessage = "";
            }
        }

        return substituteMessage;
    }

    public String getTaskComment() {
        return taskComment;
    }

    public String getTaskStatus() {
        return !isGroupBlockItem ? taskStatus : "";
    }

    public String getOwnerGroup() {
        return taskOwnerGroup;
    }

    public boolean isRaisedRights() {
        return raisedRights;
    }

    public NodeRef getCompoundWorkflowNodeRef() {
        return compoundWorkflowNodeRef;
    }

    public void setSeparator(boolean isSeparator) {
        separator = isSeparator;
    }

    public boolean isSeparator() {
        return separator;
    }

    public void setZebra(boolean isZebra) {
        zebra = isZebra;
    }

    public boolean isZebra() {
        return zebra;
    }

    public String getSeparatorClass() {
        return separator ? "workflow-separator" : "";
    }

    public String getDueDateHistoryModalId() {
        if (isGroupBlockItem) {
            return "";
        }
        return DueDateHistoryModalComponent.getTaskExtensionHistoryModalId(taskNodeRef.getId());
    }

    public boolean isShowDueDateHistoryModal() {
        List<DueDateHistoryRecord> historyRecords = getDueDateHistoryRecords();
        return historyRecords != null && !historyRecords.isEmpty();
    }

    public String getDueDateHistoryAlert() {
        if (isGroupBlockItem) {
            return "";
        }
        StringBuilder sb = new StringBuilder("");
        List<DueDateHistoryRecord> dueDateHistoryRecords = getDueDateHistoryRecords();
        if (!dueDateHistoryRecords.isEmpty()) {
            sb.append("<a href=\"\" onclick=\"alert('");
            DateFormat dateFormat = new SimpleDateFormat("dd.M.yyyy");
            int recordCounter = 0;
            for (DueDateHistoryRecord historyRecord : dueDateHistoryRecords) {
                sb.append(StringEscapeUtils.escapeJavaScript(MessageUtil.getMessage("task_due_date_history_previous_date"))).append(" ");
                sb.append(StringEscapeUtils.escapeJavaScript(dateFormat.format(historyRecord.getPreviousDate())));
                sb.append(StringEscapeUtils.escapeJavaScript(MessageUtil.getMessage("task_due_date_history_change_reason"))).append(" ");
                sb.append(StringEscapeUtils.escapeJavaScript(historyRecord.getChangeReason()));
                recordCounter++;
                if (recordCounter < dueDateHistoryRecords.size()) {
                    sb.append("\\n");
                }
            }
            sb.append("');return false;\">" + StringEscapeUtils.escapeHtml(MessageUtil.getMessage("task_due_date_history_show_history_start")) + "&nbsp;"
                    + StringEscapeUtils.escapeHtml(MessageUtil.getMessage("task_due_date_history_show_history_end")) + "</a>");
        }
        return sb.toString();
    }

    public boolean isGroupBlockItem() {
        return isGroupBlockItem;
    }

    public String getGroupName() {
        return taskOwnerGroup;
    }

    public String getWorkflowGroupTasksUrl() {
        return workflowGroupTasksUrl;
    }

    public void setWorkflowGroupTasksUrl(String workflowGroupTasksUrl) {
        this.workflowGroupTasksUrl = workflowGroupTasksUrl;
    }

    public void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public List<DueDateHistoryRecord> getDueDateHistoryRecords() {
        return dueDateHistoryRecords;
    }

    public void setStartedDateTime(Date startedDateTime) {
        this.startedDateTime = startedDateTime;
    }

    public void setCompletedDateTime(Date completedDateTime) {
        this.completedDateTime = completedDateTime;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public void setResponsible(boolean isResponsible) {
        this.isResponsible = isResponsible;
    }

    public void setProposedDueDate(Date proposedDueDate) {
        this.proposedDueDate = proposedDueDate;
    }

    public void setTaskResolution(String taskResolution) {
        this.taskResolution = taskResolution;
    }

    public void setTaskOutcome(String taskOutcome) {
        this.taskOutcome = taskOutcome;
    }

    public void setTaskComment(String taskComment) {
        this.taskComment = taskComment;
    }

    public void setOwnerSubstituteName(String ownerSubstituteName) {
        this.ownerSubstituteName = ownerSubstituteName;
    }

    public void setTaskOwnerGroup(String taskOwnerGroup) {
        this.taskOwnerGroup = taskOwnerGroup;
    }

    public void setTaskStatus(String taskStatus) {
        this.taskStatus = taskStatus;
    }

    public void setDueDateHistoryRecords(List<DueDateHistoryRecord> dueDateHistoryRecords) {
        this.dueDateHistoryRecords = dueDateHistoryRecords;
    }

    public void setIndexInWorkflow(int indexInWorkflow) {
        this.indexInWorkflow = indexInWorkflow;
    }

    public int getIndexInWorkflow() {
        return indexInWorkflow;
    }

    public void setNumberOfTasksInGroup(Integer numberOfTasksInGroup) {
        this.numberOfTasksInGroup = numberOfTasksInGroup;
    }

    public Integer getNumberOfTasksInGroup() {
        return numberOfTasksInGroup;
    }

    public NodeRef getTaskNodeRef() {
        return taskNodeRef;
    }

    public NodeRef getWorkflowNodeRef() {
        return workflowNodeRef;
    }

    public void setWorkflowNodeRef(NodeRef workflowNodeRef) {
        this.workflowNodeRef = workflowNodeRef;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }
}
