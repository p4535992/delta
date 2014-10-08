package ee.webmedia.alfresco.document.search.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAssociationsService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentConfigService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.alfresco.web.config.PropertySheetConfigElement;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

import ee.webmedia.alfresco.casefile.service.CaseFile;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docconfig.service.DocumentConfig;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.document.search.model.AssocSearchObjectType;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.document.search.service.AssocBlockObject;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

/**
 * Refactored from SearchBlockBean.
 */
public abstract class AbstractSearchBlockBean implements DialogDataProvider {
    public static final String PARAM_NODEREF = "nodeRef";
    protected NodeRef sourceObjectRef;
    protected String searchValue;
    protected Date regDateTimeBegin;
    protected Date regDateTimeEnd;
    protected List<String> selectedDocumentTypes;
    protected List<AssocBlockObject> assocBlockObjects;
    private boolean expanded;
    protected DocumentConfig config;
    protected transient UIPropertySheet propertySheet;
    protected Node filter;

    public void initSearch(NodeRef sourceObjectRef, String renderAssocObjectFieldValueBinding) {
        reset();
        this.sourceObjectRef = sourceObjectRef;
        loadConfig(renderAssocObjectFieldValueBinding);
        filter = getNewFilter();
        BeanHelper.getPropertySheetStateBean().resetAdditionalStateHolders(getBeanName(), config.getStateHolders(), this);
    }

    protected void loadConfig(String renderAssocObjectFieldValueBinding) {
        config = getDocumentConfigService().getAssocObjectSearchConfig(getBeanName(), renderAssocObjectFieldValueBinding);
    }

    protected Node getNewFilter() {
        Map<QName, Serializable> data = new HashMap<QName, Serializable>();
        data.put(DocumentSearchModel.Props.STORE, new ArrayList<String>());
        data.put(DocumentSearchModel.Props.DOCUMENT_TYPE, new ArrayList<String>());
        data.put(DocumentSearchModel.Props.OBJECT_TYPE, AssocSearchObjectType.DOCUMENT.name());
        return new TransientNode(DocumentSearchModel.Types.OBJECT_FILTER, null, data);
    }

    public boolean isDocumentVolumeColumnVisible() {
        return getDocumentService().isVolumeColumnEnabled();
    }

    protected abstract String getBeanName();

    public void reset() {
        searchValue = null;
        regDateTimeBegin = null;
        regDateTimeEnd = null;
        assocBlockObjects = null;
        sourceObjectRef = null;
        expanded = false;
        selectedDocumentTypes = null;
        filter = null;
        config = null;
        propertySheet = null;
    }

    /** @param event from JSP */
    public void setup(ActionEvent event) {
        try {
            assocBlockObjects = BeanHelper.getDocumentSearchService().searchAssocObjects(getFilter());
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(e);
            assocBlockObjects = Collections.<AssocBlockObject> emptyList();
        }
    }

    public void addAssocDocHandler(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, PARAM_NODEREF));
        saveAssocNow(sourceObjectRef, nodeRef, getDefaultAssocType());
    }

    protected abstract QName getDefaultAssocType();

    protected void saveAssocNow(final NodeRef sourceRef, final NodeRef targetRef, final QName assocType) {
        final NodeService nodeService = BeanHelper.getNodeService();
        final List<AssociationRef> targetAssocs = nodeService.getTargetAssocs(sourceRef, assocType);
        for (AssociationRef associationRef : targetAssocs) {
            if (associationRef.getTargetRef().equals(targetRef) && associationRef.getTypeQName().equals(assocType)) {
                addAssocExistsErrorMsg();
                return;
            }
        }
        List<NodeRef> newAssocs = getNewAssocs();
        if (newAssocs != null) {
            for (NodeRef nodeRef : newAssocs) {
                if (nodeRef.equals(sourceRef)) {
                    addAssocExistsErrorMsg();
                    return;
                }
            }
        }
        try {
            if (RepoUtil.isSaved(sourceRef)) {
                BeanHelper.getDocLockService().checkForLock(sourceRef);
            }
            if (RepoUtil.isSaved(targetRef)) {
                BeanHelper.getDocLockService().checkForLock(targetRef);
            }
            createAssoc(sourceRef, targetRef, assocType);
        } catch (NodeLockedException e) {
            NodeRef nodeRef = e.getNodeRef();
            String messageId = nodeRef.equals(sourceRef) ? "document_assocAdd_error_sourceLocked" : "document_assocAdd_error_targetLocked";
            BeanHelper.getDocumentLockHelperBean().handleLockedNode(messageId, nodeRef);
            return;
        }
        doPostSave();
    }

    protected List<NodeRef> getNewAssocs() {
        return null;
    }

    private void addAssocExistsErrorMsg() {
        MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "document_assocAdd_error_alreadyExists");
    }

    protected void createAssoc(final NodeRef sourceRef, final NodeRef targetRef, final QName assocType) {
        getDocumentAssociationsService().createAssoc(sourceRef, targetRef, assocType);
    }

    protected void doPostSave() {
        // subclasses can override
    }

    public int getCount() {
        if (assocBlockObjects == null) {
            return 0;
        }
        return assocBlockObjects.size();
    }

    public void hideSearchBlock(@SuppressWarnings("unused") ActionEvent event) {
        expanded = false;
        if (filter != null && filter.getProperties() != null) {
            filter.getProperties().put(DocumentSearchModel.Props.STORE.toString(), new ArrayList<String>());
        }
    }

    public abstract String getSearchBlockTitle();

    protected boolean searchCases() {
        return false;
    }

    public abstract String getActionColumnFileName();

    public boolean isShowSearch() {
        return assocBlockObjects != null && assocBlockObjects.size() > 0;
    }

    public List<AssocBlockObject> getDocuments() {
        return assocBlockObjects;
    }

    public String getSearchValue() {
        return searchValue;
    }

    public void setSearchValue(String searchValue) {
        this.searchValue = searchValue;
    }

    public Date getRegDateTimeBegin() {
        return regDateTimeBegin;
    }

    public void setRegDateTimeBegin(Date regDateTimeBegin) {
        this.regDateTimeBegin = regDateTimeBegin;
    }

    public Date getRegDateTimeEnd() {
        return regDateTimeEnd;
    }

    public void setRegDateTimeEnd(Date regDateTimeEnd) {
        this.regDateTimeEnd = regDateTimeEnd;
    }

    public List<String> getSelectedDocumentTypes() {
        return selectedDocumentTypes;
    }

    public void setSelectedDocumentTypes(List<String> selectedDocumentTypes) {
        this.selectedDocumentTypes = selectedDocumentTypes;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public Node getFilter() {
        return filter;
    }

    public void setPropertySheet(UIPropertySheet propertySheet) {
        this.propertySheet = propertySheet;
    }

    @Override
    public UIPropertySheet getPropertySheet() {
        return propertySheet;
    }

    public PropertySheetConfigElement getPropertySheetConfigElement() {
        return config.getPropertySheetConfigElement();
    }

    @Override
    public DocumentDynamic getDocument() {
        return null;
    }

    @Override
    public CaseFile getCaseFile() {
        return null;
    }

    @Override
    public Node getNode() {
        return getFilter();
    }

    @Override
    public boolean isInEditMode() {
        return true;
    }

    @Override
    public void switchMode(boolean inEditMode) {
    }

}
