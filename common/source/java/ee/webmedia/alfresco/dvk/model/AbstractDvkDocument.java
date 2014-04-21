package ee.webmedia.alfresco.dvk.model;

public class AbstractDvkDocument {

    private String senderOrgName;
    /**
     * asutuse kood, mitte dokumendi registreerimisnumber
     */
    private String senderRegNr;
    private String senderEmail;
    private String textContent;

    public String getSenderRegNr() {
        return senderRegNr;
    }

    /* dvk:... */
    public void setSenderRegNr(String senderRegNr) {
        this.senderRegNr = senderRegNr;
    }

    public String getSenderOrgName() {
        return senderOrgName;
    }

    public void setSenderOrgName(String senderOrgName) {
        this.senderOrgName = senderOrgName;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public String getTextContent() {
        return textContent;
    }
}
