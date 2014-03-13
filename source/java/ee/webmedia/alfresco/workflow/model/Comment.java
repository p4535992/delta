package ee.webmedia.alfresco.workflow.model;

import java.util.Date;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.model.NodeBaseVO;
import ee.webmedia.alfresco.workflow.service.Task;

public class Comment extends NodeBaseVO {

    private static final long serialVersionUID = 1L;
    private long commentId = -1;
    private String compoundWorkflowId;
    private Date created;
    private String creatorId;
    private String creatorName;
    private String commentText;
    // used to access comments in unsaved workflow
    private int indexInWorkflow = -1;

    public Comment(String compoundWorkflowId, Date created, String creatorId, String creatorName, String commentText) {
        Assert.isTrue(StringUtils.isNotBlank(creatorName));
        Assert.isTrue(StringUtils.length(commentText) > 0);
        this.compoundWorkflowId = compoundWorkflowId;
        this.created = created;
        this.creatorId = creatorId;
        this.creatorName = creatorName;
        this.commentText = commentText;
    }

    public String getCompoundWorkflowId() {
        return compoundWorkflowId;
    }

    public void setCompoundWorkflowId(String compoundWorkflowId) {
        this.compoundWorkflowId = compoundWorkflowId;
    }

    public Date getCreated() {
        return created;
    }

    public String getCreatedStr() {
        Date createdDateTime = getCreated();
        return createdDateTime != null ? Task.dateTimeFormat.format(createdDateTime) : "";
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getCommentText() {
        return commentText;
    }

    public String getCommentTextEscapeJs() {
        return StringEscapeUtils.escapeJavaScript(StringEscapeUtils.escapeHtml(getCommentText()));
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public boolean isShowEditLink() {
        return AuthenticationUtil.getRunAsUser().equals(creatorId);
    }

    public int getIndexInWorkflow() {
        return indexInWorkflow;
    }

    public void setIndexInWorkflow(int indexInWorkflow) {
        this.indexInWorkflow = indexInWorkflow;
    }

    public long getCommentId() {
        return commentId;
    }

    public void setCommentId(long commentId) {
        this.commentId = commentId;
    }

}
