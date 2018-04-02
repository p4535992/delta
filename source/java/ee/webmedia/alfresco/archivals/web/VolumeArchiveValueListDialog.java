package ee.webmedia.alfresco.archivals.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.alfresco.web.config.DialogsConfigElement.DialogButtonConfig;

import ee.webmedia.alfresco.archivals.model.ActivityStatus;
import ee.webmedia.alfresco.archivals.model.ActivityType;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.volume.search.model.VolumeSearchModel;

public class VolumeArchiveValueListDialog extends VolumeArchiveBaseDialog {

    private static final long serialVersionUID = 1L;

    @Override
    protected void initFilterItems() {
        super.initFilterItems();
        getFilter().getProperties().put(VolumeSearchModel.Props.IS_APPRAISED.toString(), Boolean.FALSE);
    }

    public String getVolumeListTitle() {
        return MessageUtil.getMessage("archivals_volume_waiting_evaluation_list_title");
    }

    @Override
    protected List<String> getRenderedFilterFields() {
        if (renderedFilterFields == null) {
            renderedFilterFields = new ArrayList<String>(Arrays.asList(VolumeSearchModel.Props.VALID_TO.toPrefixString(),
                    VolumeSearchModel.Props.VALID_TO_END_DATE.toPrefixString(),
                    VolumeSearchModel.Props.HAS_ARCHIVAL_VALUE.toPrefixString(),
                    VolumeSearchModel.Props.STATUS.toPrefixString(),
                    VolumeSearchModel.Props.EVENT_PLAN.toPrefixString()));
            if (showStoreFilterField()) {
                renderedFilterFields.add(VolumeSearchModel.Props.STORE.toPrefixString());
            }
        }
        return renderedFilterFields;
    }

    @Override
    protected void loadStores() {
        loadAllStores();
    }

    @Override
    public boolean isShowStore() {
        return true;
    }

    @Override
    public boolean isShowStatusColumn() {
        return true;
    }

    @Override
    public List<DialogButtonConfig> getAdditionalButtons() {
        List<DialogButtonConfig> buttons = new ArrayList<DialogButtonConfig>(1);
        addGenerateExcelFileButton(buttons);
        return buttons;
    }

    @Override
    public void cancelAction(ActionEvent actionEvent) {
        setConfirmGeneration(false);
    }

    @Override
    public String getGenerateExcelFileConfirmationMessage() {
        return MessageUtil.getMessage("archivals_volume_generate_word_file_evaluation_confirm");
    }

    public void generateExcelFile(ActionEvent event) {
        setConfirmGeneration(false);
        generateActivityAndExcelFile(ActivityType.TO_APPRAISE_DOC, "archivals_volume_generate_word_file_evaluation_template", ActivityStatus.FINISHED,
                "archivals_volume_generate_word_file_evaluation_success");
    }

}
