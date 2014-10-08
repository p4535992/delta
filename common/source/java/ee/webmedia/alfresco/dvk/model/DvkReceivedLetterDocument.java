<<<<<<< HEAD
package ee.webmedia.alfresco.dvk.model;

import java.util.Date;

import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

/**
 * @author Ats Uiboupin
 */
@AlfrescoModelType(uri = DvkModel.URI)
public interface DvkReceivedLetterDocument extends ILetterDocument {

    /**
     * @return dhl_id - unique id assigned to the sent document by the DVK server
     */
    String getDvkId();

    void setDvkId(String dvkId);

    Date getLetterDeadLine();

    void setLetterDeadLine(Date letterDeadLine);

}
=======
package ee.webmedia.alfresco.dvk.model;

import java.util.Date;

public class DvkReceivedLetterDocument extends AbstractDvkDocument {

    /**
     * aka dhl_id - unique id assigned to the sent document by the DVK server
     */
    private String dvkId;
    private Date letterDeadLine;

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

    public Date getLetterSenderDocSignDate() {
        return letterSenderDocSignDate;
    }

    public void setLetterSenderDocSignDate(Date letterSenderDocSignDate) {
        this.letterSenderDocSignDate = letterSenderDocSignDate;
    }

    public String getLetterSenderDocNr() {
        return letterSenderDocNr;
    }

    public void setLetterSenderDocNr(String letterSenderDocNr) {
        this.letterSenderDocNr = letterSenderDocNr;
    }

    public String getLetterSenderTitle() {
        return letterSenderTitle;
    }

    public void setLetterSenderTitle(String letterSenderTitle) {
        this.letterSenderTitle = letterSenderTitle;
    }

    // End: LetterSender
    // START: AccessRights

    public String getLetterAccessRestriction() {
        return letterAccessRestriction;
    }

    public void setLetterAccessRestriction(String letterAccessRestriction) {
        this.letterAccessRestriction = letterAccessRestriction;
    }

    public Date getLetterAccessRestrictionBeginDate() {
        return letterAccessRestrictionBeginDate;
    }

    public void setLetterAccessRestrictionBeginDate(Date letterAccessRestrictionBeginDate) {
        this.letterAccessRestrictionBeginDate = letterAccessRestrictionBeginDate;
    }

    public Date getLetterAccessRestrictionEndDate() {
        return letterAccessRestrictionEndDate;
    }

    public void setLetterAccessRestrictionEndDate(Date letterAccessRestrictionEndDate) {
        this.letterAccessRestrictionEndDate = letterAccessRestrictionEndDate;
    }

    public String getLetterAccessRestrictionReason() {
        return letterAccessRestrictionReason;
    }

    public void setLetterAccessRestrictionReason(String letterAccessRestrictionReason) {
        this.letterAccessRestrictionReason = letterAccessRestrictionReason;
    }

    public String getDvkId() {
        return dvkId;
    }

    public void setDvkId(String dvkId) {
        this.dvkId = dvkId;
    }

    public Date getLetterDeadLine() {
        return letterDeadLine;
    }

    public void setLetterDeadLine(Date letterDeadLine) {
        this.letterDeadLine = letterDeadLine;
    }

}
>>>>>>> develop-5.1
