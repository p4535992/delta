package ee.webmedia.alfresco.archivals.web;

import java.util.Arrays;
import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.util.Pair;
import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.utils.MessageUtil;

public class ArchivationActionsDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;
    @SuppressWarnings("unchecked")
    private static final List<Pair<String, String>> ARCHIVATION_ACTIONS = Arrays.asList(
            new Pair<String, String>(MessageUtil.getMessage("archivals_volume_move_to_archivation_list"), "dialog:moveVolumeToArchiveListDialog"),
            new Pair<String, String>(MessageUtil.getMessage("archivals_volume_waiting_evaluation_list"), "dialog:volumeArchivalValueListDialog"),
            new Pair<String, String>(MessageUtil.getMessage("archivals_volume_waiting_review_list"), "dialog:waitingOverviewVolumeListDialog"),
            new Pair<String, String>(MessageUtil.getMessage("archivals_volume_waiting_transfer_list"), "dialog:waitingForTransferVolumeListDialog"),
            new Pair<String, String>(MessageUtil.getMessage("archivals_volume_in_transfer_list"), "dialog:transferringToUamVolumeListDialog"),
            new Pair<String, String>(MessageUtil.getMessage("archivals_volume_waiting_destruction_list"), "dialog:waitingForDestructionVolumeListDialog"),
            new Pair<String, String>(MessageUtil.getMessage("archival_activities"), "dialog:archivalActivitiesListDialog"));

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return null;
    }

    public List<Pair<String, String>> getArchivationActions() {
        return ARCHIVATION_ACTIONS;
    }

}
