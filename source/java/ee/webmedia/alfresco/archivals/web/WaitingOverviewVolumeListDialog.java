package ee.webmedia.alfresco.archivals.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.config.DialogsConfigElement.DialogButtonConfig;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.lang.time.FastDateFormat;

import ee.webmedia.alfresco.archivals.model.ActivityStatus;
import ee.webmedia.alfresco.archivals.model.ActivityType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.eventplan.model.FirstEvent;
import ee.webmedia.alfresco.utils.ComparableTransformer;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.search.model.VolumeSearchModel;

public class WaitingOverviewVolumeListDialog extends VolumeArchiveBaseDialog {

    private static final long serialVersionUID = 1L;

    protected static ComparatorChain comparator;

    static {
        ComparatorChain chain = new ComparatorChain();
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Volume>() {
            @Override
            public Comparable<Date> tr(Volume volume) {
                return volume.getNextEventDate();
            }
        }, new NullComparator()));
        chain.addComparator(VolumeArchiveBaseDialog.BASE_COMPARATOR);
        comparator = chain;
    }

    @Override
    protected void loadStores() {
        loadAllStores();
    }

    @Override
    protected void initFilterItems() {
        super.initFilterItems();
        Node filter = getFilter();
        filter.getProperties().put(VolumeSearchModel.Props.NEXT_EVENT.toString(), Collections.singletonList(FirstEvent.REVIEW.name()));
        filter.getAspects().add(VolumeSearchModel.Aspects.PLANNED_REVIEW);
    }

    @Override
    protected Comparator<Volume> getComparator() {
        return comparator;
    }

    @Override
    public boolean isShowNextEventDateColumn() {
        return true;
    }

    @Override
    public String getNextEventLabel() {
        return MessageUtil.getMessage("volume_search_planned_review");
    }

    @Override
    public boolean isShowRetaintionColumns() {
        return true;
    }

    public String getVolumeListTitle() {
        return MessageUtil.getMessage("archivals_volume_waiting_review_list_title");
    }

    @Override
    protected List<String> getRenderedFilterFields() {
        if (renderedFilterFields == null) {
            renderedFilterFields = new ArrayList<String>(Arrays.asList(
                    VolumeSearchModel.Props.NEXT_EVENT_DATE.toPrefixString(),
                    VolumeSearchModel.Props.NEXT_EVENT_DATE_END_DATE.toPrefixString(),
                    VolumeSearchModel.Props.RETAIN_UNTIL_DATE.toPrefixString(),
                    VolumeSearchModel.Props.RETAIN_UNTIL_DATE_END_DATE.toPrefixString(),
                    VolumeSearchModel.Props.EVENT_PLAN.toPrefixString()));
        }
        return renderedFilterFields;
    }

    @Override
    public List<DialogButtonConfig> getAdditionalButtons() {
        List<DialogButtonConfig> buttons = new ArrayList<DialogButtonConfig>(2);
        addGenerateWordFileButton(buttons);
        buttons.add(new DialogButtonConfig("volumeSetNEwReviewDateButton", null, "archivals_volume_set_new_review_date", "#{DialogManager.bean.setNewReviewDate}", "false",
                null));
        return buttons;
    }

    @Override
    public void cancelAction(ActionEvent actionEvent) {
        setConfirmGenerateWordFile(false);
    }

    public void generateWordFile(ActionEvent event) {
        setConfirmGenerateWordFile(false);
        generateActivityAndWordFile(ActivityType.TO_REVIEW_DOC, "archivals_volume_generate_word_file_overview_template", ActivityStatus.FINISHED,
                "archivals_volume_generate_word_file_overview_success", true);
    }

    @Override
    public String getGenerateWordFileConfirmationMessage() {
        return MessageUtil.getMessage("archivals_volume_generate_word_file_overview_confirm");
    }

    public void setNewReviewDate() {
        if (checkVolumesSelected()) {
            BeanHelper.getConfirmVolumeArchiveActionDialog().setupNewReviewDate(getSelectedVolumes(), this);
            WebUtil.navigateTo(AlfrescoNavigationHandler.DIALOG_PREFIX + CONFIRM_VOLUME_ARCHIVE_ACTION_OUTCOME);
        }
    }

    public void generateNewReviewDateActivity(Date newReviewDate) {
        NodeRef activityRef = generateActivityAndWordFile(ActivityType.CHANGED_NEXT_REVIEW_DATE, "archivals_volume_new_review_date_template", ActivityStatus.IN_PROGRESS,
                null, false);
        if (activityRef != null) {
            List<NodeRef> selectedVolumes = getSelectedVolumes();
            BeanHelper.getArchivalsService().setNewReviewDate(selectedVolumes, newReviewDate, activityRef);
            MessageUtil.addInfoMessage("archivals_volume_next_review_date_success", selectedVolumes.size(), newReviewDate != null ? FastDateFormat.getInstance("dd.MM.yyyy")
                    .format(newReviewDate) : newReviewDate);
        }
    }

}
