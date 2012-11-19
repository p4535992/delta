package ee.webmedia.alfresco.archivals.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.config.DialogsConfigElement.DialogButtonConfig;

import ee.webmedia.alfresco.archivals.model.ActivityStatus;
import ee.webmedia.alfresco.archivals.model.ActivityType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.eventplan.model.FirstEvent;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.search.model.VolumeSearchModel;

/**
 * @author Riina Tens
 */
public class WaitingForTransferVolumeListDialog extends VolumeArchiveBaseDialog {

    private static final long serialVersionUID = 1L;
    private boolean confirmMarkForTransfer;
    private boolean confirmExportToUam;

    @Override
    protected void initFilterItems() {
        super.initFilterItems();
        Node filter = getFilter();
        filter.getAspects().add(VolumeSearchModel.Aspects.PLANNED_TRANSFER);
        filter.getProperties().put(VolumeSearchModel.Props.NEXT_EVENT.toString(), Collections.singletonList(FirstEvent.TRANSFER.name()));
        filter.getProperties().put(VolumeSearchModel.Props.IS_APPRAISED.toString(), Boolean.TRUE);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Comparator<Volume> getComparator() {
        return WaitingOverviewVolumeListDialog.comparator;
    }

    @Override
    protected void searchVolumes() {
        volumes = BeanHelper.getDocumentSearchService().searchVolumesForArchiveList(filter, true, false, storeNodeRefs);
    }

    @Override
    public boolean isShowNextEventDateColumn() {
        return true;
    }

    @Override
    public String getNextEventLabel() {
        return MessageUtil.getMessage("volume_search_planned_transfer");
    }

    @Override
    public boolean isShowRetaintionColumns() {
        return true;
    }

    @Override
    public boolean isShowMarkedForTransferColumn() {
        return true;
    }

    @Override
    public boolean isShowExportedForUamColumn() {
        return true;
    }

    @Override
    public boolean isShowStore() {
        return renderedFilterFields.contains(VolumeSearchModel.Props.STORE);
    }

    @Override
    public boolean isShowExportToUamButton() {
        return BeanHelper.getUserService().isArchivist(); // Administrator or archivist
    }

    public String getVolumeListTitle() {
        return MessageUtil.getMessage("archivals_volume_waiting_transfer_list_title");
    }

    @Override
    protected List<QName> getRenderedFilterFields() {
        if (renderedFilterFields == null) {
            renderedFilterFields = new ArrayList<QName>(Arrays.asList(
                    VolumeSearchModel.Props.NEXT_EVENT_DATE,
                    VolumeSearchModel.Props.NEXT_EVENT_DATE_END_DATE,
                    VolumeSearchModel.Props.HAS_ARCHIVAL_VALUE,
                    VolumeSearchModel.Props.RETAIN_PERMANENT,
                    VolumeSearchModel.Props.MARKED_FOR_TRANSFER,
                    VolumeSearchModel.Props.EXPORTED_FOR_UAM,
                    VolumeSearchModel.Props.RETAIN_UNTIL_DATE,
                    VolumeSearchModel.Props.RETAIN_UNTIL_DATE_END_DATE,
                    VolumeSearchModel.Props.EVENT_PLAN));
            if (showStoreFilterField()) {
                renderedFilterFields.add(VolumeSearchModel.Props.STORE);
            }
        }
        return renderedFilterFields;
    }

    @Override
    public List<DialogButtonConfig> getAdditionalButtons() {
        List<DialogButtonConfig> buttons = new ArrayList<DialogButtonConfig>(2);
        addGenerateWordFileButton(buttons);
        buttons.add(new DialogButtonConfig("volumeMarkForTransferButton", null, "archivals_volume_mark_for_transfer", "#{DialogManager.bean.markForTransferConfirm}", "false",
                null));
        buttons.add(new DialogButtonConfig("volumeExportToUamButton", null, "archivals_volume_export_to_uam", "#{DialogManager.bean.exportToUamConfirm}", "false", null));
        return buttons;
    }

    public void generateWordFile(ActionEvent event) {
        setConfirmGenerateWordFile(false);
        generateActivityAndWordFile(ActivityType.TO_TRANSFER_DOC, "archivals_volume_generate_word_file_to_transfer_template", ActivityStatus.FINISHED,
                "archivals_volume_generate_word_file_to_transfer_success", true);
    }

    @Override
    public String getGenerateWordFileConfirmationMessage() {
        return MessageUtil.getMessage("archivals_volume_generate_word_file_to_transfer_confirm");
    }

    @Override
    public boolean isConfirmMarkForTransfer() {
        return confirmMarkForTransfer;
    }

    public void markForTransferConfirm() {
        if (checkVolumesSelected()) {
            confirmMarkForTransfer = true;
        }
    }

    public void markForTransfer(ActionEvent event) {
        confirmMarkForTransfer = false;
        NodeRef activityRef = generateActivityAndWordFile(ActivityType.MARKED_FOR_TRANSFER, "archivals_volume_mark_for_transfer_template", ActivityStatus.IN_PROGRESS,
                null, false);
        if (activityRef != null) {
            List<NodeRef> selectedVolumes = getSelectedVolumes();
            BeanHelper.getArchivalsService().markForTransfer(selectedVolumes, activityRef);
            MessageUtil.addInfoMessage("archivals_volume_mark_for_transfer_success", selectedVolumes.size());
        }
    }

    @Override
    public void cancelAction(ActionEvent actionEvent) {
        setConfirmGenerateWordFile(false);
        confirmMarkForTransfer = false;
        confirmExportToUam = false;
    }

    public void exportToUamConfirm() {
        if (checkVolumesSelected()) {
            confirmExportToUam = true;
        }
    }

    public void exportToUam(ActionEvent event) {
        confirmExportToUam = false;
        Date exportStartDate = new Date();
        NodeRef activityRef = generateActivityAndWordFile(ActivityType.EXPORTED_FOR_UAM, "archivals_volume_export_to_uam_template", ActivityStatus.IN_PROGRESS,
                null, true, "archivals_volume_export_to_uam_error_no_template");
        if (activityRef != null) {
            List<NodeRef> selectedVolumes = getSelectedVolumes();
            BeanHelper.getArchivalsService().exportToUam(selectedVolumes, exportStartDate, activityRef);
            MessageUtil.addInfoMessage("archivals_volume_export_to_uam_success", selectedVolumes.size());
        }
    }

    @Override
    public boolean isConfirmExportToUam() {
        return confirmExportToUam;
    }

}
