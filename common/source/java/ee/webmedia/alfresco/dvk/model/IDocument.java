package ee.webmedia.alfresco.dvk.model;

//TODO: if not used, remove the interface
public interface IDocument {

    /**
     * @return Senders organization number from business register
     */
    String getSenderRegNr();

    void setSenderRegNr(String senderRegNr);

    /**
     * @return Senders organization name according to business register
     */
    String getSenderOrgName();

    void setSenderOrgName(String senderOrgName);

    String getSenderEmail();

    void setSenderEmail(String senderEmail);

}
