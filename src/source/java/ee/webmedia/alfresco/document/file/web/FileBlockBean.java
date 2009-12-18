package ee.webmedia.alfresco.document.file.web;

import java.io.Serializable;
import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.NavigationBean;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.service.DocumentService;

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

    public void init(Node node) {
        this.nodeRef = node.getNodeRef();
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
