package ee.webmedia.alfresco.workflow.model;

import java.io.Serializable;
import java.util.Date;

import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

/**
 * Wrapper class for displaying compound workflow info in list
 */
public class CompoundWorkflowWithObject implements Serializable {

    private static final long serialVersionUID = 1L;
    private final CompoundWorkflow compoundWorkflow;
    private String objectTitle;
    private String workflowStatus;
    private NodeRef parent;

    public CompoundWorkflowWithObject(CompoundWorkflow compoundWorkflow) {
        Assert.notNull(compoundWorkflow);
        this.compoundWorkflow = compoundWorkflow;
    }

    public String getTitle() {
        return compoundWorkflow.getTitle();
    }

    public void setObjectTitle(String objectTitle) {
        this.objectTitle = objectTitle;
    }

    public String getObjectTitle() {
        if (compoundWorkflow.isDocumentWorkflow() || compoundWorkflow.isCaseFileWorkflow()) {
            return objectTitle;
        }
        return WorkflowUtil.getCompoundWorkflowDocMsg(compoundWorkflow.getNumberOfDocuments(), MessageUtil.getMessage("compound_workflow_independent_no_documents"));
    }

    public String getStatus() {
        return compoundWorkflow.getStatus();
    }

    public CompoundWorkflow getCompoundWorkflow() {
        return compoundWorkflow;
    }

    public Date getStartedDate() {
        return compoundWorkflow.getStartedDateTime();
    }

    public String getStartedDateStr() {
        return compoundWorkflow.getStartedDateStr();
    }

    public Date getCreatedDateTime() {
        return compoundWorkflow.getCreatedDateTime();
    }

    public String getWorkflowStatus() {
        return workflowStatus;
    }

    public void setWorkflowStatus(String workflowStatus) {
        this.workflowStatus = workflowStatus;
    }

    public NodeRef getParent() {
        return parent;
    }

    public void setParent(NodeRef parent) {
        this.parent = parent;
    }

}
