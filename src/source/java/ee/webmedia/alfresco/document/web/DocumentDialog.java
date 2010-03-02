package ee.webmedia.alfresco.document.web;

import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.INTERNAL_APPLICATION;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.LEAVING_LETTER;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.REPORT;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.TRAINING_APPLICATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.faces.application.NavigationHandler;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.config.DialogsConfigElement.DialogButtonConfig;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.web.ClearStateNotificationHandler;
import ee.webmedia.alfresco.document.associations.model.DocAssocInfo;
import ee.webmedia.alfresco.document.associations.web.AssocsBlockBean;
import ee.webmedia.alfresco.document.file.web.FileBlockBean;
import ee.webmedia.alfresco.document.log.web.DocumentLogBlockBean;
import ee.webmedia.alfresco.document.metadata.web.MetadataBlockBean;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.search.web.SearchBlockBean;
import ee.webmedia.alfresco.document.sendout.web.SendOutBlockBean;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.type.web.TypeBlockBean;
import ee.webmedia.alfresco.document.web.evaluator.RegisterDocumentEvaluator;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.web.WorkflowBlockBean;

/**
 * @author Alar Kvell
 */
public class DocumentDialog extends BaseDialogBean implements ClearStateNotificationHandler.ClearStateListener {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "DocumentDialog";

    private static final String ERR_TEMPLATE_NOT_FOUND = "document_errorMsg_template_not_found";
    private static final String ERR_TEMPLATE_PROCESSING_FAILED = "document_errorMsg_template_processsing_failed";
    /** FollowUps is with the same type */
    private static final List<QName> regularFollowUpTypes = Arrays.asList(ERRAND_APPLICATION_DOMESTIC, ERRAND_ORDER_ABROAD, LEAVING_LETTER, TRAINING_APPLICATION, INTERNAL_APPLICATION, REPORT);
    
    private static final String PARAM_DOCUMENT_TYPE = "documentType";
    private static final String PARAM_DOCUMENT_NODE_REF = "documentNodeRef";
    private static final String PARAM_NODEREF = "nodeRef";
    
    private transient DocumentService documentService;
    private transient DocumentTemplateService documentTemplateService;

    private SearchBlockBean searchBlockBean;
    private TypeBlockBean typeBlockBean;
    private MetadataBlockBean metadataBlockBean;
    private FileBlockBean fileBlockBean;
    private SendOutBlockBean sendOutBlockBean;
    private AssocsBlockBean assocsBlockBean;
    private WorkflowBlockBean workflowBlockBean;
    private DocumentLogBlockBean documentLogBlockBean;
    private boolean isDraft;
    private boolean showDocsAndCasesAssocs;

    private Node node;

    /**
     * Should be called only when the document was received from DVK.  
     */
    public void selectedValueChanged(ValueChangeEvent event) {
        String selectedType = (String)event.getNewValue();
        if (StringUtils.isBlank(selectedType)) {
            return;
        }
        QName newType = QName.createQName(selectedType);
        if (node.getType().equals(newType)) {
            return;
        }
        getDocumentService().changeTypeInMemory(node, newType);
        
        setupAction(true);
        metadataBlockBean.setOwnerCurrentUser();
        typeBlockBean.setSelected(newType.toString());
    }
    
    public void populateTemplate(ActionEvent event) {
        try {
            getDocumentTemplateService().populateTemplate(new NodeRef(ActionUtil.getParam(event, PARAM_DOCUMENT_NODE_REF)));
        } catch (FileNotFoundException e) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), ERR_TEMPLATE_NOT_FOUND);
        } catch (RuntimeException e) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), ERR_TEMPLATE_PROCESSING_FAILED);
        }
        fileBlockBean.restore();
        documentLogBlockBean.restore();
    }
    
    public void create(ActionEvent event) {
        QName documentTypeId = QName.resolveToQName(getNamespaceService(), ActionUtil.getParam(event, PARAM_DOCUMENT_TYPE));
        node = getDocumentService().createDocument(documentTypeId);
        isDraft = true;
        ((MenuBean) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), MenuBean.BEAN_NAME)).collapseMenuItems(null);
    }

    public void open(ActionEvent event) {
        open(new NodeRef(ActionUtil.getParam(event, PARAM_NODEREF)));
    }

    public void open(NodeRef nodeRef) {
	    createSnapshot();
        node = getDocumentService().getDocument(nodeRef);
        /** open a doc for editing if it's from dvk */
        if (isFromDVK() || isFromIncoming()) {
            isDraft = true;
        } else {
            isDraft = false;
        }
    }

    public void copy(ActionEvent event) {
        if (node == null) throw new RuntimeException("No current document");

        node = getDocumentService().copyDocument(node.getNodeRef());
        setupAction(true);
    }

    public void endDocument(ActionEvent event) {
        Assert.notNull(node, "No current document");
        getDocumentService().endDocument(node.getNodeRef());
        // refresh metadata block
        metadataBlockBean.init(node.getNodeRef(), isDraft);
        documentLogBlockBean.restore();
    }

    public void reopenDocument(ActionEvent event) {
        Assert.notNull(node, "No current document");
        getDocumentService().reopenDocument(node.getNodeRef());
        // refresh metadata block
        metadataBlockBean.init(node.getNodeRef(), isDraft);
    }

    public void deleteDocument(ActionEvent event) {
        Assert.notNull(node, "No current document");
        getDocumentService().deleteDocument(node.getNodeRef());
        reset();
        // go back
        FacesContext fc = FacesContext.getCurrentInstance();
        NavigationHandler navigationHandler = fc.getApplication().getNavigationHandler();
        navigationHandler.handleNavigation(fc, null, getDefaultCancelOutcome());
    }
    
    public void toggleProceeding(ActionEvent event) {
        if (node == null) throw new RuntimeException("No current document");
        
        String status = (String) node.getProperties().get(DocumentCommonModel.Props.DOC_STATUS.toString());
        if (DocumentStatus.WORKING.equals(status)) {
            getDocumentService().stopDocumentPreceedingAndUpdateStatus(node.getNodeRef());
            
        } else if (DocumentStatus.STOPPED.equals(status)) {
            getDocumentService().continueDocumentPreceedingAndUpdateStatus(node.getNodeRef());
        }
        
        node = getDocumentService().getDocument(node.getNodeRef());
        setupAction(false);
    }
    
    public void createFollowUp(ActionEvent event) {
        if (node == null)
            throw new RuntimeException("No current document");

        QName followUp = null;
        QName type = node.getType();
        if (DocumentSubtypeModel.Types.INCOMING_LETTER.equals(type) ||
                DocumentSubtypeModel.Types.OUTGOING_LETTER.equals(type) ||
                DocumentSubtypeModel.Types.CONTRACT_SIM.equals(type) ||
                DocumentSubtypeModel.Types.CONTRACT_SMIT.equals(type) ||
                DocumentSubtypeModel.Types.TENDERING_APPLICATION.equals(type)) {

            followUp = QName.createQName(DocumentSubtypeModel.URI, ActionUtil.getParam(event, PARAM_DOCUMENT_TYPE));
        } else if (regularFollowUpTypes.contains(type)) {
            followUp = type;
        } else {
            throw new RuntimeException("FollowUp not possible for document of type " + type);
        }
        node = getDocumentService().createFollowUp(followUp, node.getNodeRef());
        setupAction(true);
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
        setupAction(true);
    }
    
    public void setupAction(boolean mode) {
        isDraft = mode;
        if (isDraft) {
            metadataBlockBean.editDocument(node);
        } else {
            metadataBlockBean.viewDocument(node);
        }
        fileBlockBean.init(node);
        sendOutBlockBean.init(node);
        assocsBlockBean.init(node);
        workflowBlockBean.init(node.getNodeRef());
        documentLogBlockBean.init(node);
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
        return "";
    }
    
    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        metadataBlockBean.init(node.getNodeRef(), isDraft);
        if (isFromDVK() || isFromIncoming()) {
            metadataBlockBean.setOwnerCurrentUser();
        }
        fileBlockBean.init(node);
        sendOutBlockBean.init(node);
        typeBlockBean.init();
        assocsBlockBean.init(node);
        searchBlockBean.init(node);
        workflowBlockBean.init(node.getNodeRef());
        documentLogBlockBean.init(node);

        ClearStateNotificationHandler clearStateNotificationHandler = (ClearStateNotificationHandler) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), ClearStateNotificationHandler.BEAN_NAME);
        clearStateNotificationHandler.addClearStateListener(this);
    }

    @Override
    public void restored() {
        if (!restoreSnapshot()) {
            fileBlockBean.restore();
            sendOutBlockBean.restore();
            assocsBlockBean.restore();
            documentLogBlockBean.restore();
        }
        workflowBlockBean.restore();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (metadataBlockBean.isInEditMode()) {
            if (isFromDVK()) {
                /** It's possible to change the type of the node that came from DVK */
                getDocumentService().changeType(node);
            }
            metadataBlockBean.save();
            getDocumentService().getDocumentLogService().addDocumentLog(node.getNodeRef(), MessageUtil.getMessage(isDraft ? "document_log_status_created" : "document_log_status_changed"));
            documentLogBlockBean.restore();
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
        searchBlockBean.reset();
        assocsBlockBean.reset();
        workflowBlockBean.reset();
        documentLogBlockBean.reset();
        isDraft = false;
        showDocsAndCasesAssocs = false;
    }
    
    public void saveAndRegisterContinue() {
        // similar documents were found before, finish registering
        metadataBlockBean.saveAndRegister();
        searchBlockBean.setFoundSimilar(false);
    }
    
    public void saveAndRegister() {
        // search for similar documents if it's an incoming letter
        if (metadataBlockBean.getDocumentType().getId().equals(DocumentSubtypeModel.Types.INCOMING_LETTER)) {
            String senderRegNum = (String) metadataBlockBean.getDocument().getProperties().get(DocumentSpecificModel.Props.SENDER_REG_NUMBER.toString());
            searchBlockBean.findSimilarDocuments(senderRegNum);
        } 
        
        // just register if not an incoming letter or no similar documents found
        if (!searchBlockBean.isFoundSimilar()) {
            metadataBlockBean.saveAndRegister();
        }
    }
    
    @Override
    public List<DialogButtonConfig> getAdditionalButtons() {
        List<DialogButtonConfig> buttons = new ArrayList<DialogButtonConfig>(1);
        RegisterDocumentEvaluator registrationEval = new RegisterDocumentEvaluator();
        if(metadataBlockBean.isInEditMode() && 
          (metadataBlockBean.getDocumentType().getId().equals(DocumentSubtypeModel.Types.LICENCE) 
        || metadataBlockBean.getDocumentType().getId().equals(DocumentSubtypeModel.Types.INCOMING_LETTER))
        && registrationEval.evaluateAdditionalButton(metadataBlockBean.getDocument())) {
            if (searchBlockBean.isFoundSimilar()) {
                buttons.add(new DialogButtonConfig("documentRegisterButton", null, "document_registerDoc_continue", "#{DocumentDialog.saveAndRegisterContinue}", "false", null));
            } else {
                buttons.add(new DialogButtonConfig("documentRegisterButton", null, "document_registerDoc", "#{DocumentDialog.saveAndRegister}", "false", null));
            }
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
    
    public boolean isFromDVK() {
        return getDocumentService().isFromDVK(node.getNodeRef());
    }
    
    public boolean isFromIncoming() {
        return getDocumentService().isFromIncoming(node.getNodeRef());
    }

    public boolean isShowSearchBlock() {
        if((searchBlockBean.isIncludeCases() && !metadataBlockBean.isInEditMode())) {
            return true;
        }
        return metadataBlockBean.isInEditMode() && searchBlockBean.isShow() && !searchBlockBean.isFoundSimilar() && (isFromDVK() || isFromIncoming());
    }
    
    public String getSearchBlockTitle() {
        if(isFromDVK() || isFromIncoming()) {
            return MessageUtil.getMessage("document_search_base_title");
        }
        return MessageUtil.getMessage("document_search_docOrCase_title");
    }
    
    public boolean isShowFoundSimilar() {
        return metadataBlockBean.isInEditMode() && searchBlockBean.isFoundSimilar();
    }
    
    public boolean isShowTypeBlock() {
        return metadataBlockBean.isInEditMode() && isFromDVK();
    }
    
    public void addFollowUpHandler(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, PARAM_NODEREF));
        addTargetAssoc(nodeRef, DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP);
    }
    
    public void addReplyHandler(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, PARAM_NODEREF));
        addTargetAssoc(nodeRef, DocumentCommonModel.Assocs.DOCUMENT_REPLY);
    }

    public void addAssocDocHandler(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, PARAM_NODEREF));
        saveAssocNow(node.getNodeRef(), nodeRef, DocumentCommonModel.Assocs.DOCUMENT_2_DOCUMENT);
    }

    public void addAssocDoc2CaseHandler(ActionEvent event) {
        NodeRef caseRef = new NodeRef(ActionUtil.getParam(event, PARAM_NODEREF));
        final QName assocType = CaseModel.Associations.CASE_DOCUMENT;
        saveAssocNow(caseRef, node.getNodeRef(), assocType);
    }

    private void saveAssocNow(NodeRef sourceRef, NodeRef targetRef, final QName assocType) {
        getNodeService().createAssociation(sourceRef, targetRef, assocType);
        assocsBlockBean.restore();
        searchBlockBean.setIncludeCaseTitles(false);
    }
    
    public void searchDocsAndCases(ActionEvent event) {
        this.showDocsAndCasesAssocs = true;
        searchBlockBean.init(metadataBlockBean.getDocument());
        searchBlockBean.setIncludeCaseTitles(true);
    }
    
    
    private void addTargetAssoc(NodeRef targetRef, QName targetType) {
        final DocAssocInfo docAssocInfo = searchBlockBean.addTargetAssoc(targetRef, targetType);
        assocsBlockBean.getDocAssocInfos().add(docAssocInfo);
        setupAction(true); //FIXME: dmitri, miks seda meetodit välja kutsusid? kui ainult lisatud seose kuvamiseks enne salvestamist, siis
        metadataBlockBean.updateFollowUpOrReplyProperties(targetRef);
    }

    /**
     * Move all the files to the selected nodeRef, delete the current doc
     * and show the nodeRef doc.
     * @param event
     */
    public void addFilesHandler(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, PARAM_NODEREF));
        boolean success = fileBlockBean.moveAllFiles(nodeRef);
        
        if (success) {
            getDocumentService().deleteDocument(node.getNodeRef());
            node = getDocumentService().getDocument(nodeRef);
            setupAction(false);
        }
    }


    // START: snapshot logic (for supporting multiple concurrent document views)
    private Stack<Snapshot> snapshots = new Stack<Snapshot>();

    // creates snapshot of DocumentDialog state (if needed)
    private void createSnapshot() {
        // if there's no node, there's no previous DocumentDialog state
        // so no need to take a snapshot
        if (node != null) {
            snapshots.push(new Snapshot(this));
        }
    }

    // restores DocumentDialog to previous state (if needed)
    private boolean restoreSnapshot() {
        // if there's a node, then current DocumentDialog have not been closed
        // so we shouldn't restore snapshot
        if (node == null && !snapshots.empty()) {
            snapshots.pop().restoreState(this);

            // just re-init other beans
            fileBlockBean.init(node);
            sendOutBlockBean.init(node);
            typeBlockBean.init();
            assocsBlockBean.init(node);
            workflowBlockBean.init(node.getNodeRef());
            documentLogBlockBean.init(node);
            return true;
        }
        return false;
    }

    @Override
    public void clearState() {
        snapshots.clear();
    }

    private static class Snapshot implements Serializable {
        private static final long serialVersionUID = 1L;

        private Node node;
        private boolean isDraft;
        private boolean showDocsAndCasesAssocs;
        private MetadataBlockBean.Snapshot metadataSnapshot;
        private SearchBlockBean.Snapshot searchSnapshot;

        private Snapshot(DocumentDialog dialog) {
            this.node = dialog.node;
            this.isDraft = dialog.isDraft;
            this.showDocsAndCasesAssocs = dialog.showDocsAndCasesAssocs;
            metadataSnapshot = dialog.metadataBlockBean.createSnapshot();
            searchSnapshot = dialog.searchBlockBean.createSnapshot();
        }

        private void restoreState(DocumentDialog dialog) {
            dialog.node = this.node;
            dialog.isDraft = this.isDraft;
            dialog.showDocsAndCasesAssocs = this.showDocsAndCasesAssocs;
            dialog.metadataBlockBean.restoreSnapshot(metadataSnapshot);
            dialog.searchBlockBean.restoreSnapshot(searchSnapshot);
        }
    }
    // END: snapshot logic
    
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
    
    public void setSearchBlockBean(SearchBlockBean searchBlockBean) {
        this.searchBlockBean = searchBlockBean;
    }
    
    public SearchBlockBean getSearch() {
        return searchBlockBean;
    }

    public void setTypeBlockBean(TypeBlockBean typeBlockBean) {
        this.typeBlockBean = typeBlockBean;
    }

    public TypeBlockBean getType() {
        return typeBlockBean;
    }

    public boolean isDraft() {
        return isDraft;
    }
    
    public boolean isShowDocsAndCasesAssocs() {
        return showDocsAndCasesAssocs;
    }

    public void setSendOutBlockBean(SendOutBlockBean sendOutBlockBean) {
        this.sendOutBlockBean = sendOutBlockBean;
    }
    
    public void setAssocsBlockBean(AssocsBlockBean assocsBlockBean) {
        this.assocsBlockBean = assocsBlockBean;
    }

    public void setWorkflowBlockBean(WorkflowBlockBean workflowBlockBean) {
        this.workflowBlockBean = workflowBlockBean;
    }

    public WorkflowBlockBean getWorkflow() {
        return workflowBlockBean;
    }

    public DocumentLogBlockBean getDocumentLog() {
        return documentLogBlockBean;
    }

    public void setDocumentLogBlockBean(DocumentLogBlockBean documentLogBlockBean) {
        this.documentLogBlockBean = documentLogBlockBean;
    }

    // END: getters / setters
}
