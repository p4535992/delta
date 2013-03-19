package ee.webmedia.alfresco.archivals.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.config.DialogsConfigElement.DialogButtonConfig;

import ee.webmedia.alfresco.archivals.model.ActivityStatus;
import ee.webmedia.alfresco.archivals.model.ActivityType;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.volume.search.model.VolumeSearchModel;

/**
 * @author Riina Tens
 */
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
    protected List<QName> getRenderedFilterFields() {
        if (renderedFilterFields == null) {
            renderedFilterFields = new ArrayList<QName>(Arrays.asList(VolumeSearchModel.Props.VALID_TO,
                    VolumeSearchModel.Props.VALID_TO_END_DATE,
                    VolumeSearchModel.Props.HAS_ARCHIVAL_VALUE,
                    VolumeSearchModel.Props.STATUS,
                    VolumeSearchModel.Props.EVENT_PLAN));
            if (showStoreFilterField()) {
                renderedFilterFields.add(VolumeSearchModel.Props.STORE);
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
        addGenerateWordFileButton(buttons);
        return buttons;
    }

    @Override
    public void cancelAction(ActionEvent actionEvent) {
        setConfirmGenerateWordFile(false);
    }

    @Override
    public String getGenerateWordFileConfirmationMessage() {
        return MessageUtil.getMessage("archivals_volume_generate_word_file_evaluation_confirm");
    }

    public void generateWordFile(ActionEvent event) {
        setConfirmGenerateWordFile(false);
        generateActivityAndWordFile(ActivityType.TO_APPRAISE_DOC, "archivals_volume_generate_word_file_evaluation_template", ActivityStatus.FINISHED,
                "archivals_volume_generate_word_file_evaluation_success", true);
    }

}
