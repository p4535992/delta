package ee.webmedia.alfresco.dvk.model;

import java.util.Date;

public abstract class AbstractLetterDocument extends AbstractDocument implements ILetterDocument {

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

    // START: LetterSender

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
}
