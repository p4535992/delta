package ee.webmedia.alfresco.volume.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getArchivalsService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getCaseService;
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
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.collections.Closure;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.log.web.LogBlockBean;
import ee.webmedia.alfresco.document.search.web.AbstractSearchBlockBean;
import ee.webmedia.alfresco.document.search.web.BlockBeanProviderProvider;
import ee.webmedia.alfresco.document.search.web.SearchBlockBean;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
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
public class VolumeDetailsDialog extends BaseDialogBean implements BlockBeanProviderProvider {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "VolumeDetailsDialog";

    private static final String PARAM_SERIES_NODEREF = "seriesNodeRef";
    private static final String PARAM_VOLUME_NODEREF = "volumeNodeRef";

    private Volume currentEntry;
    private List<DeletedDocument> deletedDocuments;
    private boolean newVolume;
    private transient UIPropertySheet propertySheet;

    @Override
    public void init(Map<String, String> parameters) {
        super.init(parameters);
        Node node = currentEntry.getNode();
        BeanHelper.getSearchBlockBean().init(node);
        BeanHelper.getAssocsBlockBean().init(node);
    }

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
        showDetails(volumeNodeRef);
    }

    public void showDetails(NodeRef volumeNodeRef) {
        reload(volumeNodeRef);
        deletedDocuments = getVolumeService().getDeletedDocuments(volumeNodeRef);
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

    public void searchDocsAndCases(@SuppressWarnings("unused") ActionEvent event) {
        SearchBlockBean searchBlockBean = BeanHelper.getSearchBlockBean();
        searchBlockBean.init(currentEntry.getNode());
        searchBlockBean.setExpanded(true);
    }

    public boolean isAssocsBlockExpanded() {
        return true;
    }

    public Node getCurrentNode() {
        return currentEntry.getNode();
    }

    public Node getNode() {
        return getCurrentNode();
    }

    public Volume getCurrentVolume() {
        return currentEntry;
    }

    public boolean isShowDocsAndCasesAssocs() {
        return RepoUtil.isSaved(currentEntry.getNode());
    }

    public boolean isShowAddAssocsLink() {
        return RepoUtil.isSaved(currentEntry.getNode());
    }

    public boolean isShowSearchBlock() {
        return BeanHelper.getSearchBlockBean().isExpanded();
    }

    public boolean isInEditMode() {
        return false;
    }

    public void close(@SuppressWarnings("unused") ActionEvent event) {
        WmNode currentVolumeNode = currentEntry.getNode();
        if (currentVolumeNode == null || currentVolumeNode.isUnsaved()) {
            return;
        }
        if (!isClosed()) {
            try {
                getVolumeService().closeVolume(currentEntry.getNodeRef());
                reload(currentEntry.getNode().getNodeRef());
            } catch (UnableToPerformException e) {
                MessageUtil.addStatusMessage(e);
                return;
            }
            MessageUtil.addInfoMessage("volume_close_success");
            clearPropSheet();
        }
    }

    public void open(@SuppressWarnings("unused") ActionEvent event) {
        WmNode currentVolumeNode = currentEntry.getNode();
        if (currentVolumeNode == null || currentVolumeNode.isUnsaved()) {
            return;
        }
        if (!isOpened()) {
            try {
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

    public String delete() {
        WmNode currentVolumeNode = currentEntry.getNode();
        if (currentVolumeNode == null || currentVolumeNode.isUnsaved()) {
            return null;
        }
        if (!isOpened()) {
            try {
                getVolumeService().delete(currentEntry);
                MessageUtil.addInfoMessage("volume_delete_success");
                return getDefaultCancelOutcome();
            } catch (UnableToPerformException e) {
                MessageUtil.addStatusMessage(e);
                return null;
            }
        }
        return null;
    }

    private void clearPropSheet() {
        if (propertySheet != null) {
            propertySheet.getChildren().clear();
        }
    }

    public void archive(@SuppressWarnings("unused") ActionEvent event) {
        Assert.notNull(currentEntry, "No current volume");
        NodeRef archivedVolumeNodeRef = archiveVolume(currentEntry.getNode().getNodeRef());
        reload(archivedVolumeNodeRef);
        MessageUtil.addInfoMessage("volume_archive_success");
        clearPropSheet();
    }

    public NodeRef archiveVolume(NodeRef volumeRef) {
        return getArchivalsService().archiveVolumeOrCaseFile(volumeRef);
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
                || getCaseService().getCasesCountByVolume(currentEntry.getNode().getNodeRef()) > 0);
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
        return DocListUnitStatus.CLOSED.equals(status) || DocListUnitStatus.DESTROYED.equals(status);
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

    @Override
    public AbstractSearchBlockBean getSearch() {
        return BeanHelper.getSearchBlockBean();
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

    @Override
    public LogBlockBean getLog() {
        // not used
        return null;
    }
}
