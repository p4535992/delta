package ee.webmedia.alfresco.dvk.model;

import java.util.Collection;
import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

/**
 * @author Ats Uiboupin
 *
 */
public class DvkSendDocumentsImpl implements DvkSendDocuments {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DvkSendDocumentsImpl.class);

    private String senderOrgName;
    private String senderRegNr;
    private String senderEmail;
    /**
     * DateTime when the document was signed or affirmed
     */
    private Date letterSenderDocSignDate;
    /**
     * Identificator of the document in the senders system
     */
    private String letterSenderDocNr;
    private String letterSenderTitle;
    //

    // AccessRights
    private String letterAccessRestriction;
    private Date letterAccessRestrictionBeginDate;
    private Date letterAccessRestrictionEndDate;
    private String letterAccessRestrictionReason;

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
            Assert.assertNotNull("SenderOrgName must be given", getSenderOrgName());
            Assert.assertNotNull("SenderRegNr must be given", getSenderRegNr());
            Assert.assertNotNull("SenderEmail must be given", getSenderEmail());
            Assert.assertNotNull("LetterSenderDocSignDate must be given", getLetterSenderDocSignDate());
            Assert.assertNotNull("LetterSenderDocNr must be given", getLetterSenderDocNr());
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
    // START: LetterSender

    @Override
    public String getSenderRegNr() {
        return senderRegNr;
    }

    @Override
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

    @Override
    public Date getLetterSenderDocSignDate() {
        return letterSenderDocSignDate;
    }

    @Override
    public void setLetterSenderDocSignDate(Date letterSenderDocSignDate) {
        this.letterSenderDocSignDate = letterSenderDocSignDate;
    }

    @Override
    public String getLetterSenderDocNr() {
        return letterSenderDocNr;
    }

    @Override
    public void setLetterSenderDocNr(String letterSenderDocNr) {
        this.letterSenderDocNr = letterSenderDocNr;
    }

    @Override
    public String getLetterSenderTitle() {
        return letterSenderTitle;
    }

    @Override
    public void setLetterSenderTitle(String letterSenderTitle) {
        this.letterSenderTitle = letterSenderTitle;
    }

    // End: LetterSender
    // START: AccessRights

    @Override
    public String getLetterAccessRestriction() {
        return letterAccessRestriction;
    }

    @Override
    public void setLetterAccessRestriction(String letterAccessRestriction) {
        this.letterAccessRestriction = letterAccessRestriction;
    }

    @Override
    public Date getLetterAccessRestrictionBeginDate() {
        return letterAccessRestrictionBeginDate;
    }

    @Override
    public void setLetterAccessRestrictionBeginDate(Date letterAccessRestrictionBeginDate) {
        this.letterAccessRestrictionBeginDate = letterAccessRestrictionBeginDate;
    }

    @Override
    public Date getLetterAccessRestrictionEndDate() {
        return letterAccessRestrictionEndDate;
    }

    @Override
    public void setLetterAccessRestrictionEndDate(Date letterAccessRestrictionEndDate) {
        this.letterAccessRestrictionEndDate = letterAccessRestrictionEndDate;
    }

    @Override
    public String getLetterAccessRestrictionReason() {
        return letterAccessRestrictionReason;
    }

    @Override
    public void setLetterAccessRestrictionReason(String letterAccessRestrictionReason) {
        this.letterAccessRestrictionReason = letterAccessRestrictionReason;
    }

    // End: AccessRights
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
