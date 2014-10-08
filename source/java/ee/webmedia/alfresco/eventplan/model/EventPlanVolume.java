package ee.webmedia.alfresco.eventplan.model;

import java.util.Calendar;
import java.util.Date;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.utils.CalendarUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;

public class EventPlanVolume extends EventPlanCommon {
    private static final long serialVersionUID = 1L;

    public EventPlanVolume(WmNode node) {
        super(node);
    }

    public NodeRef getEventPlan() {
        return (NodeRef) getProp(EventPlanModel.Props.EVENT_PLAN);
    }

    public void setEventPlan(NodeRef eventPlan) {
        setProp(EventPlanModel.Props.EVENT_PLAN, eventPlan);
    }

    public String getNextEvent() {
        return getProp(EventPlanModel.Props.NEXT_EVENT);
    }

    public void setNextEvent(String nextEvent) {
        setProp(EventPlanModel.Props.NEXT_EVENT, nextEvent);
    }

    public Date getNextEventDate() {
        return getProp(EventPlanModel.Props.NEXT_EVENT_DATE);
    }

    public void setNextEventDate(Date nextEventDate) {
        setProp(EventPlanModel.Props.NEXT_EVENT_DATE, nextEventDate);
    }

    public boolean isMarkedForTransfer() {
        return getPropBoolean(EventPlanModel.Props.MARKED_FOR_TRANSFER);
    }

    public void setMarkedForTransfer(boolean markedForTransfer) {
        setProp(EventPlanModel.Props.MARKED_FOR_TRANSFER, markedForTransfer);
    }

    public boolean isExportedForUam() {
        return getPropBoolean(EventPlanModel.Props.EXPORTED_FOR_UAM);
    }

    public void setExportedForUam(boolean exportedForUam) {
        setProp(EventPlanModel.Props.EXPORTED_FOR_UAM, exportedForUam);
    }

    public Date getExportedForUamDateTime() {
        return getProp(EventPlanModel.Props.EXPORTED_FOR_UAM_DATE_TIME);
    }

    public void setExportedForUamDateTime(Date exportedForUamDateTime) {
        setProp(EventPlanModel.Props.EXPORTED_FOR_UAM_DATE_TIME, exportedForUamDateTime);
    }

    public boolean isTransferConfirmed() {
        return getPropBoolean(EventPlanModel.Props.TRANSFER_CONFIRMED);
    }

    public void setTransferConfirmed(boolean transferConfirmed) {
        setProp(EventPlanModel.Props.TRANSFER_CONFIRMED, transferConfirmed);
    }

    public Date getTransferedDateTime() {
        return getProp(EventPlanModel.Props.TRANSFERED_DATE_TIME);
    }

    public void setTransferedDateTime(Date transferedDateTime) {
        setProp(EventPlanModel.Props.TRANSFERED_DATE_TIME, transferedDateTime);
    }

    public boolean isMarkedForDestruction() {
        return getPropBoolean(EventPlanModel.Props.MARKED_FOR_DESTRUCTION);
    }

    public void setMarkedForDestruction(boolean markedForDestruction) {
        setProp(EventPlanModel.Props.MARKED_FOR_DESTRUCTION, markedForDestruction);
    }

    public boolean isDisposalActCreated() {
        return getPropBoolean(EventPlanModel.Props.DISPOSAL_ACT_CREATED);
    }

    public void setDisposalActCreated(boolean disposalActCreated) {
        setProp(EventPlanModel.Props.DISPOSAL_ACT_CREATED, disposalActCreated);
    }

    public Date getDisposalDateTime() {
        return getProp(EventPlanModel.Props.DISPOSAL_DATE_TIME);
    }

    public void setDisposalDateTime(Date disposalDateTime) {
        setProp(EventPlanModel.Props.DISPOSAL_DATE_TIME, disposalDateTime);
    }

    public Date getValidFrom() {
        return getProp(VolumeModel.Props.VALID_FROM);
    }

    public Date getValidTo() {
        return getProp(VolumeModel.Props.VALID_TO);
    }

    public void setValidTo(Date validTo) {
        setProp(VolumeModel.Props.VALID_TO, validTo);
    }

    public void initFromEventPlan(EventPlan plan) {
        if (plan == null) {
            return;
        }

        setEventPlan(plan.getNode().getNodeRef());
        setAppraised(plan.isAppraised());
        setHasArchivalValue(plan.isHasArchivalValue());
        setRetainPermanent(plan.isRetainPermanent());
        setRetaintionStart(plan.getRetaintionStart());
        setRetaintionPeriod(plan.getRetaintionPeriod());

        Date validFrom = getValidFrom();
        Date validTo = getValidTo();

        Date retainUntilDate = null;
        if (RetaintionStart.FIXED_DATE.is(plan.getRetaintionStart())) {
            retainUntilDate = plan.getRetainUntilDate();
        } else if (RetaintionStart.FROM_CREATION.is(plan.getRetaintionStart()) && validFrom != null && plan.getRetaintionPeriod() != null) {
            retainUntilDate = DateUtils.addYears(validFrom, plan.getRetaintionPeriod());
        } else if (RetaintionStart.FROM_CLOSING.is(plan.getRetaintionStart()) && validTo != null && plan.getRetaintionPeriod() != null) {
            retainUntilDate = DateUtils.addYears(validTo, plan.getRetaintionPeriod());
        } else if (RetaintionStart.FROM_CLOSING_YEAR_END.is(plan.getRetaintionStart()) && validTo != null && plan.getRetaintionPeriod() != null) {
            retainUntilDate = DateUtils.truncate(DateUtils.addYears(validTo, plan.getRetaintionPeriod() + 1), Calendar.YEAR);
        }
        setRetainUntilDate(retainUntilDate);

        setNextEvent(plan.getFirstEvent());

        Date nextEventDate = null;
        if (FirstEventStart.FIXED_DATE.is(plan.getFirstEventStart())) {
            nextEventDate = getNextEventDate();
        } else if (FirstEventStart.FROM_CREATION.is(plan.getFirstEventStart()) && validFrom != null && plan.getFirstEventPeriod() != null) {
            nextEventDate = DateUtils.addYears(validFrom, plan.getFirstEventPeriod());
        } else if (FirstEventStart.FROM_CLOSING.is(plan.getFirstEventStart()) && validTo != null && plan.getFirstEventPeriod() != null) {
            nextEventDate = DateUtils.addYears(validTo, plan.getFirstEventPeriod());
        } else if (FirstEventStart.FROM_CLOSING_YEAR_END.is(plan.getFirstEventStart()) && validTo != null && plan.getFirstEventPeriod() != null) {
            nextEventDate = DateUtils.truncate(DateUtils.addYears(validTo, plan.getFirstEventPeriod() + 1), Calendar.YEAR);
        }
        setNextEventDate(nextEventDate);

        setArchivingNote(plan.getArchivingNote());
    }

    public boolean validate() {
        if (isAppraised() && !isRetainPermanent() && StringUtils.isBlank(getRetaintionStart())) {
            MessageUtil.addErrorMessage("eventplan_volume_error_retaintionStart");
        } else if (isAppraised() && !isRetainPermanent() && !isHasArchivalValue() && !RetaintionStart.FIXED_DATE.name().equals(getRetaintionStart())
                && getRetaintionPeriod() == null) {
            MessageUtil.addErrorMessage("eventplan_volume_error_retaintionPeriod");
        } else if (isAppraised() && !isRetainPermanent() && RetaintionStart.FIXED_DATE.name().equals(getRetaintionStart()) && getRetainUntilDate() == null) {
            MessageUtil.addErrorMessage("eventplan_volume_error_retainUntilDate1");
        } else if (isAppraised() && StringUtils.isBlank(getArchivingNote())) {
            MessageUtil.addErrorMessage("eventplan_volume_error_archivingNote");
        } else if (getRetainUntilDate() != null &&
                (getValidFrom() != null && CalendarUtil.getDaysBetweenSigned(getValidFrom(), getRetainUntilDate()) < 0)
                || (getValidTo() != null && CalendarUtil.getDaysBetweenSigned(getValidTo(), getRetainUntilDate()) < 0)) {
            MessageUtil.addErrorMessage("eventplan_volume_error_retainUntilDate2");
        } else if (getRetaintionPeriod() != null && (getRetaintionPeriod() <= 0 || getRetaintionPeriod() >= 999)) {
            MessageUtil.addErrorMessage("eventplan_error_retaintionPeriod2");
        } else {
            return true;
        }
        return false;
    }

}
