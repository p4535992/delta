package ee.webmedia.alfresco.volume.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getCaseService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getVolumeService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.collections.Closure;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.archivals.model.ArchivalsModel;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.web.BeanHelper;
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
    private boolean volumeRefInvalid;

    private static final Log LOG = LogFactory.getLog(VolumeDetailsDialog.class);

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

    public String action() {
        String dialogPrefix = AlfrescoNavigationHandler.DIALOG_PREFIX;
        boolean tempState = volumeRefInvalid;
        volumeRefInvalid = false;
        return dialogPrefix + (tempState ? VolumeListDialog.DIALOG_NAME : "volumeDetailsDialog");
    }

    // START: jsf actions/accessors
    public void showDetails(ActionEvent event) {
        NodeRef volumeNodeRef = ActionUtil.getParam(event, PARAM_VOLUME_NODEREF, NodeRef.class);
        if (!nodeExists(volumeNodeRef)) {
            MessageUtil.addInfoMessage("volume_noderef_not_found");
            volumeRefInvalid = true;
            return;
        }
        addInfoMessage(volumeNodeRef);
        reload(volumeNodeRef);
        deletedDocuments = getVolumeService().getDeletedDocuments(volumeNodeRef);
    }

    private void addInfoMessage(NodeRef volumeRef) {
        Volume vol = getVolumeService().getVolumeByNodeRef(volumeRef);
        Boolean marked = (Boolean) vol.getProperty(VolumeModel.Props.MARKED_FOR_ARCHIVING.toString());
        if (marked != null && marked) {
            MessageUtil.addInfoMessage("volume_marked_for_archiving");
        }
    }

    private void reload(NodeRef volumeNodeRef) {
        currentEntry = getVolumeService().getVolumeByNodeRef(volumeNodeRef);
    }

    public void addNewVolume(ActionEvent event) {
        clearPropSheet();
        newVolume = true;
        NodeRef seriesRef = new NodeRef(ActionUtil.getParam(event, PARAM_SERIES_NODEREF));
        // create new node for currentEntry
        currentEntry = getVolumeService().createVolume(seriesRef);
        deletedDocuments = Collections.<DeletedDocument> emptyList();
    }

    public Node getCurrentNode() {
        return currentEntry == null ? null : currentEntry.getNode();
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
                Pair<String, Object[]> error = getVolumeService().closeVolume(currentEntry);
                reload(currentEntry.getNode().getNodeRef());
                if (error != null) {
                    MessageUtil.addErrorMessage(error.getFirst(), error.getSecond());
                } else {
                    MessageUtil.addInfoMessage("volume_close_success");
                }
            } catch (UnableToPerformException e) {
                MessageUtil.addStatusMessage(e);
                return;
            }
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
                removeVolumeFromArchiveList(currentVolumeNode.getNodeRef());
                getVolumeService().openVolume(currentEntry);
                reload(currentEntry.getNode().getNodeRef());
            } catch (UnableToPerformException e) {
                MessageUtil.addStatusMessage(e);
                return;
            }
            MessageUtil.addInfoMessage("volume_open_success");
            clearPropSheet();
        }
    }

    private void removeVolumeFromArchiveList(NodeRef volumeNodeRef) {
        BeanHelper.getArchivalsService().removeVolumeFromArchivingList(volumeNodeRef);
    }

    private void clearPropSheet() {
        if (propertySheet != null) {
            propertySheet.getChildren().clear();
        }
    }

    private void setArchivingProperty(NodeRef volumeRef, Boolean value) {
        Volume volume = getVolumeService().getVolumeByNodeRef(volumeRef);
        volume.setProperty(VolumeModel.Props.MARKED_FOR_ARCHIVING.toString(), value);
        getVolumeService().saveOrUpdate(volume);
    }

    public void archive(@SuppressWarnings("unused") ActionEvent event) {
        Assert.notNull(currentEntry, "No current volume");
        NodeRef volumeRef = currentEntry.getNode().getNodeRef();
        if (!BeanHelper.getNodeService().exists(volumeRef)) {
            MessageUtil.addInfoMessage("volume_noderef_not_found");
            return;
        }

        if (!isVolumeInArchivingQueue(volumeRef)) {
            BeanHelper.getArchivalsService().addVolumeToArchivingList(volumeRef);
            setArchivingProperty(volumeRef, Boolean.TRUE);
            LOG.info("Volume with nodeRef=" + volumeRef + " was added to archive queue.");
        } else {
            MessageUtil.addInfoMessage("volume_archive_already_in_queue");
            return;
        }
        MessageUtil.addInfoMessage("volume_archive_added_to_queue");
    }

    private boolean isVolumeInArchivingQueue(NodeRef volumeRef) {
        List<NodeRef> archiveJobs = BeanHelper.getArchivalsService().getAllInQueueJobs();
        for (NodeRef jobRef : archiveJobs) {
            if (volumeRef.equals(getNodeService().getProperty(jobRef, ArchivalsModel.Props.VOLUME_REF))) {
                return true;
            }
        }
        return false;
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
        return currentEntry != null ? currentEntry.getNode() : null;
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

    // TODO should be with is prefix
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
        return deletedDocuments == null ? new ArrayList<DeletedDocument>() : deletedDocuments;
    }

    // FIXME to milleks see meetod?
    public void setDeletedDocuments(List<DeletedDocument> deletedDocuments) {
        this.deletedDocuments = deletedDocuments;
    }
    // END: getters / setters
}
