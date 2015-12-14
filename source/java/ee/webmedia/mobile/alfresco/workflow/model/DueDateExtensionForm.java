package ee.webmedia.mobile.alfresco.workflow.model;

import java.io.Serializable;
import java.util.Date;

import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.format.annotation.DateTimeFormat;

public class DueDateExtensionForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @DateTimeFormat(pattern = "dd.MM.yyyy")
    private Date newDueDate;
    @DateTimeFormat(pattern = "dd.MM.yyyy")
    private Date extensionDueDate;
    private String reason;
    private String userId;
    private String userName;
    private String initialExtensionDueDate;
    private NodeRef compoundWorkflowRef;

    public Date getNewDueDate() {
        return newDueDate;
    }

    public void setNewDueDate(Date newDueDate) {
        this.newDueDate = newDueDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Date getExtensionDueDate() {
        return extensionDueDate;
    }

    public void setExtensionDueDate(Date extensionDueDate) {
        this.extensionDueDate = extensionDueDate;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getInitialExtensionDueDate() {
        return initialExtensionDueDate;
    }

    public void setInitialExtensionDueDate(String initialExtensionDueDate) {
        this.initialExtensionDueDate = initialExtensionDueDate;
    }
    
    public NodeRef getCompoundWorkflowRef() {
        return compoundWorkflowRef;
    }

    public void setCompoundWorkflowRef(NodeRef compoundWorkflowRef) {
        this.compoundWorkflowRef = compoundWorkflowRef;
    }

}
