package ee.webmedia.mobile.alfresco.workflow.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

/**
 * Model for lightweight task representation.
 */
public class Task implements Comparable<Task>, Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String kind;
    private QName type;
    private String resolution;
    private String creatorName;
    private String senderName;
    private Date dueDate;
    private boolean viewedByOwner;
    private NodeRef nodeRef;
    private NodeRef compoundWorkflowRef;
    private String comment;
    private Map<String, String> actions;
    // property from parent workflow
    private boolean isSignTogether;
    private Integer reviewTaskOutcome;
    private String commentLabel;
    private String typeStr;
    private List<TaskFile> files;
    private Date completedDateTime;
    private String ownerNameWithSubstitute;
    private String commentAndLinks;

    public Task() {
        actions = new HashMap<String, String>();
    }

    public Task(ee.webmedia.alfresco.workflow.service.Task task) {
        setNodeRef(task.getNodeRef());
        setComment(task.getComment());
        resolution = task.getResolution();
        dueDate = task.getDueDate();
        type = task.getType();
        actions = new HashMap<String, String>();
        nodeRef = task.getNodeRef();
    }

    public boolean isOverDue() {
        return dueDate != null && new Date().after(dueDate);
    }

    @Override
    public int compareTo(Task other) {
        if (other == null || other.getDueDate() == null) {
            return -1;
        }

        if (dueDate == null) {
            return 1;
        }

        return other.getDueDate().compareTo(dueDate);
    }

    @Override
    public String toString() {
        return "Task [id=" + id + ", title=" + title + ", kind=" + kind + ", type=" + type + ", resolution=" + resolution + ", creatorName=" + creatorName + ", senderName="
                + senderName + ", dueDate=" + dueDate + ", viewedByOwner=" + viewedByOwner + "]";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public QName getType() {
        return type;
    }

    public void setType(QName type) {
        this.type = type;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public boolean isViewedByOwner() {
        return viewedByOwner;
    }

    public void setViewedByOwner(boolean viewedByOwner) {
        this.viewedByOwner = viewedByOwner;
    }

    public NodeRef getCompoundWorkflowRef() {
        return compoundWorkflowRef;
    }

    public void setCompoundWorkflowRef(NodeRef compoundWorkflowRef) {
        this.compoundWorkflowRef = compoundWorkflowRef;
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(NodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Map<String, String> getActions() {
        return actions;
    }

    public void setActions(Map<String, String> actions) {
        this.actions = actions;
    }

    public boolean isSignTogether() {
        return isSignTogether;
    }

    public void setSignTogether(boolean isSignTogether) {
        this.isSignTogether = isSignTogether;
    }

    public Integer getReviewTaskOutcome() {
        return reviewTaskOutcome;
    }

    public void setReviewTaskOutcome(Integer reviewTaskOutcome) {
        this.reviewTaskOutcome = reviewTaskOutcome;
    }

    public String getCommentLabel() {
        return commentLabel;
    }

    public void setCommentLabel(String commentLabel) {
        this.commentLabel = commentLabel;
    }

    public String getTypeStr() {
        return typeStr;
    }

    public void setTypeStr(String typeStr) {
        this.typeStr = typeStr;
    }

    public List<TaskFile> getFiles() {
        return files;
    }

    public void setFiles(List<TaskFile> files) {
        this.files = files;
    }

    public Date getCompletedDateTime() {
        return completedDateTime;
    }

    public void setCompletedDateTime(Date completedDateTime) {
        this.completedDateTime = completedDateTime;
    }

    public String getOwnerNameWithSubstitute() {
        return ownerNameWithSubstitute;
    }

    public void setOwnerNameWithSubstitute(String ownerNameWithSubstitute) {
        this.ownerNameWithSubstitute = ownerNameWithSubstitute;
    }

    public String getCommentAndLinks() {
        return commentAndLinks;
    }

    public void setCommentAndLinks(String commentAndLinks) {
        this.commentAndLinks = commentAndLinks;
    }

}
