package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import java.io.File;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.FileUploadBean;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.AbstractImportDialog;
import ee.webmedia.alfresco.docadmin.service.DynamicType;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Dialog used to import {@link DynamicType}s
 * 
 * @author Ats Uiboupin
 */
public class DynamicTypesImportDialog<D extends DynamicType> extends AbstractImportDialog {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DynamicTypesImportDialog.class);
    public static final String DYNAMIC_TYPES_IMPORT_DIALOG_BEAN_NAME = "DynamicTypesImportDialog_BeanName";
    private static final long serialVersionUID = 1L;
    private final Class<D> dynamicTypeClass;
    private final String msgPrefix;

    protected DynamicTypesImportDialog(Class<D> dynamicTypeClass) {
        this(dynamicTypeClass, StringUtils.uncapitalize(dynamicTypeClass.getSimpleName()));
    }

    private DynamicTypesImportDialog(Class<D> dynamicTypeClass, String msgPrefix) {
        super("xml", msgPrefix + "s_import_error_wrongExtension");
        this.dynamicTypeClass = dynamicTypeClass;
        this.msgPrefix = msgPrefix;
    }

    @Override
    public boolean isRequiresNewTransaction() {
        return false; // importHelper.importDynamicTypes handler transactions itself
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        FileUploadBean fileUploadBean = getFileUploadBean();
        final File upFile = fileUploadBean.getFile();
        try {
            getDocumentAdminService().getImportHelper().importDynamicTypes(upFile, dynamicTypeClass);
            MessageUtil.addInfoMessage(msgPrefix + "s_import_success");
            return outcome;
        } catch (RuntimeException e) {
            File file2 = fileUploadBean.getFile();
            if (file2 != null) {
                file2.delete();
            }
            fileUploadBean.setProblematicFile(true);
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), msgPrefix + "s_import_failed", getFileName());
            throw e; // let BaseDialogBean handle it
        }
    }

    @Override
    public String getFileUploadSuccessMsg() {
        return super.getFileUploadSuccessMsg() + " " + MessageUtil.getMessage(msgPrefix + "s_import_uploadSuccessMsgSuffix", getFinishButtonLabel());
    }

    @Override
    public boolean isFinishButtonVisible(boolean dialogConfOKButtonVisible) {
        FileUploadBean fileUploadBean = getFileUploadBean();
        return fileUploadBean != null && fileUploadBean.getFile() != null && !fileUploadBean.isProblematicFile();
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage(msgPrefix + "s_import_start");
    }

}
