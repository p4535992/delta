package ee.webmedia.alfresco.dvk.model;

import org.w3c.dom.Node;

public class DvkSendWorkflowDocuments extends AbstractDvkDocument {

    private String recipientsRegNr;
    private String recipientStructuralUnit;
    org.w3c.dom.Node docNode;
    boolean isDocumentNode = true;

    public String getRecipientsRegNr() {
        return recipientsRegNr;
    }

    public void setRecipientsRegNr(String recipientsRegNr) {
        this.recipientsRegNr = recipientsRegNr;
    }

    public Node getRecipientDocNode() {
        return docNode;
    }

    public void setRecipientDocNode(Node docNode) {
        this.docNode = docNode;
    }

    public boolean isDocumentNode() {
        return isDocumentNode;
    }

    public void setIsDocumentNode(boolean isDocumentNode) {
        this.isDocumentNode = isDocumentNode;
    }

    public void setRecipientStructuralUnit(String recipientStructuralUnit) {
        this.recipientStructuralUnit = recipientStructuralUnit;
    }

    public String getRecipientStructuralUnit() {
        return recipientStructuralUnit;
    }
}
