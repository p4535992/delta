package ee.webmedia.alfresco.common.web;

import java.io.File;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.FileUploadBean;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.commons.lang.StringUtils;

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

    protected AbstractImportDialog(String acceptedFileExtension) {
        this(acceptedFileExtension, null);
    }

    protected AbstractImportDialog(String acceptedFileExtension, String wrongExtensionMsg) {
        this.acceptedFileExtension = acceptedFileExtension;
        if (wrongExtensionMsg == null) {
            wrongExtensionMsg = "import_error_wrongExtension";
        }
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
        return getDefaultFinishOutcome();
    }

    private void clearUpload() {
        if (file != null) {
            file.delete();
        }
        file = null;
        // remove the file upload bean from the session
        FacesContext ctx = FacesContext.getCurrentInstance();
        FileUploadBean fileUploadBean = getFileUploadBean();
        if (fileUploadBean != null) {
            fileUploadBean.setProblematicFile(false);
            ctx.getExternalContext().getSessionMap().remove(FileUploadBean.FILE_UPLOAD_BEAN_NAME);
        }
    }

    public String getFileUploadSuccessMsg() {
        return MessageUtil.getMessage("file_upload_success", getFileUploadBean().getFileName());
    }

    // START: getters / setters

    /**
     * @return Returns the name of the file
     */
    public String getFileName() {
        // try and retrieve the file and filename from the file upload bean
        // representing the file we previously uploaded.
        FileUploadBean fileBean = getFileUploadBean();
        if (fileBean != null && !fileBean.isProblematicFile()) {
            file = fileBean.getFile();
            if (file == null) {
                fileName = null;
            } else {
                final String uploadedFileName = fileBean.getFileName();
                if (isCorrectExtension(uploadedFileName)) {
                    fileName = uploadedFileName;
                }
            }
        } else {
            fileName = null;
        }
        return fileName;
    }

    public void setFileName(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            MessageUtil.addErrorMessage("import_error_nameIsBlank", getFileName());
            clearUpload();
            this.fileName = null;
            return;
        }
        if (!isCorrectExtension(fileName)) {
            MessageUtil.addErrorMessage(wrongExtensionMsg, acceptedFileExtension, getFileName());
            clearUpload(); // Do this to avoid FileUploadBean multiple file mode
            this.fileName = null;
            return;
        }
        this.fileName = fileName;
    }

    protected boolean isCorrectExtension(String fName) {
        return StringUtils.endsWith(fName, "." + acceptedFileExtension);
    }

    // END: getters / setters
}
