package ee.webmedia.alfresco.dvk.model;

public class DvkSendReviewTaskImpl extends DvkSendWorkflowDocumentsImpl implements DvkSendReviewTask {

    private String institutionName;
    private String senderName;

    @Override
    public String getInstitutionName() {
        return institutionName;
    }

    @Override
    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    @Override
    public String getSenderName() {
        return senderName;
    }

    @Override
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    @Override
    public boolean isDocumentNode() {
        return false;
    }

}
