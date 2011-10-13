package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import java.io.File;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.FileUploadBean;

import ee.webmedia.alfresco.common.web.AbstractImportDialog;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Dialog used to import documentTypes
 * 
 * @author Ats Uiboupin
 */
public class DocumentTypesImportDialog extends AbstractImportDialog {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentTypesImportDialog.class);
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "DocumentTypesImportDialog";

    protected DocumentTypesImportDialog() {
        super("xml");
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        log.info("Starting to import docTypes");

        FileUploadBean fileUploadBean = getFileUploadBean();
        final File upFile = fileUploadBean.getFile();
        try {
            getDocumentAdminService().importDocumentTypes(upFile);
            MessageUtil.addInfoMessage("docTypes_import_success");
            return outcome;
        } catch (RuntimeException e) {
            File file2 = fileUploadBean.getFile();
            if (file2 != null) {
                file2.delete();
            }
            fileUploadBean.setProblematicFile(true);
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "docTypes_import_failed", getFileName());
            throw e; // let BaseDialogBean handle it
        }
    }

    @Override
    public String getFileUploadSuccessMsg() {
        return super.getFileUploadSuccessMsg() + " " + MessageUtil.getMessage("docTypes_import_uploadSuccessMsgSuffix", getFinishButtonLabel());
    }

    @Override
    public boolean isFinishButtonVisible(boolean dialogConfOKButtonVisible) {
        FileUploadBean fileUploadBean = getFileUploadBean();
        return fileUploadBean != null && fileUploadBean.getFile() != null && !fileUploadBean.isProblematicFile();
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage("docTypes_import_start");
    }

}
