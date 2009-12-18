package ee.webmedia.alfresco.dvk.model;

import java.util.Date;

import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

/**
 * @author Ats Uiboupin
 *
 */
@AlfrescoModelType(uri = DvkModel.URI)
public interface LetterSender {

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

    /**
     * @return DateTime when the document was signed or affirmed
     */
    Date getLetterSenderDocSignDate();

    void setLetterSenderDocSignDate(Date letterSenderDocSignDate);

    /**
     * @return Identificator of the document in the senders system
     */
    String getLetterSenderDocNr();

    void setLetterSenderDocNr(String letterSenderDocNr);

    String getLetterSenderTitle();

    void setLetterSenderTitle(String letterSenderTitle);

}
