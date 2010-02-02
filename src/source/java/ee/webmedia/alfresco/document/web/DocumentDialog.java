package ee.webmedia.alfresco.document.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.config.DialogsConfigElement.DialogButtonConfig;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.file.web.FileBlockBean;
import ee.webmedia.alfresco.document.metadata.web.MetadataBlockBean;

import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.*;
import ee.webmedia.alfresco.document.sendout.web.SendOutBlockBean;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.web.evaluator.RegisterDocumentEvaluator;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Alar Kvell
 */
public class DocumentDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    private static final String ERR_TEMPLATE_NOT_FOUND = "document_errorMsg_template_not_found";
    private static final String ERR_TEMPLATE_PROCESSING_FAILED = "document_errorMsg_template_processsing_failed";
    /** FollowUps is with the same type */
    private static final List<QName> regularFollowUpTypes = Arrays.asList(ERRAND_APPLICATION_DOMESTIC, ERRAND_ORDER_ABROAD, LEAVING_LETTER, TRAINING_APPLICATION, INTERNAL_APPLICATION, REPORT);
    
    private static final String PARAM_DOCUMENT_TYPE = "documentType";
    private static final String PARAM_DOCUMENT_NODE_REF = "documentNodeRef";
    
    private transient DocumentService documentService;
    private transient DocumentTemplateService documentTemplateService;
    private MetadataBlockBean metadataBlockBean;
    private FileBlockBean fileBlockBean;
    private SendOutBlockBean sendOutBlockBean;
    private boolean isDraft;

    private Node node;
    
    public void populateTemplate(ActionEvent event) {
        try {
            getDocumentTemplateService().populateTemplate(new NodeRef(ActionUtil.getParam(event, PARAM_DOCUMENT_NODE_REF)));
        } catch (FileNotFoundException e) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), ERR_TEMPLATE_NOT_FOUND);
        } catch (RuntimeException e) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), ERR_TEMPLATE_PROCESSING_FAILED);
        }
        fileBlockBean.restore();
    }
    
    public void create(ActionEvent event) {
        QName documentTypeId = QName.resolveToQName(getNamespaceService(), ActionUtil.getParam(event, PARAM_DOCUMENT_TYPE));
        node = getDocumentService().createDocument(documentTypeId);
        isDraft = true;
        ((MenuBean) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), MenuBean.BEAN_NAME)).collapseMenuItems(null);
    }

    public void open(ActionEvent event) {
        node = getDocumentService().getDocument(new NodeRef(ActionUtil.getParam(event, "nodeRef")));
        isDraft = false;
    }

    public void copy(ActionEvent event) {
        if (node == null) throw new RuntimeException("No current document");

        node = getDocumentService().copyDocument(node.getNodeRef());
        setupAction(node);
    }
    
    public void createFollowUp(ActionEvent event) {
        if (node == null)
            throw new RuntimeException("No current document");

        QName followUp = null;
        QName type = node.getType();
        if (DocumentSubtypeModel.Types.INCOMING_LETTER.equals(type)) {
            followUp = DocumentSubtypeModel.Types.OUTGOING_LETTER;
        } else if (DocumentSubtypeModel.Types.OUTGOING_LETTER.equals(type)) {
            followUp = DocumentSubtypeModel.Types.INCOMING_LETTER;
        } else if (DocumentSubtypeModel.Types.CONTRACT_SIM.equals(type) ||
                DocumentSubtypeModel.Types.CONTRACT_SMIT.equals(type) ||
                DocumentSubtypeModel.Types.TENDERING_APPLICATION.equals(type)) {
            followUp = QName.createQName(DocumentSubtypeModel.URI, ActionUtil.getParam(event, PARAM_DOCUMENT_TYPE));
        } else if (regularFollowUpTypes.contains(type)) {
            followUp = type;
        } else {
            throw new RuntimeException("FollowUp not possible for document of type " + type);
        }
        node = getDocumentService().createFollowUp(followUp, node.getNodeRef());
        setupAction(node);
    }
    
    public void createReply(ActionEvent event) {
        if (node == null)
            throw new RuntimeException("No current document");

        QName replyType = null;
        if (DocumentSubtypeModel.Types.INCOMING_LETTER.equals(node.getType())) {
            replyType = DocumentSubtypeModel.Types.OUTGOING_LETTER;
        } else if (DocumentSubtypeModel.Types.CONTRACT_SIM.equals(node.getType())) {
            replyType = DocumentSubtypeModel.Types.INSTRUMENT_OF_DELIVERY_AND_RECEIPT;
        } else {
            throw new RuntimeException("Reply not possible for document of type " + node.getType());
        }

        node = getDocumentService().createReply(replyType, node.getNodeRef());
        setupAction(node);
    }
    
    private void setupAction(Node document) {
        isDraft = true;
        metadataBlockBean.editDocument(document);
        fileBlockBean.init(document);
        sendOutBlockBean.init(node);
    }
    
    @Override
    public String getMoreActionsConfigId() {
        if (!metadataBlockBean.isInEditMode()) {
            if (DocumentSubtypeModel.Types.TENDERING_APPLICATION.equals(node.getType()) ||
                    DocumentSubtypeModel.Types.INCOMING_LETTER.equals(node.getType()) ||
                    DocumentSubtypeModel.Types.OUTGOING_LETTER.equals(node.getType()) ||
                    DocumentSubtypeModel.Types.CONTRACT_SIM.equals(node.getType()) ||
                    DocumentSubtypeModel.Types.CONTRACT_SMIT.equals(node.getType())) {
                return "document_more_actions";
            }
        }
        return null;
    }
    
    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        metadataBlockBean.init(node.getNodeRef(), isDraft);
        fileBlockBean.init(node);
        sendOutBlockBean.init(node);
    }

    @Override
    public void restored() {
        fileBlockBean.restore();
        sendOutBlockBean.restore();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (metadataBlockBean.isInEditMode()) {
            metadataBlockBean.save();
            isDraft = false;
            isFinished = false;
            return null;
        }
        reset();
        return outcome;
    }

    @Override
    public String cancel() {
        if (metadataBlockBean.isInEditMode() && !isDraft) {
            metadataBlockBean.cancel();
            return null;
        }
        reset();
        if (metadataBlockBean.isInEditMode() && isDraft) {
            getDocumentService().deleteDocument(node.getNodeRef());
        }
        return super.cancel();
    }

    private void reset() {
        node = null;
        metadataBlockBean.reset();
        fileBlockBean.reset();
        sendOutBlockBean.reset();
        isDraft = false;
    }
    
    @Override
    public List<DialogButtonConfig> getAdditionalButtons() {
        List<DialogButtonConfig> buttons = new ArrayList<DialogButtonConfig>(1);
        RegisterDocumentEvaluator registrationEval = new RegisterDocumentEvaluator();
        if(metadataBlockBean.isInEditMode() && 
          (metadataBlockBean.getDocumentType().getId().equals(DocumentSubtypeModel.Types.LICENCE) 
        || metadataBlockBean.getDocumentType().getId().equals(DocumentSubtypeModel.Types.INCOMING_LETTER))
        && registrationEval.evaluateAdditionalButton(metadataBlockBean.getDocument())) {
            buttons.add(new DialogButtonConfig("document_register_button", null, "document_registerDoc", "#{MetadataBlockBean.saveAndRegister}", "false", null));
        }

        return buttons;
    }

    @Override
    public String getContainerTitle() {
        return metadataBlockBean.getDocumentTypeName();
    }

    @Override
    public Object getActionsContext() {
        return metadataBlockBean.getDocument();
    }

    // dialog/container.jsp contains a specific check for a dialog named 'showSpaceDetails'
    public Node getSpace() {
        return null;
    }

    public Node getNode() {
        return node;
    }
    
    // START: getters / setters
    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public DocumentService getDocumentService() {
        if (documentService == null) {
            documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(DocumentService.BEAN_NAME);
        }
        return documentService;
    }
    
    public void setDocumentTemplateService(DocumentTemplateService documentTemplateService) {
        this.documentTemplateService = documentTemplateService;
    }

    public DocumentTemplateService getDocumentTemplateService() {
        if (documentTemplateService == null) {
            documentTemplateService = (DocumentTemplateService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(DocumentTemplateService.BEAN_NAME);
        }
        return documentTemplateService;
    }

    public void setMetadataBlockBean(MetadataBlockBean metadataBlockBean) {
        this.metadataBlockBean = metadataBlockBean;
    }

    public MetadataBlockBean getMeta() {
        return metadataBlockBean;
    }

    public void setFileBlockBean(FileBlockBean fileBlockBean) {
        this.fileBlockBean = fileBlockBean;
    }

    public FileBlockBean getFile() {
        return fileBlockBean;
    }

    public boolean isDraft() {
        return isDraft;
    }

    public void setSendOutBlockBean(SendOutBlockBean sendOutBlockBean) {
        this.sendOutBlockBean = sendOutBlockBean;
    }

    public SendOutBlockBean getSendOut() {
        return sendOutBlockBean;
    }

    // END: getters / setters
}
