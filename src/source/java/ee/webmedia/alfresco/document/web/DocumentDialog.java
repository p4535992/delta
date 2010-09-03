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

import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
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
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.log.web.LogBlockBean;
import ee.webmedia.alfresco.document.metadata.web.MetadataBlockBean;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.permissions.DocumentFileWriteDynamicAuthority;
import ee.webmedia.alfresco.document.search.web.SearchBlockBean;
import ee.webmedia.alfresco.document.sendout.web.SendOutBlockBean;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.type.web.TypeBlockBean;
import ee.webmedia.alfresco.document.web.evaluator.RegisterDocumentEvaluator;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.web.WorkflowBlockBean;

/**
 * @author Alar Kvell
 */
public class DocumentDialog extends BaseDialogBean implements ClearStateNotificationHandler.ClearStateListener {
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentDialog.class);

    public static final String BEAN_NAME = "DocumentDialog";

    private static final String ERR_TEMPLATE_NOT_FOUND = "document_errorMsg_template_not_found";
    private static final String ERR_TEMPLATE_PROCESSING_FAILED = "document_errorMsg_template_processsing_failed";
    /** FollowUps is with the same type */
    private static final List<QName> regularFollowUpTypes = Arrays.asList(ERRAND_APPLICATION_DOMESTIC, ERRAND_ORDER_ABROAD, LEAVING_LETTER,
            TRAINING_APPLICATION, INTERNAL_APPLICATION, REPORT);

    private static final String PARAM_DOCUMENT_TYPE = "documentType";
    private static final String PARAM_DOCUMENT_NODE_REF = "documentNodeRef";
    private static final String PARAM_NODEREF = "nodeRef";

    private transient DocumentService documentService;
    private transient DocumentLogService documentLogService;
    private transient DocumentTemplateService documentTemplateService;
    private transient WorkflowService workflowService;

    private SearchBlockBean searchBlockBean;
    private TypeBlockBean typeBlockBean;
    private MetadataBlockBean metadataBlockBean;
    private FileBlockBean fileBlockBean;
    private SendOutBlockBean sendOutBlockBean;
    private AssocsBlockBean assocsBlockBean;
    private WorkflowBlockBean workflowBlockBean;
    private LogBlockBean logBlockBean;
    private boolean isDraft;
    private boolean showDocsAndCasesAssocs;
    private boolean skipInit;

    private Node node;

    public String action() {
        if (!getNodeService().exists(getNode().getNodeRef())) {
            return getDefaultCancelOutcome();
        }
        return "dialog:document";
    }

    /**
     * Should be called only when the document was received from DVK.
     */
    public void selectedValueChanged(ValueChangeEvent event) {
        String selectedType = (String) event.getNewValue();
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
            final String wordFileDisplayName = getDocumentTemplateService().populateTemplate(new NodeRef(ActionUtil.getParam(event, PARAM_DOCUMENT_NODE_REF)));
            MessageUtil.addInfoMessage("document_createWordFile_success", wordFileDisplayName);
        } catch (FileNotFoundException e) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), ERR_TEMPLATE_NOT_FOUND);
        } catch (InvalidNodeRefException e) {
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addErrorMessage(context, "document_createWordFile_error_docDeleted");
            context.getApplication().getNavigationHandler().handleNavigation(context, null, getDefaultCancelOutcome());
            return;
        } catch (NodeLockedException e) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "document_createWordFile_error_docLocked");
        } catch (RuntimeException e) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), ERR_TEMPLATE_PROCESSING_FAILED);
        }
        fileBlockBean.restore();
        logBlockBean.restore();
    }

    public void create(ActionEvent event) {
        createSnapshot();
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
        if (isFromDVK() || isFromImap()) {
            isDraft = true;
        } else {
            isDraft = false;
        }
    }

    public void copy(@SuppressWarnings("unused") ActionEvent event) {
        if (node == null)
            throw new RuntimeException("No current document");
        try {
            Node newNode = getDocumentService().copyDocument(node.getNodeRef());
            createSnapshot();
            skipInit = true;
            node = newNode;
            setupAction(true);
        } catch (InvalidNodeRefException e) {
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addErrorMessage(context, "document_copy_error_docDeleted");
            context.getApplication().getNavigationHandler().handleNavigation(context, null, getDefaultCancelOutcome());
        }
    }

    public void endDocument(@SuppressWarnings("unused") ActionEvent event) {
        Assert.notNull(node, "No current document");
        getDocumentService().endDocument(node.getNodeRef());
        // change property status of Node as well(in addition to changing it in repository) to avoid fetching node again just to reload single property needed
        // for file-block
        node.getProperties().put(DocumentCommonModel.Props.DOC_STATUS.toString(), DocumentStatus.FINISHED.getValueName());
        // refresh metadata block
        metadataBlockBean.init(node.getNodeRef(), isDraft);
        logBlockBean.restore();
        fileBlockBean.restore();
        MessageUtil.addInfoMessage("document_end_success");
    }

    public void reopenDocument(@SuppressWarnings("unused") ActionEvent event) {
        Assert.notNull(node, "No current document");
        final Map<String, Object> docProps = node.getProperties();
        final String docStatusBeforeReopen = (String) docProps.get(DocumentCommonModel.Props.DOC_STATUS.toString());
        getDocumentService().reopenDocument(node.getNodeRef());
        // change property status of Node as well(in addition to changing it in repository) to avoid fetching node again just to reload single property needed
        // for file-block
        final String docStatusAfterReopen = DocumentStatus.WORKING.getValueName();
        docProps.put(DocumentCommonModel.Props.DOC_STATUS.toString(), docStatusAfterReopen);
        // refresh metadata block
        metadataBlockBean.init(node.getNodeRef(), isDraft);
        if (!StringUtils.equals(docStatusBeforeReopen, docStatusAfterReopen)) {
            MessageUtil.addInfoMessage("document_reopen_success");
        }
    }

    public void deleteDocument(@SuppressWarnings("unused") ActionEvent event) {
        Assert.notNull(node, "No current document");
        try {
            getDocumentService().deleteDocument(node.getNodeRef());
        } catch (AccessDeniedException e) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "document_delete_error_accessDenied");
            return;
        } catch (NodeLockedException e) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "document_delete_error_docLocked");
            return;
        } catch (InvalidNodeRefException e) {
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addErrorMessage(context, "document_delete_error_docDeleted");
            context.getApplication().getNavigationHandler().handleNavigation(context, null, getDefaultCancelOutcome());
            return;
        }
        reset();
        // go back
        FacesContext fc = FacesContext.getCurrentInstance();
        NavigationHandler navigationHandler = fc.getApplication().getNavigationHandler();
        navigationHandler.handleNavigation(fc, null, getDefaultCancelOutcome());
        MessageUtil.addInfoMessage("document_delete_success");
    }

    public boolean isInprogressCompoundWorkflows() {
        return getWorkflowService().hasInprogressCompoundWorkflows(node.getNodeRef());
    }

    public void createFollowUp(ActionEvent event) {
        if (node == null)
            throw new RuntimeException("No current document");

        createSnapshot();
        this.skipInit = true;
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
        try {
            node = getDocumentService().createFollowUp(followUp, node.getNodeRef());
            setupAction(true);
        } catch (InvalidNodeRefException e) {
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addErrorMessage(context, "document_addFollowUp_error_docDeleted");
        }
    }

    public void createReply(@SuppressWarnings("unused") ActionEvent event) {
        if (node == null)
            throw new RuntimeException("No current document");

        createSnapshot();
        this.skipInit = true;
        QName replyType = null;
        if (DocumentSubtypeModel.Types.INCOMING_LETTER.equals(node.getType())) {
            replyType = DocumentSubtypeModel.Types.OUTGOING_LETTER;
        } else if (DocumentSubtypeModel.Types.CONTRACT_SIM.equals(node.getType())) {
            replyType = DocumentSubtypeModel.Types.INSTRUMENT_OF_DELIVERY_AND_RECEIPT;
        } else {
            throw new RuntimeException("Reply not possible for document of type " + node.getType());
        }
        try {
            node = getDocumentService().createReply(replyType, node.getNodeRef());
            setupAction(true);
        } catch (InvalidNodeRefException e) {
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addErrorMessage(context, "document_addReply_error_docDeleted");
        }
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
        logBlockBean.init(node);
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
        if (skipInit) {
            skipInit = false;
            return;
        }
        super.init(params);
        metadataBlockBean.init(node.getNodeRef(), isDraft);
        if (isFromDVK() || isFromImap()) {
            metadataBlockBean.setOwnerCurrentUser();
        }
        fileBlockBean.init(node);
        sendOutBlockBean.init(node);
        typeBlockBean.init();
        assocsBlockBean.init(node);
        searchBlockBean.init(node);
        workflowBlockBean.init(node.getNodeRef());
        logBlockBean.init(node);

        ClearStateNotificationHandler clearStateNotificationHandler //
        = (ClearStateNotificationHandler) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), ClearStateNotificationHandler.BEAN_NAME);
        clearStateNotificationHandler.addClearStateListener(this);
    }

    @Override
    public void restored() {
        try {
            final boolean snapshotRestored = restoreSnapshot();
            if (!snapshotRestored) {
                fileBlockBean.restore();
                sendOutBlockBean.restore();
                assocsBlockBean.restore();
                logBlockBean.restore();
                workflowBlockBean.restore();
            }
        } catch (UnableToPerformException e) {
            final FacesContext context = FacesContext.getCurrentInstance();
            // no need to add statusMessage, as it is already added
            context.getApplication().getNavigationHandler().handleNavigation(context, null, getDefaultCancelOutcome());
        }
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (metadataBlockBean.isInEditMode()) {
            if (isFromDVK()) {
                /** It's possible to change the type of the node that came from DVK */
                getDocumentService().changeType(node);
            }
            metadataBlockBean.save(isDraft);
            logBlockBean.restore();
            isDraft = false;
            isFinished = false;
            ((MenuBean) FacesHelper.getManagedBean(context, MenuBean.BEAN_NAME)).processTaskItems(); // Update UserWorkingDocuments number
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
        logBlockBean.reset();
        isDraft = false;
        showDocsAndCasesAssocs = false;
        skipInit = false;
    }

    private String getStatus() {
        return (String) node.getProperties().get(DocumentCommonModel.Props.DOC_STATUS.toString());
    }

    public void saveAndRegister() {
        // search for similar documents if it's an incoming letter
        if (metadataBlockBean.getDocumentType().getId().equals(DocumentSubtypeModel.Types.INCOMING_LETTER)) {
            String senderRegNum = (String) metadataBlockBean.getDocument().getProperties().get(DocumentSpecificModel.Props.SENDER_REG_NUMBER.toString());
            searchBlockBean.findSimilarDocuments(senderRegNum);
        }

        // just register if not an incoming letter or no similar documents found
        if (!searchBlockBean.isFoundSimilar()) {
            metadataBlockBean.saveAndRegister(isDraft);
            isDraft = false;
        }
        logBlockBean.restore();
    }

    public void registerDocument(ActionEvent event) {
        try {
            metadataBlockBean.registerDocument(event);
            // change property status of Node as well(in addition to changing it in repository) to avoid fetching node again just to reload single property
            // needed for file-block
            final Serializable updatedStatus = getNodeService().getProperty(node.getNodeRef(), DocumentCommonModel.Props.DOC_STATUS);
            node.getProperties().put(DocumentCommonModel.Props.DOC_STATUS.toString(), updatedStatus);
            logBlockBean.restore();
            fileBlockBean.restore();
        } catch (InvalidNodeRefException e) {
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addErrorMessage(context, "document_registerDoc_error_docDeleted");
            context.getApplication().getNavigationHandler().handleNavigation(context, null, getDefaultCancelOutcome());
        }
    }

    @Override
    public List<DialogButtonConfig> getAdditionalButtons() {
        List<DialogButtonConfig> buttons = new ArrayList<DialogButtonConfig>(1);
        RegisterDocumentEvaluator registrationEval = new RegisterDocumentEvaluator();
        if (metadataBlockBean.isInEditMode() &&
                (metadataBlockBean.getDocumentType().getId().equals(DocumentSubtypeModel.Types.LICENCE)
                || metadataBlockBean.getDocumentType().getId().equals(DocumentSubtypeModel.Types.INCOMING_LETTER))
                && registrationEval.evaluateAdditionalButton(metadataBlockBean.getDocument())) {
            if (searchBlockBean.isFoundSimilar()) {
                buttons.add(new DialogButtonConfig("documentRegisterButton", null, "document_registerDoc_continue",
                        "#{DocumentDialog.saveAndRegisterContinue}", "false", null));
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

    public boolean isFromImap() {
        return getDocumentService().isFromIncoming(node.getNodeRef()) || getDocumentService().isFromSent(node.getNodeRef());
    }

    public boolean isShowSearchBlock() {
        if ((searchBlockBean.isIncludeCases() && !metadataBlockBean.isInEditMode())) {
            return true;
        }
        return metadataBlockBean.isInEditMode() && searchBlockBean.isShow() && !searchBlockBean.isFoundSimilar() && (isFromDVK() || isFromImap());
    }

    public void hideSearchBlock(@SuppressWarnings("unused") ActionEvent event) {
        searchBlockBean.setIncludeCaseTitles(false);
    }

    public String getSearchBlockTitle() {
        if (isFromDVK() || isFromImap()) {
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
        final List<AssociationRef> targetAssocs = getNodeService().getTargetAssocs(sourceRef, assocType);
        for (AssociationRef associationRef : targetAssocs) {
            if (associationRef.getTargetRef().equals(targetRef) && associationRef.getTypeQName().equals(assocType)) {
                MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "document_assocAdd_error_alreadyExists");
                return;
            }
        }
        getNodeService().createAssociation(sourceRef, targetRef, assocType);
        assocsBlockBean.restore();
        MessageUtil.addInfoMessage("document_assocAdd_success");
    }

    public void searchDocsAndCases(@SuppressWarnings("unused") ActionEvent event) {
        this.showDocsAndCasesAssocs = true;
        searchBlockBean.init(metadataBlockBean.getDocument());
        searchBlockBean.setIncludeCaseTitles(true);
    }

    private void addTargetAssoc(NodeRef targetRef, QName targetType) {
        final DocAssocInfo docAssocInfo = searchBlockBean.addTargetAssoc(targetRef, targetType);
        assocsBlockBean.getDocAssocInfos().add(docAssocInfo);
        setupAction(true); // FIXME: dmitri, miks seda meetodit välja kutsusid? kui ainult lisatud seose kuvamiseks enne salvestamist, siis
        metadataBlockBean.updateFollowUpOrReplyProperties(targetRef);
        MessageUtil.addInfoMessage("document_assocAdd_success");
    }

    /**
     * Move all the files to the selected nodeRef, delete the current doc
     * and show the nodeRef doc.
     * 
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
    /**
     * restores DocumentDialog to previous state if needed - meaning that node is null or doesn't exist.
     * 
     * @return false if there was no need to restore document(node was not null and it still existed in repository). <br>
     *         true if node was restored.
     * @throws UnableToPerformException if node should have been restored(because node is null or doesn't exist anymore), but non of the documents in snapshots
     *             stack were not applicable for restoring.
     */
    private boolean restoreSnapshot() {
        // if there's a node, then current DocumentDialog has not been closed
        // so we shouldn't restore snapshot
        final boolean nodeDeleted = node != null && !getNodeService().exists(node.getNodeRef());
        final boolean mustRestore = node == null || nodeDeleted;
        if (mustRestore) {
            if (snapshots.empty()) {
                throw new UnableToPerformException(MessageSeverity.INFO, "document_restore_error_stackEmpty");
            }
            if (nodeDeleted) {
                MessageUtil.addInfoMessage(FacesContext.getCurrentInstance(), "document_restore_error_docDeleted");
            }
            snapshots.pop().restoreState(this);
            boolean recursivelyRestored = false;
            if (node == null || !getNodeService().exists(node.getNodeRef())) {
                recursivelyRestored = restoreSnapshot();
            }
            if (!recursivelyRestored) {
                log.debug("didn't restore document snapshot recursively");
                // node exists, just re-init other beans as well
                fileBlockBean.init(node);
                sendOutBlockBean.init(node);
                typeBlockBean.init();
                assocsBlockBean.init(node);
                workflowBlockBean.init(node.getNodeRef());
                logBlockBean.init(node);
            } else {
                log.debug("restored document snapshot recursively");
            }
            return true; // restored from snapshot
        }
        return false; // not restored
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
        private boolean skipInit;
        private MetadataBlockBean.Snapshot metadataSnapshot;
        private SearchBlockBean.Snapshot searchSnapshot;

        private Snapshot(DocumentDialog dialog) {
            this.node = dialog.node;
            this.isDraft = dialog.isDraft;
            this.skipInit = dialog.skipInit;
            this.showDocsAndCasesAssocs = dialog.showDocsAndCasesAssocs;
            metadataSnapshot = dialog.metadataBlockBean.createSnapshot();
            searchSnapshot = dialog.searchBlockBean.createSnapshot();
        }

        private void restoreState(DocumentDialog dialog) {
            dialog.node = this.node;
            dialog.isDraft = this.isDraft;
            dialog.skipInit = this.skipInit;
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

    private WorkflowService getWorkflowService() {
        if (workflowService == null) {
            workflowService = (WorkflowService) FacesContextUtils.getRequiredWebApplicationContext(//
                    FacesContext.getCurrentInstance()).getBean(WorkflowService.BEAN_NAME);
        }
        return workflowService;
    }

    public DocumentService getDocumentService() {
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

    public LogBlockBean getDocumentLog() {
        return logBlockBean;
    }

    public void setLogBlockBean(LogBlockBean logBlockBean) {
        this.logBlockBean = logBlockBean;
    }

    // END: getters / setters
}
