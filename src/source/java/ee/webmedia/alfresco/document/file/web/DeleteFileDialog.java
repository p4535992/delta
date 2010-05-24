package ee.webmedia.alfresco.document.file.web;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.content.DeleteContentDialog;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Dmitri Melnikov
 */
public class DeleteFileDialog extends DeleteContentDialog {
    private static final long serialVersionUID = 1L;

    private transient DocumentService documentService;
    private transient DocumentLogService documentLogService;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        NodeRef document = null;
        Node file = browseBean.getDocument();
        if (file != null) {
            document = getNodeService().getPrimaryParent(file.getNodeRef()).getParentRef();
        }
        try {
            super.finishImpl(context, outcome);
            
            if(file != null && (file.getType().equals(DocumentCommonModel.Types.DOCUMENT) || file.getType().equals(ContentModel.TYPE_CONTENT))) {
                getDocumentLogService().addDocumentLog(document, MessageUtil.getMessage(context, "document_log_status_fileDeleted", file.getName()));
                getDocumentService().updateSearchableFiles(document);
            }
            
            if(file != null && (file.getType().equals(DocumentSubtypeModel.Types.OUTGOING_LETTER) || file.getType().equals(DocumentSubtypeModel.Types.INCOMING_LETTER) || file.getType().equals(ContentModel.TYPE_CONTENT))) {
                ((MenuBean) FacesHelper.getManagedBean(context, MenuBean.BEAN_NAME)).processTaskItems();
            }
        } catch (NodeLockedException e) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "file_delete_error_locked");
        }
        return outcome;
    }

    @Override
    protected String doPostCommitProcessing(FacesContext context, String outcome) {
        super.doPostCommitProcessing(context, outcome);
        return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
    }

    protected DocumentService getDocumentService() {
        if (documentService == null) {
            documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(DocumentService.BEAN_NAME);
        }
        return documentService;
    }

    protected DocumentLogService getDocumentLogService() {
        if (documentLogService == null) {
            documentLogService = (DocumentLogService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(DocumentLogService.BEAN_NAME);
        }
        return documentLogService;
    }

}
