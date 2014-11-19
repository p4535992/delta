<<<<<<< HEAD
package ee.webmedia.alfresco.document.associations.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAssociationsService;

import java.io.Serializable;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.content.DeleteContentDialog;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

public class DeleteAssocDialog extends DeleteContentDialog {

    private static final long serialVersionUID = 1L;
    transient private DocumentService documentService;

    private NodeRef sourceNodeRef;
    private NodeRef targetNodeRef;
    private QName assocType;
    private boolean isDocumentWorkflowAssoc;

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DeleteAssocDialog.class);

    @Override
    public void init(Map<String, String> parameters) {
        super.init(parameters);
    }

    private void reset() {
        sourceNodeRef = null;
        targetNodeRef = null;
        isDocumentWorkflowAssoc = false;
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
            if (isDocumentWorkflowAssoc) {
                boolean isSavedCompoundWorkflow = RepoUtil.isSaved(targetNodeRef);
                if (isSavedCompoundWorkflow) {
                    getDocumentAssociationsService().deleteWorkflowAssoc(sourceNodeRef, targetNodeRef);
                } else {
                    BeanHelper.getCompoundWorkflowAssocListDialog().removeUnsavedAssoc(sourceNodeRef);
                }
                if (isSavedCompoundWorkflow) {
                    Map<QName, Serializable> props = BeanHelper.getNodeService().getProperties(targetNodeRef);
                    NodeRef mainDocumentRef = (NodeRef) props.get(WorkflowCommonModel.Props.MAIN_DOCUMENT);
                    if (sourceNodeRef.equals(mainDocumentRef)) {
                        mainDocumentRef = null;
                    }
                    BeanHelper.getCompoundWorkflowDialog().updateCompoundWorkflowMainDoc(mainDocumentRef, sourceNodeRef);
                } else {
                    BeanHelper.getCompoundWorkflowDialog().updateAssocRelatedBlocks();
                }
            } else {
                getDocumentAssociationsService().deleteAssoc(sourceNodeRef, targetNodeRef, assocType);
            }
        } catch (Exception e) {
            log.error("Deleting association failed!", e);
            throw new RuntimeException(e);
        }
        MessageUtil.addInfoMessage("document_assocDelete_success");
        return null;
    }

    @Override
    protected String doPostCommitProcessing(FacesContext context, String outcome) {
        return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
    }

    public void setupAssoc(ActionEvent event) {
        reset();
        String sourceNodeRefString = ActionUtil.getParam(event, "sourceNodeRef");
        if (!(sourceNodeRefString == null || sourceNodeRefString.equals("null"))) {
            sourceNodeRef = new NodeRef(sourceNodeRefString);
        }

        String targetNodeRefString = ActionUtil.getParam(event, "targetNodeRef");
        if (!(targetNodeRefString == null || targetNodeRefString.equals("null"))) {
            targetNodeRef = new NodeRef(targetNodeRefString);
        }

        String typeQNameStr = ActionUtil.getParam(event, "type");
        if (!(typeQNameStr == null || typeQNameStr.equals("null"))) {
            assocType = QName.createQName(typeQNameStr);
        }
    }

    public void setupWorkflowAssoc(ActionEvent event) {
        setupAssoc(event);
        isDocumentWorkflowAssoc = true;
    }

    @Override
    public String getContainerTitle() {
        return "Seose kustutamine";
    }

}
=======
package ee.webmedia.alfresco.document.associations.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAssociationsService;

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
    private boolean source;

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
                getDocumentAssociationsService().deleteAssoc(document, caseNodeRef, CaseModel.Associations.CASE_DOCUMENT);
            } else if (source) {
                getDocumentAssociationsService().deleteAssoc(nodeRef, document, null);
            } else {
                getDocumentAssociationsService().deleteAssoc(document, nodeRef, null);
            }
        } catch (Exception e) {
            log.error("Deleting association failed!", e);
            throw new RuntimeException(e);
        }
        MessageUtil.addInfoMessage("document_assocDelete_success");
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
        if (!(caseNodeRefString == null || caseNodeRefString.equals("null"))) {
            caseNodeRef = new NodeRef(caseNodeRefString);
        }

        String nodeRefString = ActionUtil.getParam(event, "nodeRef");
        if (!(nodeRefString == null || nodeRefString.equals("null"))) {
            nodeRef = new NodeRef(nodeRefString);
        }

        String sourceNode = ActionUtil.getParam(event, "source");
        if (!(sourceNode == null || sourceNode.equals("null"))) {
            source = Boolean.parseBoolean(sourceNode);
        }
    }

    @Override
    public String getContainerTitle() {
        return MessageUtil.getMessage("document_deleteAssocDialog_deleteAssoc");
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
