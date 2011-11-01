package ee.webmedia.alfresco.document.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDialogHelperBean;
import static ee.webmedia.alfresco.document.einvoice.web.TransactionsTemplateDetailsDialog.MODAL_KEY_ENTRY_SAP_NUMBER;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD_MV;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.INSTRUMENT_OF_DELIVERY_AND_RECEIPT_MV;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.INTERNAL_APPLICATION;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.INTERNAL_APPLICATION_MV;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.LEAVING_LETTER;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.REPORT;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.REPORT_MV;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.RESOLUTION_MV;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.TRAINING_APPLICATION;
import static ee.webmedia.alfresco.utils.RepoUtil.addAssoc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIPanel;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

import org.alfresco.config.Config;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.config.ActionsConfigElement;
import org.alfresco.web.config.ActionsConfigElement.ActionDefinition;
import org.alfresco.web.config.ActionsConfigElement.ActionGroup;
import org.alfresco.web.config.DialogsConfigElement.DialogButtonConfig;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.listener.RefreshEventListener;
import ee.webmedia.alfresco.common.propertysheet.modalLayer.ModalLayerComponent.ModalLayerSubmitEvent;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.ClearStateNotificationHandler;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.document.associations.model.DocAssocInfo;
import ee.webmedia.alfresco.document.associations.web.AssocsBlockBean;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceUtil;
import ee.webmedia.alfresco.document.einvoice.web.TransactionsBlockBean;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.web.FileBlockBean;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.log.web.LogBlockBean;
import ee.webmedia.alfresco.document.metadata.web.MetadataBlockBean;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.search.web.SearchBlockBean;
import ee.webmedia.alfresco.document.sendout.web.SendOutBlockBean;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.type.web.TypeBlockBean;
import ee.webmedia.alfresco.document.web.FavoritesModalComponent.AddToFavoritesEvent;
import ee.webmedia.alfresco.document.web.evaluator.RegisterDocumentEvaluator;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.PermissionDeniedException;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.web.WorkflowBlockBean;

/**
 * @author Alar Kvell
 */
public class DocumentDialog extends BaseDialogBean implements ClearStateNotificationHandler.ClearStateListener, RefreshEventListener, DialogDataProvider {
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentDialog.class);

    public static final String BEAN_NAME = "DocumentDialog";
    public static final String DIALOG_DOCUMENT = "dialog:document";

    private static final String ERR_TEMPLATE_NOT_FOUND = "document_errorMsg_template_not_found";
    private static final String ERR_TEMPLATE_PROCESSING_FAILED = "document_errorMsg_template_processsing_failed";
    /** FollowUps is with the same type */
    private static final List<QName> regularFollowUpTypes = Arrays.asList(ERRAND_APPLICATION_DOMESTIC, ERRAND_ORDER_ABROAD, ERRAND_ORDER_ABROAD_MV,
            LEAVING_LETTER,
            TRAINING_APPLICATION, INTERNAL_APPLICATION, INTERNAL_APPLICATION_MV, REPORT, REPORT_MV, INSTRUMENT_OF_DELIVERY_AND_RECEIPT_MV, RESOLUTION_MV);

    private static final String PARAM_DOCUMENT_TYPE = "documentType";
    private static final String PARAM_DOCUMENT_NODE_REF = "documentNodeRef";
    private static final String PARAM_NODEREF = "nodeRef";

    private transient DocumentService documentService;
    private transient DocumentLogService documentLogService;
    private transient DocumentTemplateService documentTemplateService;
    private transient WorkflowService workflowService;
    private transient UIPanel modalContainer;

    private SearchBlockBean searchBlockBean;
    private TypeBlockBean typeBlockBean;
    private MetadataBlockBean metadataBlockBean;
    private FileBlockBean fileBlockBean;
    private SendOutBlockBean sendOutBlockBean;
    private AssocsBlockBean assocsBlockBean;
    private WorkflowBlockBean workflowBlockBean;
    private TransactionsBlockBean transactionsBlockBean;
    private LogBlockBean logBlockBean;
    private boolean isDraft;
    private boolean showDocsAndCasesAssocs;
    private boolean skipInit;
    private boolean docReloadDisabled;

    private Node node;
    private List<NodeRef> newInvoiceDocuments = new ArrayList<NodeRef>();

    public String action() {
        if (openDynamicDocument) {
            openDynamicDocument = false;
            return null;
        }
        try {
            validatePermissions();
        } catch (PermissionDeniedException e) {
            return e.getNavigationOutcome();
        }
        return DIALOG_DOCUMENT;
    }

    /**
     * @throws AccessDeniedException when access is denied. For simplicity exception.getMessage() contains outcome for navigation
     */
    private void validatePermissions() throws AccessDeniedException {
        Node docNode = getNode();
        if (docNode == null || !getNodeService().exists(docNode.getNodeRef())) {
            throw new PermissionDeniedException("node doesn't exist anymore", getDefaultCancelOutcome());
        }
        try {
            validatePermission(docNode, "documentOpen_failed_missingPermission", DocumentCommonModel.Privileges.VIEW_DOCUMENT_META_DATA);
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(e);
            throw new PermissionDeniedException(MessageUtil.getMessage(e), null);
        }
    }

    public void handleLockedNode(String messageId) {
        handleLockedNode(messageId, node.getNodeRef());
    }

    private void handleLockedNode(String messageId, NodeRef nodeRef) {
        MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), messageId,
                BeanHelper.getUserService().getUserFullName((String) getNodeService().getProperty(nodeRef, ContentModel.PROP_LOCK_OWNER)));
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
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(FacesContext.getCurrentInstance(), e);
        } catch (FileNotFoundException e) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), ERR_TEMPLATE_NOT_FOUND);
        } catch (InvalidNodeRefException e) {
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addErrorMessage(context, "document_createWordFile_error_docDeleted");
            WebUtil.navigateTo(getDefaultCancelOutcome(), context);
            return;
        } catch (NodeLockedException e) {
            handleLockedNode("document_createWordFile_error_docLocked");
        } catch (RuntimeException e) {
            log.error("Populate template failed", e);
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

    private boolean openDynamicDocument = false;

    public void open(ActionEvent event) {
        final NodeRef docRef = new NodeRef(ActionUtil.getParam(event, PARAM_NODEREF));
        // TODO DLSeadist temporary
        if (DocumentCommonModel.Types.DOCUMENT.equals(getNodeService().getType(docRef))) {
            openDynamicDocument = true;
            BeanHelper.getDocumentDynamicDialog().openFromDocumentList(event);
            return;
        }
        open(docRef);
    }

    public void open(NodeRef docRef) {
        Node permissionCheckNode = new Node(docRef);
        if (!permissionCheckNode.hasPermission(DocumentCommonModel.Privileges.VIEW_DOCUMENT_META_DATA)) {
            node = permissionCheckNode; // update node for action() method!
            return; // in action() method error will be shown
        }
        createSnapshot();
        node = getDocumentService().getDocument(docRef);
        /** open a doc for editing if it's from dvk */
        if (isFromDVK() || isFromImap() || isIncomingInvoice()) {
            isDraft = true;
        } else {
            isDraft = false;
        }
        metadataBlockBean.init(docRef, false, this);
        metadataBlockBean.setSkipInvoiceMessages(true);
    }

    public void copy(@SuppressWarnings("unused") ActionEvent event) {
        if (node == null) {
            throw new RuntimeException("No current document");
        }

        // V1 to V2 copy isn't supported
        if (CollectionUtils.containsAny(node.getAspects(), Arrays.asList(DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD,
                DocumentSpecificModel.Aspects.ERRAND_APPLICATION_DOMESTIC, DocumentSpecificModel.Aspects.TRAINING_APPLICATION))) {
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addInfoMessage(context, "document_copy_error_docVersionChanged");
            WebUtil.navigateTo(getDefaultCancelOutcome(), context);
            return;
        }

        try {
            Node newNode = getDocumentService().copyDocument(node.getNodeRef());
            createSnapshot();
            skipInit = true;
            node = newNode;
            setupAction(true);
        } catch (InvalidNodeRefException e) {
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addErrorMessage(context, "document_copy_error_docDeleted");
            WebUtil.navigateTo(getDefaultCancelOutcome(), context);
        }
    }

    public void endDocument(@SuppressWarnings("unused") ActionEvent event) {
        Node node = getDocumentDialogHelperBean().getNode();
        Assert.notNull(node, "No current document");
        final Map<String, Object> docProps = node.getProperties();
        final String docStatusBefore = (String) docProps.get(DocumentCommonModel.Props.DOC_STATUS.toString());
        try {
            getDocumentService().endDocument(node.getNodeRef());
        } catch (NodeLockedException e) {
            handleLockedNode("document_end_error_docLocked");
            return;
        }
        final String docStatusAfter = DocumentStatus.FINISHED.getValueName();
        // refresh metadata block
        getDocumentDialogHelperBean().switchMode(false);
        if (!StringUtils.equals(docStatusBefore, docStatusAfter)) {
            MessageUtil.addInfoMessage("document_end_success");
        }
    }

    public void reopenDocument(@SuppressWarnings("unused") ActionEvent event) {
        Node node = getDocumentDialogHelperBean().getNode();
        Assert.notNull(node, "No current document");
        final Map<String, Object> docProps = node.getProperties();
        final String docStatusBefore = (String) docProps.get(DocumentCommonModel.Props.DOC_STATUS.toString());
        try {
            getDocumentService().reopenDocument(node.getNodeRef());
        } catch (NodeLockedException e) {
            handleLockedNode("document_reopen_error_docLocked");
            return;
        }
        final String docStatusAfter = DocumentStatus.WORKING.getValueName();
        // refresh metadata block
        getDocumentDialogHelperBean().switchMode(false);
        if (!StringUtils.equals(docStatusBefore, docStatusAfter)) {
            MessageUtil.addInfoMessage("document_reopen_success");
        }
    }

    public void deleteDocument(@SuppressWarnings("unused") ActionEvent event) {
        Node node = getDocumentDialogHelperBean().getNode();
        Assert.notNull(node, "No current document");
        try {
            validatePermission(node, DocumentCommonModel.Privileges.DELETE_DOCUMENT_META_DATA);
            getDocumentService().deleteDocument(node.getNodeRef());
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(e);
            return;
        } catch (AccessDeniedException e) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "document_delete_error_accessDenied");
            return;
        } catch (NodeLockedException e) {
            handleLockedNode("document_delete_error_docLocked");
            return;
        } catch (InvalidNodeRefException e) {
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addErrorMessage(context, "document_delete_error_docDeleted");
            WebUtil.navigateTo(getDefaultCancelOutcome(), context);
            return;
        }
        // go back
        WebUtil.navigateTo(BeanHelper.getDialogManager().cancel());
        MessageUtil.addInfoMessage("document_delete_success");
    }

    public boolean isInprogressCompoundWorkflows() {
        return getDocumentDialogHelperBean().isNotWorkingOrNotEditable();
    }

    public void createFollowUp(ActionEvent event) {
        if (node == null) {
            throw new RuntimeException("No current document");
        }

        createSnapshot();
        skipInit = true;
        QName followUp = null;
        QName type = node.getType();
        if (DocumentSubtypeModel.Types.INCOMING_LETTER.equals(type) ||
                DocumentSubtypeModel.Types.INCOMING_LETTER_MV.equals(type) ||
                DocumentSubtypeModel.Types.OUTGOING_LETTER.equals(type) ||
                DocumentSubtypeModel.Types.OUTGOING_LETTER_MV.equals(type) ||
                DocumentSubtypeModel.Types.CONTRACT_SIM.equals(type) ||
                DocumentSubtypeModel.Types.CONTRACT_SMIT.equals(type) ||
                DocumentSubtypeModel.Types.CONTRACT_MV.equals(type) ||
                DocumentSubtypeModel.Types.TENDERING_APPLICATION.equals(type)) {

            followUp = QName.createQName(DocumentSubtypeModel.URI, ActionUtil.getParam(event, PARAM_DOCUMENT_TYPE));
        } else if (DocumentSubtypeModel.Types.MINUTES_MV.equals(type)) {
            followUp = DocumentSubtypeModel.Types.RESOLUTION_MV;
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
        } catch (NodeLockedException e) {
            handleLockedNode("document_addFollowUp_error_docLocked");
        }
    }

    public void createReply(@SuppressWarnings("unused") ActionEvent event) {
        if (node == null) {
            throw new RuntimeException("No current document");
        }

        createSnapshot();
        skipInit = true;
        QName replyType = null;
        if (DocumentSubtypeModel.Types.INCOMING_LETTER.equals(node.getType())) {
            replyType = DocumentSubtypeModel.Types.OUTGOING_LETTER;
        } else if (DocumentSubtypeModel.Types.INCOMING_LETTER_MV.equals(node.getType())) {
            replyType = DocumentSubtypeModel.Types.OUTGOING_LETTER_MV;
        } else if (DocumentSubtypeModel.Types.CONTRACT_SIM.equals(node.getType())) {
            replyType = DocumentSubtypeModel.Types.INSTRUMENT_OF_DELIVERY_AND_RECEIPT;
        } else if (DocumentSubtypeModel.Types.CONTRACT_MV.equals(node.getType())) {
            replyType = DocumentSubtypeModel.Types.INSTRUMENT_OF_DELIVERY_AND_RECEIPT_MV;
        } else {
            throw new RuntimeException("Reply not possible for document of type " + node.getType());
        }
        try {
            node = getDocumentService().createReply(replyType, node.getNodeRef());
            setupAction(true);
        } catch (InvalidNodeRefException e) {
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addErrorMessage(context, "document_addReply_error_docDeleted");
        } catch (NodeLockedException e) {
            handleLockedNode("document_addReply_error_docLocked");
        }
    }

    public void sendToSap(ActionEvent event) {
        if (!transactionsBlockBean.checkTotalSum()) {
            // transactionBlockBean is responsible for setting error messages
            return;
        }
        Pair<File, Integer> transFileAndCount = null;
        Pair<File, Integer> einvoiceFileAndCount = null;
        boolean sendTransactions = !Boolean.TRUE.equals(metadataBlockBean.getDocument().getProperties().get(DocumentSpecificModel.Props.XXL_INVOICE))
                && StringUtils.isBlank((String) metadataBlockBean.getDocument().getProperties().get(DocumentSpecificModel.Props.PURCHASE_ORDER_SAP_NUMBER));
        if (sendTransactions) {
            transFileAndCount = EInvoiceUtil.getTransOrInvoiceFileAndCount(fileBlockBean.getFiles(), true);
            Integer transactionFileCount = transFileAndCount.getSecond();
            if (transactionFileCount > 1) {
                MessageUtil.addErrorMessage("document_sendToSap_transMultipleXmlFiles");
                return;
            }
            if (transactionFileCount == 0 && transactionsBlockBean.getTransactions().isEmpty()) {
                MessageUtil.addErrorMessage("document_sendToSap_noTransactionData");
                return;
            }
        } else {
            einvoiceFileAndCount = EInvoiceUtil.getTransOrInvoiceFileAndCount(fileBlockBean.getFiles(), false);
            if (einvoiceFileAndCount.getSecond() == 0) {
                MessageUtil.addErrorMessage("document_sendToSap_noEInvoiceData");
                return;
            }
            if (einvoiceFileAndCount.getSecond() > 1) {
                MessageUtil.addErrorMessage("document_sendToSap_einvoiceMultipleXmlFiles");
                return;
            }
        }
        try {
            if (sendTransactions) {
                if (transFileAndCount.getSecond() > 0) {
                    BeanHelper.getDvkService().sendInvoiceFileToSap(metadataBlockBean.getDocument(), transFileAndCount.getFirst());
                } else {
                    BeanHelper.getDvkService().generateAndSendInvoiceFileToSap(metadataBlockBean.getDocument(), transactionsBlockBean.getTransactions());
                }
            } else {
                BeanHelper.getDvkService().sendInvoiceFileToSap(metadataBlockBean.getDocument(), einvoiceFileAndCount.getFirst());
            }
            BeanHelper.getDocumentLogService().addDocumentLog(node.getNodeRef(), MessageUtil.getMessage("document_log_status_send_to_sap"));
            BeanHelper.getDocumentService().setDocStatusFinished(node.getNodeRef());
            BeanHelper.getWorkflowService().finishUserActiveResponsibleInProgressTask(node.getNodeRef(), MessageUtil.getMessage("task_comment_sentToSap"));
            reloadDocAndClearPropertySheet(false);
            MessageUtil.addInfoMessage("document_send_to_sap_success");
        } catch (Exception e) {
            String messageKey = "document_sendToSap_errorSendingOrGeneratingXml";
            log.error(MessageUtil.getMessage(messageKey), e);
            MessageUtil.addErrorMessage(messageKey);
            return;
        }
    }

    public List<String> getFavoriteDirectoryNames(@SuppressWarnings("unused") FacesContext context, @SuppressWarnings("unused") UIInput selectComponent) {
        return getDocumentService().getFavoriteDirectoryNames();
    }

    public void addFavorite(ActionEvent event) {
        getDocumentService().addFavorite(getDocumentDialogHelperBean().getNodeRef(), ((AddToFavoritesEvent) event).getFavoriteDirectoryName());
    }

    public void removeFavorite(@SuppressWarnings("unused") ActionEvent event) {
        getDocumentService().removeFavorite(getDocumentDialogHelperBean().getNodeRef());
    }

    public void sendToSapManually(ActionEvent event) {
        if (!transactionsBlockBean.checkTotalSum()) {
            // transactionBlockBean is responsible for setting error messages
            return;
        }
        ModalLayerSubmitEvent sendToSapEvent = (ModalLayerSubmitEvent) event;
        String entrySapNumber = (String) sendToSapEvent.getSubmittedValue(MODAL_KEY_ENTRY_SAP_NUMBER);
        if (StringUtils.isBlank(entrySapNumber)) {
            return;
        }
        metadataBlockBean.getDocument().getProperties().put(DocumentSpecificModel.Props.ENTRY_SAP_NUMBER.toString(), entrySapNumber);
        metadataBlockBean.getDocument().getProperties().put(DocumentCommonModel.Props.DOC_STATUS.toString(), DocumentStatus.FINISHED.getValueName());
        BeanHelper.getDocumentService().updateDocument(metadataBlockBean.getDocument());
        BeanHelper.getDocumentLogService().addDocumentLog(node.getNodeRef(), MessageUtil.getMessage("document_log_status_send_to_sap_manually"));
        BeanHelper.getWorkflowService().finishUserActiveResponsibleInProgressTask(node.getNodeRef(), MessageUtil.getMessage("task_comment_sentToSap_manually"));
        reloadDocAndClearPropertySheet(false);
        logBlockBean.restore();
        MessageUtil.addInfoMessage("save_success");
    }

    public void setupAction(boolean mode) {
        isDraft = mode;
        if (isDraft) {
            metadataBlockBean.editNewDocument(node);
        } else {
            metadataBlockBean.viewDocument(node);
        }
        fileBlockBean.init(node);
        assocsBlockBean.init(node);
        workflowBlockBean.init(node);
        sendOutBlockBean.init(node);
        logBlockBean.init(node);
        transactionsBlockBean.init(metadataBlockBean.getDocument(), this);

        getDocumentDialogHelperBean().reset(this);
    }

    @Override
    public String getMoreActionsConfigId() {
        if (!metadataBlockBean.isInEditMode()) {
            if (DocumentSubtypeModel.Types.TENDERING_APPLICATION.equals(node.getType()) ||
                    DocumentSubtypeModel.Types.INCOMING_LETTER.equals(node.getType()) ||
                    DocumentSubtypeModel.Types.INCOMING_LETTER_MV.equals(node.getType()) ||
                    DocumentSubtypeModel.Types.OUTGOING_LETTER.equals(node.getType()) ||
                    DocumentSubtypeModel.Types.OUTGOING_LETTER_MV.equals(node.getType()) ||
                    DocumentSubtypeModel.Types.CONTRACT_SIM.equals(node.getType()) ||
                    DocumentSubtypeModel.Types.CONTRACT_SMIT.equals(node.getType()) ||
                    DocumentSubtypeModel.Types.CONTRACT_MV.equals(node.getType())) {

                Config config = org.alfresco.web.app.Application.getConfigService(FacesContext.getCurrentInstance()).getGlobalConfig();
                final ActionsConfigElement actionsConfig = (ActionsConfigElement) config.getConfigElement(ActionsConfigElement.CONFIG_ELEMENT_ID);
                final ActionGroup actionGroup = actionsConfig.getActionGroup("document_more_actions");
                for (String actionId : actionGroup) {
                    ActionDefinition action = actionsConfig.getActionDefinition(actionId);
                    if (action.Evaluator == null || action.Evaluator.evaluate(node)) {
                        return "document_more_actions";
                    }
                }
            }
        }
        return "";
    }

    @Override
    public void init(Map<String, String> params) {
        validatePermissions();
        BeanHelper.getVisitedDocumentsBean().getVisitedDocuments().add(node.getNodeRef());
        if (skipInit) {
            searchBlockBean.init(node, isIncomingInvoice());
            skipInit = false;
            return;
        }
        super.init(params);
        metadataBlockBean.init(node.getNodeRef(), isDraft, this);

        if (isFromDVK() || isFromImap() || isIncomingInvoice()) {
            String ownerName = (String) node.getProperties().get(DocumentCommonModel.Props.OWNER_NAME);
            if (StringUtils.isEmpty(ownerName) || !isIncomingInvoice()) {
                metadataBlockBean.setOwnerCurrentUser();
            }
            if (isIncomingInvoice() && ownerName == null) {
                MessageUtil.addErrorMessage("document_errorMsg_invoice_owner_not_found");
            }
            showDocsAndCasesAssocs = false;
        }

        getDocumentDialogHelperBean().reset(this);

        fileBlockBean.init(node);
        typeBlockBean.init();
        assocsBlockBean.init(node);
        searchBlockBean.init(node, isIncomingInvoice());
        workflowBlockBean.init(node);
        sendOutBlockBean.init(node);
        logBlockBean.init(node);
        transactionsBlockBean.init(metadataBlockBean.getDocument(), this);

        BeanHelper.getClearStateNotificationHandler().addClearStateListener(this);

        // Add favorite modal component
        FavoritesModalComponent modal = new FavoritesModalComponent();
        modal.setActionListener(FacesContext.getCurrentInstance().getApplication().createMethodBinding("#{DocumentDialog.addFavorite}", UIActions.ACTION_CLASS_ARGS));
        modal.setId("favorite-popup-" + FacesContext.getCurrentInstance().getViewRoot().createUniqueId());
        List<UIComponent> children = ComponentUtil.getChildren(getModalContainer());
        children.clear();
        children.add(modal);
    }

    public void reloadDoc() {
        node = getDocumentService().getDocument(node.getNodeRef());
        metadataBlockBean.reloadDoc();
    }

    public void reloadDocAndClearPropertySheet(boolean addInvoiceMessages) {
        node = getDocumentService().getDocument(node.getNodeRef());
        metadataBlockBean.reloadDocAndClearPropertySheet(addInvoiceMessages);
        transactionsBlockBean.restore(metadataBlockBean.getDocument());
    }

    @Override
    public void restored() {
        try {
            final boolean snapshotRestored = restoreSnapshot();
            BeanHelper.getVisitedDocumentsBean().getVisitedDocuments().add(node.getNodeRef());
            if (!snapshotRestored) {
                if (!docReloadDisabled) {
                    reloadDocAndClearPropertySheet(true);
                }
                fileBlockBean.restore();
                sendOutBlockBean.restore();
                assocsBlockBean.restore();
                logBlockBean.restore();
                workflowBlockBean.restore();
                if (!docReloadDisabled) {
                    transactionsBlockBean.restore();
                } else {
                    docReloadDisabled = false;
                }
            }
        } catch (UnableToPerformException e) {
            final FacesContext context = FacesContext.getCurrentInstance();
            // no need to add statusMessage, as it is already added
            WebUtil.navigateTo(getDefaultCancelOutcome(), context);
        }

        getDocumentDialogHelperBean().reset(this);
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (metadataBlockBean.isInEditMode()) {
            if (isFromDVK()) {
                /** It's possible to change the type of the node that came from DVK */
                getDocumentService().changeType(node);
            }
            if (transactionsBlockBean.saveTransactions()) {
                metadataBlockBean.save(isDraft, newInvoiceDocuments);
                notifyModeChanged();
                transactionsBlockBean.restore(metadataBlockBean.getDocument());
            }
            logBlockBean.restore();
            searchBlockBean.reset();
            isDraft = false;
            isFinished = false;
            ((MenuBean) FacesHelper.getManagedBean(context, MenuBean.BEAN_NAME)).processTaskItems(); // Update UserWorkingDocuments number
            return null;
        }
        reset();
        return outcome;
    }

    @Override
    public boolean isFinishButtonVisible(boolean dialogConfOKButtonVisible) {
        return getMeta().isInEditMode();
    }

    public void notifyModeChanged() {
        transactionsBlockBean.onModeChanged();
    }

    @Override
    public boolean isInEditMode() {
        return metadataBlockBean.isInEditMode();
    }

    @Override
    public String cancel() {
        if (metadataBlockBean.isInEditMode() && !isDraft) {
            metadataBlockBean.cancel();
            transactionsBlockBean.restore();
            notifyModeChanged();
            return null;
        }
        reset();
        if (metadataBlockBean.isInEditMode() && isDraft) {
            getDocumentService().deleteDocument(node.getNodeRef());
            for (NodeRef invoiceRef : newInvoiceDocuments) {
                getDocumentService().deleteDocument(invoiceRef);
            }
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
        transactionsBlockBean.reset();
        isDraft = false;
        showDocsAndCasesAssocs = false;
        skipInit = false;
        newInvoiceDocuments = new ArrayList<NodeRef>();

        getDocumentDialogHelperBean().reset(this);
    }

    private String getStatus() {
        return (String) node.getProperties().get(DocumentCommonModel.Props.DOC_STATUS.toString());
    }

    public void saveAndRegisterContinue() {
        // similar documents were found before, finish registering
        metadataBlockBean.saveAndRegister(isDraft, newInvoiceDocuments);
        searchBlockBean.setFoundSimilar(false);
    }

    public void saveAndRegister() {
        // search for similar documents if it's an incoming letter
        QName docType = metadataBlockBean.getDocumentType().getId();
        if (docType.equals(DocumentSubtypeModel.Types.INCOMING_LETTER) || docType.equals(DocumentSubtypeModel.Types.INCOMING_LETTER_MV)) {
            String senderRegNum = (String) metadataBlockBean.getDocument().getProperties().get(DocumentSpecificModel.Props.SENDER_REG_NUMBER.toString());
            searchBlockBean.findSimilarDocuments(senderRegNum, docType);
        }

        // just register if not an incoming letter or no similar documents found
        if (!searchBlockBean.isFoundSimilar()) {
            metadataBlockBean.saveAndRegister(isDraft, newInvoiceDocuments);
            isDraft = false;
        }
        logBlockBean.restore();
        transactionsBlockBean.restore();
    }

    public void registerDocument(ActionEvent event) {
        try {
            metadataBlockBean.registerDocument(event);
            // change property status of Node as well(in addition to changing it in repository) to avoid fetching node again just to reload single property
            // needed for file-block
            final Serializable updatedStatus = getNodeService().getProperty(getDocumentDialogHelperBean().getNodeRef(), DocumentCommonModel.Props.DOC_STATUS);
            getDocumentDialogHelperBean().getProps().put(DocumentCommonModel.Props.DOC_STATUS.toString(), updatedStatus);
        } catch (InvalidNodeRefException e) {
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addErrorMessage(context, "document_registerDoc_error_docDeleted");
            WebUtil.navigateTo(getDefaultCancelOutcome(), context);
        }
    }

    @Override
    public List<DialogButtonConfig> getAdditionalButtons() {
        List<DialogButtonConfig> buttons = new ArrayList<DialogButtonConfig>(1);
        RegisterDocumentEvaluator registrationEval = new RegisterDocumentEvaluator();
        if (metadataBlockBean.isInEditMode() &&
                (metadataBlockBean.getDocumentType().getId().equals(DocumentSubtypeModel.Types.LICENCE)
                        || metadataBlockBean.getDocumentType().getId().equals(DocumentSubtypeModel.Types.INCOMING_LETTER)
                        || metadataBlockBean.getDocumentType().getId().equals(DocumentSubtypeModel.Types.INCOMING_LETTER_MV))
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

    @Override
    public Node getNode() {
        return node;
    }

    public boolean isFromDVK() {
        return getDocumentService().isFromDVK(node.getNodeRef());
    }

    public boolean isIncomingInvoice() {
        return getDocumentService().isIncomingInvoice(node.getNodeRef());
    }

    // doccom:docStatus=lõpetatud
    public boolean isClosedOrNotEditable() {
        return DocumentStatus.FINISHED.getValueName().equals(node.getProperties().get(DocumentCommonModel.Props.DOC_STATUS))
                || isNotEditable();
    }

    // doccom:docStatus!=töös
    public boolean isNotWorkingOrNotEditable() {
        return getDocumentDialogHelperBean().isNotWorkingOrNotEditable();
    }

    public boolean isNotWorkingAndFinishedOrNotEditable() {
        return !(DocumentStatus.WORKING.getValueName().equals(node.getProperties().get(DocumentCommonModel.Props.DOC_STATUS))
                || DocumentStatus.FINISHED.getValueName().equals(node.getProperties().get(DocumentCommonModel.Props.DOC_STATUS)))
                || isNotEditable();
    }

    public boolean isInvoiceXmlOrIsClosed() {
        return Boolean.TRUE.equals(node.getProperties().get(DocumentSpecificModel.Props.INVOICE_XML)) || isClosedOrNotEditable();
    }

    public boolean isNotEditable() {
        return getDocumentDialogHelperBean().isNotEditable();
    }

    public boolean isFromImap() {
        return getDocumentService().isFromIncoming(node.getNodeRef()) || getDocumentService().isFromSent(node.getNodeRef());
    }

    public boolean isShowSearchBlock() {
        if ((searchBlockBean.isExpanded() && !metadataBlockBean.isInEditMode())) {
            return true;
        }
        return metadataBlockBean.isInEditMode() && searchBlockBean.isShow() && !searchBlockBean.isFoundSimilar()
                && ((isFromDVK() || isFromImap() || isIncomingInvoice()) && !isNotEditable());
    }

    public boolean isShowTransactionsBlock() {
        return DocumentSubtypeModel.Types.INVOICE.equals(node.getType());
    }

    public void hideSearchBlock(@SuppressWarnings("unused") ActionEvent event) {
        searchBlockBean.setExpanded(false);
    }

    public String getSearchBlockTitle() {
        if ((isFromDVK() && !isNotEditable()) || isFromImap() || isIncomingInvoice()) {
            return MessageUtil.getMessage("document_search_base_title");
        }
        return MessageUtil.getMessage("document_search_docOrCase_title");
    }

    public boolean isShowFoundSimilar() {
        return metadataBlockBean.isInEditMode() && searchBlockBean.isFoundSimilar();
    }

    public boolean isShowTypeBlock() {
        return metadataBlockBean.isInEditMode() && isFromDVK() && !isNotEditable();
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

    private void saveAssocNow(final NodeRef sourceRef, final NodeRef targetRef, final QName assocType) {
        final List<AssociationRef> targetAssocs = getNodeService().getTargetAssocs(sourceRef, assocType);
        for (AssociationRef associationRef : targetAssocs) {
            if (associationRef.getTargetRef().equals(targetRef) && associationRef.getTypeQName().equals(assocType)) {
                MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "document_assocAdd_error_alreadyExists");
                return;
            }
        }
        try {
            getDocumentService().createAssoc(sourceRef, targetRef, assocType);
        } catch (NodeLockedException e) {
            NodeRef nodeRef = e.getNodeRef();
            String messageId = nodeRef.equals(sourceRef) ? "document_assocAdd_error_sourceLocked" : "document_assocAdd_error_targetLocked";
            handleLockedNode(messageId, nodeRef);
            return;
        }
        assocsBlockBean.restore();
        MessageUtil.addInfoMessage("document_assocAdd_success");
    }

    public void searchDocsAndCases(@SuppressWarnings("unused") ActionEvent event) {
        showDocsAndCasesAssocs = true;
        searchBlockBean.init(metadataBlockBean.getDocument(), isIncomingInvoice());
        searchBlockBean.setExpanded(true);
    }

    private void addTargetAssoc(NodeRef targetRef, QName targetType) {
        final DocAssocInfo docAssocInfo = getDocumentService().getDocAssocInfo(addAssoc(searchBlockBean.getNode(), targetRef, targetType, true), true);
        searchBlockBean.setShow(false);
        assocsBlockBean.getDocAssocInfos().add(docAssocInfo);
        assocsBlockBean.init(node);
        metadataBlockBean.editNewDocument(node);
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

    public void addFile(@SuppressWarnings("unused") ActionEvent event) {
        docReloadDisabled = true;
        BeanHelper.getAddFileDialog().start(null);
    }

    @Override
    public void refresh() {
        fileBlockBean.restore();
    }

    @Override
    public UIPropertySheet getPropertySheet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void switchMode(boolean inEditMode) {
        Assert.isTrue(!inEditMode);
        getMeta().viewDocument(getDocumentService().getDocument(metadataBlockBean.getDocument().getNodeRef()));
    }

    // START: snapshot logic (for supporting multiple concurrent document views)
    private final Stack<Snapshot> snapshots = new Stack<Snapshot>();

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
                typeBlockBean.init();
                assocsBlockBean.init(node);
                workflowBlockBean.init(node);
                sendOutBlockBean.init(node);
                logBlockBean.init(node);
                transactionsBlockBean.init(node, this);

                getDocumentDialogHelperBean().reset(this);
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

        private final Node node;
        private final boolean isDraft;
        private final boolean showDocsAndCasesAssocs;
        private final boolean skipInit;
        private final MetadataBlockBean.Snapshot metadataSnapshot;
        private final SearchBlockBean.Snapshot searchSnapshot;
        private final List<NodeRef> newInvoiceDocuments;

        private Snapshot(DocumentDialog dialog) {
            node = dialog.node;
            isDraft = dialog.isDraft;
            skipInit = dialog.skipInit;
            showDocsAndCasesAssocs = dialog.showDocsAndCasesAssocs;
            metadataSnapshot = dialog.metadataBlockBean.createSnapshot();
            searchSnapshot = dialog.searchBlockBean.createSnapshot();
            newInvoiceDocuments = dialog.newInvoiceDocuments;
        }

        private void restoreState(DocumentDialog dialog) {
            dialog.node = node;
            dialog.isDraft = isDraft;
            dialog.skipInit = skipInit;
            dialog.showDocsAndCasesAssocs = showDocsAndCasesAssocs;
            dialog.metadataBlockBean.restoreSnapshot(metadataSnapshot);
            dialog.searchBlockBean.restoreSnapshot(searchSnapshot);
            dialog.newInvoiceDocuments = newInvoiceDocuments;
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

    public List<NodeRef> getNewInvoiceDocuments() {
        return newInvoiceDocuments;
    }

    public void setTransactionsBlockBean(TransactionsBlockBean transactionsBlockBean) {
        this.transactionsBlockBean = transactionsBlockBean;
    }

    public TransactionsBlockBean getTransactionsBlockBean() {
        return transactionsBlockBean;
    }

    public UIPanel getModalContainer() {
        if (modalContainer == null) {
            modalContainer = new UIPanel();
        }
        return modalContainer;
    }

    public void setModalContainer(UIPanel modalContainer) {
        this.modalContainer = modalContainer;
    }

    // END: getters / setters
}