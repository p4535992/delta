package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.web.DocTypeDetailsDialog.DocTypeDialogSnapshot;
import ee.webmedia.alfresco.docdynamic.web.BaseSnapshotCapableDialog;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageData;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Details of document type.
 * To open this dialog just call actionListener. You must not set action attribute on actionLink that opens this dialog nor any other way perform navigation, as actionListener
 * handles navigation
 * 
 * @author Ats Uiboupin
 */
public class DocTypeDetailsDialog extends BaseSnapshotCapableDialog<DocTypeDialogSnapshot, DocumentType> {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "DocTypeDetailsDialog";

    // START: Block beans
    private FieldsListBean fieldsListBean;
    private FollowupAssocsListBean followupAssocsListBean;
    private ReplyAssocsListBean replyAssocsListBean;
    private VersionsListBean versionsListBean;

    // END: Block beans

    /**
     * Contains fields that contain state to be used when restoring dialog
     * 
     * @author Ats Uiboupin
     */
    static class DocTypeDialogSnapshot implements BaseSnapshotCapableDialog.Snapshot {
        private static final long serialVersionUID = 1L;
        private DocumentType docType;
        private boolean addNewLatestDocumentTypeVersion = true;
        /** only initialized when not showing latest version */
        private Integer docTypeVersion;
        /** only initialized when not showing latest version */
        public NodeRef docTypeVersionRef;

        public boolean isLatestVersion() {
            return docTypeVersion == null;
        }

        @Override
        public String getOpenDialogNavigationOutcome() {
            return AlfrescoNavigationHandler.DIALOG_PREFIX + "docTypeDetailsDialog";
        }

        @Override
        public String toString() {
            return super.toString() + "[addNewLatestDocumentTypeVersion=" + addNewLatestDocumentTypeVersion
                    + ", docTypeVersion=" + docTypeVersion + ", docType=" + docType + "]";
        }
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) {
        if (validate() && save()) {
            MessageUtil.addInfoMessage("save_success");
        } else {
            isFinished = false;
        }
        return null;
    }

    boolean save() {
        try {
            fieldsListBean.doReorder();
            Pair<DocumentType, MessageData> result = getDocumentAdminService().saveOrUpdateDocumentType(getCurrentSnapshot().docType);
            DocumentType saveOrUpdateDocumentType = result.getFirst();
            getCurrentSnapshot().addNewLatestDocumentTypeVersion = true;
            updateDialogState(saveOrUpdateDocumentType, getCurrentSnapshot(), null);
            MessageData messageData = result.getSecond();
            if (messageData != null) {
                MessageUtil.addStatusMessage(messageData);
            }
            return true;
        } catch (RuntimeException e) {
            handleException(e);
        }
        return false;
    }

    boolean validate() {
        boolean valid = true;
        // constraints known right now are validated by converters / validators before calling this method
        return valid;
    }

    private void resetFields() {
        DocTypeDialogSnapshot currentSnapshot = getCurrentSnapshot();
        if (currentSnapshot != null) {
            currentSnapshot.docType = null;
        }
        followupAssocsListBean.resetOrInit(null);
        replyAssocsListBean.resetOrInit(null);
        fieldsListBean.resetOrInit(null);
        versionsListBean.resetOrInit(null);
        // don't assign null to injected beans
    }

    @Override
    public Object getActionsContext() {
        return getCurrentSnapshot().docType;
    }

    // START: jsf actions/accessors
    public void addNew(@SuppressWarnings("unused") ActionEvent event) {
        init(getDocumentAdminService().createNewUnSaved());
    }

    /** used by delete action to do actual deleting (after user has confirmed deleting in DeleteDialog) */
    public String deleteDocType(ActionEvent event) {
        NodeRef docTypeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        getDocumentAdminService().deleteDocumentType(docTypeRef);
        return getCloseOutcome(2);
    }

    public void showDetails(ActionEvent event) {
        init(ActionUtil.getParam(event, "nodeRef", NodeRef.class));
    }

    /** replace docType in memory with fresh copy from repo */
    void refreshDocType() {
        DocTypeDialogSnapshot currentSnapshot = getCurrentSnapshot();
        currentSnapshot.addNewLatestDocumentTypeVersion = true;
        DocumentType documentType = getDocumentAdminService().getDocumentType(currentSnapshot.docType.getNodeRef());
        updateDialogState(documentType, currentSnapshot, null);
    }

    private void init(NodeRef docTypeRef) {
        init(getDocumentAdminService().getDocumentType(docTypeRef));
    }

    private void init(DocumentType documentType) {
        init(documentType, null);
    }

    void init(DocumentType documentType, NodeRef docTypeVersionRef) {
        if (documentType == null) {
            resetFields();
            return;
        }
        updateDialogState(documentType, createSnapshot(new DocTypeDialogSnapshot()), docTypeVersionRef);
    }

    private void updateDialogState(DocumentType documentType, DocTypeDialogSnapshot currentSnapshot, NodeRef docTypeVersionRef) {
        currentSnapshot.docType = documentType;
        DocumentTypeVersion docTypeVersion;
        if (docTypeVersionRef != null) {
            docTypeVersion = documentType.getDocumentTypeVersions().getChildByNodeRef(docTypeVersionRef);
            currentSnapshot.docTypeVersionRef = docTypeVersionRef;
            currentSnapshot.docTypeVersion = docTypeVersion.getVersionNr();
        } else {
            if (currentSnapshot.addNewLatestDocumentTypeVersion) {
                documentType.addNewLatestDocumentTypeVersion();
            }
            docTypeVersion = documentType.getLatestDocumentTypeVersion();
            versionsListBean.resetOrInit(documentType);
        }
        currentSnapshot.addNewLatestDocumentTypeVersion = false;
        fieldsListBean.init(docTypeVersion);
        followupAssocsListBean.resetOrInit(this);
        replyAssocsListBean.resetOrInit(this);
    }

    @Override
    public boolean isFinishButtonVisible(boolean dialogConfOKButtonVisible) {
        return isShowingLatestVersion();
    }

    @Override
    protected void resetOrInit(DocumentType dataProvider) {
        if (dataProvider != null) {
            updateDialogState(dataProvider, getCurrentSnapshot(), getCurrentSnapshot().docTypeVersionRef);
        } else {
            resetFields();
        }
    }

    @Override
    protected DocumentType getDataProvider() {
        DocTypeDialogSnapshot currentSnapshot = getCurrentSnapshot();
        return currentSnapshot != null ? currentSnapshot.docType : null;
    }

    /** used by jsp propertySheetGrid */
    public Node getCurrentNode() {
        return getCurrentSnapshot().docType.getNode();
    }

    /** used by web-client propertySheet */
    public boolean isSaved() {
        return getCurrentSnapshot().docType.isSaved();
    }

    /** used by jsp */
    public boolean isShowSystematicComment() {
        return StringUtils.isNotBlank(getCurrentSnapshot().docType.getSystematicComment());
    }

    /** used by jsp */
    public DocumentType getDocType() {
        return getCurrentSnapshot().docType;
    }

    /** used by jsp */
    public FieldsListBean getFieldsListBean() {
        return fieldsListBean;
    }

    /** injected by spring */
    public void setFieldsListBean(FieldsListBean fieldsListBean) {
        this.fieldsListBean = fieldsListBean;
    }

    /** used by jsp */
    public FollowupAssocsListBean getFollowupAssocsListBean() {
        return followupAssocsListBean;
    }

    /** injected by spring */
    public void setFollowupAssocsListBean(FollowupAssocsListBean followupAssocsListBean) {
        this.followupAssocsListBean = followupAssocsListBean;
    }

    /** injected by spring */
    public void setVersionsListBean(VersionsListBean versionsListBean) {
        this.versionsListBean = versionsListBean;
    }

    /** used by jsp */
    public ReplyAssocsListBean getReplyAssocsListBean() {
        return replyAssocsListBean;
    }

    /** injected by spring */
    public void setReplyAssocsListBean(ReplyAssocsListBean replyAssocsListBean) {
        this.replyAssocsListBean = replyAssocsListBean;
    }

    /** JSP */
    public boolean isAddFieldVisible() {
        return true;
    }

    public boolean isShowingLatestVersion() {
        DocTypeDialogSnapshot currentSnapshot = getCurrentSnapshot();
        if (currentSnapshot == null) {
            // Workaround to NullpointerException
            // FIXME current dialog is DocTypeDetailsDialog and user clicks some menu item (for example to open DocTypeListDialog)
            //
            // FIXME CL_TASK 177667 Ats -> Kaarel: when leaving dialog clearState() method (that clears snapshots) is called in
            // ApplyRequestValues phase by calling MenuBean.clearViewStack(menuBean.getActiveItemId(), clientId);
            //
            // Better JSF components method bindings are evaluated in the same phase
            // correct solution would probably be to call clearState() (or even whole navigation logic in UIMenuComponent.queueEvent()) later (for example in the beginning of
            // InvokeApplication phase)
            return false;
        }
        return currentSnapshot.isLatestVersion();
    }

    public String getMetaFieldsListLabel() {
        DocTypeDialogSnapshot currentSnapshot = getCurrentSnapshot();
        Integer docTypeVersion = currentSnapshot.docTypeVersion;
        Integer verNr = docTypeVersion != null ? docTypeVersion : currentSnapshot.docType.getLatestVersion();
        return MessageUtil.getMessage("doc_type_details_panel_metadata", verNr);
    }

    // END: jsf actions/accessors
}
