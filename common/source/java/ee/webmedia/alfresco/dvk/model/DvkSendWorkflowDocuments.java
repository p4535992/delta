<<<<<<< HEAD
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
=======
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
