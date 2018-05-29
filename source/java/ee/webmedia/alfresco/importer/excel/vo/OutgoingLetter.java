package ee.webmedia.alfresco.importer.excel.vo;

import ee.webmedia.alfresco.document.model.LetterDocument;

public class OutgoingLetter extends LetterDocument {

    private String signerName;
    /** doccom:recipientName */
    private String recipientName;

    public void setSignerName(String signerName) {
        this.signerName = signerName;
    }

    public String getSignerName() {
        return signerName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientName() {
        return recipientName;
    }
}
