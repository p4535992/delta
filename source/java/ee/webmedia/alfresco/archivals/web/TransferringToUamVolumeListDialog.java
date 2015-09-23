package ee.webmedia.alfresco.archivals.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.config.DialogsConfigElement.DialogButtonConfig;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.lang.time.FastDateFormat;

import ee.webmedia.alfresco.archivals.model.ActivityStatus;
import ee.webmedia.alfresco.archivals.model.ActivityType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.ComparableTransformer;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.search.model.VolumeSearchModel;

public class TransferringToUamVolumeListDialog extends VolumeArchiveBaseDialog {

    private static final long serialVersionUID = 1L;

    private boolean confirmTransfer;

    protected static ComparatorChain comparator;

    static {
        ComparatorChain chain = new ComparatorChain();
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Volume>() {
            @Override
            public Comparable<Date> tr(Volume volume) {
                return volume.getExportedForUamDateTime();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Volume>() {
            @Override
            public Comparable<Date> tr(Volume volume) {
                return volume.getRetainUntilDate();
            }
        }, new NullComparator()));
        chain.addComparator(VolumeArchiveBaseDialog.BASE_COMPARATOR);
        comparator = chain;
    }

    @Override
    protected void initFilterItems() {
        super.initFilterItems();
        getFilter().getProperties().put(VolumeSearchModel.Props.EXPORTED_FOR_UAM.toString(), Boolean.TRUE);
        getFilter().getProperties().put(VolumeSearchModel.Props.TRANSFER_CONFIRMED.toString(), Boolean.FALSE);
    }

    @Override
    protected Comparator<Volume> getComparator() {
        return comparator;
    }

    public String getVolumeListTitle() {
        return MessageUtil.getMessage("archivals_volume_in_transfer_list_title");
    }

    @Override
    public boolean isShowExportedForUamDateTimeColumn() {
        return true;
    }

    @Override
    public boolean isShowTransferringDeletingNextEventColumn() {
        return true;
    }

    @Override
    public boolean isShowTransferringNextEventDateColumn() {
        return true;
    }

    @Override
    public boolean isShowRetaintionColumns() {
        return true;
    }

    @Override
    protected List<String> getRenderedFilterFields() {
        if (renderedFilterFields == null) {
            renderedFilterFields = new ArrayList<String>(Arrays.asList(
                    VolumeSearchModel.Props.EXPORTED_FOR_UAM_DATE_TIME.toPrefixString(),
                    VolumeSearchModel.Props.EXPORTED_FOR_UAM_DATE_TIME_END_DATE.toPrefixString(),
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
        List<DialogButtonConfig> buttons = new ArrayList<DialogButtonConfig>(1);
        addGenerateWordFileButton(buttons);
        buttons.add(new DialogButtonConfig("volumeSetNextEventDestructionButton", null, "archivals_volume_set_next_event_destruction",
                "#{DialogManager.bean.setNextEventDestruction}", "false", null));
        buttons.add(new DialogButtonConfig("volumeConfirmTransferButton", null, "archivals_volume_confirm_transfer",
                "#{DialogManager.bean.transferConfirm}", "false", null));
        return buttons;
    }

    public void generateWordFile(ActionEvent event) {
        setConfirmGenerateWordFile(false);
        generateActivityAndWordFile(ActivityType.IN_TRANSFER_DOC, "archivals_volume_generate_word_file_in_transfer_template", ActivityStatus.FINISHED,
                "archivals_volume_generate_word_file_in_transfer_success", true);
    }

    @Override
    public String getGenerateWordFileConfirmationMessage() {
        return MessageUtil.getMessage("archivals_volume_generate_word_file_in_transfer_confirm");
    }

    public void setNextEventDestruction() {
        if (checkVolumesSelected()) {
            BeanHelper.getConfirmVolumeArchiveActionDialog().setupNextEventDestruction(getSelectedVolumes(), this);
            WebUtil.navigateTo(AlfrescoNavigationHandler.DIALOG_PREFIX + CONFIRM_VOLUME_ARCHIVE_ACTION_OUTCOME);
        }
    }

    public void generateNextEventDestructionActivity(Date newDate) {
        NodeRef activityRef = generateActivityAndWordFile(ActivityType.CHANGED_TRANSFER_NEXT_EVENT, "archivals_volume_next_event_destruction_template", ActivityStatus.IN_PROGRESS,
                null, false);
        if (activityRef != null) {
            List<NodeRef> selectedVolumes = getSelectedVolumes();
            BeanHelper.getArchivalsService().setNextEventDestruction(selectedVolumes, newDate, activityRef);
            MessageUtil.addInfoMessage("archivals_volume_next_event_destruction_success", selectedVolumes.size(), newDate != null ? FastDateFormat.getInstance("dd.MM.yyyy")
                    .format(newDate) : newDate);
        }
    }

    @Override
    public boolean isConfirmTransfer() {
        return confirmTransfer;
    }

    public void transferConfirm() {
        if (checkVolumesSelected()) {
            confirmTransfer = true;
        }
    }

    public void transfer(ActionEvent event) {
        confirmTransfer = false;
        Date confirmationDate = new Date();
        NodeRef activityRef = generateActivityAndWordFile(ActivityType.CONFIRM_TRANSFER, "archivals_volume_transfer_template", ActivityStatus.IN_PROGRESS,
                null, false);
        if (activityRef != null) {
            List<NodeRef> selectedVolumes = getSelectedVolumes();
            BeanHelper.getArchivalsService().confirmTransfer(selectedVolumes, confirmationDate, activityRef);
            MessageUtil.addInfoMessage("archivals_volume_transfer_success", selectedVolumes.size());
        }
    }

    @Override
    public void cancelAction(ActionEvent actionEvent) {
        setConfirmGenerateWordFile(false);
        confirmTransfer = false;
    }
}
