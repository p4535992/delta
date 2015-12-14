package ee.webmedia.mobile.alfresco.workflow.model;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

public class LockMessage {

    private NodeRef compoundWorkflowRef;
    private List<String> messages;

    public NodeRef getCompoundWorkflowRef() {
        return compoundWorkflowRef;
    }

    public void setCompoundWorkflowRef(NodeRef compoundWorkflowRef) {
        this.compoundWorkflowRef = compoundWorkflowRef;
    }


    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

}
