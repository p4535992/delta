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

    public WorkflowBlockItem(Task task, boolean raisedRights) {
        this.task = task;
        this.raisedRights = raisedRights;
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
        return MessageUtil.getMessage(task.getParent().getType().getLocalName());
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
                return ((WorkflowBlockItem) input).getDueDate();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((WorkflowBlockItem) input).getTaskOwnerName();
            }
        }, new NullComparator()));
        COMPARATOR = chain;
    }
}
