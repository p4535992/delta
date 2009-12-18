package ee.webmedia.alfresco.document.web;

import java.util.Collections;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.service.VolumeService;

public class DocumentListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    private transient DocumentService documentService;
    private transient VolumeService volumeService;
    private Volume parent;
    private List<Document> documents;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        resetFields();
        return outcome;
    }

    @Override
    public String cancel() {
        resetFields();
        return super.cancel();
    }

    public void showAll(ActionEvent event) {
        final String param = ActionUtil.getParam(event, "volumeNodeRef");
        parent = getVolumeService().getVolumeByNoderef(param);
    }

    public List<Document> getEntries() {
        documents = getDocumentService().getAllDocumentsByVolume(parent.getNode().getNodeRef());
        Collections.sort(documents);
        return documents;
    }

    public Volume getParent() {
        return parent;
    }

    public String getListTitle() {
        if (parent.isContainsCases()) {
            return "TODO: <asja pealkiri>";
        } else {
            return parent.getVolumeMark() + " " + parent.getTitle();
        }
    }


    // END: jsf actions/accessors

    private void resetFields() {
        parent = null;
        documents = null;
    }

    // START: getters / setters
    public void setVolumeService(VolumeService volumeService) {
        this.volumeService = volumeService;
    }

    protected VolumeService getVolumeService() {
        if (volumeService == null) {
            volumeService = (VolumeService) FacesContextUtils.getRequiredWebApplicationContext(//
                    FacesContext.getCurrentInstance()).getBean(VolumeService.BEAN_NAME);
        }
        return volumeService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    protected DocumentService getDocumentService() {
        if (documentService == null) {
            documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext( // 
                    FacesContext.getCurrentInstance()).getBean(DocumentService.BEAN_NAME);
        }
        return documentService;
    }

    // END: getters / setters
}
