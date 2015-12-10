package ee.webmedia.alfresco.document.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentConfigService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDynamicService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getJsfBindingHelper;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPropertySheetStateBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.config.PropertySheetConfigElement;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.casefile.service.CaseFile;
import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.propertysheet.component.SimUIPropertySheet;
import ee.webmedia.alfresco.common.propertysheet.customchildrencontainer.CustomChildrenCreator;
import ee.webmedia.alfresco.common.propertysheet.modalLayer.ModalLayerComponent;
import ee.webmedia.alfresco.common.propertysheet.modalLayer.ModalLayerComponent.ModalLayerSubmitEvent;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docconfig.generator.systematic.DocumentLocationGenerator;
import ee.webmedia.alfresco.docconfig.service.DocumentConfig;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.web.DocumentListDataProvider;
import ee.webmedia.alfresco.document.service.DocumentServiceImpl;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformMultiReasonException;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.service.VolumeService;
import ee.webmedia.alfresco.volume.web.VolumeListDialog;

/**
 * Form backing bean for Document list. <br>
 * <br>
 * This Class has logic of two different, but similar versions of documents(when parent is volume or case). <br>
 * Reason is that we don't have to worry about what the parent of document in jsp files.
 */
public class DocumentListDialog extends BaseDocumentListDialog implements DialogDataProvider {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "DocumentListDialog";

    private static final String VOLUME_NODE_REF = "volumeNodeRef";
    private static final String CASE_NODE_REF = "caseNodeRef";

    private static final Log LOG = LogFactory.getLog(DocumentListDialog.class);
    private transient VolumeService volumeService;
    private transient CaseService caseService;

    // one of the following should always be null(depending of whether it is directly under volume or under case, that is under volume)
    private Volume parentVolume;
    private Case parentCase;

    private Node locationNode;

    private DocumentConfig config;

    private boolean confirmMoveAssociatedDocuments;
    private boolean showDocumentsLocationPopup;
    private List<NodeRef> selectedDocs;

    @Override
    public Object getActionsContext() {
        if (parentVolume != null) {
            return parentVolume.getNode();
        } else if (parentCase != null) {
            return parentCase.getNode();
        }

        return null;
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        getPropertySheetStateBean().reset(getConfig().getStateHolders(), this);
    }

    public void init(NodeRef parentRef) {
        init(parentRef, true);
    }

    public void init(NodeRef parentRef, boolean navigate) {
        QName type = getNodeService().getType(parentRef);
        getPropertySheetStateBean().reset(null, null);
        if (VolumeModel.Types.VOLUME.equals(type) || CaseFileModel.Types.CASE_FILE.equals(type)) {
            parentVolume = getVolumeService().getVolumeByNodeRef(parentRef, null);
            parentCase = null;
        } else if (CaseModel.Types.CASE.equals(type)) {
            parentCase = getCaseService().getCaseByNoderef(parentRef);
            parentVolume = null;
        } else {
            throw new RuntimeException("Unsupported type: " + type);
        }
        resetLimit(false);
        doInitialSearch();
        BeanHelper.getVisitedDocumentsBean().clearVisitedDocuments();
        if (navigate) {
            WebUtil.navigateTo(AlfrescoNavigationHandler.DIALOG_PREFIX + "documentListDialog");
        }
    }

    public void setup(ActionEvent event, boolean navigate) {
        final Map<String, String> parameterMap = ((UIActionLink) event.getSource()).getParameterMap();
        final String param;
        if (parameterMap.containsKey(VOLUME_NODE_REF)) {
            param = ActionUtil.getParam(event, VOLUME_NODE_REF);
            NodeRef volumeRef = new NodeRef(param);
            if (!nodeExists(volumeRef)) {
                return;
            }
            parentVolume = getVolumeService().getVolumeByNodeRef(volumeRef, null);
            parentCase = null;
        } else {
            param = ActionUtil.getParam(event, CASE_NODE_REF);
            if (!nodeExists(new NodeRef(param))) {
                return;
            }
            parentCase = getCaseService().getCaseByNoderef(param);
            parentVolume = null;
        }
        resetLimit(false);
        doInitialSearch();
        BeanHelper.getVisitedDocumentsBean().clearVisitedDocuments();
        if (navigate) {
            WebUtil.navigateTo(AlfrescoNavigationHandler.DIALOG_PREFIX + "documentListDialog");
        }
    }

    public String action() {
        String dialogPrefix = AlfrescoNavigationHandler.DIALOG_PREFIX;
        if (parentVolume == null && parentCase == null) {
            MessageUtil.addInfoMessage("volume_noderef_not_found");
            return dialogPrefix + VolumeListDialog.DIALOG_NAME;
        }
        return dialogPrefix + "documentListDialog";
    }

    public void setup(ActionEvent event) {
        setup(event, true);
    }

    public void updateLocationSelect(@SuppressWarnings("unused") ActionEvent event) {
        Set<String> documentTypeIds = new HashSet<String>();
        DocumentDynamicService documentDynamicService = BeanHelper.getDocumentDynamicService();
        for (Entry<NodeRef, Boolean> entry : getListCheckboxes().entrySet()) {
            if (entry.getValue()) {
                documentTypeIds.add(documentDynamicService.getDocumentType(entry.getKey()));
            }
        }
        if (documentTypeIds.size() == 0) {
            MessageUtil.addErrorMessage("document_move_none_selected");
            showDocumentsLocationPopup = false;
            return;
        }
        getLocationNode().getProperties().put(DocumentLocationGenerator.DOCUMENT_TYPE_IDS.toString(), documentTypeIds);
        showDocumentsLocationPopup = true;
        getPropertySheetStateBean().reset(getConfig().getStateHolders(), this);
    }

    public void massChangeDocLocation(ActionEvent event) {
        ModalLayerSubmitEvent changeEvent = (ModalLayerSubmitEvent) event;
        int actionIndex = changeEvent.getActionIndex();
        showDocumentsLocationPopup = false;
        if (actionIndex == ModalLayerComponent.ACTION_CLEAR) {
            locationNode = null;
            return;
        }
        Map<String, Object> locationProps = getLocationNode().getProperties();
        NodeRef function = (NodeRef) locationProps.get(DocumentCommonModel.Props.FUNCTION.toString());
        NodeRef series = (NodeRef) locationProps.get(DocumentCommonModel.Props.SERIES.toString());
        NodeRef volume = (NodeRef) locationProps.get(DocumentCommonModel.Props.VOLUME.toString());
        // caseRef is not checked here, because admins and docmanagers always have the suggest component for case property
        String caseLabel = (String) locationProps.get(DocumentLocationGenerator.CASE_LABEL_EDITABLE);
        if (!isValidLocation(function, series, volume)) {
            return;
        }
        // assume that current document list contains documents from one location, check location for first document only
        if (!getListCheckboxes().isEmpty()) {
            DocumentDynamic document = getDocumentDynamicService().getDocument(getListCheckboxes().keySet().iterator().next());
            if (DocumentServiceImpl.PropertyChangesMonitorHelper.hasSameLocation(document, function, series, volume, caseLabel)) {
                return;
            }
        }
        confirmMoveAssociatedDocuments = false;
        for (Entry<NodeRef, Boolean> entry : getListCheckboxes().entrySet()) {
            if (!entry.getValue()) {
                continue;
            }
            NodeRef docRef = entry.getKey();
            if (BeanHelper.getDocumentAssociationsService().isBaseOrReplyOrFollowUpDocument(docRef, null)) {
                confirmMoveAssociatedDocuments = true;
            }
        }
        if (confirmMoveAssociatedDocuments) {
            return;
        }
        massChangeDocLocationConfirmed(event);
    }

    public void massChangeDocLocationConfirmed(ActionEvent event) {
        resetConfirmation(event);
        massChangeDocLocationSave(false);
    }

    public void massChangeDocLocationSave(boolean isNewCaseFileCreated) {
        Map<String, Object> locationProps = getLocationNode().getProperties();
        final NodeRef function = (NodeRef) locationProps.get(DocumentCommonModel.Props.FUNCTION.toString());
        final NodeRef series = (NodeRef) locationProps.get(DocumentCommonModel.Props.SERIES.toString());
        final NodeRef volume = (NodeRef) locationProps.get(DocumentCommonModel.Props.VOLUME.toString());
        final String caseLabel = (String) locationProps.get(DocumentLocationGenerator.CASE_LABEL_EDITABLE);
        final Set<NodeRef> updatedNodeRefs = new HashSet<NodeRef>();

        if (selectedDocs == null) {
            selectedDocs = new ArrayList<NodeRef>();
            for (Entry<NodeRef, Boolean> item : getListCheckboxes().entrySet()) {
                if (Boolean.TRUE.equals(item.getValue())) {
                    selectedDocs.add(item.getKey());
                }
            }
        }

        if (isCreateNewCaseFile(volume)) {
            BeanHelper.getCaseFileDialog().createCaseFile((String) locationNode.getProperties().get(DocumentLocationGenerator.CASE_FILE_TYPE_PROP), locationNode, selectedDocs);
            return;
        }

        int processed = 0;
        String currentDocName = null;
        try {
            RetryingTransactionHelper retryingTransactionHelper = BeanHelper.getTransactionService().getRetryingTransactionHelper();
            for (final NodeRef docRef : selectedDocs) {
                if (updatedNodeRefs.contains(docRef)) {
                    // document was already moved as followup or reply document of some selected document
                    continue;
                }
                retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {

                    @Override
                    public Void execute() throws Throwable {
                        updateDocumentInMassChangeLocation(function, series, volume, caseLabel, updatedNodeRefs, docRef);
                        return null;
                    }
                }, false, !isNewCaseFileCreated);
                processed++;
            }
        } catch (NodeLockedException e) {
            BeanHelper.getDocumentLockHelperBean().handleLockedNode("document_location_change_error_assoc_locked", e.getNodeRef(), new Object[] { processed, currentDocName });
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(FacesContext.getCurrentInstance(), e);
        } catch (UnableToPerformMultiReasonException e) {
            DocumentDynamic erroneusDocument = e.getDocument();
            if (erroneusDocument != null) {
                LOG.debug("Error mass changing document location, erroneous document:\n" + erroneusDocument, e);
                MessageUtil.addErrorMessage("mass_change_document_location_error", BeanHelper.getDocumentAdminService().getDocumentTypeName(erroneusDocument.getDocumentTypeId()),
                        erroneusDocument.getDocName(), StringUtils.defaultString(erroneusDocument.getRegNumber(), MessageUtil.getMessage("document_log_status_empty")));
            } else {
                LOG.debug("Error mass changing document location", e);
                MessageUtil.addErrorMessage("mass_change_document_location_general_error");
            }
            MessageUtil.addStatusMessages(FacesContext.getCurrentInstance(), e.getMessageDataWrapper());
        }
        doInitialSearch();
        BeanHelper.getVisitedDocumentsBean().clearVisitedDocuments();
    }

    private boolean isCreateNewCaseFile(NodeRef volumeRef) {
        final boolean createNewCaseFile = volumeRef != null && RepoUtil.isUnsaved(volumeRef);
        return createNewCaseFile;
    }

    public void setSelectedDocs(List<NodeRef> selectedDocs) {
        this.selectedDocs = selectedDocs;
    }

    private void updateDocumentInMassChangeLocation(NodeRef function, NodeRef series, NodeRef volume, String caseLabel, Set<NodeRef> updatedNodeRefs, NodeRef docRef) {
        DocumentDynamic document = getDocumentDynamicService().getDocument(docRef);
        List<String> saveListenerBeans = getDocumentConfigService().getSaveListenerBeanNames(document.getNode());
        document.setFunction(function);
        document.setSeries(series);
        document.setVolume(volume);
        document.setCase(null);
        document.getNode().getProperties().put(DocumentLocationGenerator.CASE_LABEL_EDITABLE.toString(), caseLabel);
        List<Pair<NodeRef, NodeRef>> updatedRefs = getDocumentDynamicService().updateDocumentGetDocAndNodeRefs(document, saveListenerBeans, true, true)
                .getSecond();
        for (Pair<NodeRef, NodeRef> pair : updatedRefs) {
            updatedNodeRefs.add(pair.getFirst());
            updatedNodeRefs.add(pair.getSecond());
        }
    }

    public void resetConfirmation(@SuppressWarnings("unused") ActionEvent event) {
        confirmMoveAssociatedDocuments = false;
    }

    public boolean isConfirmMoveAssociatedDocuments() {
        return confirmMoveAssociatedDocuments;
    }

    public boolean isShowDocumentsLocationPopup() {
        return showDocumentsLocationPopup;
    }

    private boolean isValidLocation(NodeRef functionRef, NodeRef seriesRef, NodeRef volumeRef) {
        if (functionRef == null || seriesRef == null || volumeRef == null) {
            MessageUtil.addErrorMessage("document_validationMsg_mandatory_functionSeriesVolume");
            return false;
        }
        return true;
    }

    public DocumentConfig getConfig() {
        if (config == null) {
            config = getDocumentConfigService().getDocLocationConfig();
        }
        return config;
    }

    @Override
    protected void limitChangedEvent() {
        doInitialSearch();
        BeanHelper.getVisitedDocumentsBean().clearVisitedDocuments();
    }

    @Override
    public void restored() {
        BeanHelper.getVisitedDocumentsBean().resetVisitedDocuments(documentProvider);
    }

    protected void doInitialSearch() {
        NodeRef parentRef = null;
        locationNode = null;
        selectedDocs = null;
        setListCheckboxes(new HashMap<NodeRef, Boolean>());
        getPropertySheetStateBean().reset(getConfig().getStateHolders(), this);
        if (parentCase != null) {
            parentRef = parentCase.getNode().getNodeRef();
        } else if (parentVolume != null) {// assuming that parentVolume is volume
            parentRef = parentVolume.getNode().getNodeRef();
        }
        List<NodeRef> docNodeRefs = setLimited(getChildNodes(parentRef, getLimit()));
        documentProvider = new DocumentListDataProvider(docNodeRefs, true, DOC_PROPS_TO_LOAD);
        final boolean debugEnabled = LOG.isDebugEnabled();
        if (debugEnabled) {
            LOG.debug("Found " + documentProvider.getListSize() + " document(s) during initial search. Limit: " + getLimit());
        }
        resetModals();
        clearRichList();
    }

    private Pair<List<NodeRef>, Boolean> getChildNodes(NodeRef parentRef, int limit) {
        if (parentRef == null) {
            return Pair.newInstance(Collections.<NodeRef> emptyList(), false);
        }
        return getDocumentSearchService().searchAllDocumentsByParentRef(parentRef, limit);
    }

    @Override
    public String cancel() {
        locationNode = null;
        parentVolume = null;
        parentCase = null;
        getPropertySheetStateBean().reset(null, null);
        return super.cancel();
    }

    @Override
    public String getContainerTitle() {
        if (parentVolume != null && parentVolume.isContainsCases()) {
            return MessageUtil.getMessage("document_case_and_document_list");
        }

        return MessageUtil.getMessage("document_list");
    }

    @Override
    public String getListTitle() {
        if (parentCase != null) {
            return parentCase.getTitle();
        } else if (parentVolume != null) {
            return MessageUtil.getMessage("document_list_title", parentVolume.getVolumeMark(), parentVolume.getTitle());
        } else {
            return "";
        }
    }

    public CustomChildrenCreator getDocumentRowFileGenerator() {
        return ComponentUtil.getDocumentRowFileGenerator(FacesContext.getCurrentInstance().getApplication(), 5);
    }

    @Override
    public boolean isShowCheckboxes() {
        if (!BeanHelper.getUserService().isDocumentManager()) {
            return false;
        }
        GeneralService generalService = BeanHelper.getGeneralService();
        if (parentCase != null) {
            if (generalService.isArchivalsStoreRef(parentCase.getNode().getNodeRef().getStoreRef())) {
                return true;
            }
            return DocListUnitStatus.OPEN.getValueName().equals(parentCase.getStatus());
        } else if (parentVolume != null) {
            if (generalService.isArchivalsStoreRef(parentVolume.getNode().getNodeRef().getStoreRef())) {
                return true;
            }
            return DocListUnitStatus.OPEN.getValueName().equals(parentVolume.getStatus());
        }
        return false;
    }

    public PropertySheetConfigElement getLocationNodeConfig() {
        return getConfig().getPropertySheetConfigElement();
    }

    public Node getLocationNode() {
        if (locationNode == null) {
            locationNode = new WmNode(null, DocumentCommonModel.Types.DOCUMENT);
        }
        return locationNode;
    }

    public void setLocationNode(Node locationNode) {
        this.locationNode = locationNode;
    }

    private void resetModals() {
        String bindingName = getPropertySheetBindingName();
        UIPropertySheet propertySheetComponent = (UIPropertySheet) getJsfBindingHelper().getComponentBinding(bindingName);
        if (propertySheetComponent != null && !propertySheetComponent.getChildren().isEmpty()) {
            return;
        }
        final FacesContext context = FacesContext.getCurrentInstance();
        final Application application = context.getApplication();

        // Access restriction change reason
        DocumentLocationModalComponent locationModal = new DocumentLocationModalComponent();
        locationModal.setActionListener(application.createMethodBinding("#{DialogManager.bean.massChangeDocLocation}", UIActions.ACTION_CLASS_ARGS));
        List<UIComponent> modalChildren = ComponentUtil.getChildren(locationModal);
        propertySheetComponent = generatePropSheet();
        getJsfBindingHelper().addBinding(bindingName, propertySheetComponent);

        modalChildren.clear();
        modalChildren.add(propertySheetComponent);

        List<UIComponent> children = ComponentUtil.getChildren(getPanel());
        children.clear();
        children.add(locationModal);
    }

    private UIPropertySheet generatePropSheet() {
        FacesContext context = FacesContext.getCurrentInstance();
        Application application = context.getApplication();
        SimUIPropertySheet sheet = new SimUIPropertySheet();
        sheet.setId("doc-metadata");
        sheet.setValidationEnabled(false);
        sheet.setMode("edit");
        Map<String, Object> sheetAttributes = ComponentUtil.getAttributes(sheet);
        sheetAttributes.put("externalConfig", Boolean.TRUE);
        sheetAttributes.put("labelStyleClass", "propertiesLabel wrap");
        sheetAttributes.put("columns", 1);
        sheet.setValueBinding("binding", application.createValueBinding("#{DialogManager.bean.propSheet}")); // this is friggin important!!
        sheet.setValueBinding("config", application.createValueBinding("#{DialogManager.bean.locationNodeConfig}"));
        sheet.setValueBinding("value", application.createValueBinding("#{DialogManager.bean.locationNode}"));
        return sheet;
    }

    @Override
    public void clean() {
        super.clean();
        parentVolume = null;
        parentCase = null;
        locationNode = null;
        config = null;
        getJsfBindingHelper().removeBinding(getPropertySheetBindingName());
        selectedDocs = null;
    }

    // END: jsf actions/accessors

    // START: getters / setters

    protected VolumeService getVolumeService() {
        if (volumeService == null) {
            volumeService = BeanHelper.getVolumeService();
        }
        return volumeService;
    }

    protected CaseService getCaseService() {
        if (caseService == null) {
            caseService = BeanHelper.getCaseService();
        }
        return caseService;
    }

    @Override
    public UIPropertySheet getPropertySheet() {
        return (UIPropertySheet) getJsfBindingHelper().getComponentBinding(getPropertySheetBindingName());
    }

    public void setPropSheet(UIPropertySheet propSheet) {
        getJsfBindingHelper().addBinding(getPropertySheetBindingName(), propSheet);
    }

    // END: getters / setters

    @Override
    public DocumentDynamic getDocument() {
        return null;
    }

    @Override
    public Node getNode() {
        return getLocationNode();
    }

    @Override
    public boolean isInEditMode() {
        return true;
    }

    @Override
    public void switchMode(boolean inEditMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CaseFile getCaseFile() {
        throw new RuntimeException("Not used!");
    }

}
