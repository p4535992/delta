package ee.webmedia.alfresco.document.file.web;

import java.io.Serializable;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.NavigationBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.springframework.util.Assert;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Dmitri Melnikov
 */
public class FileBlockBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private transient FileService fileService;
    private transient DocumentService documentService;
    private NavigationBean navigationBean;
    private List<File> files;
    private NodeRef nodeRef;

    public void toggleActive(ActionEvent event) {
        NodeRef fileNodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        try {
            final boolean active = getFileService().toggleActive(fileNodeRef);
            restore(); // refresh the files list
            MessageUtil.addInfoMessage(active ? "file_toggle_active_success" : "file_toggle_deactive_success", getFileName(fileNodeRef));
        } catch (NodeLockedException e) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "file_inactive_toggleFailed");
        }
    }

    private String getFileName(NodeRef fileNodeRef) {
        return Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getFileFolderService().getFileInfo(fileNodeRef).getName();
    }

    public void transformToPdf(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        final FileInfo pdfFileInfo = getFileService().transformToPdf(nodeRef);
        restore(); // refresh the files list
        if (pdfFileInfo != null) {
            MessageUtil.addInfoMessage("file_generate_pdf_success", pdfFileInfo.getName());
        } else {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "file_generate_pdf_failed");
        }
    }

    public void init(Node node) {
        this.nodeRef = node.getNodeRef();
        Assert.notNull(nodeRef, "nodeRef is null - node: " + node);
        restore();
        // Alfresco's AddContentDialog.saveContent uses
        // navigationBean.getCurrentNodeId() for getting the folder to save to
        navigationBean.setCurrentNodeId(node.getId());
    }

    public void reset() {
        files = null;
        nodeRef = null;
        navigationBean.setCurrentNodeId(getDocumentService().getDrafts().getId());
    }

    public void restore() {
        files = getFileService().getAllFiles(nodeRef);
    }

    public boolean moveAllFiles(NodeRef toRef) {
        try {
            getFileService().moveAllFiles(nodeRef, toRef);
            return true;
        } catch (DuplicateChildNodeNameException e) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "add_file_existing_file", e.getName());
        } catch (FileExistsException e) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "add_file_existing_file", e.getName());
        } catch (FileNotFoundException e) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "file_not_found");
        }
        return false;
    }

    /**
     * Used in JSP page.
     * 
     * @return
     */
    public List<File> getFiles() {
        return files;
    }

    // START: getters / setters
    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public FileService getFileService() {
        if (fileService == null) {
            fileService = (FileService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(FileService.BEAN_NAME);
        }
        return fileService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public DocumentService getDocumentService() {
        if (documentService == null) {
            documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(DocumentService.BEAN_NAME);
        }
        return documentService;
    }

    public void setNavigationBean(NavigationBean navigationBean) {
        this.navigationBean = navigationBean;
    }
    // END: getters / setters
}