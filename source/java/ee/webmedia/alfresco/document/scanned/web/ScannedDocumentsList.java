package ee.webmedia.alfresco.document.scanned.web;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.utils.ActionUtil;

/**
 * Dialog for scanned documents list
 */
public class ScannedDocumentsList extends BaseDialogBean {
    private static final long serialVersionUID = 0L;

    private transient FileService fileService;
    private List<File> files;
    private NodeRef folderRef;

    public void init(ActionEvent event) {
        folderRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        readFiles();
    }

    @Override
    public void restored() {
        readFiles();
    }

    private void readFiles() {
        files = getFileService().getScannedFiles(folderRef);
        Collections.sort(files, new Comparator<File>() {

            @Override
            public int compare(File f1, File f2) {
                if (f1 == null || f1.getCreated() == null) {
                    return (f2 == null || f2.getCreated() == null) ? 0 : -1;
                }
                if (f2 == null || f2.getCreated() == null) {
                    return 1;
                }
                return f2.getCreated().compareTo(f1.getCreated());
            }
        });
    }

    public List<File> getFiles() {
        return files;
    }

    @Override
    public String cancel() {
        folderRef = null;
        return super.cancel();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return null; // Finish button is always hidden
    }

    public FileService getFileService() {
        if (fileService == null) {
            fileService = (FileService) FacesContextUtils.getRequiredWebApplicationContext(
                    FacesContext.getCurrentInstance()).getBean(FileService.BEAN_NAME);
        }
        return fileService;
    }
}
