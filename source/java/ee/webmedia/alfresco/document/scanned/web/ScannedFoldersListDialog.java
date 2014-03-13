package ee.webmedia.alfresco.document.scanned.web;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;

/**
 * Dialog showing list of folders that contain scanned documents
 */
public class ScannedFoldersListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 0L;

    private transient FileService fileService;
    private List<File> files;

    public void init(@SuppressWarnings("unused") ActionEvent event) {
        readFiles();
    }

    @Override
    public void restored() {
        readFiles();
    }

    private void readFiles() {
        files = getFileService().getScannedFolders();
    }

    public List<File> getFiles() {
        return files;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // Finish button is always hidden
        return null;
    }

    public FileService getFileService() {
        if (fileService == null) {
            fileService = (FileService) FacesContextUtils.getRequiredWebApplicationContext(
                    FacesContext.getCurrentInstance()).getBean(FileService.BEAN_NAME);
        }
        return fileService;
    }

}
