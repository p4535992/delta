package ee.webmedia.alfresco.workflow.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 * @author Alar Kvell
 */
public class CompoundWorkflow extends BaseWorkflowObject implements Serializable {
    private static final long serialVersionUID = 1L;

    private NodeRef parent;
    private List<Workflow> workflows = new ArrayList<Workflow>();
    private List<Workflow> removedWorkflows = new ArrayList<Workflow>();

    protected CompoundWorkflow(WmNode node, NodeRef parent) {
        super(node);
        Assert.notNull(parent);
        this.parent = parent;
    }

    public NodeRef getParent() {
        return parent;
    }

    public List<Workflow> getWorkflows() {
        return Collections.unmodifiableList(workflows);
    }

    protected List<Workflow> getRemovedWorkflows() {
        return removedWorkflows;
    }

    public void removeWorkflow(int index) {
        removedWorkflows.add(workflows.remove(index));
    }

    protected void addWorkflow(Workflow workflow) {
        workflows.add(workflow);
    }

    protected void addWorkflow(Workflow workflow, int index) {
        workflows.add(index, workflow);
    }

    public String getOwnerId() {
        return getProp(WorkflowCommonModel.Props.OWNER_ID);
    }

    public void setOwnerId(String ownerId) {
        setProp(WorkflowCommonModel.Props.OWNER_ID, ownerId);
    }

    public String getOwnerName() {
        return getProp(WorkflowCommonModel.Props.OWNER_NAME);
    }

    public void setOwnerName(String ownerName) {
        setProp(WorkflowCommonModel.Props.OWNER_NAME, ownerName);
    }

    @Override
    protected String additionalToString() {
        return "\n  parent=" + getParent() + "\n  workflows=" + WmNode.toString(getWorkflows()) + "\n  removedWorkflows="
                + WmNode.toString(getRemovedWorkflows());
    }

}
