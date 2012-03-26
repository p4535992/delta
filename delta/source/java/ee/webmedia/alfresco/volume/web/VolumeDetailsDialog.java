package ee.webmedia.alfresco.volume.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getArchivalsService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getCaseService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getVolumeService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.collections.Closure;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.volume.model.DeletedDocument;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * Form backing bean for Volumes details
 * 
 * @author Ats Uiboupin
 */
public class VolumeDetailsDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "VolumeDetailsDialog";

    private static final String PARAM_SERIES_NODEREF = "seriesNodeRef";
    private static final String PARAM_VOLUME_NODEREF = "volumeNodeRef";

    private Volume currentEntry;
    private List<DeletedDocument> deletedDocuments;
    private boolean newVolume;
    private transient UIPropertySheet propertySheet;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        getVolumeService().saveOrUpdate(currentEntry);
        resetFields();
        MessageUtil.addInfoMessage("save_success");
        return outcome;
    }

    @Override
    public String cancel() {
        resetFields();
        return super.cancel();
    }

    // START: jsf actions/accessors
    public void showDetails(ActionEvent event) {
        NodeRef volumeNodeRef = ActionUtil.getParam(event, PARAM_VOLUME_NODEREF, NodeRef.class);
        reload(volumeNodeRef);
        deletedDocuments = getVolumeService().getDeletedDocuments(volumeNodeRef);
    }

    private void reload(NodeRef volumeNodeRef) {
        currentEntry = getVolumeService().getVolumeByNodeRef(volumeNodeRef);
    }

    public void addNewVolume(ActionEvent event) {
        newVolume = true;
        NodeRef seriesRef = new NodeRef(ActionUtil.getParam(event, PARAM_SERIES_NODEREF));
        // create new node for currentEntry
        currentEntry = getVolumeService().createVolume(seriesRef);
        deletedDocuments = Collections.<DeletedDocument> emptyList();
    }

    public Node getCurrentNode() {
        return currentEntry.getNode();
    }

    public Volume getCurrentVolume() {
        return currentEntry;
    }

    public void close(@SuppressWarnings("unused") ActionEvent event) {
        Node currentVolumeNode = currentEntry.getNode();
        if (currentVolumeNode instanceof TransientNode || currentVolumeNode == null) {
            return;
        }
        if (!isClosed()) {
            try {
                getVolumeService().closeVolume(currentEntry);
                reload(currentEntry.getNode().getNodeRef());
            } catch (UnableToPerformException e) {
                MessageUtil.addErrorMessage(e.getMessage());
                return;
            }
            MessageUtil.addInfoMessage("volume_close_success");
            clearPropSheet();
        }
    }

    public void open(@SuppressWarnings("unused") ActionEvent event) {
        Node currentVolumeNode = currentEntry.getNode();
        if (currentVolumeNode instanceof TransientNode || currentVolumeNode == null) {
            return;
        }
        if (!isOpened()) {
            try {
                getVolumeService().openVolume(currentEntry);
                reload(currentEntry.getNode().getNodeRef());
            } catch (UnableToPerformException e) {
                MessageUtil.addErrorMessage(e.getMessage());
                return;
            }
            MessageUtil.addInfoMessage("volume_open_success");
            clearPropSheet();
        }
    }

    private void clearPropSheet() {
        if (propertySheet != null) {
            propertySheet.getChildren().clear();
        }
    }

    public void archive(@SuppressWarnings("unused") ActionEvent event) {
        Assert.notNull(currentEntry, "No current volume");
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        NodeRef archivedVolumeNodeRef = getArchivalsService().archiveVolume(currentEntry.getNode().getNodeRef(),
                String.format(MessageUtil.getMessage("volume_archiving_note"), df.format(new Date())));
        reload(archivedVolumeNodeRef);
        ((MenuBean) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), MenuBean.BEAN_NAME)).updateTree();
        MessageUtil.addInfoMessage("volume_archive_success");
    }

    /**
     * Query callback method executed by the component generated by GeneralSelectorGenerator.
     * This method is part of the contract to the GeneralSelectorGenerator, it is up to the backing bean
     * to execute whatever query is appropriate and populate <code>selectComponent</code> with selection items.<br>
     * 
     * @param context - FacesContext for creating selection items
     * @param selectComponent - selectComponent that will be rendered(use <code>selectComponent.getChildren()</code> to add selection items)
     * @return A collection of UISelectItem objects containing the selection items to show on form.
     */
    public List<SelectItem> findVolumeTypesAllowed(FacesContext context, UIInput selectComponent) {
        @SuppressWarnings("unchecked")
        List<String> seriesVolTypes = (List<String>) BeanHelper.getNodeService().getProperty(currentEntry.getSeriesNodeRef(), SeriesModel.Props.VOL_TYPE);
        List<SelectItem> selectItems = new ArrayList<SelectItem>(seriesVolTypes.size());
        ComponentUtil.addDefault(selectItems, context);
        for (String volType : seriesVolTypes) {
            if (!volType.equals(VolumeType.CASE_FILE.name())) {
                selectItems.add(new SelectItem(volType, MessageUtil.getMessage(VolumeType.valueOf(volType))));
            }
        }
        WebUtil.sort(selectItems);
        return selectItems;
    }

    public boolean isClosed() {
        return getVolumeService().isClosed(getCurrentNode());
    }

    public boolean isOpened() {
        return getVolumeService().isOpened(getCurrentNode());
    }

    public boolean isNew() {
        return newVolume;
    }

    @Override
    public Object getActionsContext() {
        return currentEntry;
    }

    public Boolean disableContainsCases() {
        return !isNew() && (DocListUnitStatus.CLOSED.equals(currentEntry.getStatus())
                            || DocListUnitStatus.DESTROYED.equals(currentEntry.getStatus())
                            || getCaseService().getCasesCountByVolume(currentEntry.getNode().getNodeRef()) > 0
                            || getDocumentService().getDocumentsCountByVolumeOrCase(currentEntry.getNode().getNodeRef()) > 0);
    }

    public Boolean disableCasesCreatableByUser() {
        return !Boolean.TRUE.equals(currentEntry.getNode().getProperties().get(VolumeModel.Props.CONTAINS_CASES))
                || DocListUnitStatus.CLOSED.equals(currentEntry.getStatus())
                || DocListUnitStatus.DESTROYED.equals(currentEntry.getStatus());
    }

    // TODO Vladimir: should be with is prefix
    public Boolean volumeMarkFieldReadOnly() {
        Map<String, Object> properties = currentEntry.getNode().getProperties();
        String status = (String) properties.get(VolumeModel.Props.STATUS);
        return DocListUnitStatus.CLOSED.equals(status) || DocListUnitStatus.DESTROYED.equals(status)
                || VolumeType.SUBJECT_FILE.name().equals(properties.get(VolumeModel.Props.VOLUME_TYPE));
    }

    public void volumeTypeValueChanged(final ValueChangeEvent event) {
        ComponentUtil.executeLater(PhaseId.INVOKE_APPLICATION, getPropertySheet(), new Closure() {
            @Override
            public void execute(Object input) {
                if (event.getNewValue().equals(VolumeType.SUBJECT_FILE.name())) {
                    String seriesMark = (String) BeanHelper.getNodeService().getProperty(getCurrentVolume().getSeriesNodeRef(), SeriesModel.Props.SERIES_IDENTIFIER);
                    getCurrentNode().getProperties().put(VolumeModel.Props.VOLUME_MARK.toString(),
                            seriesMark);
                }
                if (propertySheet != null) {
                    propertySheet.getChildren().clear();
                    propertySheet.getClientValidations().clear();
                    propertySheet.setMode(null);
                    propertySheet.setNode(null);
                }
            }
        });
    }

    public void containsCasesValueChanged(final ValueChangeEvent event) {
        ComponentUtil.executeLater(PhaseId.INVOKE_APPLICATION, getPropertySheet(), new Closure() {
            @Override
            public void execute(Object input) {
                getCurrentNode().getProperties().put(VolumeModel.Props.CASES_CREATABLE_BY_USER.toString(), event.getNewValue().equals(Boolean.TRUE));
                if (propertySheet != null) {
                    propertySheet.getChildren().clear();
                    propertySheet.getClientValidations().clear();
                    propertySheet.setMode(null);
                    propertySheet.setNode(null);
                }
            }
        });
    }

    // END: jsf actions/accessors

    private void resetFields() {
        currentEntry = null;
        newVolume = false;
        propertySheet = null;
        deletedDocuments = null;
    }

    // START: getters / setters

    public void setPropertySheet(UIPropertySheet propertySheet) {
        this.propertySheet = propertySheet;
    }

    public UIPropertySheet getPropertySheet() {
        return propertySheet;
    }

    public List<DeletedDocument> getDeletedDocuments() {
        return deletedDocuments;
    }

    // FIXME Ats to Kaarel - milleks see meetod?
    public void setDeletedDocuments(List<DeletedDocument> deletedDocuments) {
        this.deletedDocuments = deletedDocuments;
    }
    // END: getters / setters
}
