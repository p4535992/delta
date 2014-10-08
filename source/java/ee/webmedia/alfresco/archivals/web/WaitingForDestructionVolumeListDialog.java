package ee.webmedia.alfresco.archivals.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.config.DialogsConfigElement.DialogButtonConfig;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.archivals.model.ActivityStatus;
import ee.webmedia.alfresco.archivals.model.ActivityType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.eventplan.model.FirstEvent;
import ee.webmedia.alfresco.utils.ComparableTransformer;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.search.model.VolumeSearchModel;
import ee.webmedia.alfresco.volume.service.VolumeService;

public class WaitingForDestructionVolumeListDialog extends VolumeArchiveBaseDialog {

    private static final long serialVersionUID = 1L;
    private boolean confirmComposeDisposalAct;
    private boolean confirmStartDestruction;
    private boolean confirmStartSimpleDestruction;

    protected static ComparatorChain comparator;

    static {
        ComparatorChain chain = new ComparatorChain();
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Volume>() {
            @Override
            public Comparable<String> tr(Volume volume) {
                return volume.getRetaintionDescription();
            }
        }, new NullComparator(AppConstants.getNewCollatorInstance())));
        chain.addComparator(TransferringToUamVolumeListDialog.comparator);
        comparator = chain;
    }

    @Override
    protected void initFilterItems() {
        super.initFilterItems();
        getFilter().getAspects().add(VolumeSearchModel.Aspects.PLANNED_DESTRUCTION);
    }

    @Override
    protected void searchVolumes() {
        volumes = BeanHelper.getDocumentSearchService().searchVolumesForArchiveList(filter, false, true, storeNodeRefs);
    }

    @Override
    protected Comparator<Volume> getComparator() {
        return comparator;
    }

    public String getVolumeListTitle() {
        return MessageUtil.getMessage("archivals_volume_waiting_destruction_list_title");
    }

    @Override
    public boolean isShowExportedForUamDateTimeColumn() {
        return true;
    }

    @Override
    public boolean isShowRetaintionColumns() {
        return true;
    }

    @Override
    public boolean isShowDestructionColumns() {
        return true;
    }

    @Override
    public boolean isShowTransferringDeletingNextEventColumn() {
        return true;
    }

    @Override
    protected List<String> getRenderedFilterFields() {
        if (renderedFilterFields == null) {
            renderedFilterFields = new ArrayList<String>(Arrays.asList(
                    VolumeSearchModel.Props.MARKED_FOR_DESTRUCTION.toPrefixString(),
                    VolumeSearchModel.Props.DISPOSAL_ACT_CREATED.toPrefixString(),
                    VolumeSearchModel.Props.EXPORTED_FOR_UAM_DATE_TIME.toPrefixString(),
                    VolumeSearchModel.Props.EXPORTED_FOR_UAM_DATE_TIME_END_DATE.toPrefixString(),
                    VolumeSearchModel.Props.NEXT_EVENT_DATE.toPrefixString(),
                    VolumeSearchModel.Props.NEXT_EVENT_DATE_END_DATE.toPrefixString(),
                    VolumeSearchModel.Props.HAS_ARCHIVAL_VALUE.toPrefixString(),
                    VolumeSearchModel.Props.RETAIN_PERMANENT.toPrefixString(),
                    VolumeSearchModel.Props.EVENT_PLAN.toPrefixString()));
            if (showStoreFilterField()) {
                renderedFilterFields.add(VolumeSearchModel.Props.STORE.toPrefixString());
            }
            if (showNextEventFilterField()) {
                renderedFilterFields.add(VolumeSearchModel.Props.NEXT_EVENT.toPrefixString());
            }
        }
        return renderedFilterFields;
    }

    @Override
    public boolean isShowStore() {
        return BeanHelper.getGeneralService().hasAdditionalArchivalStores();
    }

    @Override
    public List<DialogButtonConfig> getAdditionalButtons() {
        List<DialogButtonConfig> buttons = new ArrayList<DialogButtonConfig>(2);
        buttons.add(new DialogButtonConfig("volumeComposeDisposalActButton", null, "archivals_volume_compose_disposal_act", "#{DialogManager.bean.composeDisposalActConfirm}",
                "false", null));
        buttons.add(new DialogButtonConfig("volumeStartDestructionButton", null, "archivals_volume_start_destruction", "#{DialogManager.bean.startDestructionConfirm}",
                "false", null));
        if (BeanHelper.getArchivalsService().isSimpleDestructionEnabled()) {
            buttons.add(new DialogButtonConfig("volumeStartSimpleDestructionButton", null, "archivals_volume_start_simple_destruction",
                    "#{DialogManager.bean.startSimpleDestructionConfirm}", "false", null));
        }
        return buttons;
    }

    @Override
    public void cancelAction(ActionEvent actionEvent) {
        confirmComposeDisposalAct = false;
        confirmStartDestruction = false;
        confirmStartSimpleDestruction = false;
    }

    public void composeDisposalActConfirm() {
        if (checkVolumesSelected()) {
            confirmComposeDisposalAct = true;
        }
    }

    public void composeDisposalAct(ActionEvent event) {
        confirmComposeDisposalAct = false;
        NodeRef activityRef = generateActivityAndWordFile(ActivityType.DISPOSAL_ACT_DOC, "archivals_volume_compose_disposal_act_template", ActivityStatus.IN_PROGRESS,
                null, true, "archivals_volume_compose_disposal_act_error_no_template");
        if (activityRef != null) {
            BeanHelper.getArchivalsService().setDisposalActCreated(getSelectedVolumes(), activityRef);
            MessageUtil.addInfoMessage("archivals_volume_compose_disposal_act_success");
        }
    }

    @Override
    public boolean isConfirmComposeDisposalAct() {
        return confirmComposeDisposalAct;
    }

    public void startDestructionConfirm() {
        if (checkVolumesSelected()) {
            confirmStartDestruction = true;
        }
    }

    public void startDestruction(ActionEvent event) {
        confirmStartDestruction = false;
        disposeVolumes("archivals_volume_start_destruction_template", "archivals_volume_start_destruction_error_no_template", ActivityType.DESTRUCTION,
                "archivals_volume_destruction_with_disposition_act", "archivals_volume_start_destruction_success", "applog_archivals_volume_disposed");
    }

    private void disposeVolumes(String template, String missingTemplateErrorMsgKey, ActivityType activityType, String docCommentMsgKey, String successMsgKey, String logMessageKey) {
        Date destructionStartDate = new Date();
        String templateName = MessageUtil.getMessage(template);
        NodeRef templateRef = BeanHelper.getDocumentTemplateService().getArchivalReportTemplateByName(templateName);
        if (templateRef == null) {
            MessageUtil.addErrorMessage(missingTemplateErrorMsgKey, templateName);
            return;
        }
        List<NodeRef> selectedVolumes = getSelectedVolumes();
        if (!validateVolumesForDisposal(selectedVolumes, false)) {
            return;
        }
        NodeRef activityRef = generateActivityAndWordFile(activityType, ActivityStatus.IN_PROGRESS, null, null);
        if (activityRef != null) {
            BeanHelper.getArchivalsService().disposeVolumes(selectedVolumes, destructionStartDate, MessageUtil.getMessage(docCommentMsgKey),
                    activityRef, templateRef, MessageUtil.getMessage(logMessageKey));
            MessageUtil.addInfoMessage(successMsgKey, selectedVolumes.size());
        }
    }

    private boolean validateVolumesForDisposal(List<NodeRef> selectedVolumes, boolean simpleDestruction) {
        VolumeService volumeService = BeanHelper.getVolumeService();
        Map<Long, QName> propertyTypes = new HashMap<Long, QName>();
        for (NodeRef volumeRef : selectedVolumes) {
            Volume volume = volumeService.getVolumeByNodeRef(volumeRef, propertyTypes);
            if (!(volume.isAppraised() && StringUtils.isNotBlank(volume.getArchivingNote()))) {
                MessageUtil.addErrorMessage("archivals_volume_start_destruction_error_no_appraise_or_arch_note");
                return false;
            }
            if (!simpleDestruction && !volume.isDisposalActCreated()) {
                MessageUtil.addErrorMessage("archivals_volume_start_destruction_error_no_disposal_act");
                return false;
            }
            String nextEventStr = volume.getNextEvent();
            FirstEvent nextEvent = StringUtils.isNotBlank(nextEventStr) ? FirstEvent.valueOf(nextEventStr) : null;
            boolean notSimpleDestructionEvent = FirstEvent.SIMPLE_DESTRUCTION != nextEvent;
            if ((FirstEvent.DESTRUCTION != nextEvent && notSimpleDestructionEvent)
                    || (simpleDestruction && notSimpleDestructionEvent)) {
                MessageUtil.addErrorMessage(simpleDestruction ? "archivals_volume_start_simple_destruction_error_wrong_next_event"
                        : "archivals_volume_start_destruction_error_wrong_next_event");
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isConfirmStartDestruction() {
        return confirmStartDestruction;
    }

    public void startSimpleDestructionConfirm() {
        if (checkVolumesSelected()) {
            confirmStartSimpleDestruction = true;
        }
    }

    public void startSimpleDestruction(ActionEvent event) {
        confirmStartSimpleDestruction = false;
        disposeVolumes("archivals_volume_start_simple_destruction_template", "archivals_volume_start_simple_destruction_error_no_template", ActivityType.SIMPLE_DESTRUCTION,
                "archivals_volume_destruction_without_disposition_act", "archivals_volume_start_simple_destruction_success", "applog_archivals_volume_simple_disposed");
    }

    @Override
    public boolean isConfirmStartSimpleDestruction() {
        return confirmStartSimpleDestruction;
    }

}
