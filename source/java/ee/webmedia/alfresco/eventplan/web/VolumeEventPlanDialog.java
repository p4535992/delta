package ee.webmedia.alfresco.eventplan.web;

import static ee.webmedia.alfresco.common.model.NodeBaseVO.convertNullToFalse;
import static ee.webmedia.alfresco.common.web.BeanHelper.getEventPlanService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserConfirmHelper;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.commons.lang.ObjectUtils;
import org.joda.time.Days;
import org.joda.time.LocalDate;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.model.NodeBaseVO;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.eventplan.model.EventPlan;
import ee.webmedia.alfresco.eventplan.model.EventPlanModel;
import ee.webmedia.alfresco.eventplan.model.EventPlanVolume;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.WebUtil;

public class VolumeEventPlanDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    private EventPlanVolume plan;
    private EventPlan predefinedPlan;

    public boolean isVolumeEventPlanDialog() {
        return true;
    }

    public NodeBaseVO getPlan() {
        return plan;
    }

    public EventPlan getPredefinedPlan() {
        return predefinedPlan;
    }

    public boolean isInEditMode() {
        String status = plan.getProp(DocumentDynamicModel.Props.STATUS);
        return !DocListUnitStatus.DESTROYED.getValueName().equals(status) &&
                (StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.equals(plan.getNodeRef().getStoreRef()) || getUserService().isArchivist());
    }

    @Override
    public boolean isFinishButtonVisible(boolean dialogConfOKButtonVisible) {
        return isInEditMode();
    }

    public boolean isUnderDocumentList() {
        return StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.equals(plan.getNodeRef().getStoreRef());
    }

    public void view(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        plan = getEventPlanService().getEventPlanVolume(nodeRef);
        setPredefinedEventPlan(plan.getEventPlan());
    }

    private void setPredefinedEventPlan(NodeRef predefinedEventPlanNodeRef) {
        if (predefinedEventPlanNodeRef == null) {
            predefinedPlan = new EventPlan(getGeneralService().createNewUnSaved(EventPlanModel.Types.EVENT_PLAN, null));
        } else {
            predefinedPlan = getEventPlanService().getEventPlan(predefinedEventPlanNodeRef);
        }
        predefinedPlan.setProp(RepoUtil.createTransientProp("volumeEventPlan"), predefinedEventPlanNodeRef);
        String retaintionPeriodLabel = predefinedPlan.getRetaintionPeriodLabel();
        predefinedPlan.setProp(RepoUtil.createTransientProp("retaintionPeriodLabel"), retaintionPeriodLabel);
        String firstEventDetailedLabel = predefinedPlan.getFirstEventDetailedLabel();
        predefinedPlan.setProp(RepoUtil.createTransientProp("firstEventDetailedLabel"), firstEventDetailedLabel);

        plan.setEventPlan(predefinedEventPlanNodeRef);
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (plan.validate()) {

            boolean exportedToUam = convertNullToFalse((Boolean) getNodeService().getProperty(plan.getNodeRef(), EventPlanModel.Props.EXPORTED_FOR_UAM));
            if (exportedToUam && !plan.isExportedForUam()) {
                getUserConfirmHelper().setup("eventplan_volume_confirm_exportedForUam", null, "#{VolumeEventPlanDialog.finishConfirmed1}", null);
                return null;
            }

            return finishConfirmed1(null);
        }
        return null;
    }

    public String finishConfirmed1(@SuppressWarnings("unused") ActionEvent event) {
        boolean transferConfirmed = convertNullToFalse((Boolean) getNodeService().getProperty(plan.getNodeRef(), EventPlanModel.Props.TRANSFER_CONFIRMED));
        if (transferConfirmed && !plan.isTransferConfirmed()) {
            getUserConfirmHelper().setup("eventplan_volume_confirm_transferConfirmed", null, "#{VolumeEventPlanDialog.finishConfirmed2}", null);
            return null;
        }

        return finishConfirmed2(null);
    }

    public String finishConfirmed2(@SuppressWarnings("unused") ActionEvent event) {
        if (plan.isMarkedForDestruction() && !plan.isTransferConfirmed()
                && (plan.isRetainPermanent() || (plan.getRetainUntilDate() != null && Days.daysBetween(new LocalDate(), new LocalDate(plan.getRetainUntilDate())).getDays() > 0))) {
            getUserConfirmHelper().setup("eventplan_volume_confirm_markedForDestruction", null, "#{VolumeEventPlanDialog.finishConfirmed3}", null);
            return null;
        }

        return finishConfirmed3(null);
    }

    public String finishConfirmed3(ActionEvent event) {
        getEventPlanService().save(plan);
        MessageUtil.addInfoMessage("save_success");
        if (event != null) {
            WebUtil.navigateTo(getDefaultFinishOutcome());
            return null;
        }
        return getDefaultFinishOutcome();
    }

    @SuppressWarnings("unused")
    public List<SelectItem> getEventPlans(FacesContext context, UIInput input) {
        List<EventPlan> eventPlans = getEventPlanService().getEventPlans();

        List<SelectItem> options = new ArrayList<SelectItem>();
        options.add(new SelectItem("", MessageUtil.getMessage("select_default_label")));
        for (EventPlan eventPlan : eventPlans) {
            options.add(new SelectItem(eventPlan.getNode().getNodeRef(), eventPlan.getName()));
        }
        return options;
    }

    public void eventPlanChanged(ValueChangeEvent event) {
        final NodeRef selectedPredefinedPlanRef = (NodeRef) event.getNewValue();
        if (selectedPredefinedPlanRef == null || ObjectUtils.equals(selectedPredefinedPlanRef, plan.getEventPlan())) {
            setPredefinedEventPlan(selectedPredefinedPlanRef);
        } else {
            setPredefinedEventPlan(selectedPredefinedPlanRef);
            getUserConfirmHelper().setup("eventplan_volume_change_confirm", null, "#{VolumeEventPlanDialog.eventPlanChangeConfirmed}", null);
        }
    }

    public void eventPlanChangeConfirmed(@SuppressWarnings("unused") ActionEvent event) {
        if (predefinedPlan.isSaved()) {
            plan.initFromEventPlan(predefinedPlan);
        }
    }

}
