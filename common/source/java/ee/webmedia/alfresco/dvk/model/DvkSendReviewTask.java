<<<<<<< HEAD
package ee.webmedia.alfresco.dvk.model;

/**
 * @author Riina Tens
 */
public interface DvkSendReviewTask extends DvkSendWorkflowDocuments {

    String getInstitutionName();

    void setInstitutionName(String institutionName);

    String getSenderName();

    void setSenderName(String senderName);

}
=======
package ee.webmedia.alfresco.dvk.model;

public class DvkSendReviewTask extends DvkSendWorkflowDocuments {

    private String institutionName;
    private String senderName;
    private String workflowTitle;
    private String taskId;

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    @Override
    public boolean isDocumentNode() {
        return false;
    }

    public void setWorkflowTitle(String workflowTitle) {
        this.workflowTitle = workflowTitle;
    }

    public String getWorkflowTitle() {
        return workflowTitle;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
}
>>>>>>> develop-5.1
