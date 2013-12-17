package ee.webmedia.alfresco.dvk.model;

import org.w3c.dom.Node;

public class DvkSendWorkflowDocumentsImpl extends AbstractDocument implements DvkSendWorkflowDocuments {

    private String recipientsRegNr;
    org.w3c.dom.Node docNode;
    boolean isDocumentNode = true;

    @Override
    public String getRecipientsRegNr() {
        return recipientsRegNr;
    }

    @Override
    public void setRecipientsRegNr(String recipientsRegNr) {
        this.recipientsRegNr = recipientsRegNr;
    }

    @Override
    public Node getRecipientDocNode() {
        return docNode;
    }

    @Override
    public void setRecipientDocNode(Node docNode) {
        this.docNode = docNode;
    }

    @Override
    public boolean isDocumentNode() {
        return isDocumentNode;
    }

    @Override
    public void setIsDocumentNode(boolean isDocumentNode) {
        this.isDocumentNode = isDocumentNode;
    }

}
