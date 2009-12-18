package ee.webmedia.alfresco.dvk.model;

import java.util.Date;

import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

/**
 * @author Ats Uiboupin
 *
 */
// use URIs defined by the interfaces
@AlfrescoModelType(uri = "")
public class DvkReceivedDocumentImpl implements DvkReceivedDocument {
    /**
     * aka dhl_id - unique id assigned to the sent document by the DVK server
     */
    private String dvkId;
    /**
     * Senders organization number from business register
     */
    private String senderRegNr;
    /**
     * Senders organization name according to business register
     */
    private String senderOrgName;
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
    private Date letterDeadLine;

    // AccessRights
    private String letterAccessRestriction;
    private Date letterAccessRestrictionBeginDate;
    private Date letterAccessRestrictionEndDate;
    private String letterAccessRestrictionReason;

    @Override
    public String getDvkId() {
        return dvkId;
    }

    @Override
    public void setDvkId(String dvkId) {
        this.dvkId = dvkId;
    }

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

    @Override
    public Date getLetterDeadLine() {
        return letterDeadLine;
    }

    @Override
    public void setLetterDeadLine(Date letterDeadLine) {
        this.letterDeadLine = letterDeadLine;
    }

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

}
