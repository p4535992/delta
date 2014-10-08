package ee.webmedia.alfresco.eventplan.model;

import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.eventplan.model.EventPlanModel.Props;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Entity class for EventPlan record.
 */
public class EventPlan extends EventPlanCommon implements Comparable<EventPlan> {

    private static final long serialVersionUID = 1L;

    public EventPlan(WmNode node) {
        super(node);
    }

    @Override
    public int compareTo(EventPlan o) {
        return AppConstants.DEFAULT_COLLATOR.compare(getName(), o.getName());
    }

    public String getName() {
        return getProp(Props.NAME);
    }

    public void setName(String name) {
        setProp(Props.NAME, name);
    }

    public String getFirstEvent() {
        return getProp(Props.FIRST_EVENT);
    }

    public void setFirstEvent(String firstEvent) {
        setProp(Props.FIRST_EVENT, firstEvent);
    }

    public String getFirstEventLabel() {
        String firstEvent = getFirstEvent();
        return getNextEventLabel(firstEvent);
    }

    public static String getNextEventLabel(String firstEvent) {
        return StringUtils.isBlank(firstEvent) ? "" : MessageUtil.getMessage(FirstEvent.valueOf(firstEvent));
    }

    public String getFirstEventDetailedLabel() {
        String label = getFirstEventLabel();
        if (StringUtils.isNotBlank(getFirstEventStart())) {
            label += ", " + MessageUtil.getMessage(FirstEventStart.valueOf(getFirstEventStart()));
        }
        if (getFirstEventPeriod() != null) {
            label += ", " + getFirstEventPeriod() + " aastat";
        }
        return label;
    }

    public String getFirstEventStart() {
        return getProp(Props.FIRST_EVENT_START);
    }

    public void setFirstEventStart(String firstEventStart) {
        setProp(Props.FIRST_EVENT_START, firstEventStart);
    }

    public Integer getFirstEventPeriod() {
        return getProp(Props.FIRST_EVENT_PERIOD);
    }

    public void setFirstEventPeriod(Integer firstEventPeriod) {
        setProp(Props.FIRST_EVENT_PERIOD, firstEventPeriod);
    }

    public String getArchivingNoteShort() {
        String note = getArchivingNote();
        return note != null && note.length() > 50 ? note.substring(0, 50) + "..." : note;
    }

    public boolean validate() {
        if (!isRetainPermanent() && StringUtils.isBlank(getRetaintionStart())) {
            MessageUtil.addErrorMessage("eventplan_error_retaintionStart");
        } else if (!isRetainPermanent() && !isHasArchivalValue() && !RetaintionStart.FIXED_DATE.name().equals(getRetaintionStart()) && getRetaintionPeriod() == null) {
            MessageUtil.addErrorMessage("eventplan_error_retaintionPeriod1");
        } else if (!isRetainPermanent() && RetaintionStart.FIXED_DATE.name().equals(getRetaintionStart()) && getRetainUntilDate() == null) {
            MessageUtil.addErrorMessage("eventplan_error_retainUntilDate");
        } else if (isAppraised() && StringUtils.isBlank(getArchivingNote())) {
            MessageUtil.addErrorMessage("eventplan_error_archivingNote");
        } else if (getRetaintionPeriod() != null && (getRetaintionPeriod() <= 0 || getRetaintionPeriod() >= 999)) {
            MessageUtil.addErrorMessage("eventplan_error_retaintionPeriod2");
        } else {
            return true;
        }
        return false;
    }

}
