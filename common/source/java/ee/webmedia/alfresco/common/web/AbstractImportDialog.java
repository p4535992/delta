package ee.webmedia.alfresco.common.web;

import java.io.File;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.FileUploadBean;
import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Base dialog for importing information from files
 */
public abstract class AbstractImportDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    protected File file;
    private String fileName;
    private final String acceptedFileExtension;
    private final String wrongExtensionMsg;

    protected AbstractImportDialog(String acceptedFileExtension, String wrongExtensionMsg) {
        this.acceptedFileExtension = acceptedFileExtension;
        this.wrongExtensionMsg = wrongExtensionMsg;
    }

    protected FileUploadBean getFileUploadBean() {
        FileUploadBean fileBean = (FileUploadBean) FacesContext.getCurrentInstance().getExternalContext()
                .getSessionMap().get(FileUploadBean.FILE_UPLOAD_BEAN_NAME);
        return fileBean;
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        reset();
    }

    @Override
    public String cancel() {
        reset();
        return super.cancel();
    }

    public String reset() {
        fileName = null;
        clearUpload();
        return "dialog:close";
    }

    protected void clearUpload() {
        if (file != null) {
            file.delete();
        }
        file = null;
        // remove the file upload bean from the session
        FacesContext ctx = FacesContext.getCurrentInstance();
        ctx.getExternalContext().getSessionMap().remove(FileUploadBean.FILE_UPLOAD_BEAN_NAME);
    }

    public abstract String getFileUploadSuccessMsg();

    // START: getters / setters

    /**
     * @return Returns the name of the file
     */
    public String getFileName() {
        // try and retrieve the file and filename from the file upload bean
        // representing the file we previously uploaded.
        FileUploadBean fileBean = getFileUploadBean();
        if (fileBean != null) {
            file = fileBean.getFile();
            final String fileName = fileBean.getFileName();
            if (isCorrectExtension(fileName)) {
                this.fileName = fileName;
            }
        }
        return fileName;
    }

    public void setFileName(String fileName) {
        if (!isCorrectExtension(fileName)) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), wrongExtensionMsg, getFileName());
            this.fileName = null;
            return;
        }
        this.fileName = fileName;
    }

    protected boolean isCorrectExtension(String fileName) {
        return fileName != null && fileName.endsWith(acceptedFileExtension);
    }

    // END: getters / setters
}
