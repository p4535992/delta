package ee.webmedia.alfresco.document.file.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocLockService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDialogHelperBean;

import java.io.Serializable;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;

public class CommentFileDialog extends BaseDialogBean {
    public static final String BEAN_NAME = "CommentFileDialog";
    private static final long serialVersionUID = 1L;

    private NodeRef fileRef;
    private NodeRef docRef;
    private String comment;

    @Override
    public String cancel() {
        handleLock(false);
        init();
        return super.cancel();
    }

    private void init() {
        fileRef = null;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        validatePermission(docRef, Privilege.EDIT_DOCUMENT);
        Map<QName, Serializable> properties = getNodeService().getProperties(fileRef);
        properties.put(FileModel.Props.COMMENT, comment);
        getNodeService().setProperties(fileRef, properties);
        MessageUtil.addInfoMessage("save_success");
        handleLock(false);
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
            comment = (String) getNodeService().getProperty(fileRef, FileModel.Props.COMMENT);
            handleLock(true);
            WebUtil.navigateTo("dialog:commentFile");
        } catch (NodeLockedException e) {
            BeanHelper.getDocumentLockHelperBean().handleLockedNode("file_comment_file_validation_alreadyLocked", e.getNodeRef());
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

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }
}
