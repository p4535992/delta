package ee.webmedia.mobile.alfresco.workflow.model;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

public class DelegationMessagesRequest {

    private NodeRef compoundWorkflowRef;
    private List<String> dueDates;
    private List<String> taskOwners;
    private List<String> messages;
    private String taskType;

    public NodeRef getCompoundWorkflowRef() {
        return compoundWorkflowRef;
    }

    public void setCompoundWorkflowRef(NodeRef compoundWorkflowRef) {
        this.compoundWorkflowRef = compoundWorkflowRef;
    }

    public List<String> getDueDates() {
        return dueDates;
    }

    public void setDueDates(List<String> dueDates) {
        this.dueDates = dueDates;
    }

    public List<String> getTaskOwners() {
        return taskOwners;
    }

    public void setTaskOwners(List<String> taskOwners) {
        this.taskOwners = taskOwners;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

}
