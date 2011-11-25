package ee.webmedia.alfresco.docdynamic.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDialogHelperBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDynamicService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPropertySheetStateBean;
import static ee.webmedia.alfresco.docconfig.generator.systematic.AccessRestrictionGenerator.ACCESS_RESTRICTION_CHANGE_REASON_ERROR;

import java.io.Serializable;
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

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.lock.NodeLockedException;
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
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docconfig.generator.PropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.service.DocumentConfig;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.docdynamic.web.AccessRestrictionChangeReasonModalComponent.AccessRestrictionChangeReasonEvent;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicDialog.DocDialogSnapshot;
import ee.webmedia.alfresco.document.associations.web.AssocsBlockBean;
import ee.webmedia.alfresco.document.file.web.FileBlockBean;
import ee.webmedia.alfresco.document.log.web.LogBlockBean;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.web.SearchBlockBean;
import ee.webmedia.alfresco.document.sendout.web.SendOutBlockBean;
import ee.webmedia.alfresco.document.web.FavoritesModalComponent;
import ee.webmedia.alfresco.simdhs.servlet.ExternalAccessServlet;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageData;
import ee.webmedia.alfresco.utils.MessageDataWrapper;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformMultiReasonException;
import ee.webmedia.alfresco.utils.UserUtil;
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
    private String renderedModal;
    private boolean showConfirmationPopup;

    // TODO lemmiku tegevus katki? kas foorumi tegevused töötavad?
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
        DocumentDynamic doc = getDocumentDynamicService().createNewDocumentInDrafts(documentTypeId);
        open(doc.getNodeRef(), doc, true);
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

    public void hideSearchBlock(@SuppressWarnings("unused") ActionEvent event) {
        getSearchBlock().setExpanded(false);
    }

    private SearchBlockBean getSearchBlock() {
        return (SearchBlockBean) getBlocks().get(SearchBlockBean.class);
    }

    public boolean isShowDocsAndCasesAssocs() {
        return getCurrentSnapshot().showDocsAndCasesAssocs;
    }

    public boolean isIncomingInvoice() {
        return getDocument().isIncomingInvoice();
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
        BeanHelper.getDocumentDynamicService().changeTypeInMemory(getDocument(), newType);
        openOrSwitchModeCommon(document, true);
        setOwnerCurrentUser();
    }

    /** Used in jsp */
    public String getOnChangeStyleClass() {
        return ComponentUtil.getOnChangeStyleClass();
    }

    public void setOwnerCurrentUser() {
        setOwner(AuthenticationUtil.getRunAsUser());
    }

    public void setOwner(String userName) {
        Map<QName, Serializable> personProps = getPersonProps(userName);

        Map<String, Object> docProps = getDocument().getNode().getProperties();
        docProps.put(DocumentCommonModel.Props.OWNER_ID.toString(), personProps.get(ContentModel.PROP_USERNAME));
        docProps.put(DocumentCommonModel.Props.OWNER_NAME.toString(), UserUtil.getPersonFullName1(personProps));
        docProps.put(DocumentCommonModel.Props.OWNER_JOB_TITLE.toString(), personProps.get(ContentModel.PROP_JOBTITLE));
        String orgstructName = BeanHelper.getOrganizationStructureService().getOrganizationStructure((String) personProps.get(ContentModel.PROP_ORGID));
        docProps.put(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT.toString(), orgstructName);
        docProps.put(DocumentCommonModel.Props.OWNER_EMAIL.toString(), personProps.get(ContentModel.PROP_EMAIL));
        docProps.put(DocumentCommonModel.Props.OWNER_PHONE.toString(), personProps.get(ContentModel.PROP_TELEPHONE));
    }

    private Map<QName, Serializable> getPersonProps(String userName) {
        return BeanHelper.getUserService().getUserProperties(userName);
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

            // TODO Alar: refactor subnode logic

            // Lock or unlock the node also
            getDocumentDialogHelperBean().reset(getDataProvider());
            DocumentLockHelperBean documentLockHelperBean = BeanHelper.getDocumentLockHelperBean();
            documentLockHelperBean.lockOrUnlockIfNeeded(documentLockHelperBean.isLockingAllowed(inEditMode));

            List<Node> subNodeList = currentSnapshot.document.getNode().getAllChildAssociations(DocumentCommonModel.Types.METADATA_CONTAINER);
            if (subNodeList != null) {
                for (Node subNode : subNodeList) {
                    setSubNodeProps(subNode);
                }
            }

            currentSnapshot.inEditMode = inEditMode;
            if (!inEditMode) {
                currentSnapshot.viewModeWasOpenedInThePast = true;
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

    private void setSubNodeProps(Node subNode) {
        subNode.getProperties().put(DocumentAdminModel.Props.OBJECT_TYPE_ID.toString(), getDocument().getDocumentTypeId());
        subNode.getProperties().put(DocumentAdminModel.Props.OBJECT_TYPE_VERSION_NR.toString(), getDocument().getDocumentTypeVersionNr());
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

        DocumentDynamic savedDocument = null;
        try {
            // May throw UnableToPerformException or UnableToPerformMultiReasonException
            savedDocument = getDocumentDynamicService().updateDocument(getDocument(), getConfig().getSaveListenerBeanNames());
        } catch (UnableToPerformMultiReasonException e) {
            if (!handleAccessRestrictionChange(e)) {
                return null;
            }

            // This is handled in BaseDialogBean
            throw e;
        }

        if (savedDocument.isAccessRestrictionPropsChanged()) {
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

    private boolean handleAccessRestrictionChange(UnableToPerformMultiReasonException e) {
        final MessageDataWrapper messageDataWrapper = e.getMessageDataWrapper();
        if (messageDataWrapper.getFeedbackItemCount() == 1 && ACCESS_RESTRICTION_CHANGE_REASON_ERROR.equals(messageDataWrapper.iterator().next().getMessageKey())) {
            isFinished = false;
            renderedModal = AccessRestrictionChangeReasonModalComponent.ACCESS_RESTRICTION_CHANGE_REASON_MODAL_ID;
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
        getDocument().setProp(DocumentCommonModel.Props.ACCESS_RESTRICTION_CHANGE_REASON, ((AccessRestrictionChangeReasonEvent) event).getReason());
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
        return validatePermissionWithErrorMessage(docRef, DocumentCommonModel.Privileges.EDIT_DOCUMENT_META_DATA);
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
        getPropertySheetStateBean().reset(getStateHolders(), provider);
        getDocumentDialogHelperBean().reset(provider);
        resetModals();
        super.resetOrInit(provider); // reset blocks
    }

    private void resetModals() {
        renderedModal = null;
        showConfirmationPopup = false;
        // Add favorite modal component
        FavoritesModalComponent favoritesModal = new FavoritesModalComponent();
        final FacesContext context = FacesContext.getCurrentInstance();
        final Application application = context.getApplication();
        favoritesModal.setActionListener(application.createMethodBinding("#{DocumentDialog.addFavorite}", UIActions.ACTION_CLASS_ARGS));
        favoritesModal.setId("favorite-popup-" + context.getViewRoot().createUniqueId());

        // Access restriction change reason
        AccessRestrictionChangeReasonModalComponent reasonModal = new AccessRestrictionChangeReasonModalComponent();
        reasonModal.setActionListener(application.createMethodBinding("#{DocumentDynamicDialog.setAccessRestrictionChangeReason}", UIActions.ACTION_CLASS_ARGS));
        favoritesModal.setId("access-restriction-change-reason-popup-" + context.getViewRoot().createUniqueId());

        List<UIComponent> children = ComponentUtil.getChildren(getModalContainer());
        children.clear();
        children.add(favoritesModal);
        children.add(reasonModal);
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

    // TODO Alar: refactor subnode logic

    public void addSubNode(ActionEvent event) {
        // TODO do we need to set default values when adding a new child node?
        final Node docNode = getParentNode(event);
        QName partyType = DocumentCommonModel.Types.METADATA_CONTAINER;
        final WmNode subNode = getGeneralService().createNewUnSaved(partyType, null);
        setSubNodeProps(subNode);
        QName partyAssoc = DocumentCommonModel.Types.METADATA_CONTAINER;
        docNode.addChildAssociations(partyAssoc, subNode);
    }

    public void removeSubNode(ActionEvent event) {
        final Node docNode = getParentNode(event);
        final String assocIndexParam = ActionUtil.getParam(event, SubPropertySheetItem.PARAM_ASSOC_INDEX);
        final int assocIndex = Integer.parseInt(assocIndexParam);
        QName partyAssoc = DocumentCommonModel.Types.METADATA_CONTAINER;
        docNode.removeChildAssociations(partyAssoc, assocIndex);
    }

    private Node getParentNode(ActionEvent event) {
        final SubPropertySheetItem propSheet = ComponentUtil.getAncestorComponent(event.getComponent(), SubPropertySheetItem.class);
        return propSheet.getParentPropSheetNode();
    }

}
