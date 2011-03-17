package ee.webmedia.alfresco.volume.web;

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
import org.springframework.util.Assert;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.archivals.service.ArchivalsService;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.volume.exception.VolumeContainsCasesException;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * Form backing bean for Volumes details
 * 
 * @author Ats Uiboupin
 */
public class VolumeDetailsDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    private static final String PARAM_SERIES_NODEREF = "seriesNodeRef";
    private static final String PARAM_VOLUME_NODEREF = "volumeNodeRef";
    private transient VolumeService volumeService;
    private transient ArchivalsService archivalsService;
    private transient CaseService caseService;
    private transient DocumentService documentService;
    private Volume currentEntry;
    private boolean newVolume;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        try {
            getVolumeService().saveOrUpdate(currentEntry);
        } catch (VolumeContainsCasesException e) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "volume_contains_docs_or_cases");
            super.isFinished = false;
            return null;
        }       
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

    public String close() {
        if (currentEntry.getNode() instanceof TransientNode) {
            return null;
        }
        if (!isClosed()) {
            try {
                getVolumeService().closeVolume(currentEntry);
            }
            catch (VolumeContainsCasesException e){
                MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "volume_contains_docs_or_cases");
                super.isFinished = false;
                return null;
            }
            MessageUtil.addInfoMessage("volume_close_success");
            return getDefaultFinishOutcome();
        }
        return null;
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
        return volumeService.isClosed(getCurrentNode());
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
    }

    // START: getters / setters
    protected VolumeService getVolumeService() {
        if (volumeService == null) {
            volumeService = (VolumeService) FacesContextUtils.getRequiredWebApplicationContext(
                    FacesContext.getCurrentInstance()).getBean(VolumeService.BEAN_NAME);
        }
        return volumeService;
    }

    protected ArchivalsService getArchivalsService() {
        if (archivalsService == null) {
            archivalsService = (ArchivalsService) FacesContextUtils.getRequiredWebApplicationContext(
                    FacesContext.getCurrentInstance()).getBean(ArchivalsService.BEAN_NAME);
        }
        return archivalsService;
    }
    
    protected CaseService getCaseService() {
        if (caseService == null) {
            caseService = (CaseService) FacesContextUtils.getRequiredWebApplicationContext(
                    FacesContext.getCurrentInstance()).getBean(CaseService.BEAN_NAME);
        }
        return caseService;
    } 
    
    protected DocumentService getDocumentService() {
        if (documentService == null) {
            documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext(
                    FacesContext.getCurrentInstance()).getBean(DocumentService.BEAN_NAME);
        }
        return documentService;
    }    

    public void setVolumeService(VolumeService volumeService) {
        this.volumeService = volumeService;
    }

    // END: getters / setters
}