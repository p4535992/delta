package ee.webmedia.alfresco.eventplan.model;

import java.util.Date;

import org.apache.commons.lang.time.FastDateFormat;

import ee.webmedia.alfresco.common.model.NodeBaseVO;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.eventplan.model.EventPlanModel.Props;

/**
 * Entity class for EventPlan record.
 */
public class EventPlanCommon extends NodeBaseVO {
    private static final long serialVersionUID = 1L;

    public static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("dd.MM.yyyy");

    public EventPlanCommon(WmNode node) {
        setNode(node);
    }

    public void setNode(WmNode node) {
        this.node = node;
    }

    public final boolean isAppraised() {
        return getPropBoolean(Props.IS_APPRAISED);
    }

    public final void setAppraised(boolean appraised) {
        setProp(Props.IS_APPRAISED, appraised);
    }

    public final boolean isHasArchivalValue() {
        return getPropBoolean(Props.HAS_ARCHIVAL_VALUE);
    }

    public final void setHasArchivalValue(boolean hasArchivalValue) {
        setProp(Props.HAS_ARCHIVAL_VALUE, hasArchivalValue);
    }

    public final boolean isRetainPermanent() {
        return getPropBoolean(Props.RETAIN_PERMANENT);
    }

    public final void setRetainPermanent(boolean retainPermanent) {
        setProp(Props.RETAIN_PERMANENT, retainPermanent);
    }

    public final String getRetaintionStart() {
        return getProp(Props.RETAINTION_START);
    }

    public final void setRetaintionStart(String retaintionStart) {
        setProp(Props.RETAINTION_START, retaintionStart);
    }

    public final Integer getRetaintionPeriod() {
        return getProp(Props.RETAINTION_PERIOD);
    }

    public final void setRetaintionPeriod(Integer retaintionPeriod) {
        setProp(Props.RETAINTION_PERIOD, retaintionPeriod);
    }

    public final String getRetaintionPeriodLabel() {
        if (isRetainPermanent()) {
            return "Alaline";
        } else if (RetaintionStart.FIXED_DATE.name().equals(getRetaintionStart()) && getRetainUntilDate() != null) {
            return "Kuni " + DATE_FORMAT.format(getRetainUntilDate());
        } else if (!RetaintionStart.FIXED_DATE.name().equals(getRetaintionStart()) && getRetaintionPeriod() != null) {
            return getRetaintionPeriod() + " aastat";
        }
        return "Teadmata";
    }

    public final Date getRetainUntilDate() {
        return getProp(Props.RETAIN_UNTIL_DATE);
    }

    public final void setRetainUntilDate(Date retainUntilDate) {
        setProp(Props.RETAIN_UNTIL_DATE, retainUntilDate);
    }

    public final String getArchivingNote() {
        return getProp(Props.ARCHIVING_NOTE);
    }

    public final void setArchivingNote(String archivingNote) {
        setProp(Props.ARCHIVING_NOTE, archivingNote);
    }

}
