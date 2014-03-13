package ee.webmedia.alfresco.workflow.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

public class CompoundWorkflow extends BaseWorkflowObject implements Serializable {
    private static final long serialVersionUID = 1L;

    private final NodeRef parent;
    private final List<Workflow> workflows = new ArrayList<Workflow>();
    private List<CompoundWorkflow> otherCompoundWorkflows = new ArrayList<CompoundWorkflow>();
    private final List<Workflow> removedWorkflows = new ArrayList<Workflow>();

    public CompoundWorkflow(WmNode node, NodeRef parent) {
        super(node);
        Assert.notNull(parent);
        this.parent = parent;
    }

    public CompoundWorkflow copy() {
        return copyImpl(new CompoundWorkflow(getNode().clone(), parent));
    }

    @Override
    protected <T extends BaseWorkflowObject> T copyImpl(T copy) {
        CompoundWorkflow compoundWorkflow = (CompoundWorkflow) super.copyImpl(copy);
        for (Workflow workflow : workflows) {
            compoundWorkflow.workflows.add(workflow.copy(compoundWorkflow));
        }
        for (Workflow removedWorkflow : removedWorkflows) {
            compoundWorkflow.removedWorkflows.add(removedWorkflow.copy(compoundWorkflow));
        }
        @SuppressWarnings("unchecked")
        T result = (T) compoundWorkflow;
        return result;
    }

    public NodeRef getParent() {
        return parent;
    }

    public List<Workflow> getWorkflows() {
        return Collections.unmodifiableList(workflows);
    }

    /**
     * NB! At moment it is not quaranteed that this property contains updated info from repo,
     * it is used only to pass (possibly changed) compound workflows to and from AssignmentWorkflowType.
     * Returned list doesn't (and MUST NOT) contain this compound workflow.
     * 
     * @return
     */
    public List<CompoundWorkflow> getOtherCompoundWorkflows() {
        return otherCompoundWorkflows;
    }

    public void setOtherCompoundWorkflows(List<CompoundWorkflow> otherCompoundWorkflows) {
        Assert.notNull(otherCompoundWorkflows);
        this.otherCompoundWorkflows = otherCompoundWorkflows;
    }

    protected List<Workflow> getRemovedWorkflows() {
        return removedWorkflows;
    }

    public void removeWorkflow(int index) {
        removedWorkflows.add(workflows.remove(index));
    }

    public void addWorkflow(Workflow workflow) {
        workflows.add(workflow);
    }

    protected void addWorkflow(Workflow workflow, int index) {
        workflows.add(index, workflow);
    }

    @Override
    public String getOwnerId() {
        return getProp(WorkflowCommonModel.Props.OWNER_ID);
    }

    @Override
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
