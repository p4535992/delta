package ee.webmedia.alfresco.dvk.web;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.dvk.service.DvkService;

/**
 * Email attachments list dialog.
 */
public class DvkCorruptListDialog extends BaseDialogBean {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DvkCorruptListDialog.class);
    private static final long serialVersionUID = 1L;

    private transient GeneralService generalService;
    private transient FileFolderService fileFolderService;
    private transient FileService fileService;
    private transient DvkService dvkService;

    public DvkCorruptListDialog() {
        log.debug("construct DvkCorruptListDialog");
    }

    private List<File> files;

    /** @param event from JSP */
    public void init(ActionEvent event) {
        readFiles();
    }

    @Override
    public void restored() {
        readFiles();
    }

    private void readFiles() {
        String corruptDvkDocumentsPath = getDvkService().getCorruptDvkDocumentsPath();
        NodeRef corruptFolder = getGeneralService().getNodeRef(corruptDvkDocumentsPath);
        files = getFileService().getAllFilesExcludingDigidocSubitemsAndIncludingDecContainers(corruptFolder);
    }

    public List<File> getFiles() {
        return files;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return null; // Finish button is always hidden
    }

    public Node getNode() {
        return null;
    }

    private GeneralService getGeneralService() {
        if (generalService == null) {
            generalService = (GeneralService) FacesContextUtils.getRequiredWebApplicationContext(
                    FacesContext.getCurrentInstance()).getBean(GeneralService.BEAN_NAME);
        }
        return generalService;
    }

    private FileService getFileService() {
        if (fileService == null) {
            fileService = (FileService) FacesContextUtils.getRequiredWebApplicationContext(
                    FacesContext.getCurrentInstance()).getBean(FileService.BEAN_NAME);
        }
        return fileService;
    }

    @Override
    protected FileFolderService getFileFolderService() {
        if (fileFolderService == null) {
            fileFolderService = (FileFolderService) FacesContextUtils.getRequiredWebApplicationContext(
                    FacesContext.getCurrentInstance()).getBean("FileFolderService");
        }
        return fileFolderService;
    }

    private DvkService getDvkService() {
        if (dvkService == null) {
            dvkService = (DvkService) FacesContextUtils.getRequiredWebApplicationContext(
                    FacesContext.getCurrentInstance()).getBean(DvkService.BEAN_NAME);
        }
        return dvkService;
    }

}
