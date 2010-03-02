package ee.webmedia.alfresco.document.scanned.web;

import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.springframework.web.jsf.FacesContextUtils;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import java.util.List;

/**
 * Dialog for scanned documents list
 *
 * @author Romet Aidla
 */
public class ScannedDocumentsList extends BaseDialogBean {
    private static final long serialVersionUID = 0L;

    private transient FileService fileService;
    private List<File> files;
    
    public void init(ActionEvent event) {
        readFiles();
    }

    @Override
    public void restored() {
        readFiles();
    }

    private void readFiles() {
        files = getFileService().getScannedFiles();
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
