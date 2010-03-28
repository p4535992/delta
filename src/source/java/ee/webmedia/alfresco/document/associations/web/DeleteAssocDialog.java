package ee.webmedia.alfresco.document.associations.web;

import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.content.DeleteContentDialog;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

public class DeleteAssocDialog extends DeleteContentDialog {

    private static final long serialVersionUID = 1L;
    transient private DocumentService documentService; 
    
    private NodeRef document;
    private NodeRef nodeRef;
    private NodeRef caseNodeRef;
    
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DeleteAssocDialog.class);
    
    @Override
    public void init(Map<String, String> parameters) {
        super.init(parameters);
    }
    
    private void reset() {
        document = null;
        nodeRef = null;
        caseNodeRef = null;
    }

    @Override
    protected String getErrorMessageId() {
        return "document_deleteAssocDialog_deleteAssocError";
    }
    
    @Override
    public String getConfirmMessage() {
        return MessageUtil.getMessage("document_deleteAssocDialog_deleteAssocConfirm");
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) {
        try {
            if (caseNodeRef != null) {
                getDocumentService().deleteAssoc(document, caseNodeRef, CaseModel.Associations.CASE_DOCUMENT);
            } else {
                getDocumentService().deleteAssoc(document, nodeRef, null);
            }
        } catch (Exception e) {
            log.error("Deleting association failed!", e);
            throw new RuntimeException(e);
        }
        return null;
    }
    
    @Override
    protected String doPostCommitProcessing(FacesContext context, String outcome) {
        return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
    }
    
    public void setupAssoc(ActionEvent event) {
        reset();
        
        document = new NodeRef(ActionUtil.getParam(event, "documentRef"));

        String caseNodeRefString = ActionUtil.getParam(event, "caseNodeRef");
        if(!(caseNodeRefString == null || caseNodeRefString.equals("null"))) {
            caseNodeRef =  new NodeRef(caseNodeRefString);
        }
        
        String nodeRefString = ActionUtil.getParam(event, "nodeRef");
        if(!(nodeRefString == null || nodeRefString.equals("null"))) {
            nodeRef =  new NodeRef(nodeRefString);
        }
    }
    
    @Override
    public String getContainerTitle() {
        return "Seose kustutamine";
    }
    
    // START: getters / setters

    public DocumentService getDocumentService() {
        if (documentService == null) {
            documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(DocumentService.BEAN_NAME);
        }
        return documentService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }
    
    // END: getters / setters

}
