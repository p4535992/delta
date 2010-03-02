package ee.webmedia.alfresco.document.file.web;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.content.DeleteContentDialog;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Dmitri Melnikov
 */
public class DeleteFileDialog extends DeleteContentDialog {
    private static final long serialVersionUID = 1L;

    private transient DocumentService documentService;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        NodeRef document = null;
        Node file = browseBean.getDocument();
        if (file != null) {
            document = getNodeService().getPrimaryParent(file.getNodeRef()).getParentRef();
        }
        super.finishImpl(context, outcome);
        
        if(file != null && file.getType().equals(DocumentCommonModel.Types.DOCUMENT)) {
            getDocumentService().getDocumentLogService().addDocumentLog(document,
                    MessageUtil.getMessage(context, "document_log_status_fileDeleted", new Object[] { file.getName() }));
            getDocumentService().updateSearchableFiles(document);
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
}
