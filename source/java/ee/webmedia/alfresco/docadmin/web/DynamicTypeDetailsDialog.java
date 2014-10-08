package ee.webmedia.alfresco.docadmin.web;

<<<<<<< HEAD
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;

=======
>>>>>>> develop-5.1
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.DynamicType;
<<<<<<< HEAD
=======
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docadmin.service.MetadataItem;
>>>>>>> develop-5.1
import ee.webmedia.alfresco.docadmin.web.DynamicTypeDetailsDialog.DynTypeDialogSnapshot;
import ee.webmedia.alfresco.docdynamic.web.BaseSnapshotCapableDialog;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageData;
import ee.webmedia.alfresco.utils.MessageUtil;
<<<<<<< HEAD

/**
 * Base dialog for editing {@link DynamicType} details
 * 
 * @author Ats Uiboupin
=======
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang.StringUtils;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

/**
 * Base dialog for editing {@link DynamicType} details
>>>>>>> develop-5.1
 */
public abstract class DynamicTypeDetailsDialog<D extends DynamicType, S extends DynTypeDialogSnapshot<D>> extends BaseSnapshotCapableDialog<S, D> {
    private static final long serialVersionUID = 1L;

    // START: Block beans
    protected FieldsListBean fieldsListBean;
    protected VersionsListBean<D> versionsListBean;
    private final Class<D> dynTypeClass;

    public DynamicTypeDetailsDialog(Class<D> dynTypeClass) {
        this.dynTypeClass = dynTypeClass;
    }

    /**
     * Contains fields that contain state to be used when restoring dialog
<<<<<<< HEAD
     * 
     * @author Ats Uiboupin
=======
>>>>>>> develop-5.1
     */
    public abstract static class DynTypeDialogSnapshot<D extends DynamicType> implements BaseSnapshotCapableDialog.Snapshot {
        private static final long serialVersionUID = 1L;

        private D dynType;
        private boolean addNewLatestDocumentTypeVersion = true;
        /** only initialized when not showing latest version */
        private Integer docTypeVersion;
        /** only initialized when not showing latest version */
        public NodeRef docTypeVersionRef;

        public boolean isLatestVersion() {
            return docTypeVersion == null;
        }

        @Override
        public abstract String getOpenDialogNavigationOutcome();

        @Override
        public String toString() {
            return super.toString() + "[addNewLatestDynTypeVersion=" + addNewLatestDocumentTypeVersion
                    + ", dynTypeVersion=" + docTypeVersion + ", dynType=" + dynType + "]";
        }

        public D getDynType() {
            return dynType;
        }

        public void setDynType(D docType) {
            this.dynType = docType;
        }
    }

    abstract protected S newSnapshot();

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
<<<<<<< HEAD
            Pair<D, MessageData> result = getDocumentAdminService().saveOrUpdateDynamicType(getCurrentSnapshot().getDynType());
=======
            Pair<D, MessageData> result = getDocumentAdminService().saveOrUpdateDynamicType(getCurrentSnapshot().getDynType(), false);
>>>>>>> develop-5.1
            D saveOrUpdateDocumentType = result.getFirst();
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
<<<<<<< HEAD
        boolean valid = true;
        // constraints known right now are validated by converters / validators before calling this method
        return valid;
    }

=======
        // Other constraints known right now are validated by converters / validators before calling this method
        return validateRelatedOutgoingDecElements();
    }

    private boolean validateRelatedOutgoingDecElements() {
        boolean valid = true;
        Map<String, List<String>> usedDecElements = new CaseInsensitiveMap<String, List<String>>();

        List<? extends MetadataItem> metaFieldsList = fieldsListBean.getMetaFieldsList();
        for (MetadataItem item : metaFieldsList) {
            if (item instanceof FieldGroup) {
                List<? extends Field> list = ((FieldGroup) item).getFields().getList();
                collectRelatedOutgoingDecElements(usedDecElements, list.toArray(new Field[list.size()]));
            }
            if (item instanceof Field) {
                collectRelatedOutgoingDecElements(usedDecElements, (Field) item);
            }
        }

        for (Map.Entry<String, List<String>> entry : usedDecElements.entrySet()) {
            if (entry.getValue().size() > 1) {
                MessageUtil.addErrorMessage("docType_save_error_overlapping_related_outgoing_dec_elements", StringUtils.join(entry.getValue(), ", "));
                valid = false;
            }
        }

        return valid;
    }

    private void collectRelatedOutgoingDecElements(Map<String, List<String>> usedDecElements, Field... fields) {
        if (fields == null) {
            return;
        }

        for (Field field : fields) {
            List<String> outgoingDecElements = field.getRelatedOutgoingDecElement();
            if (outgoingDecElements == null) {
                continue;
            }

            for (String element : outgoingDecElements) {
                if (StringUtils.isBlank(element)) {
                    continue;
                }
                List<String> fieldList = usedDecElements.get(element);
                if (fieldList == null) {
                    fieldList = new ArrayList<String>();
                    usedDecElements.put(element, fieldList);
                }
                fieldList.add(field.getName());
            }
        }
    }

>>>>>>> develop-5.1
    protected void resetFields() {
        S currentSnapshot = getCurrentSnapshot();
        if (currentSnapshot != null) {
            currentSnapshot.setDynType(null);
        }
        fieldsListBean.resetOrInit(null);
        versionsListBean.resetOrInit(null);
        // don't assign null to injected beans
    }

    @Override
    public Object getActionsContext() {
        S currentSnapshot = getCurrentSnapshot();
        if (currentSnapshot == null) {
            return null;
        }
        return currentSnapshot.getDynType();
    }

    public Class<D> getDynTypeClass() {
        return dynTypeClass;
    }

    // START: jsf actions/accessors
    public void addNew(@SuppressWarnings("unused") ActionEvent event) {
        init(getDocumentAdminService().createNewUnSavedDynamicType(dynTypeClass));
    }

    /** used by delete action to do actual deleting (after user has confirmed deleting in DeleteDialog) */
<<<<<<< HEAD
    public String deleteType(ActionEvent event) {
        NodeRef dynTypeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        BeanHelper.getDocumentAdminService().deleteDynamicType(dynTypeRef);
        return getCloseOutcome(2);
=======
    public void deleteType(ActionEvent event) {
        NodeRef dynTypeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        BeanHelper.getDocumentAdminService().deleteDynamicType(dynTypeRef);
>>>>>>> develop-5.1
    }

    /** replace dynType in memory with fresh copy from repo */
    void refreshDocType() {
        S currentSnapshot = getCurrentSnapshot();
        currentSnapshot.addNewLatestDocumentTypeVersion = true;
        D documentType = getDocumentTypeWithoutOlderDTVersionChildren(currentSnapshot.getDynType().getNodeRef());
        updateDialogState(documentType, currentSnapshot, null);
    }

    void init(NodeRef docTypeRef) {
        init(getDocumentTypeWithoutOlderDTVersionChildren(docTypeRef));
    }

    /**
     * @return D without fetching children of older {@link DocumentTypeVersion} nodes
     */
    private D getDocumentTypeWithoutOlderDTVersionChildren(NodeRef docTypeRef) {
        return getDocumentAdminService().getDynamicType(dynTypeClass, docTypeRef, DocumentAdminService.DOC_TYPE_WITHOUT_OLDER_DT_VERSION_CHILDREN);
    }

    protected void init(D documentType) {
        init(documentType, null);
    }

    void init(D documentType, NodeRef docTypeVersionRef) {
        if (documentType == null) {
            resetFields();
            return;
        }
        updateDialogState(documentType, createSnapshot(newSnapshot()), docTypeVersionRef);
    }

    protected void updateDialogState(D documentType, S currentSnapshot, NodeRef docTypeVersionRef) {
        currentSnapshot.setDynType(documentType);
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
    }

    @Override
    public boolean isFinishButtonVisible(boolean dialogConfOKButtonVisible) {
        return isShowingLatestVersion();
    }

    @Override
    protected void resetOrInit(D dataProvider) {
        if (dataProvider != null) {
            updateDialogState(dataProvider, getCurrentSnapshot(), getCurrentSnapshot().docTypeVersionRef);
        } else {
            resetFields();
        }
    }

    @Override
    protected D getDataProvider() {
        S currentSnapshot = getCurrentSnapshot();
        return currentSnapshot != null ? currentSnapshot.getDynType() : null;
    }

    /** used by jsp propertySheetGrid */
    public Node getCurrentNode() {
        return getCurrentSnapshot().getDynType().getNode();
    }

    /** used by web-client propertySheet */
    public boolean isSaved() {
        return getCurrentSnapshot().getDynType().isSaved();
    }

    /** used by jsp */
    public D getDocType() {
        return getCurrentSnapshot().getDynType();
    }

    /** used by jsp */
    public FieldsListBean getFieldsListBean() {
        return fieldsListBean;
    }

    /** injected by spring */
    public void setFieldsListBean(FieldsListBean fieldsListBean) {
        this.fieldsListBean = fieldsListBean;
    }

    /** injected by spring */
    public void setVersionsListBean(VersionsListBean<D> versionsListBean) {
        this.versionsListBean = versionsListBean;
    }

    /** used by jsp */
    public VersionsListBean<D> getVersionsListBean() {
        return versionsListBean;
    }

    /** JSP */
    public boolean isAddFieldVisible() {
        return true;
    }

    /** JSP - field-list-bean.jsp is used by this dialog and by {@link FieldGroupDetailsDialog} where reference to this bean is needed */
    public DynamicTypeDetailsDialog<? extends DynamicType, ?> getDynamicTypeDetailsDialog() {
        return this;
    }

    public boolean isShowingLatestVersion() {
        S currentSnapshot = getCurrentSnapshot();
        if (currentSnapshot == null) {
            // Workaround to NullpointerException
            // FIXME current dialog is DocTypeDetailsDialog and user clicks some menu item (for example to open DocTypeListDialog)
            //
<<<<<<< HEAD
            // FIXME CL_TASK 177667 Ats -> Kaarel: when leaving dialog clearState() method (that clears snapshots) is called in
=======
            // FIXME CL_TASK 177667 -> when leaving dialog clearState() method (that clears snapshots) is called in
>>>>>>> develop-5.1
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
        S currentSnapshot = getCurrentSnapshot();
        if (currentSnapshot == null) {
            return null;
        }
        Integer docTypeVersion = currentSnapshot.docTypeVersion;
        Integer verNr = docTypeVersion != null ? docTypeVersion : currentSnapshot.getDynType().getLatestVersion();
        return MessageUtil.getMessage("doc_type_details_panel_metadata", verNr);
    }

    // END: jsf actions/accessors
}
