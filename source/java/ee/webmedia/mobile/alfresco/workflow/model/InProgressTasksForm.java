package ee.webmedia.mobile.alfresco.workflow.model;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;

public class InProgressTasksForm {

    private Map<String, Task> inProgressTasks;
    private NodeRef compoundWorkflowRef;
    private NodeRef containerRef;
    private String phoneNumber;
    private Long signingFlowId;
    private String signingFlowView;
    private String mobileIdChallengeId;
    private Map<String, String> actions;

    public InProgressTasksForm() {
        inProgressTasks = new HashMap<String, Task>();
        actions = new HashMap<String, String>();
    }

    public InProgressTasksForm(Map<String, Task> inProgressTasks, NodeRef compoundWorkflowRef, NodeRef containerRef) {
        this.inProgressTasks = inProgressTasks;
        this.compoundWorkflowRef = compoundWorkflowRef;
        this.containerRef = containerRef;
    }

    public Map<String, Task> getInProgressTasks() {
        return inProgressTasks;
    }

    public void setInProgressTasks(Map<String, Task> inProgressTasks) {
        this.inProgressTasks = inProgressTasks;
    }

    public Task getTask(NodeRef taskRef) {
        return inProgressTasks.get(taskRef.toString());
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public NodeRef getCompoundWorkflowRef() {
        return compoundWorkflowRef;
    }

    public void setCompoundWorkflowRef(NodeRef compoundWorkflowRef) {
        this.compoundWorkflowRef = compoundWorkflowRef;
    }

    public NodeRef getContainerRef() {
        return containerRef;
    }

    public void setContainerRef(NodeRef containerRef) {
        this.containerRef = containerRef;
    }

    public Long getSigningFlowId() {
        return signingFlowId;
    }

    public void setSigningFlowId(Long signingFlowId) {
        this.signingFlowId = signingFlowId;
    }

    public void setSigningFlowView(String signingFlowView) {
        this.signingFlowView = signingFlowView;
    }

    public String getSigningFlowView() {
        return signingFlowView;
    }

    public Map<String, String> getActions() {
        return actions;
    }

    public void setActions(Map<String, String> actions) {
        this.actions = actions;
    }

    public String getMobileIdChallengeId() {
        return mobileIdChallengeId;
    }

    public void setMobileIdChallengeId(String mobileIdChallengeId) {
        this.mobileIdChallengeId = mobileIdChallengeId;
    }

}
