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
