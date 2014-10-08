package ee.webmedia.alfresco.archivals.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.config.DialogsConfigElement.DialogButtonConfig;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.volume.search.model.VolumeSearchModel;

public class MoveVolumeToArchiveListDialog extends VolumeArchiveBaseDialog {

    private static final long serialVersionUID = 1L;

    private boolean confirmArchive;

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(MoveVolumeToArchiveListDialog.class);

    @Override
    protected void initFilterItems() {
        super.initFilterItems();
        filter.getProperties().put(VolumeSearchModel.Props.STORE.toString(), Arrays.asList(BeanHelper.getFunctionsService().getFunctionsRoot()));
        filter.getProperties().put(VolumeSearchModel.Props.STATUS.toString(), Collections.singletonList(DocListUnitStatus.CLOSED.getValueName()));
    }

    @Override
    protected boolean executeInitialSearch() {
        Long months = BeanHelper.getParametersService().getLongParameter(Parameters.MOVE_TO_ARCHIVE_LIST_DEFAULT_MONTHS);
        return months != null && months > 0;
    }

    public String getVolumeListTitle() {
        return MessageUtil.getMessage("archivals_volume_move_to_archivation_list_title");
    }

    @Override
    protected List<QName> getRenderedFilterFields() {
        if (renderedFilterFields == null) {
            renderedFilterFields = new ArrayList<QName>(Arrays.asList(
                    VolumeSearchModel.Props.VALID_TO,
                    VolumeSearchModel.Props.VALID_TO_END_DATE,
                    VolumeSearchModel.Props.EVENT_PLAN));
        }
        return renderedFilterFields;
    }

    @Override
    public boolean isShowStatusColumn() {
        return true;
    }

    @Override
    public List<DialogButtonConfig> getAdditionalButtons() {
        List<DialogButtonConfig> buttons = new ArrayList<DialogButtonConfig>(1);
        buttons.add(new DialogButtonConfig("volumeArchiveButton", null, "archivals_volume_archive", "#{DialogManager.bean.archiveConfirm}", "false", null));
        return buttons;
    }

    public void archiveConfirm() {
        if (checkVolumesSelected()) {
            confirmArchive = true;
        }
    }

    public void archive(ActionEvent event) {
        confirmArchive = false;
        final List<NodeRef> volumesToArchive = getSelectedVolumes();
        for (NodeRef ref : volumesToArchive) {
            if (!nodeExists(ref)) {
                LOG.warn("Archiving of volume [nodeRef=" + ref + "] failed. Node does not exist!");
                continue;
            }
            if (!BeanHelper.getArchivalsService().isVolumeInArchivingQueue(ref)) {
                BeanHelper.getArchivalsService().addVolumeOrCaseToArchivingList(ref);
                LOG.info("Volume with nodeRef=" + ref + " was added to archive queue.");
            } else {
                LOG.info("Volume [nodeRef=" + ref + "] has already been added to archiving queue.");
                continue;
            }
        }
        MessageUtil.addInfoMessage("archivals_volume_archive_started", volumesToArchive.size());
    }

    @Override
    public void cancelAction(ActionEvent actionEvent) {
        confirmArchive = false;
    }

    @Override
    public boolean isConfirmArchive() {
        return confirmArchive;
    }

}
