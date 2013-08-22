package ee.webmedia.alfresco.workflow.model;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.springframework.web.util.HtmlUtils;

import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.workflow.service.Task;

/**
 * @author Kaarel Jõgeva
 */
public class WorkflowBlockItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Task task;
    private boolean raisedRights = false;
    private boolean separator = false;
    private boolean zebra = false;

    public WorkflowBlockItem(Task task, boolean raisedRights) {
        this.task = task;
        this.raisedRights = raisedRights;
    }

    public WorkflowBlockItem(boolean separatorAndNotZebra) {
        task = null;
        separator = separatorAndNotZebra;
        zebra = !separatorAndNotZebra;
    }

    public Date getStartedDateTime() {
        return task.getStartedDateTime();
    }

    public Date getDueDate() {
        return task.getDueDate();
    }

    public String getTaskCreatorName() {
        return task.getCreatorName();
    }

    public String getWorkflowType() {
        if (isResponsibleTask()) {
            return MessageUtil.getMessage("assignmentWorkflow_coOwner");
        }
        return MessageUtil.getMessage(task.getParent().getType().getLocalName());
    }

    private boolean isResponsibleTask() {
        return WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW.equals(task.getParent().getType()) && !task.isResponsible();
    }

    public String getTaskOwnerName() {
        return task.getOwnerName();
    }

    public String getTaskResolution() {
        return task.getResolution();
    }

    public Date getCompletedDateTime() {
        return task.getCompletedDateTime();
    }

    public int getWorkflowIndex() {
        return task.getWorkflowIndex();
    }

    public int getTaskIndexInWorkflow() {
        return task.getTaskIndexInWorkflow();
    }

    public String getTaskOutcome() {
        StringBuffer sb = new StringBuffer(200);
        if (StringUtils.isNotBlank(task.getOutcome())) {
            sb.append(HtmlUtils.htmlEscape(task.getOutcome()));
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

    public String getTaskComment() {
        return task.getComment();
    }

    public String getTaskStatus() {
        return task.getStatus();
    }

    public boolean isRaisedRights() {
        return raisedRights;
    }

    public NodeRef getCompoundWorkflowNodeRef() {
        return task.getParent().getParent().getNode().getNodeRef();
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
        // in case of assignment tasks, responsible tasks come first
        chain.addComparator(new TransformingComparator(new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((WorkflowBlockItem) input).isResponsibleTask() ? 1 : 0;
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
