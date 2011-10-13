package ee.webmedia.alfresco.volume.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getArchivalsService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getCaseService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getVolumeService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.volume.model.Volume;

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
        String volumeNodeRef = ActionUtil.getParam(event, PARAM_VOLUME_NODEREF);
        currentEntry = getVolumeService().getVolumeByNodeRef(volumeNodeRef);
    }

    public void addNewVolume(ActionEvent event) {
        newVolume = true;
        NodeRef seriesRef = new NodeRef(ActionUtil.getParam(event, PARAM_SERIES_NODEREF));
        // create new node for currentEntry
        currentEntry = getVolumeService().createVolume(seriesRef);
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
        currentEntry = getVolumeService().getVolumeByNodeRef(archivedVolumeNodeRef); // refresh screen with archived volume data
        ((MenuBean) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), MenuBean.BEAN_NAME)).updateTree();
        MessageUtil.addInfoMessage("volume_archive_success");
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

    // END: jsf actions/accessors

    private void resetFields() {
        currentEntry = null;
        newVolume = false;
        propertySheet = null;
    }

    // START: getters / setters

    public void setPropertySheet(UIPropertySheet propertySheet) {
        this.propertySheet = propertySheet;
    }

    public UIPropertySheet getPropertySheet() {
        return propertySheet;
    }
    // END: getters / setters
}
