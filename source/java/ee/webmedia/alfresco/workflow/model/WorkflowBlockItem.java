package ee.webmedia.alfresco.workflow.model;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.util.Assert;
import org.springframework.web.util.HtmlUtils;

import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.workflow.service.DueDateHistoryRecord;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.web.DueDateHistoryModalComponent;

public class WorkflowBlockItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("dd.MM.yyyy");
    private final Task task;
    private List<WorkflowBlockItem> groupItems;
    private String groupName;
    private int groupWorkflowIndex = -1;
    private boolean raisedRights = false;
    private boolean separator = false;
    private boolean zebra = false;
    private boolean isGroupBlockItem = false;
    private String workflowGroupTasksUrl;

    public WorkflowBlockItem(Task task, boolean raisedRights) {
        this.task = task;
        this.raisedRights = raisedRights;
        isGroupBlockItem = false;
    }

    public WorkflowBlockItem(String groupName, int groupWorkflowIndex, boolean raisedRights) {
        Assert.isTrue(StringUtils.isNotBlank(groupName));
        task = null;
        this.groupName = groupName;
        this.groupWorkflowIndex = groupWorkflowIndex;
        this.raisedRights = raisedRights;
        isGroupBlockItem = true;
    }

    public WorkflowBlockItem(boolean separatorAndNotZebra) {
        task = null;
        separator = separatorAndNotZebra;
        zebra = !separatorAndNotZebra;
    }

    public List<WorkflowBlockItem> getGroupItems() {
        if (groupItems == null) {
            groupItems = new ArrayList<WorkflowBlockItem>();
        }
        return groupItems;
    }

    public Date getStartedDateTime() {
        return !isGroupBlockItem ? task.getStartedDateTime() : null;
    }

    public Date getDueDate() {
        return !isGroupBlockItem ? task.getDueDate() : null;
    }

    public String getTaskCreatorName() {
        return !isGroupBlockItem ? task.getCreatorName() : null;
    }

    public String getWorkflowType() {
        if (isCoResponsibleTask()) {
            return MessageUtil.getMessage("assignmentWorkflow_coOwner");
        }
        Task tmpTask = task != null ? task : groupItems.get(0).getTask();
        return MessageUtil.getMessage(tmpTask.getParent().getType().getLocalName());
    }

    public Task getTask() {
        return task;
    }

    private boolean isCoResponsibleTask() {
        return task != null ? WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW.equals(task.getParent().getType()) && !task.isResponsible() : false;
    }

    public String getTaskOwnerName() {
        return isGroupBlockItem ? groupName : task.getOwnerName();
    }

    public String getTaskResolution() {
        if (isGroupBlockItem) {
            return "";
        }
        if (task.isType(WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK)) {
            Date proposedDueDate = task.getProposedDueDate();
            return MessageUtil.getMessage("task_due_date_extension_resolution",
                    proposedDueDate != null ? DATE_FORMAT.format(proposedDueDate) : MessageUtil.getMessage("task_empty_value"), task.getResolution());
        }
        return task.getResolution();
    }

    public Date getCompletedDateTime() {
        return task.getCompletedDateTime();
    }

    public int getWorkflowIndex() {
        return isGroupBlockItem ? groupWorkflowIndex : task.getWorkflowIndex();
    }

    public int getTaskIndexInWorkflow() {
        Integer taskIndexInWorkflow = task.getTaskIndexInWorkflow();
        return taskIndexInWorkflow != null ? taskIndexInWorkflow : -1;
    }

    public String getTaskOutcome() {
        if (isGroupBlockItem) {
            return "";
        }
        StringBuffer sb = new StringBuffer(200);
        String outcome = task.getOutcome();
        if (StringUtils.isNotBlank(outcome) && !outcome.equals(MessageUtil.getMessage("task_completed_outcome"))) {
            sb.append(HtmlUtils.htmlEscape(outcome));
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
        return sb.toString();
    }

    public String getTaskOutcomeWithSubstituteNote() {
        if (isGroupBlockItem) {
            return "";
        }
        String substitute = task.getOwnerSubstituteName();
        return getTaskOutcome() + (StringUtils.isNotBlank(substitute) ? " " + MessageUtil.getMessage("task_substitute_summary", substitute) : "");
    }

    public String getTaskComment() {
        return task.getComment();
    }

    public String getTaskStatus() {
        return !isGroupBlockItem ? task.getStatus() : "";
    }

    public String getOwnerGroup() {
        return task.getOwnerGroup();
    }

    public boolean isRaisedRights() {
        return raisedRights;
    }

    public NodeRef getCompoundWorkflowNodeRef() {
        Task tmpTask = task != null ? task : groupItems.get(0).getTask();
        return tmpTask.getParent().getParent().getNodeRef();
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

    public boolean isTask() {
        return !zebra && !separator;
    }

    public List<DueDateHistoryRecord> getDueDateHistoryRecords() {
        if (isGroupBlockItem) {
            return null;
        }
        return task.getDueDateHistoryRecords();
    }

    public String getDueDateHistoryModalId() {
        if (isGroupBlockItem) {
            return "";
        }
        return DueDateHistoryModalComponent.getTaskExtensionHistoryModalId(task.getNodeRef().getId());
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
        List<DueDateHistoryRecord> dueDateHistoryRecords = task.getDueDateHistoryRecords();
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
        return groupName;
    }

    public String getWorkflowGroupTasksUrl() {
        return workflowGroupTasksUrl;
    }

    public void setWorkflowGroupTasksUrl(String workflowGroupTasksUrl) {
        this.workflowGroupTasksUrl = workflowGroupTasksUrl;
    }

    public int getGroupWorkflowIndex() {
        return groupWorkflowIndex;
    }

    public static final Comparator<WorkflowBlockItem> COMPARATOR;
    static {
        // ComparatorChain is not thread-safe at construction time, but it is thread-safe to perform multiple comparisons after all the setup operations are
        // complete.
        ComparatorChain chain = new ComparatorChain();
        chain.addComparator(new TransformingComparator(new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((WorkflowBlockItem) input).getStartedDateTime();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((WorkflowBlockItem) input).getWorkflowIndex();
            }
        }, new NullComparator()));
        // in case of assignment ant order assignment tasks, responsible tasks come first
        chain.addComparator(new TransformingComparator(new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((WorkflowBlockItem) input).isCoResponsibleTask() ? 1 : 0;
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((WorkflowBlockItem) input).getTaskIndexInWorkflow();
            }
        }, new NullComparator()));
        COMPARATOR = chain;
    }
}
