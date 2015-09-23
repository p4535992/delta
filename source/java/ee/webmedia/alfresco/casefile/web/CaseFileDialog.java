package ee.webmedia.alfresco.casefile.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getArchivalsService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getCaseFileLogService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getCaseFileService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDialogHelperBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDynamicService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentLockHelperBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNotificationService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPropertySheetStateBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSendOutService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.docdynamic.web.ChangeReasonModalComponent.DELETE_DOCUMENT_REASON_MODAL_ID;
import static ee.webmedia.alfresco.docdynamic.web.DocumentDynamicDialog.validateExists;
import static ee.webmedia.alfresco.docdynamic.web.DocumentDynamicDialog.validatePermissionWithErrorMessage;
import static ee.webmedia.alfresco.privilege.service.PrivilegeUtil.isAdminOrDocmanagerWithPermission;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIPanel;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.config.PropertySheetConfigElement;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.casefile.service.CaseFile;
import ee.webmedia.alfresco.casefile.service.DocumentToCompoundWorkflow;
import ee.webmedia.alfresco.casefile.web.CaseFileDialog.CaseFileDialogSnapshot;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docconfig.generator.PropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.service.DocumentConfig;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.docdynamic.web.BaseSnapshotCapableDialog;
import ee.webmedia.alfresco.docdynamic.web.BaseSnapshotCapableWithBlocksDialog;
import ee.webmedia.alfresco.docdynamic.web.ChangeReasonModalComponent;
import ee.webmedia.alfresco.docdynamic.web.ChangeReasonModalComponent.ChangeReasonEvent;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicBlock;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicDialog;
import ee.webmedia.alfresco.docdynamic.web.DocumentLockHelperBean;
import ee.webmedia.alfresco.document.associations.model.DocAssocInfo;
import ee.webmedia.alfresco.document.associations.web.AssocsBlockBean;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.web.AbstractSearchBlockBean;
import ee.webmedia.alfresco.document.search.web.BlockBeanProviderProvider;
import ee.webmedia.alfresco.document.search.web.DocumentListDataProvider;
import ee.webmedia.alfresco.document.search.web.SearchBlockBean;
import ee.webmedia.alfresco.document.sendout.model.SendInfo;
import ee.webmedia.alfresco.document.web.DocumentListDialog;
import ee.webmedia.alfresco.document.web.FavoritesModalComponent;
import ee.webmedia.alfresco.document.web.FavoritesModalComponent.AddToFavoritesEvent;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.privilege.service.PrivilegeUtil;
import ee.webmedia.alfresco.user.model.UserModel;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformMultiReasonException;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.web.WorkflowBlockBean;

public class CaseFileDialog extends BaseSnapshotCapableWithBlocksDialog<CaseFileDialogSnapshot, DocumentDynamicBlock, DialogDataProvider> implements DialogDataProvider,
BlockBeanProviderProvider {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "CaseFileDialog";
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(CaseFileDialog.class);

    private DocumentListDataProvider documents;
    private List<DocumentToCompoundWorkflow> documentWorkflows;
    private String renderedModal;

    /** @param event */
    public void createDraft(ActionEvent event) {
        String typeId = ActionUtil.getParam(event, "typeId");
        documentWorkflows = null;
        createCaseFile(typeId, null, false, false, null, false);
    }

    public void createCaseFile(String typeId, DocumentDynamic documentToAdd, boolean registerDoc, boolean sendDocNotifications, List<String> docSaveListeners,
            boolean isRelocatingAssociations) {
        CaseFile caseFile = createCaseFile(typeId);
        if (documentToAdd != null) {
            caseFile.setFunction(documentToAdd.getFunction());
            caseFile.setSeries(documentToAdd.getSeries());
        }
        open(caseFile.getNodeRef(), caseFile, true);
        CaseFileDialogSnapshot currentSnapshot = getCurrentSnapshot();
        if (currentSnapshot != null) {
            currentSnapshot.documentToAdd = documentToAdd;
            currentSnapshot.registerDoc = registerDoc;
            currentSnapshot.sendDocNotifications = sendDocNotifications;
            currentSnapshot.docSaveListeners = docSaveListeners;
            currentSnapshot.isRelocatingAssociationsNeeded = isRelocatingAssociations;
        }
    }

    public void createCaseFile(String typeId, Node locationNode, List<NodeRef> massChangeLocationSelectedDocs) {
        CaseFile caseFile = createCaseFile(typeId);
        Map<String, Object> locationProps = locationNode.getProperties();
        caseFile.setFunction((NodeRef) locationProps.get(DocumentCommonModel.Props.FUNCTION.toString()));
        caseFile.setSeries((NodeRef) locationProps.get(DocumentCommonModel.Props.SERIES.toString()));
        open(caseFile.getNodeRef(), caseFile, true);
        CaseFileDialogSnapshot currentSnapshot = getCurrentSnapshot();
        currentSnapshot.massChangeLocationSelectedDocs = massChangeLocationSelectedDocs;
        currentSnapshot.massChangeLocationNode = locationNode;
    }

    private CaseFile createCaseFile(String typeId) {
        CaseFile caseFile = BeanHelper.getCaseFileService().createNewCaseFileInDrafts(typeId).getFirst();
        caseFile.setDraft(true);
        return caseFile;
    }

    /** @param event */
    public void openFromChildList(ActionEvent event) {
        if (!ActionUtil.hasParam(event, "nodeRef")) {
            return;
        }
        NodeRef childNodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        NodeRef docRef = BeanHelper.getGeneralService().getAncestorNodeRefWithType(childNodeRef, CaseFileModel.Types.CASE_FILE);
        if (docRef != null) {
            openFromDocumentList(docRef);
        }
    }

    /** @param event */
    public void openFromDocumentList(ActionEvent event) {
        openFromDocumentList(new NodeRef(ActionUtil.getParam(event, "nodeRef")));
    }

    /** @param docRef */
    private void openFromDocumentList(NodeRef docRef) {
        if (DocumentDynamicDialog.validateExists(docRef)) {
            open(docRef, false);
        }
    }

    /** @param event */
    public void openFromEventPlanVolumeList(ActionEvent event) {
        NodeRef docRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        if (DocumentDynamicDialog.validateExists(docRef)) {
            open(docRef, true);
        }
    }

    public void open(NodeRef caseFileRef, boolean inEditMode) {
        if (!validateOpen(getCaseFileService().getCaseFile(caseFileRef), inEditMode)) {
            return;
        }
        createSnapshot(new CaseFileDialogSnapshot());
        openOrSwitchModeCommon(caseFileRef, inEditMode);
    }

    private void open(NodeRef caseFileRef, CaseFile caseFile, boolean inEditMode) {
        if (!validateOpen(caseFile, inEditMode)) {
            return;
        }
        createSnapshot(new CaseFileDialogSnapshot());
        openOrSwitchModeCommon(caseFileRef, caseFile, inEditMode, null);
    }

    public void deleteCaseFile(ActionEvent event) {
        Node node = getDocumentDialogHelperBean().getNode();
        Assert.notNull(node, "No current case file");
        try {
            // Before asking for reason let's check if it is locked
            BeanHelper.getDocLockService().checkForLock(node.getNodeRef());
            if (!(event instanceof ChangeReasonEvent) || StringUtils.isBlank(((ChangeReasonEvent) event).getReason())) {
                renderedModal = DELETE_DOCUMENT_REASON_MODAL_ID;
                return;
            }
            getCaseFileService().deleteCaseFile(node.getNodeRef(), ((ChangeReasonEvent) event).getReason());
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(e);
            return;
        } catch (NodeLockedException e) {
            NodeRef lockedRef = e.getNodeRef();
            if (CaseFileModel.Types.CASE_FILE.equals(getNodeService().getType(lockedRef))) {
                BeanHelper.getDocumentLockHelperBean().handleLockedNode("caseFile_delete_error_locked");
            } else {
                BeanHelper.getDocumentLockHelperBean().handleLockedNode("caseFile_delete_error_doc_locked", lockedRef,
                        getNodeService().getProperty(lockedRef, DocumentCommonModel.Props.DOC_NAME));
            }
            resetModals();
            return;
        } catch (InvalidNodeRefException e) {
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addErrorMessage(context, "caseFile_delete_error_deleted");
            WebUtil.navigateTo(getDefaultCancelOutcome(), context);
            return;
        }
        // go back
        WebUtil.navigateWithCancel();
        MessageUtil.addInfoMessage("caseFile_delete_success");
    }

    public void showCaseFileLink(@SuppressWarnings("unused") ActionEvent event) {
        renderedModal = CaseFileLinkGeneratorModalComponent.CASE_FILE_LINK_MODAL_ID;
    }

    public void archiveCaseFile(@SuppressWarnings("unused") ActionEvent event) {
        archiveCaseFile(getDocumentDialogHelperBean().getNode().getNodeRef());
        MessageUtil.addInfoMessage("caseFile_archive_success");
    }

    public void archiveCaseFile(NodeRef caseFileRef) {
        getArchivalsService().addVolumeOrCaseToArchivingList(caseFileRef);
    }

    // =========================================================================
    // Navigation with buttons - edit/save/back
    // =========================================================================

    /** @param event */
    public void switchToEditMode(ActionEvent event) {
        if (isInEditMode()) {
            throw new RuntimeException("CaseFile metadata block is already in edit mode");
        }

        // Permission check
        CaseFile caseFile = getCaseFile();
        if (!validateExists(caseFile.getNodeRef()) || !validateOpen(caseFile, true)) {
            return;
        }

        // Switch from view mode to edit mode
        switchMode(true);
    }

    @Override
    public String cancel() {
        if (getCurrentSnapshot() == null) {
            Throwable e = new Throwable("!!!!!!!!!!!!!!!!!!!!!!!!! Cancel is called too many times !!!!!!!!!!!!!!!!!!!!!!!!!");
            LOG.warn(e.getMessage(), e);
            return cancel(false);
        }

        if (!isInEditMode() || !getCurrentSnapshot().viewModeWasOpenedInThePast || !canRestore()) {
            getDocumentDynamicService().deleteDocumentIfDraft(getCaseFile().getNodeRef());
            return super.cancel(); // closeDialogSnapshot
        }
        // Switch from edit mode back to view mode
        switchMode(false);
        return null;
    }

    public void searchDocsAndCases(@SuppressWarnings("unused") ActionEvent event) {
        getCurrentSnapshot().showDocsAndCasesAssocs = true;
        SearchBlockBean searchBlockBean = (SearchBlockBean) getBlocks().get(SearchBlockBean.class);
        searchBlockBean.init(getDataProvider());
        searchBlockBean.setExpanded(true);
    }

    @Override
    protected Map<Class<? extends DocumentDynamicBlock>, DocumentDynamicBlock> getBlocks() {
        Map<Class<? extends DocumentDynamicBlock>, DocumentDynamicBlock> blocks = super.getBlocks();
        if (blocks.isEmpty()) {
            blocks.put(WorkflowBlockBean.class, BeanHelper.getWorkflowBlockBean());
            blocks.put(AssocsBlockBean.class, BeanHelper.getAssocsBlockBean());
            blocks.put(SearchBlockBean.class, BeanHelper.getSearchBlockBean());
        }
        return blocks;
    }

    @Override
    public String getContainerTitle() {
        DocumentConfig config = getConfig();
        if (config == null) {
            return null;
        }
        return config.getDocType().getName();
    }

    @Override
    public String getMoreActionsConfigId() {
        return "";
    }

    @Override
    public boolean isFinishButtonVisible(boolean dialogConfOKButtonVisible) {
        return isInEditMode();
    }

    public boolean isShowSearchBlock() {
        SearchBlockBean searchBlockBean = (SearchBlockBean) getBlocks().get(SearchBlockBean.class);
        return searchBlockBean != null && searchBlockBean.isExpanded() && getCurrentSnapshot() != null && !getCurrentSnapshot().inEditMode;
    }

    public boolean isAssocsBlockExpanded() {
        return true;
    }

    public boolean isShowDocsAndCasesAssocs() {
        return getCurrentSnapshot() != null && getCurrentSnapshot().showDocsAndCasesAssocs;
    }

    public boolean isShowAddAssocsLink() {
        return BeanHelper.getDocumentDialogHelperBean().isInWorkspace() && !isInEditMode() && (userHasDeleteRights(getDataProvider()) || hasInProgressTask());
    }

    private boolean hasInProgressTask() {
        List<NodeRef> compoundWorkflows = BeanHelper.getWorkflowBlockBean().getCompoundWorkflows();
        return !compoundWorkflows.isEmpty() && BeanHelper.getWorkflowDbService().hasInProgressTasks(compoundWorkflows, AuthenticationUtil.getRunAsUser());
    }

    @Override
    public void clean() {
        clearState();
        documents = null;
        documentWorkflows = null;
        renderedModal = null;
        for (DocumentDynamicBlock block : getBlocks().values()) {
            block.clean();
        }
    }

    // =========================================================================
    // Action handlers
    // =========================================================================

    public void addToFavorites(ActionEvent event) {
        if (!(event instanceof AddToFavoritesEvent)) {
            renderedModal = FavoritesModalComponent.ADD_TO_FAVORITES_MODAL_ID;
            return;
        }
        if (event instanceof AddToFavoritesEvent) {
            BeanHelper.getCaseFileFavoritesService().addFavorite(getNode().getNodeRef(), ((AddToFavoritesEvent) event).getFavoriteDirectoryName(), true);
            renderedModal = null;
        }
    }

    public void removeFavorite(@SuppressWarnings("unused") ActionEvent event) {
        BeanHelper.getCaseFileFavoritesService().removeFavorite(getDocumentDialogHelperBean().getNodeRef());
    }

    public void closeCaseFile(@SuppressWarnings("unused") ActionEvent event) {
        getCaseFileService().closeCaseFile(getCaseFile());
        switchMode(false);
        MessageUtil.addInfoMessage("casefile_closed");
    }

    public void openCaseFile(@SuppressWarnings("unused") ActionEvent event) {
        try {
            getCaseFileService().openCaseFile(getCaseFile());
            switchMode(false);
            MessageUtil.addInfoMessage("casefile_opened");
        } catch (UnableToPerformException e) {
            MessageUtil.addErrorMessage(e.getMessageKey());
        }
    }

    public void addNotification() {
        BeanHelper.getNotificationService().addNotificationAssocForCurrentUser(getNode().getNodeRef(), UserModel.Assocs.CASE_FILE_NOTIFICATION,
                UserModel.Aspects.CASE_FILE_NOTIFICATIONS);
    }

    public void removeNotification() {
        BeanHelper.getNotificationService().removeNotificationAssocForCurrentUser(getNode().getNodeRef(), UserModel.Assocs.CASE_FILE_NOTIFICATION,
                UserModel.Aspects.CASE_FILE_NOTIFICATIONS);
    }

    // =========================================================================

    static class CaseFileDialogSnapshot implements BaseSnapshotCapableDialog.Snapshot {
        private static final long serialVersionUID = 1L;

        private CaseFile caseFile;
        private boolean inEditMode;
        private boolean viewModeWasOpenedInThePast = false; // intended initial value
        private DocumentConfig config;
        private boolean showDocsAndCasesAssocs;
        private DocumentDynamic documentToAdd;
        private boolean registerDoc;
        private boolean sendDocNotifications;
        private List<String> docSaveListeners;
        private List<NodeRef> massChangeLocationSelectedDocs;
        private Node massChangeLocationNode;
        private boolean isRelocatingAssociationsNeeded;

        @Override
        public String getOpenDialogNavigationOutcome() {
            return AlfrescoNavigationHandler.DIALOG_PREFIX + "caseFileDialog";
        }

        @Override
        public String toString() {
            return toString(false);
        }

        public String toString(boolean detailed) {
            return "CaseFileDialogSnapshot[caseFile=" + (caseFile == null ? null : (detailed ? caseFile : caseFile.getNodeRef())) + ", inEditMode=" + inEditMode
                    + ", viewModeWasOpenedInThePast=" + viewModeWasOpenedInThePast + ", config=" + config + "]";
        }
    }

    // =========================================================================

    private Map<String, PropertySheetStateHolder> getStateHolders() {
        DocumentConfig config = getConfig();
        if (config == null) {
            return null;
        }
        return config.getStateHolders();
    }

    public DocumentListDataProvider getDocuments() {
        return documents;
    }

    public List<DocumentToCompoundWorkflow> getDocumentWorkflows() {
        if (documentWorkflows == null) { // should only happen when user creates a new casefile but just in case try to load workflows
            CaseFile caseFile = getCaseFile();
            if (caseFile != null && caseFile.getNodeRef() != null) {
                documentWorkflows = BeanHelper.getCaseFileService().getCaseFileDocumentWorkflows(caseFile.getNodeRef());
                Collections.sort(documentWorkflows);
            } else {
                documentWorkflows = new ArrayList<>();
            }
        }
        return documentWorkflows;
    }

    @Override
    public AbstractSearchBlockBean getSearch() {
        return (AbstractSearchBlockBean) getBlocks().get(SearchBlockBean.class);
    }

    @Override
    public CaseFileLogBlockBean getLog() {
        return BeanHelper.getCaseFileLogBlockBean();
    }

    @Override
    public DocumentDynamic getDocument() {
        // Not used.
        return null;
    }

    @Override
    public WmNode getActionsContext() {
        return getNode();
    }

    private DocumentConfig getConfig() {
        CaseFileDialogSnapshot snapshot = getCurrentSnapshot();
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

    @Override
    public WmNode getNode() {
        CaseFile caseFile = getCaseFile();
        if (caseFile == null) {
            return null;
        }
        return caseFile.getNode();
    }

    @Override
    public boolean isInEditMode() {
        CaseFileDialogSnapshot snapshot = getCurrentSnapshot();
        if (snapshot == null) {
            return false;
        }
        return snapshot.inEditMode;
    }

    public boolean isInWorkspace() {
        boolean isInWorkspace = RepoUtil.isInWorkspace(getNode());
        return isInWorkspace;
    }

    public String getMode() {
        return isInEditMode() ? UIPropertySheet.EDIT_MODE : UIPropertySheet.VIEW_MODE;
    }

    public boolean isWorkflowCreatable() {
        WmNode caseFileNode = getNode();
        return getCaseFile().isStatus(DocListUnitStatus.OPEN)
                && (isAdminOrDocmanagerWithPermission(caseFileNode, Privilege.VIEW_CASE_FILE)
                        || AuthenticationUtil.getRunAsUser().equals(caseFileNode.getProperties().get(DocumentCommonModel.Props.OWNER_ID.toString()))
                        || caseFileNode.hasPermission(Privilege.EDIT_CASE_FILE));
    }

    @Override
    public void switchMode(boolean inEditMode) {
        openOrSwitchModeCommon(getCaseFile().getNodeRef(), inEditMode);
    }

    private void switchMode(boolean inEditMode, List<NodeRef> documentsToCheck) {
        NodeRef caseFileRef = getCaseFile().getNodeRef();
        openOrSwitchModeCommon(caseFileRef, getCaseFileService().getCaseFile(caseFileRef), inEditMode, documentsToCheck);
    }

    public void openOrSwitchModeCommon(NodeRef caseFileRef, boolean inEditMode) {
        openOrSwitchModeCommon(caseFileRef, getCaseFileService().getCaseFile(caseFileRef), inEditMode, null);
    }

    private void openOrSwitchModeCommon(NodeRef caseFileRef, CaseFile caseFile, boolean inEditMode, List<NodeRef> documentsToCheck) {
        CaseFileDialogSnapshot currentSnapshot = getCurrentSnapshot();
        try {
            currentSnapshot.caseFile = caseFile;
            // Lock or unlock the node also
            getDocumentDialogHelperBean().reset(getDataProvider());
            DocumentLockHelperBean documentLockHelperBean = BeanHelper.getDocumentLockHelperBean();
            documentLockHelperBean.lockOrUnlockIfNeeded(documentLockHelperBean.isLockingAllowed(inEditMode));

            currentSnapshot.inEditMode = inEditMode;
            if (!inEditMode) {
                currentSnapshot.viewModeWasOpenedInThePast = true;
                getCaseFileLogService().addCaseFileLog(currentSnapshot.caseFile.getNodeRef(), "casefile_log_opened_not_inEditMode");
            }
            currentSnapshot.config = BeanHelper.getDocumentConfigService().getConfig(getNode());
            if (currentSnapshot.caseFile.isDraft()) {
                getCurrentSnapshot().showDocsAndCasesAssocs = false;
            }
            resetOrInit(getDataProvider());
            addLastSavedDocumentAndItsAssocs(documentsToCheck);
            if (LOG.isDebugEnabled()) {
                LOG.debug("CaseFile before rendering: " + getCaseFile());
            }
        } catch (NodeLockedException e) {
            BeanHelper.getDocumentLockHelperBean().handleLockedNode("caseFile_validation_alreadyLocked");
        } catch (UnableToPerformException e) {
            throw e;
        } catch (UnableToPerformMultiReasonException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to switch mode to " + (inEditMode ? "edit" : "view") + "\n  caseFileRef=" + caseFileRef + "\n  snapshot="
                    + (currentSnapshot == null ? null : currentSnapshot.toString(true)), e);
        }
    }

    // if document is saved in CaseFileDialog.finishImpl, it may not be indexed yet,
    // in that case retrieve the last saved doc (and its associations) by nodeRef from repo
    private void addLastSavedDocumentAndItsAssocs(List<NodeRef> documentsToCheck) {
        if (documentsToCheck == null) {
            return;
        }
        for (NodeRef documentToCheck : documentsToCheck) {
            boolean documentFound = false;
            for (NodeRef docRef : documents.getObjectKeys()) {
                if (docRef.equals(documentToCheck)) {
                    documentFound = true;
                    break;
                }
            }
            if (!documentFound) {
                documents.getObjectKeys().add(documentToCheck);
            }
        }
    }

    @Override
    protected DialogDataProvider getDataProvider() {
        return getCurrentSnapshot() == null ? null : this;
    }

    @Override
    /** Transaction created by BaseDialogBean.finish method is not needed by us here, and it would be clearer to turn it off -
     * but we haven't turned it off and thus it is still created and therefore it is the super transaction.
     * Actions in finishImpl method are performed in two separate child transactions for the following reason:
     * integrity checker runs at the end of the transaction and we want integrity checker to run before switchMode is run.
     */
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (!isInEditMode()) {
            throw new RuntimeException("CaseFile metadata block is not in edit mode");
        }

        final CaseFileDialogSnapshot currentSnapshot = getCurrentSnapshot();
        final DocumentDynamic documentToAdd = currentSnapshot.documentToAdd;
        final boolean isAddDocument = documentToAdd != null;
        NodeRef savedDocumentRef = null;
        final List<NodeRef> savedDocAndItsAssocsRefs = new ArrayList<NodeRef>();

        try {
            // Do in new transaction, because we want to catch integrity checker exceptions now, not at the end of this method when mode is already switched
            savedDocumentRef = getTransactionService().getRetryingTransactionHelper().doInTransaction(
                    new RetryingTransactionCallback<NodeRef>() {
                        @Override
                        public NodeRef execute() throws Throwable {
                            // May throw UnableToPerformException or UnableToPerformMultiReasonException
                            CaseFile result = getCaseFileService().update(getCaseFile(), getConfig().getSaveListenerBeanNames());
                            currentSnapshot.caseFile = result;
                            NodeRef savedDocumentRef = null;
                            if (isAddDocument) {
                                documentToAdd.setVolume(result.getNodeRef());
                                documentToAdd.setSeries(result.getSeries());
                                documentToAdd.setFunction(result.getFunction());
                                final boolean isDraft = documentToAdd.isDraft();
                                DocumentDynamicDialog documentDynamicDialog = BeanHelper.getDocumentDynamicDialog();
                                Pair<DocumentDynamic, List<Pair<NodeRef, NodeRef>>> docAndAssocs = documentDynamicDialog.save(documentToAdd, currentSnapshot.docSaveListeners,
                                        currentSnapshot.isRelocatingAssociationsNeeded, false);
                                savedDocumentRef = docAndAssocs.getFirst().getNodeRef();
                                for (Pair<NodeRef, NodeRef> p : docAndAssocs.getSecond()) {
                                    if (p.getSecond() != null) {
                                        savedDocAndItsAssocsRefs.add(p.getSecond());
                                    }
                                }
                                if (currentSnapshot.registerDoc) {
                                    WmNode node = documentToAdd.getNode();
                                    documentDynamicDialog.register(isDraft, documentToAdd, node, node);
                                }
                                if (currentSnapshot.sendDocNotifications) { // Confirmation about missing e-mails was asked after DocumentDynamicDialog finish button
                                    Pair<List<String>, List<SendInfo>> existingAndMissingEmails = getNotificationService().getExistingAndMissingEmails(
                                            getSendOutService().getDocumentSendInfos(savedDocumentRef));
                                    documentDynamicDialog.notifyAccessRestrictionChanged(documentToAdd, existingAndMissingEmails);
                                }
                                resetDocumentToAdd(currentSnapshot);
                                documentDynamicDialog.switchMode(true);
                            }
                            List<NodeRef> massChangeLocationSelectedDocs = currentSnapshot.massChangeLocationSelectedDocs;
                            if (massChangeLocationSelectedDocs != null) {
                                Map<String, Object> locationProps = currentSnapshot.massChangeLocationNode.getProperties();
                                locationProps.put(DocumentCommonModel.Props.FUNCTION.toString(), result.getFunction());
                                locationProps.put(DocumentCommonModel.Props.SERIES.toString(), result.getSeries());
                                locationProps.put(DocumentCommonModel.Props.VOLUME.toString(), result.getNodeRef());
                                DocumentListDialog documentListDialog = BeanHelper.getDocumentListDialog();
                                documentListDialog.setLocationNode(currentSnapshot.massChangeLocationNode);
                                documentListDialog.setSelectedDocs(currentSnapshot.massChangeLocationSelectedDocs);
                                BeanHelper.getDocumentListDialog().massChangeDocLocationSave(true);
                                resetMassChangeSelectedDocs(currentSnapshot);
                            }
                            return savedDocumentRef;
                        }
                    }, false, true);
        } catch (UnableToPerformMultiReasonException e) {
            // This is handled in BaseDialogBean
            throw e;
        }

        if (savedDocumentRef != null) {
            savedDocAndItsAssocsRefs.add(savedDocumentRef);
        }
        // Do in new transaction, because otherwise saved data from the previous transaction from above is not visible
        return getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<String>() {
            @Override
            public String execute() throws Throwable {
                if (isAddDocument) {
                    BeanHelper.getDocumentDynamicDialog().switchMode(true);
                }
                // Switch from edit mode back to view mode
                switchMode(false, savedDocAndItsAssocsRefs);
                return null;
            }
        }, false, true);
    }

    public void resetDocumentToAdd(CaseFileDialogSnapshot currentSnapshot) {
        currentSnapshot.documentToAdd = null;
        currentSnapshot.docSaveListeners = null;
        currentSnapshot.registerDoc = false;
        currentSnapshot.sendDocNotifications = false;
    }

    public void resetMassChangeSelectedDocs(CaseFileDialogSnapshot currentSnapshot) {
        currentSnapshot.massChangeLocationNode = null;
        currentSnapshot.massChangeLocationSelectedDocs = null;
    }

    @Override
    public CaseFile getCaseFile() {
        CaseFileDialogSnapshot snapshot = getCurrentSnapshot();
        if (snapshot == null) {
            return null;
        }
        return snapshot.caseFile;
    }

    public String getRenderedModal() {
        return renderedModal;
    }

    public boolean isModalRendered() {
        return StringUtils.isNotBlank(renderedModal);
    }

    // =========================================================================
    // Components
    // =========================================================================

    private transient UIPropertySheet propertySheet;
    private transient UIRichList documentRichList;

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

    public UIRichList getDocumentRichList() {
        return documentRichList;
    }

    public void setDocumentRichList(UIRichList documentRichList) {
        this.documentRichList = documentRichList;
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

    @Override
    protected void resetOrInit(DialogDataProvider provider) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("resetOrInit propertySheet=" + ObjectUtils.toString(propertySheet));
        }
        if (provider == null) {
            return;
        }
        if (!provider.isInEditMode()) {
            NodeRef caseFileRef = getCaseFile().getNodeRef();
            documents = new DocumentListDataProvider(BeanHelper.getDocumentSearchService().searchAllDocumentRefsByParentRef(caseFileRef), true);
            documentWorkflows = BeanHelper.getCaseFileService().getCaseFileDocumentWorkflows(caseFileRef);
            Collections.sort(documentWorkflows);
        }
        if (propertySheet != null) {
            propertySheet.getChildren().clear();
            propertySheet.getClientValidations().clear();
            propertySheet.setNode(getNode());
            propertySheet.setMode(getMode());
            propertySheet.setConfig(getPropertySheetConfigElement());
        }
        if (documentRichList != null) {
            documentRichList.setValue(null);
        }
        getPropertySheetStateBean().reset(getStateHolders(), provider);
        getDocumentDialogHelperBean().reset(provider);
        getLog().init(getCaseFile());
        resetModals();
        super.resetOrInit(provider); // reset blocks
        boolean userHasDeleteRights = userHasDeleteRights(provider);
        for (DocAssocInfo assocInfo : ((AssocsBlockBean) getBlocks().get(AssocsBlockBean.class)).getDocAssocInfos()) {
            assocInfo.setAllowDelete(assocInfo.isAllowDelete() && userHasDeleteRights);
        }
    }

    private boolean userHasDeleteRights(DialogDataProvider provider) {
        Node node;
        if (provider == null || (node = provider.getNode()) == null) {
            return false;
        }
        NodeRef nodeRef = node.getNodeRef();
        final String currentUser = AuthenticationUtil.getRunAsUser();
        return currentUser.equals(node.getProperties().get(DocumentCommonModel.Props.OWNER_ID))
                || PrivilegeUtil.isAdminOrDocmanagerWithPermission(nodeRef, Privilege.VIEW_CASE_FILE)
                || CollectionUtils.exists(BeanHelper.getWorkflowService().getTasksInProgress(nodeRef), new Predicate() {

                    @Override
                    public boolean evaluate(Object object) {
                        return currentUser.equals(((Task) object).getOwnerId());
                    }
                });
    }

    private void resetModals() {
        renderedModal = null;
        // Add favorite modal component
        FavoritesModalComponent favoritesModal = new FavoritesModalComponent();
        final FacesContext context = FacesContext.getCurrentInstance();
        final Application application = context.getApplication();
        favoritesModal.setActionListener(application.createMethodBinding("#{CaseFileDialog.addToFavorites}", UIActions.ACTION_CLASS_ARGS));
        favoritesModal.setId("favorite-popup-" + context.getViewRoot().createUniqueId());

        ChangeReasonModalComponent deleteReasonModal = new ChangeReasonModalComponent(DELETE_DOCUMENT_REASON_MODAL_ID
                , "caseFile_delete_reason_modal_header", "caseFile_delete_reason");
        deleteReasonModal.setActionListener(application.createMethodBinding("#{CaseFileDialog.deleteCaseFile}", UIActions.ACTION_CLASS_ARGS));
        deleteReasonModal.setFinishButtonLabelId("delete");
        deleteReasonModal.setId("caseFile-delete-reason-popup-" + context.getViewRoot().createUniqueId());

        CaseFileLinkGeneratorModalComponent linkModal = new CaseFileLinkGeneratorModalComponent();
        linkModal.setId("caseFile-link-modal-" + context.getViewRoot().createUniqueId());

        List<UIComponent> children = ComponentUtil.getChildren(getModalContainer());
        children.clear();
        children.add(favoritesModal);
        children.add(deleteReasonModal);
        children.add(linkModal);
    }

    public boolean isVolumeMarkValidationDisabled() {
        return getCaseFile() == null || getDocumentDynamicService().isDraft(getCaseFile().getNodeRef());
    }

    private boolean validateOpen(CaseFile caseFile, boolean inEditMode) {
        boolean isOpen = caseFile.isStatus(DocListUnitStatus.OPEN);
        boolean isClosed = caseFile.isStatus(DocListUnitStatus.CLOSED);
        if (!isOpen && !isClosed) {
            MessageUtil.addErrorMessage("caseFile_open_error_wrong_status", DocListUnitStatus.OPEN.getValueName(), DocListUnitStatus.CLOSED.getValueName());
        }
        return !inEditMode
                && validatePermissionWithErrorMessage(caseFile.getNodeRef(), Privilege.VIEW_CASE_FILE)
                || inEditMode
                && getDocumentLockHelperBean().isLockable(caseFile.getNodeRef())
                && (isOpen && validatePermissionWithErrorMessage(caseFile.getNodeRef(), Privilege.EDIT_CASE_FILE) || isClosed
                        && getUserService().isAdministrator());
    }
}
