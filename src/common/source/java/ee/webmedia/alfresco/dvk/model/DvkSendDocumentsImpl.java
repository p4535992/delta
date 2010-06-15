package ee.webmedia.alfresco.dvk.model;

import java.util.Collection;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;

/**
 * @author Ats Uiboupin
 *
 */
public class DvkSendDocumentsImpl extends AbstractDocument implements DvkSendDocuments {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DvkSendDocumentsImpl.class);

    // LetterCompilator
    private String letterCompilatorFirstname;
    private String letterCompilatorSurname;
    private String letterCompilatorJobTitle;

    // DvkSendDocuments
    private String docType;
    private Collection<String> recipientsRegNrs;

    @Override
    public void validateOutGoing() {
        try {
            Assert.assertNotNull("LetterSenderDocSignDate must be given", getLetterSenderDocSignDate());
            Assert.assertNotNull("LetterSenderDocNr must be given", getLetterSenderDocNr());
        } catch (AssertionFailedError e) {
            log.debug("Object that was validated: '" + ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE) + "'");
            throw new UnableToPerformException(MessageSeverity.ERROR, "dvk_send_error_notRegistered", e);
        }
        try {
            Assert.assertNotNull("SenderOrgName must be given", getSenderOrgName()); // XXX: tegelikult võks olla tühi string, kuna täidetakse regNr järgi
            Assert.assertNotNull("SenderRegNr must be given", getSenderRegNr());
            Assert.assertNotNull("SenderEmail must be given", getSenderEmail());
            Assert.assertNotNull("RecipientsRegNrs must be given", getRecipientsRegNrs());
            Assert.assertTrue("There have to be at least one recipient", getRecipientsRegNrs().size()>0);
        } catch (AssertionFailedError e) {
            log.debug("Object that was validated: '" + ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE) + "'");
            throw new RuntimeException("Some of the compulsory fields have not been filled that are needed for the outgoing message", e);
        }
    }

    // START: DvkSendDocuments
    @Override
    public String getDocType() {
        return docType;
    }

    @Override
    public void setDocType(String docType) {
        this.docType = docType;
    }

    @Override
    public Collection<String> getRecipientsRegNrs() {
        return recipientsRegNrs;
    }

    @Override
    public void setRecipientsRegNrs(Collection<String> recipientsRegNrs) {
        this.recipientsRegNrs = recipientsRegNrs;
    }

    // END: DvkSendDocuments
    // START: LetterCompilator

    @Override
    public String getLetterCompilatorFirstname() {
        return letterCompilatorFirstname;
    }

    @Override
    public void setLetterCompilatorFirstname(String letterCompilatorFirstname) {
        this.letterCompilatorFirstname = letterCompilatorFirstname;
    }

    @Override
    public String getLetterCompilatorSurname() {
        return letterCompilatorSurname;
    }

    @Override
    public void setLetterCompilatorSurname(String letterCompilatorSurname) {
        this.letterCompilatorSurname = letterCompilatorSurname;
    }

    @Override
    public String getLetterCompilatorJobTitle() {
        return letterCompilatorJobTitle;
    }

    @Override
    public void setLetterCompilatorJobTitle(String letterCompilatorJobTitle) {
        this.letterCompilatorJobTitle = letterCompilatorJobTitle;
    }
    // END: LetterCompilator

}
