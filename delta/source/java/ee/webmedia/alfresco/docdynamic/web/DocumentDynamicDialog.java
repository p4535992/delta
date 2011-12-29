package ee.webmedia.alfresco.docdynamic.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAssociationsService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDialogHelperBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDynamicService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPropertySheetStateBean;
import static ee.webmedia.alfresco.docconfig.generator.systematic.AccessRestrictionGenerator.ACCESS_RESTRICTION_CHANGE_REASON_ERROR;
import static ee.webmedia.alfresco.docdynamic.web.ChangeReasonModalComponent.ACCESS_RESTRICTION_CHANGE_REASON_MODAL_ID;
import static ee.webmedia.alfresco.docdynamic.web.ChangeReasonModalComponent.DELETE_DOCUMENT_REASON_MODAL_ID;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIPanel;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.config.PropertySheetConfigElement;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.propertysheet.component.SubPropertySheetItem;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docconfig.generator.PropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.service.DocumentConfig;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.docdynamic.web.ChangeReasonModalComponent.ChangeReasonEvent;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicDialog.DocDialogSnapshot;
import ee.webmedia.alfresco.document.associations.model.DocAssocInfo;
import ee.webmedia.alfresco.document.associations.web.AssocsBlockBean;
import ee.webmedia.alfresco.document.file.web.FileBlockBean;
import ee.webmedia.alfresco.document.log.web.LogBlockBean;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentParentNodesVO;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.search.web.SearchBlockBean;
import ee.webmedia.alfresco.document.sendout.web.SendOutBlockBean;
import ee.webmedia.alfresco.document.web.FavoritesModalComponent;
import ee.webmedia.alfresco.document.web.FavoritesModalComponent.AddToFavoritesEvent;
import ee.webmedia.alfresco.document.web.evaluator.DeleteDocumentEvaluator;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.simdhs.servlet.ExternalAccessServlet;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageData;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageDataWrapper;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformMultiReasonException;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.workflow.web.WorkflowBlockBean;

/**
 * To open this dialog you must call exactly one of the methods in actionListener section, either manually or from {@code <a:actionLink actionListener="..."}
 * <p>
 * For example, to open this dialog from JSP, you should use
 * <code>&lt;a:actionLink actionListener="#{DocumentDynamicDialog.open...}" &gt;&lt;f:param name="nodeRef" value="..." /&gt;&lt;/a:actionLink&gt;</code>
 * </p>
 * <p>
 * For example, to open this dialog manually, (for example {@link ExternalAccessServlet} does this), you should call {@code documentDynamicDialog.open...(nodeRef)}
 * </p>
 * 
 * @author Alar Kvell
 */
public class DocumentDynamicDialog extends BaseSnapshotCapableWithBlocksDialog<DocDialogSnapshot, DocumentDynamicBlock, DialogDataProvider> implements DialogDataProvider {
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DocumentDynamicDialog.class);

    public static final String BEAN_NAME = "DocumentDynamicDialog";
    private static final String PARAM_NODEREF = "nodeRef";
    private String renderedModal;
    private boolean showConfirmationPopup;

    // TODO kontrollida et kustutatud dokumendi ekraanile tagasipöördumine töötaks... või tahavad teised blokid laadida uuesti asju? ja siis oleks mõtekam dialoogi mitte kuvada?

    // Closing this dialog has the following logic:
    // ... view -> *back -> close
    // ... edit -> *back -> kui on tuldud view'ist, siis sinna tagasi, muidu close; ja kui draft, siis lisaks delete
    // URL -> openView -> *back -> close -> siis kuvatakse avaleht, sest URList avamine teeb viewstack'i tühjaks ja paneb avalehe esimeseks

    // =========================================================================
    // Dialog entry points start
    // 1 - ACTIONLISTENER methods
    // =========================================================================

    public void openFromUrl(NodeRef docRef) {
        open(docRef, false);
    }

    /** @param event */
    public void openFromDocumentList(ActionEvent event) {
        NodeRef docRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        if (validateExists(docRef)) {
            // TODO if isIncomingInvoice() then inEditMode = true;
            boolean inEditMode = getDocumentDynamicService().isImapOrDvk(docRef);
            open(docRef, inEditMode);
        }
    }

    /** @param event */
    public void openView(ActionEvent event) {
        NodeRef docRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        open(docRef, false);
    }

    /** @param event */
    public void openEdit(ActionEvent event) {
        NodeRef docRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        open(docRef, true);
    }

    /** @param event */
    public void createDraft(ActionEvent event) {
        String documentTypeId = ActionUtil.getParam(event, "documentTypeId");
        DocumentDynamic doc = getDocumentDynamicService().createNewDocumentInDrafts(documentTypeId).getFirst();
        open(doc.getNodeRef(), doc, true);
    }

    public void changeByNewDocument(@SuppressWarnings("unused") ActionEvent event) {
        DocumentDynamic baseDoc = getCurrentSnapshot().document;

        Date regDateTime = baseDoc.getProp(DocumentCommonModel.Props.REG_DATE_TIME);
        String docName = MessageUtil.getMessage("docdyn_changeByNewDocument_name"//
                , BeanHelper.getDocumentAdminService().getDocumentTypeName(baseDoc.getDocumentTypeId())
                , StringUtils.defaultIfEmpty((String) baseDoc.getProp(DocumentCommonModel.Props.REG_NUMBER), "")
                , regDateTime == null ? "" : regDateTime);

        Map<QName, Serializable> overrides = new HashMap<QName, Serializable>(1);
        overrides.put(DocumentCommonModel.Props.DOC_NAME, docName);

        NodeRef docRef = getDocumentDynamicService().copyDocumentToDrafts(baseDoc, overrides,
                DocumentCommonModel.Props.REG_NUMBER,
                DocumentCommonModel.Props.REG_DATE_TIME,
                DocumentCommonModel.Props.SHORT_REG_NUMBER,
                DocumentCommonModel.Props.INDIVIDUAL_NUMBER,
                DocumentCommonModel.Props.DOC_STATUS);

        open(docRef, true);

        addTargetAssoc(baseDoc.getNodeRef(), DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP, false);
    }

    public void copyDocument(@SuppressWarnings("unused") ActionEvent event) {
        DocumentDynamic baseDoc = getCurrentSnapshot().document;

        NodeRef docRef = getDocumentDynamicService().copyDocumentToDrafts(baseDoc, null,
                DocumentSpecificModel.Props.ENTRY_DATE,
                DocumentSpecificModel.Props.ENTRY_SAP_NUMBER,
                DocumentCommonModel.Props.REG_NUMBER,
                DocumentCommonModel.Props.REG_DATE_TIME,
                DocumentCommonModel.Props.SHORT_REG_NUMBER,
                DocumentCommonModel.Props.INDIVIDUAL_NUMBER,
                DocumentCommonModel.Props.DOC_STATUS);

        getDocumentDynamicService().setOwner(docRef, AuthenticationUtil.getRunAsUser(), false);

        open(docRef, true);
    }

    public void createAssoc(ActionEvent event) {
        DocDialogSnapshot snapshot = getCurrentSnapshot();
        if (snapshot == null) {
            throw new RuntimeException("No current document");
        }
        NodeRef baseDocRef = snapshot.document.getNodeRef();
        String lockOwner = BeanHelper.getDocLockService().getLockOwnerIfLocked(baseDocRef);
        if (lockOwner != null) {
            String lockOwnerName = BeanHelper.getUserService().getUserFullName(lockOwner);
            throw new UnableToPerformException("docdyn_createAssoc_error_docLocked", lockOwnerName);
        }
        NodeRef assocModelRef = ActionUtil.getParam(event, AssocsBlockBean.PARAM_ASSOC_MODEL_REF, NodeRef.class);
        DocumentDynamic newDocument = BeanHelper.getDocumentAssociationsService().createAssociatedDocFromModel(baseDocRef, assocModelRef);
        open(newDocument.getNodeRef(), newDocument, true);
    }

    public void searchDocsAndCases(@SuppressWarnings("unused") ActionEvent event) {
        getCurrentSnapshot().showDocsAndCasesAssocs = true;
        SearchBlockBean searchBlockBean = getSearchBlock();
        searchBlockBean.init(getDocument());
        searchBlockBean.setExpanded(true);
    }

    public void addFollowUpHandler(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, PARAM_NODEREF));
        addTargetAssocAndReopen(nodeRef, DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP);
    }

    public void addReplyHandler(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, PARAM_NODEREF));
        addTargetAssocAndReopen(nodeRef, DocumentCommonModel.Assocs.DOCUMENT_REPLY);
    }

    private void addTargetAssocAndReopen(NodeRef targetRef, QName targetType) {
        addTargetAssoc(targetRef, targetType, true);
        updateFollowUpOrReplyProperties(targetRef);
        openOrSwitchModeCommon(getDocument(), true);
        getSearchBlock().setShow(false);
        MessageUtil.addInfoMessage("document_assocAdd_success");
    }

    private void addTargetAssoc(NodeRef targetRef, QName targetType, boolean isSourceAssoc) {
        AssocsBlockBean assocsBlockBean = (AssocsBlockBean) getBlocks().get(AssocsBlockBean.class);
        AssociationRef assocRef = RepoUtil.addAssoc(getNode(), targetRef, targetType, true);
        final DocAssocInfo docAssocInfo = getDocumentAssociationsService().getDocAssocInfo(assocRef, isSourceAssoc);
        assocsBlockBean.getDocAssocInfos().add(docAssocInfo);
    }

    /**
     * Called when a new (not yet saved) document is set to be a reply or a follow up to some base document
     * and is filled with some properties of the base document.
     * 
     * @param nodeRef to the base document
     */
    private void updateFollowUpOrReplyProperties(NodeRef nodeRef) {
        BeanHelper.getDocumentDynamicService().setOwner(getDocument().getNodeRef(), AuthenticationUtil.getRunAsUser(), false);
        DocumentParentNodesVO parents = getDocumentService().getAncestorNodesByDocument(nodeRef);
        Map<String, Object> docProps = getNode().getProperties();
        docProps.put(DocumentCommonModel.Props.FUNCTION.toString(), parents.getFunctionNode().getNodeRef());
        NodeRef seriesRef = parents.getSeriesNode().getNodeRef();
        docProps.put(DocumentCommonModel.Props.SERIES.toString(), seriesRef);
        docProps.put(DocumentCommonModel.Props.VOLUME.toString(), parents.getVolumeNode().getNodeRef());
        updateAccessRestrictionProperties(docProps, seriesRef);
    }

    private void updateAccessRestrictionProperties(Map<String, Object> docProps, NodeRef seriesRef) {
        final String accessRestriction = (String) docProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION.toString());
        if (StringUtils.isBlank(accessRestriction)) {
            // read serAccessRestriction-related values from series
            final Series series = BeanHelper.getSeriesService().getSeriesByNodeRef(seriesRef);
            final Map<String, Object> seriesProps = series.getNode().getProperties();
            final String serAccessRestriction = (String) seriesProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION.toString());
            final String serAccessRestrictionReason = (String) seriesProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON.toString());
            final Date serAccessRestrictionBeginDate = (Date) seriesProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE.toString());
            final Date serAccessRestrictionEndDate = (Date) seriesProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE.toString());
            final String serAccessRestrictionEndDesc = (String) seriesProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC.toString());
            // write them to the document
            docProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION.toString(), serAccessRestriction);
            docProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON.toString(), serAccessRestrictionReason);
            docProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE.toString(), serAccessRestrictionBeginDate);
            docProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE.toString(), serAccessRestrictionEndDate);
            docProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC.toString(), serAccessRestrictionEndDesc);
        }
    }

    /**
     * Move all the files to the selected nodeRef, delete the current doc
     * and show the nodeRef doc.
     * 
     * @param event
     */
    public void addFilesHandler(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, PARAM_NODEREF));
        FileBlockBean fileBlockBean = (FileBlockBean) getBlocks().get(FileBlockBean.class);
        boolean success = fileBlockBean.moveAllFiles(nodeRef);
        if (success) {
            getDocumentService().deleteDocument(getNode().getNodeRef());
            open(nodeRef, false);
        }
    }

    public void hideSearchBlock(@SuppressWarnings("unused") ActionEvent event) {
        getSearchBlock().setExpanded(false);
    }

    private SearchBlockBean getSearchBlock() {
        return (SearchBlockBean) getBlocks().get(SearchBlockBean.class);
    }

    public boolean isShowDocsAndCasesAssocs() {
        return getCurrentSnapshot().showDocsAndCasesAssocs;
    }

    /**
     * Should be called only when the document was received from DVK or Outlook.
     */
    public void selectedDocumentTypeChanged(ActionEvent event) {
        String newType = (String) ((UIInput) event.getComponent().findComponent("doc-types-select")).getValue();
        if (StringUtils.isBlank(newType)) {
            return;
        }
        DocumentDynamic document = getDocument();
        if (document.getDocumentTypeId().equals(newType)) {
            return;
        }
        BeanHelper.getDocumentDynamicService().changeTypeInMemory(document, newType);
        openOrSwitchModeCommon(document, true);
    }

    /** Used in jsp */
    public String getOnChangeStyleClass() {
        return ComponentUtil.getOnChangeStyleClass();
    }

    // =========================================================================

    static class DocDialogSnapshot implements BaseSnapshotCapableDialog.Snapshot {
        private static final long serialVersionUID = 1L;

        private DocumentDynamic document;
        private boolean inEditMode;
        private boolean viewModeWasOpenedInThePast = false; // intended initial value
        private boolean showDocsAndCasesAssocs;
        private DocumentConfig config;

        @Override
        public String getOpenDialogNavigationOutcome() {
            return AlfrescoNavigationHandler.DIALOG_PREFIX + "documentDynamicDialog";
        }

        @Override
        public String toString() {
            return toString(false);
        }

        public String toString(boolean detailed) {
            return "DocDialogSnapshot[document=" + (document == null ? null : (detailed ? document : document.getNodeRef())) + ", inEditMode=" + inEditMode
                    + ", viewModeWasOpenedInThePast=" + viewModeWasOpenedInThePast + ", config=" + config + ", showDocsAndCasesAssocs=" + showDocsAndCasesAssocs + "]";
        }
    }

    // =========================================================================

    // All dialog entry point methods must call this method
    private void open(NodeRef docRef, boolean inEditMode) {
        open(docRef, null, inEditMode);
    }

    private void open(NodeRef docRef, DocumentDynamic document, boolean inEditMode) {
        if (!validateOpen(docRef, inEditMode)) {
            return;
        }
        createSnapshot(new DocDialogSnapshot());
        if (document != null) {
            openOrSwitchModeCommon(document, inEditMode);
        } else {
            openOrSwitchModeCommon(docRef, inEditMode);
        }
    }

    @Override
    public void switchMode(boolean inEditMode) {
        openOrSwitchModeCommon(getDocument().getNodeRef(), inEditMode);
    }

    private void openOrSwitchModeCommon(NodeRef docRef, boolean inEditMode) {
        DocumentDynamic document = inEditMode
                ? getDocumentDynamicService().getDocumentWithInMemoryChangesForEditing(docRef)
                : getDocumentDynamicService().getDocument(docRef);
        openOrSwitchModeCommon(document, inEditMode);
    }

    private void openOrSwitchModeCommon(DocumentDynamic document, boolean inEditMode) {
        NodeRef docRef = document.getNodeRef();
        DocDialogSnapshot currentSnapshot = getCurrentSnapshot();
        try {
            currentSnapshot.document = document;

            // Lock or unlock the node also
            getDocumentDialogHelperBean().reset(getDataProvider());
            DocumentLockHelperBean documentLockHelperBean = BeanHelper.getDocumentLockHelperBean();
            documentLockHelperBean.lockOrUnlockIfNeeded(documentLockHelperBean.isLockingAllowed(inEditMode));

            currentSnapshot.inEditMode = inEditMode;
            if (!inEditMode) {
                if (StringUtils.isBlank(document.getRegNumber()) && document.getRegDateTime() == null && BeanHelper.getDocumentDynamicService().isShowMessageIfUnregistered()) {
                    MessageUtil.addInfoMessage("document_info_not_registered");
                }
                currentSnapshot.viewModeWasOpenedInThePast = true;
                BeanHelper.getDocumentLogService().addDocumentLog(docRef, MessageUtil.getMessage("document_log_status_opened_not_inEditMode"));
            }
            currentSnapshot.config = BeanHelper.getDocumentConfigService().getConfig(getNode());
            if (document.isDraftOrImapOrDvk()) {
                getCurrentSnapshot().showDocsAndCasesAssocs = false;
            }
            resetOrInit(getDataProvider());
            if (LOG.isDebugEnabled()) {
                LOG.debug("Document before rendering: " + getDocument());
            }
        } catch (NodeLockedException e) {
            BeanHelper.getDocumentLockHelperBean().handleLockedNode("document_validation_alreadyLocked");
        } catch (UnableToPerformException e) {
            throw e;
        } catch (UnableToPerformMultiReasonException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to switch mode to " + (inEditMode ? "edit" : "view") + "\n  docRef=" + docRef + "\n  snapshot="
                    + (currentSnapshot == null ? null : currentSnapshot.toString(true)), e);
        }
    }

    // =========================================================================
    // Blocks
    // =========================================================================

    @Override
    protected Map<Class<? extends DocumentDynamicBlock>, DocumentDynamicBlock> getBlocks() {
        Map<Class<? extends DocumentDynamicBlock>, DocumentDynamicBlock> blocks = super.getBlocks();
        if (blocks.isEmpty()) {
            blocks = new HashMap<Class<? extends DocumentDynamicBlock>, DocumentDynamicBlock>();
            blocks.put(FileBlockBean.class, BeanHelper.getFileBlockBean());
            blocks.put(LogBlockBean.class, BeanHelper.getLogBlockBean());
            blocks.put(WorkflowBlockBean.class, BeanHelper.getWorkflowBlockBean());
            blocks.put(SendOutBlockBean.class, BeanHelper.getSendOutBlockBean());
            blocks.put(AssocsBlockBean.class, BeanHelper.getAssocsBlockBean());
            blocks.put(SearchBlockBean.class, BeanHelper.getSearchBlockBean());
        }
        return blocks;
    }

    // =========================================================================
    // Navigation with buttons - edit/save/back
    // =========================================================================

    /** @param event */
    public void switchToEditMode(ActionEvent event) {
        if (isInEditMode()) {
            throw new RuntimeException("Document metadata block is already in edit mode");
        }

        // Permission check
        NodeRef docRef = getDocument().getNodeRef();
        if (!validateExists(docRef) || !validateViewMetaDataPermission(docRef) || !validateEditMetaDataPermission(docRef)) {
            return;
        }

        // Switch from view mode to edit mode
        switchMode(true);
    }

    public void sendAccessRestrictionChangedEmails(ActionEvent event) {
        DocumentDynamic document = getDocument();
        BeanHelper.getNotificationService().processAccessRestrictionChangedNotification(document, BeanHelper.getSendOutService().getDocumentSendInfos(document.getNodeRef()));
        cancel();
    }

    public String cancel(ActionEvent event) {
        return cancel();
    }

    @Override
    public String cancel() {
        if (getCurrentSnapshot() == null) {
            Throwable e = new Throwable("!!!!!!!!!!!!!!!!!!!!!!!!! Cancel is called too many times !!!!!!!!!!!!!!!!!!!!!!!!!");
            LOG.warn(e.getMessage(), e);
            return cancel(false);
        }

        if (!isInEditMode() || !getCurrentSnapshot().viewModeWasOpenedInThePast) {
            getDocumentDynamicService().deleteDocumentIfDraft(getDocument().getNodeRef());
            return super.cancel(); // closeDialogSnapshot
        }

        // Switch from edit mode back to view mode
        switchMode(false);
        return null;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (!isInEditMode()) {
            throw new RuntimeException("Document metadata block is not in edit mode");
        }

        final DocumentDynamic savedDocument;
        try {
            // Do in new transaction, because we want to catch integrity checker exceptions now, not at the end of this method when mode is already switched
            savedDocument = getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<DocumentDynamic>() {
                @Override
                public DocumentDynamic execute() throws Throwable {
                    // May throw UnableToPerformException or UnableToPerformMultiReasonException
                    return getDocumentDynamicService().updateDocument(getDocument(), getConfig().getSaveListenerBeanNames());
                }
            }, false, true);

        } catch (UnableToPerformMultiReasonException e) {
            if (!handleAccessRestrictionChange(e)) {
                return null;
            }

            // This is handled in BaseDialogBean
            throw e;
        }

        // Do in new transaction, because otherwise saved data from the new transaction above is not visible
        return getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<String>() {
            @Override
            public String execute() throws Throwable {
                if (savedDocument.isAccessRestrictionPropsChanged() && BeanHelper.getSendOutService().hasDocumentSendInfos(savedDocument.getNodeRef())) {
                    isFinished = false;
                    // modal has already been displayed, if displaying was necessary
                    renderedModal = null;
                    // confirmation popup shall be displayed
                    showConfirmationPopup = true;
                    return null;
                }
                // Switch from edit mode back to view mode
                switchMode(false);
                return null;
            }
        }, false, true);
    }

    private boolean handleAccessRestrictionChange(UnableToPerformMultiReasonException e) {
        final MessageDataWrapper messageDataWrapper = e.getMessageDataWrapper();
        if (messageDataWrapper.getFeedbackItemCount() == 1 && ACCESS_RESTRICTION_CHANGE_REASON_ERROR.equals(messageDataWrapper.iterator().next().getMessageKey())) {
            isFinished = false;
            renderedModal = ChangeReasonModalComponent.ACCESS_RESTRICTION_CHANGE_REASON_MODAL_ID;
            return false;
        }

        // Remove the error if there are other errors
        for (Iterator<MessageData> iterator = messageDataWrapper.iterator(); iterator.hasNext();) {
            final MessageData data = iterator.next();
            if (ACCESS_RESTRICTION_CHANGE_REASON_ERROR.equals(data.getMessageKey())) {
                iterator.remove();
                return true;
            }
        }

        return true;
    }

    public void setAccessRestrictionChangeReason(ActionEvent event) {
        getDocument().setProp(DocumentCommonModel.Props.ACCESS_RESTRICTION_CHANGE_REASON, ((ChangeReasonEvent) event).getReason());
        finish();
    }

    public String getRenderedModal() {
        return renderedModal;
    }

    public boolean isModalRendered() {
        return StringUtils.isNotBlank(renderedModal);
    }

    @Override
    public boolean isFinishButtonVisible(boolean dialogConfOKButtonVisible) {
        return isInEditMode();
    }

    public boolean isShowSearchBlock() {
        SearchBlockBean searchBlockBean = (SearchBlockBean) getBlocks().get(SearchBlockBean.class);
        if ((searchBlockBean.isExpanded() && !getCurrentSnapshot().inEditMode)) {
            return true;
        }
        return getCurrentSnapshot().inEditMode && searchBlockBean.isShow() && !searchBlockBean.isFoundSimilar()
                && (getDocument().isImapOrDvk() && !getDocument().isNotEditable());
    }

    public boolean isShowTypeBlock() {
        return getCurrentSnapshot().inEditMode && getDocument().isImapOrDvk() && !getDocument().isNotEditable();
    }

    public boolean isShowFoundSimilar() {
        SearchBlockBean searchBlockBean = (SearchBlockBean) getBlocks().get(SearchBlockBean.class);
        return getCurrentSnapshot().inEditMode && searchBlockBean.isFoundSimilar();
    }

    // =========================================================================
    // Action handlers
    // =========================================================================

    public void addDocumentToFavorites(ActionEvent event) {
        if (!(event instanceof AddToFavoritesEvent)) {
            renderedModal = FavoritesModalComponent.ADD_TO_FAVORITES_MODAL_ID;
            return;
        }
        if (event instanceof AddToFavoritesEvent) {
            getDocumentService().addFavorite(getNode().getNodeRef(), ((AddToFavoritesEvent) event).getFavoriteDirectoryName());
            renderedModal = null;
        }
    }

    public void deleteDocument(ActionEvent event) {
        Node node = getDocumentDialogHelperBean().getNode();
        Assert.notNull(node, "No current document");
        try {
            // Before asking for reason let's check if it is locked
            BeanHelper.getDocLockService().checkForLock(node.getNodeRef());
            if (!(event instanceof ChangeReasonEvent) || StringUtils.isBlank(((ChangeReasonEvent) event).getReason())) {
                renderedModal = DELETE_DOCUMENT_REASON_MODAL_ID;
                return;
            }

            if (!new DeleteDocumentEvaluator().evaluate(node)) {
                throw new UnableToPerformException("action_failed_missingPermission", new MessageDataImpl("permission_deleteDocumentMetaData"));
            }
            getDocumentService().deleteDocument(node.getNodeRef(), ((ChangeReasonEvent) event).getReason());
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(e);
            return;
        } catch (AccessDeniedException e) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "document_delete_error_accessDenied");
            return;
        } catch (NodeLockedException e) {
            BeanHelper.getDocumentLockHelperBean().handleLockedNode("document_delete_error_docLocked");
            return;
        } catch (InvalidNodeRefException e) {
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addErrorMessage(context, "document_delete_error_docDeleted");
            WebUtil.navigateTo(getDefaultCancelOutcome(), context);
            return;
        }
        // go back
        WebUtil.navigateWithCancel();
        MessageUtil.addInfoMessage("document_delete_success");
    }

    // =========================================================================
    // Permission checks on open or switch mode
    // =========================================================================

    private static boolean validateOpen(NodeRef docRef, boolean inEditMode) {
        if (!validateExists(docRef) || !validateViewMetaDataPermission(docRef) || (inEditMode && !validateEditMetaDataPermission(docRef))) {
            return false;
        }
        return true;
    }

    private static boolean validateExists(NodeRef docRef) {
        Assert.notNull(docRef, "docRef is not given");
        if (!BeanHelper.getNodeService().exists(docRef)) {
            MessageUtil.addErrorMessage("document_restore_error_docDeleted");
            return false;
        }
        return true;
    }

    private static boolean validateViewMetaDataPermission(NodeRef docRef) {
        return validatePermissionWithErrorMessage(docRef, DocumentCommonModel.Privileges.VIEW_DOCUMENT_META_DATA);
    }

    private static boolean validateEditMetaDataPermission(NodeRef docRef) {
        return validatePermissionWithErrorMessage(docRef, DocumentCommonModel.Privileges.EDIT_DOCUMENT);
    }

    private static boolean validatePermissionWithErrorMessage(NodeRef docRef, String permission) {
        try {
            validatePermission(docRef, permission);
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(e);
            return false;
        }
        return true;
    }

    // =========================================================================
    // Getters
    // =========================================================================

    @Override
    public DocumentDynamic getDocument() {
        DocDialogSnapshot snapshot = getCurrentSnapshot();
        if (snapshot == null) {
            return null;
        }
        return snapshot.document;
    }

    public DocumentType getDocumentType() {
        DocDialogSnapshot snapshot = getCurrentSnapshot();
        if (snapshot == null) {
            return null;
        }
        return snapshot.config.getDocType();
    }

    public DocumentTypeVersion getDocumentVersion() {
        DocDialogSnapshot snapshot = getCurrentSnapshot();
        if (snapshot == null) {
            return null;
        }
        return snapshot.config.getDocVersion();
    }

    @Override
    public WmNode getNode() {
        DocumentDynamic document = getDocument();
        if (document == null) {
            return null;
        }
        return document.getNode();
    }

    @Override
    public WmNode getActionsContext() {
        return getNode();
    }

    private DocumentConfig getConfig() {
        DocDialogSnapshot snapshot = getCurrentSnapshot();
        if (snapshot == null) {
            return null;
        }
        return snapshot.config;
    }

    public PropertySheetConfigElement getPropertySheetConfigElement() {
        DocumentConfig config = getConfig();
        if (config == null) {
            return null;
        }
        return config.getPropertySheetConfigElement();
    }

    private Map<String, PropertySheetStateHolder> getStateHolders() {
        DocumentConfig config = getConfig();
        if (config == null) {
            return null;
        }
        return config.getStateHolders();
    }

    // Metadata block

    @Override
    public boolean isInEditMode() {
        DocDialogSnapshot snapshot = getCurrentSnapshot();
        if (snapshot == null) {
            return false;
        }
        return snapshot.inEditMode;
    }

    public String getMode() {
        return isInEditMode() ? UIPropertySheet.EDIT_MODE : UIPropertySheet.VIEW_MODE;
    }

    @Override
    public String getContainerTitle() {
        DocumentConfig config = getConfig();
        if (config == null) {
            return null;
        }
        return config.getDocumentTypeName();
    }

    @Override
    public String getMoreActionsConfigId() {
        return "";
    }

    // =========================================================================
    // Components
    // =========================================================================

    private transient UIPropertySheet propertySheet;

    @Override
    public UIPropertySheet getPropertySheet() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("getPropertySheet propertySheet=" + ObjectUtils.toString(propertySheet));
        }
        // Additional checks are no longer needed, because ExternalAccessServlet behavior with JSF is now correct
        return propertySheet;
    }

    public void setPropertySheet(UIPropertySheet propertySheet) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("setPropertySheet propertySheet=" + ObjectUtils.toString(propertySheet));
        }
        // Additional checks are no longer needed, because ExternalAccessServlet behavior with JSF is now correct
        this.propertySheet = propertySheet;
    }

    @Override
    protected void resetOrInit(DialogDataProvider provider) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("resetOrInit propertySheet=" + ObjectUtils.toString(propertySheet));
        }
        if (propertySheet != null) {
            propertySheet.getChildren().clear();
            propertySheet.getClientValidations().clear();
            propertySheet.setNode(getNode());
            propertySheet.setMode(getMode());
            propertySheet.setConfig(getPropertySheetConfigElement());
        }
        BeanHelper.getVisitedDocumentsBean().getVisitedDocuments().add(getNode().getNodeRef());
        getPropertySheetStateBean().reset(getStateHolders(), provider);
        getDocumentDialogHelperBean().reset(provider);
        resetModals();
        super.resetOrInit(provider); // reset blocks
    }

    @SuppressWarnings("deprecation")
    private void resetModals() {
        renderedModal = null;
        showConfirmationPopup = false;
        // Add favorite modal component
        FavoritesModalComponent favoritesModal = new FavoritesModalComponent();
        final FacesContext context = FacesContext.getCurrentInstance();
        final Application application = context.getApplication();
        favoritesModal.setActionListener(application.createMethodBinding("#{DocumentDynamicDialog.addDocumentToFavorites}", UIActions.ACTION_CLASS_ARGS));
        favoritesModal.setId("favorite-popup-" + context.getViewRoot().createUniqueId());

        // Access restriction change reason
        ChangeReasonModalComponent accessRestrictionChangeReasonModal = new ChangeReasonModalComponent(ACCESS_RESTRICTION_CHANGE_REASON_MODAL_ID
                , "docdyn_accesRestrictionChangeReason_modal_header", "docdyn_accesRestrictionChangeReason");
        accessRestrictionChangeReasonModal.setActionListener(application.createMethodBinding("#{DocumentDynamicDialog.setAccessRestrictionChangeReason}",
                UIActions.ACTION_CLASS_ARGS));
        accessRestrictionChangeReasonModal.setId("access-restriction-change-reason-popup-" + context.getViewRoot().createUniqueId());

        ChangeReasonModalComponent docDeleteReasonModal = new ChangeReasonModalComponent(DELETE_DOCUMENT_REASON_MODAL_ID
                , "docdyn_deleteDocumentReason_modal_header", "docdyn_deleteDocumentReason");
        docDeleteReasonModal.setActionListener(application.createMethodBinding("#{DocumentDynamicDialog.deleteDocument}", UIActions.ACTION_CLASS_ARGS));
        docDeleteReasonModal.setFinishButtonLabelId("delete");
        accessRestrictionChangeReasonModal.setId("document-delete-reason-popup-" + context.getViewRoot().createUniqueId());

        List<UIComponent> children = ComponentUtil.getChildren(getModalContainer());
        children.clear();
        children.add(favoritesModal);
        children.add(accessRestrictionChangeReasonModal);
        children.add(docDeleteReasonModal);
    }

    public boolean isShowConfirmationPopup() {
        return showConfirmationPopup;
    }

    public String getAccessRestrictionChangedMsg() {
        return MessageUtil.getMessage("document_access_restriction_changed_confirmation");
    }

    @Override
    protected DialogDataProvider getDataProvider() {
        return getCurrentSnapshot() == null ? null : this;
    }

    private transient UIPanel modalContainer;

    public UIPanel getModalContainer() {
        if (modalContainer == null) {
            modalContainer = new UIPanel();
        }
        return modalContainer;
    }

    public void setModalContainer(UIPanel modalContainer) {
        this.modalContainer = modalContainer;
    }

    // =========================================================================
    // Child-node logic
    // =========================================================================

    public void addChildNode(ActionEvent event) {
        final Node parentNode = getParentNode(event);
        final QName[] childAssocTypeQNameHierarchy = getChildAssocTypeQNameHierarchy(event);

        getDocumentDynamicService().createChildNodesHierarchyAndSetDefaultPropertyValues(parentNode, childAssocTypeQNameHierarchy, getDocumentVersion());
    }

    public void removeChildNode(ActionEvent event) {
        final Node parentNode = getParentNode(event);
        final QName[] childAssocTypeQNameHierarchy = getChildAssocTypeQNameHierarchy(event);
        final QName assocTypeQName = childAssocTypeQNameHierarchy[childAssocTypeQNameHierarchy.length - 1];

        final String assocIndexParam = ActionUtil.getParam(event, SubPropertySheetItem.PARAM_ASSOC_INDEX);
        final int assocIndex = Integer.parseInt(assocIndexParam);

        parentNode.removeChildAssociations(assocTypeQName, assocIndex);
    }

    private Node getParentNode(ActionEvent event) {
        final SubPropertySheetItem propSheet = ComponentUtil.getAncestorComponent(event.getComponent(), SubPropertySheetItem.class);
        return propSheet.getParentPropSheetNode();
    }

    private QName[] getChildAssocTypeQNameHierarchy(ActionEvent event) {
        final String[] childAssocTypeQNameHierarchyParam = StringUtils.split(ActionUtil.getParam(event, "childAssocTypeQNameHierarchy"), '/');
        final QName[] childAssocTypeQNameHierarchy = new QName[childAssocTypeQNameHierarchyParam.length];
        for (int i = 0; i < childAssocTypeQNameHierarchyParam.length; i++) {
            childAssocTypeQNameHierarchy[i] = QName.createQName(childAssocTypeQNameHierarchyParam[i], getNamespaceService());
        }
        return childAssocTypeQNameHierarchy;
    }

}
