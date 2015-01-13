package ee.webmedia.alfresco.dvk.model;

import java.util.Date;

import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

@AlfrescoModelType(uri = DvkModel.URI)
public interface LetterSender {

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
