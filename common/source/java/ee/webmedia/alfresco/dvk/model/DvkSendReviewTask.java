package ee.webmedia.alfresco.dvk.model;

public interface DvkSendReviewTask extends DvkSendWorkflowDocuments {

    String getInstitutionName();

    void setInstitutionName(String institutionName);

    String getSenderName();

    void setSenderName(String senderName);

}
