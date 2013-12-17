package ee.webmedia.alfresco.dvk.model;

public interface DvkSendWorkflowDocuments extends IDocument {

    String getRecipientsRegNr();

    void setRecipientsRegNr(String regNr);

    org.w3c.dom.Node getRecipientDocNode();

    void setRecipientDocNode(org.w3c.dom.Node docNode);

    // indicate whether we are sending document or task
    boolean isDocumentNode();

    void setIsDocumentNode(boolean isDocumentNode);

}
