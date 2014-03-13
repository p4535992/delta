package ee.webmedia.alfresco.document.file.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocLockService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentLogService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.content.DeleteContentDialog;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.type.service.DocumentTypeHelper;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.utils.MessageUtil;

public class DeleteFileDialog extends DeleteContentDialog {
    private static final long serialVersionUID = 1L;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        Node file = browseBean.getDocument();
        NodeRef document = null;
        if (file.getType().equals(ContentModel.TYPE_CONTENT)) {
            document = getNodeService().getPrimaryParent(file.getNodeRef()).getParentRef();
        }
        if (getDocLockService().getLockStatus(document) == LockStatus.LOCKED) {
            // lock owned by other user
            MessageUtil.addErrorMessage("file_delete_error_locked",
                    getUserService().getUserFullName((String) getNodeService().getProperty(document, ContentModel.PROP_LOCK_OWNER)));

        } else if (getDocLockService().getLockStatus(file.getNodeRef()) == LockStatus.LOCKED) {
            // lock owned by other user
            MessageUtil.addErrorMessage("file_delete_error_locked",
                    getUserService().getUserFullName((String) getNodeService().getProperty(file.getNodeRef(), ContentModel.PROP_LOCK_OWNER)));

        } else { // could be locked: LockStatus: LOCK_OWNER | NO_LOCK | LOCK_EXPIRED
            super.finishImpl(context, outcome);

            String fileName = file.getName();
            if (document != null && getDictionaryService().isSubClass(getNodeService().getType(document), DocumentCommonModel.Types.DOCUMENT)) {
                String displayName = (String) file.getProperties().get(FileModel.Props.DISPLAY_NAME);
                if (StringUtils.isNotBlank(displayName)) {
                    fileName = displayName;
                }
                getDocumentLogService().addDocumentLog(document, MessageUtil.getMessage(context, "document_log_status_fileDeleted", fileName));
                getDocumentService().updateSearchableFiles(document);
            }

            if (file.getType().equals(ContentModel.TYPE_CONTENT) || DocumentTypeHelper.isIncomingOrOutgoingLetter(file.getType())) {
                ((MenuBean) FacesHelper.getManagedBean(context, MenuBean.BEAN_NAME)).processTaskItems();
            }
            MessageUtil.addInfoMessage("file_delete_success", fileName);
        }

        return outcome;
    }

    @Override
    protected String doPostCommitProcessing(FacesContext context, String outcome) {
        super.doPostCommitProcessing(context, outcome);
        return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
    }

}
