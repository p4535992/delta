<<<<<<< HEAD
package ee.webmedia.alfresco.dvk.model;

public class AbstractDocument implements IDocument {

    private String senderOrgName;
    /**
     * asutuse kood, mitte dokumendi registreerimisnumber
     */
    private String senderRegNr;
    private String senderEmail;

    @Override
    public String getSenderRegNr() {
        return senderRegNr;
    }

    @Override
    /* dvk:... */
    public void setSenderRegNr(String senderRegNr) {
        this.senderRegNr = senderRegNr;
    }

    @Override
    public String getSenderOrgName() {
        return senderOrgName;
    }

    @Override
    public void setSenderOrgName(String senderOrgName) {
        this.senderOrgName = senderOrgName;
    }

    @Override
    public String getSenderEmail() {
        return senderEmail;
    }

    @Override
    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }
}
=======
package ee.webmedia.alfresco.dvk.model;

public class AbstractDocument implements IDocument {

    private String senderOrgName;
    /**
     * asutuse kood, mitte dokumendi registreerimisnumber
     */
    private String senderRegNr;
    private String senderEmail;

    @Override
    public String getSenderRegNr() {
        return senderRegNr;
    }

    @Override
    /* dvk:... */
    public void setSenderRegNr(String senderRegNr) {
        this.senderRegNr = senderRegNr;
    }

    @Override
    public String getSenderOrgName() {
        return senderOrgName;
    }

    @Override
    public void setSenderOrgName(String senderOrgName) {
        this.senderOrgName = senderOrgName;
    }

    @Override
    public String getSenderEmail() {
        return senderEmail;
    }

    @Override
    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
