package ee.webmedia.alfresco.document.file.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocLockService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDialogHelperBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentLogService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getFileService;

import java.io.Serializable;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.commons.io.FilenameUtils;
import org.apache.cxf.common.util.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;

/**
 * @author Priit Pikk
 */
public class ChangeFileDialog extends BaseDialogBean {
    public static final String BEAN_NAME = "ChangeFileDialog";
    private static final long serialVersionUID = 1L;

    private NodeRef fileRef;
    private NodeRef docRef;
    private String fileNameWithoutExtension;
    private String fileName;

    @Override
    public String cancel() {
        handleLock(false);
        init();
        return super.cancel();
    }

    private void init() {
        fileRef = null;
        fileNameWithoutExtension = null;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        String newDisplayName = fileNameWithoutExtension + "." + FilenameUtils.getExtension(fileName);
        if (newDisplayName.equals(fileName)) {
            return outcome;
        }
        if (!validate()) {
            return null;
        }
        validatePermission(docRef, DocumentCommonModel.Privileges.EDIT_DOCUMENT);
        Pair<String, String> filenames = FilenameUtil.getFilenameFromDisplayname(fileRef, getFileService().getDocumentFileDisplayNames(docRef), newDisplayName,
                BeanHelper.getGeneralService());
        Map<QName, Serializable> properties = getNodeService().getProperties(fileRef);
        properties.put(ContentModel.PROP_NAME, filenames.getFirst());
        properties.put(FileModel.Props.DISPLAY_NAME, filenames.getSecond());
        getNodeService().setProperties(fileRef, properties);
        getNodeService().setProperty(fileRef, DocumentCommonModel.Props.FILE_NAMES, (Serializable) getDocumentService().getSearchableFileNames(docRef));
        MessageUtil.addInfoMessage("save_success");
        handleLock(false);
        getDocumentLogService().addDocumentLog(docRef, I18NUtil.getMessage("document_log_status_fileNameChanged", fileName, newDisplayName));
        return outcome;
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

    public void open(ActionEvent event) {
        init();
        try {
            fileRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
            docRef = getDocumentDialogHelperBean().getNodeRef();
            fileName = (String) getNodeService().getProperty(fileRef, ContentModel.PROP_NAME);
            fileNameWithoutExtension = FilenameUtils.removeExtension((String) getNodeService().getProperty(fileRef, FileModel.Props.DISPLAY_NAME));
            handleLock(true);
            WebUtil.navigateTo("dialog:changeFile");
        } catch (NodeLockedException e) {
            BeanHelper.getDocumentLockHelperBean().handleLockedNode("file_change_file_validation_alreadyLocked", e.getNodeRef());
        }
    }

    private boolean handleLock(boolean lock4Edit) {
        if (lock4Edit) {
            if (getDocLockService().setLockIfFree(fileRef) == LockStatus.LOCKED) {
                throw new NodeLockedException(fileRef);
            }
            getDocLockService().lockFile(fileRef);
            return true;
        }
        getDocLockService().unlockFile(fileRef);
        return false;
    }

    private boolean validate() {
        boolean isValid = true;
        if (StringUtils.isEmpty(fileNameWithoutExtension)) {
            MessageUtil.addErrorMessage("common_propertysheet_validator_mandatory", MessageUtil.getMessage("name"));
            isValid = false;
        }
        if (isValid && !fileNameWithoutExtension.equals(FilenameUtil.stripForbiddenWindowsCharacters(fileNameWithoutExtension))) {
            MessageUtil.addErrorMessage("add_file_invalid_file_name");
            isValid = false;
        }
        return isValid;
    }

    public void setFileName(String fileName) {
        fileNameWithoutExtension = fileName;
    }

    public String getFileName() {
        return fileNameWithoutExtension;
    }
}
