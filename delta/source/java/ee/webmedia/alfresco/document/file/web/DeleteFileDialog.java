package ee.webmedia.alfresco.document.file.web;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.content.DeleteContentDialog;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.type.service.DocumentTypeHelper;
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
        Node file = browseBean.getDocument();
        NodeRef document = null;
        if (file != null && file.getType().equals(ContentModel.TYPE_CONTENT)) {
            document = getNodeService().getPrimaryParent(file.getNodeRef()).getParentRef();
        }
        super.finishImpl(context, outcome);
        try {

            String fileName = file != null ? file.getName() : "";
            if (document != null && getDictionaryService().isSubClass(getNodeService().getType(document), DocumentCommonModel.Types.DOCUMENT)) {
                String displayName = (String) file.getProperties().get(FileModel.Props.DISPLAY_NAME);
                if (StringUtils.isNotBlank(displayName)) {
                    fileName = displayName;
                }
                getDocumentLogService().addDocumentLog(document, MessageUtil.getMessage(context, "document_log_status_fileDeleted", fileName));
                getDocumentService().updateSearchableFiles(document);
            }

            if (file != null && (file.getType().equals(ContentModel.TYPE_CONTENT) || DocumentTypeHelper.isIncomingOrOutgoingLetter(file.getType()))) {
                ((MenuBean) FacesHelper.getManagedBean(context, MenuBean.BEAN_NAME)).processTaskItems();
            }
            MessageUtil.addInfoMessage("file_delete_success", fileName);
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
