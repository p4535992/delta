<<<<<<< HEAD
package ee.webmedia.alfresco.archivals.web;

import java.util.Date;
import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Riina Tens
 */
public class ConfirmVolumeArchiveActionDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "ConfirmVolumeArchiveActionDialog";
    private Date newReviewDate;
    private WaitingOverviewVolumeListDialog waitingForOverviewVolumeListDialog;
    TransferringToUamVolumeListDialog transferringToUamVolumeListDialog;

    public void setupNewReviewDate(List<NodeRef> volumesToUpdate, WaitingOverviewVolumeListDialog waitingForOverviewVolumeListDialog) {
        MessageUtil.addInfoMessage("archivals_volume_confirm_new_review_date_info", volumesToUpdate.size());
        this.waitingForOverviewVolumeListDialog = waitingForOverviewVolumeListDialog;
        transferringToUamVolumeListDialog = null;
    }

    public void setupNextEventDestruction(List<NodeRef> volumesToUpdate, TransferringToUamVolumeListDialog transferringToUamVolumeListDialog) {
        MessageUtil.addInfoMessage("archivals_volume_confirm_next_event_destruction_info", volumesToUpdate.size());
        this.transferringToUamVolumeListDialog = transferringToUamVolumeListDialog;
        waitingForOverviewVolumeListDialog = null;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (newReviewDate != null) {
            newReviewDate.setHours(0);
            newReviewDate.setMinutes(0);
            newReviewDate.setSeconds(0);
        }
        if (isReviewDateConfirmation()) {
            waitingForOverviewVolumeListDialog.generateNewReviewDateActivity(newReviewDate);
        } else {
            transferringToUamVolumeListDialog.generateNextEventDestructionActivity(newReviewDate);
        }
        return outcome;
    }

    private boolean isReviewDateConfirmation() {
        return waitingForOverviewVolumeListDialog != null;
    }

    @Override
    public String getFinishButtonLabel() {
        if (isReviewDateConfirmation()) {
            return MessageUtil.getMessage("archivals_volume_confirm_new_review_date_button");
        }
        return MessageUtil.getMessage("archivals_volume_confirm_next_event_button");
    }

    @Override
    public String getContainerTitle() {
        if (isReviewDateConfirmation()) {
            return MessageUtil.getMessage("archivals_volume_confirm_new_review_date_dialog");
        }
        return MessageUtil.getMessage("archivals_volume_confirm_next_event_destruction_dialog");
    }

    public Date getNewReviewDate() {
        return newReviewDate;
    }

    public void setNewReviewDate(Date newReviewDate) {
        this.newReviewDate = newReviewDate;
    }

    public String getConfirmationDateTitle() {
        if (isReviewDateConfirmation()) {
            return MessageUtil.getMessage("archivals_volume_confirm_new_review_date_date");
        }
        return MessageUtil.getMessage("archivals_volume_confirm_next_event_destruction_date");
    }

    public String getBlockTitle() {
        if (isReviewDateConfirmation()) {
            return MessageUtil.getMessage("archivals_volume_confirm_new_review_date_block");
        }
        return MessageUtil.getMessage("archivals_volume_confirm_next_event_destruction_block");
    }

}
=======
package ee.webmedia.alfresco.archivals.web;

import java.util.Date;
import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.utils.MessageUtil;

public class ConfirmVolumeArchiveActionDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "ConfirmVolumeArchiveActionDialog";
    private Date newReviewDate;
    private WaitingOverviewVolumeListDialog waitingForOverviewVolumeListDialog;
    TransferringToUamVolumeListDialog transferringToUamVolumeListDialog;

    public void setupNewReviewDate(List<NodeRef> volumesToUpdate, WaitingOverviewVolumeListDialog waitingForOverviewVolumeListDialog) {
        MessageUtil.addInfoMessage("archivals_volume_confirm_new_review_date_info", volumesToUpdate.size());
        this.waitingForOverviewVolumeListDialog = waitingForOverviewVolumeListDialog;
        transferringToUamVolumeListDialog = null;
    }

    public void setupNextEventDestruction(List<NodeRef> volumesToUpdate, TransferringToUamVolumeListDialog transferringToUamVolumeListDialog) {
        MessageUtil.addInfoMessage("archivals_volume_confirm_next_event_destruction_info", volumesToUpdate.size());
        this.transferringToUamVolumeListDialog = transferringToUamVolumeListDialog;
        waitingForOverviewVolumeListDialog = null;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (newReviewDate != null) {
            newReviewDate.setHours(0);
            newReviewDate.setMinutes(0);
            newReviewDate.setSeconds(0);
        }
        if (isReviewDateConfirmation()) {
            waitingForOverviewVolumeListDialog.generateNewReviewDateActivity(newReviewDate);
        } else {
            transferringToUamVolumeListDialog.generateNextEventDestructionActivity(newReviewDate);
        }
        return outcome;
    }

    private boolean isReviewDateConfirmation() {
        return waitingForOverviewVolumeListDialog != null;
    }

    @Override
    public String getFinishButtonLabel() {
        if (isReviewDateConfirmation()) {
            return MessageUtil.getMessage("archivals_volume_confirm_new_review_date_button");
        }
        return MessageUtil.getMessage("archivals_volume_confirm_next_event_button");
    }

    @Override
    public String getContainerTitle() {
        if (isReviewDateConfirmation()) {
            return MessageUtil.getMessage("archivals_volume_confirm_new_review_date_dialog");
        }
        return MessageUtil.getMessage("archivals_volume_confirm_next_event_destruction_dialog");
    }

    public Date getNewReviewDate() {
        return newReviewDate;
    }

    public void setNewReviewDate(Date newReviewDate) {
        this.newReviewDate = newReviewDate;
    }

    public String getConfirmationDateTitle() {
        if (isReviewDateConfirmation()) {
            return MessageUtil.getMessage("archivals_volume_confirm_new_review_date_date");
        }
        return MessageUtil.getMessage("archivals_volume_confirm_next_event_destruction_date");
    }

    public String getBlockTitle() {
        if (isReviewDateConfirmation()) {
            return MessageUtil.getMessage("archivals_volume_confirm_new_review_date_block");
        }
        return MessageUtil.getMessage("archivals_volume_confirm_next_event_destruction_block");
    }

}
>>>>>>> develop-5.1
