package ee.webmedia.alfresco.document.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentConfigService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDynamicService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPropertySheetStateBean;

<<<<<<< HEAD
=======
import java.util.ArrayList;
>>>>>>> develop-5.1
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

<<<<<<< HEAD
=======
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
>>>>>>> develop-5.1
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
<<<<<<< HEAD
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
=======
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
>>>>>>> develop-5.1

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
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentServiceImpl;
import ee.webmedia.alfresco.document.web.evaluator.IsAdminOrDocManagerEvaluator;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
<<<<<<< HEAD
=======
import ee.webmedia.alfresco.utils.UnableToPerformMultiReasonException;
>>>>>>> develop-5.1
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.service.VolumeService;
<<<<<<< HEAD
=======
import ee.webmedia.alfresco.volume.web.VolumeListDialog;
>>>>>>> develop-5.1

/**
 * Form backing bean for Document list. <br>
 * <br>
 * This Class has logic of two diferent, but similar versions of documents(when parent is volume or case). <br>
 * Reason is that we don't have to worry about what the parent of document in jsp files.
<<<<<<< HEAD
 * 
 * @author Ats Uiboupin
=======
>>>>>>> develop-5.1
 */
public class DocumentListDialog extends BaseDocumentListDialog implements DialogDataProvider {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "DocumentListDialog";

    private static final String VOLUME_NODE_REF = "volumeNodeRef";
    private static final String CASE_NODE_REF = "caseNodeRef";
<<<<<<< HEAD
=======

    private static final Log LOG = LogFactory.getLog(DocumentListDialog.class);
>>>>>>> develop-5.1
    private transient VolumeService volumeService;
    private transient CaseService caseService;

    // one of the following should always be null(depending of whether it is directly under volume or under case, that is under volume)
    private Volume parentVolume;
    private Case parentCase;

    private Node locationNode;

    private DocumentConfig config;

    private transient UIPropertySheet propSheet;

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
            parentVolume = getVolumeService().getVolumeByNodeRef(parentRef);
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
<<<<<<< HEAD
=======
            if (!nodeExists(new NodeRef(param))) {
                return;
            }
>>>>>>> develop-5.1
            parentVolume = getVolumeService().getVolumeByNodeRef(param);
            parentCase = null;
        } else {
            param = ActionUtil.getParam(event, CASE_NODE_REF);
<<<<<<< HEAD
=======
            if (!nodeExists(new NodeRef(param))) {
                return;
            }
>>>>>>> develop-5.1
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

<<<<<<< HEAD
=======
    public String action() {
        String dialogPrefix = AlfrescoNavigationHandler.DIALOG_PREFIX;
        if (parentVolume == null && parentCase == null) {
            MessageUtil.addInfoMessage("volume_noderef_not_found");
            return dialogPrefix + VolumeListDialog.DIALOG_NAME;
        }
        return dialogPrefix + "documentListDialog";
    }

>>>>>>> develop-5.1
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
<<<<<<< HEAD
=======
        if (documentTypeIds.size() == 0) {
            MessageUtil.addErrorMessage("document_move_none_selected");
            showDocumentsLocationPopup = false;
            return;
        }
>>>>>>> develop-5.1
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
<<<<<<< HEAD
        massChangeDocLocationSave();
    }

    @SuppressWarnings("unchecked")
    public void massChangeDocLocationSave() {
        Map<String, Object> locationProps = getLocationNode().getProperties();
        NodeRef function = (NodeRef) locationProps.get(DocumentCommonModel.Props.FUNCTION.toString());
        NodeRef series = (NodeRef) locationProps.get(DocumentCommonModel.Props.SERIES.toString());
        NodeRef volume = (NodeRef) locationProps.get(DocumentCommonModel.Props.VOLUME.toString());
        String caseLabel = (String) locationProps.get(DocumentLocationGenerator.CASE_LABEL_EDITABLE);
        Set<NodeRef> updatedNodeRefs = new HashSet<NodeRef>();

        if (selectedDocs == null) {
            selectedDocs = (List<NodeRef>) CollectionUtils.collect(getListCheckboxes().entrySet(), new Transformer() {

                @Override
                public Object transform(Object input) {
                    Entry<NodeRef, Boolean> item = (Entry<NodeRef, Boolean>) input;
                    if (Boolean.TRUE.equals(item.getValue())) {
                        return item.getKey();
                    }
                    return null;
                }
            });
            selectedDocs.remove(null);
=======
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
>>>>>>> develop-5.1
        }

        if (isCreateNewCaseFile(volume)) {
            BeanHelper.getCaseFileDialog().createCaseFile((String) locationNode.getProperties().get(DocumentLocationGenerator.CASE_FILE_TYPE_PROP), locationNode, selectedDocs);
            return;
        }

        int processed = 0;
        String currentDocName = null;
        try {
<<<<<<< HEAD
            for (NodeRef docRef : selectedDocs) {
                if (docRef == null || updatedNodeRefs.contains(docRef)) {
                    // document was already moved as followup or reply document of some selected document
                    continue;
                }
                DocumentDynamic document = getDocumentDynamicService().getDocument(docRef);
                currentDocName = document.getDocName();
                DocumentConfig cfg = getDocumentConfigService().getConfig(document.getNode());
                document.setFunction(function);
                document.setSeries(series);
                document.setVolume(volume);
                document.setCase(null);
                document.getNode().getProperties().put(DocumentLocationGenerator.CASE_LABEL_EDITABLE.toString(), caseLabel);
                List<Pair<NodeRef, NodeRef>> updatedRefs = getDocumentDynamicService().updateDocumentGetDocAndNodeRefs(document, cfg.getSaveListenerBeanNames(), true, true)
                        .getSecond();
                for (Pair<NodeRef, NodeRef> pair : updatedRefs) {
                    updatedNodeRefs.add(pair.getFirst());
                    updatedNodeRefs.add(pair.getSecond());
                }
=======
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
>>>>>>> develop-5.1
                processed++;
            }
        } catch (NodeLockedException e) {
            BeanHelper.getDocumentLockHelperBean().handleLockedNode("document_location_change_error_assoc_locked", e.getNodeRef(), new Object[] { processed, currentDocName });
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(FacesContext.getCurrentInstance(), e);
<<<<<<< HEAD
=======
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
>>>>>>> develop-5.1
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

<<<<<<< HEAD
=======
    private void updateDocumentInMassChangeLocation(NodeRef function, NodeRef series, NodeRef volume, String caseLabel, Set<NodeRef> updatedNodeRefs, NodeRef docRef) {
        DocumentDynamic document = getDocumentDynamicService().getDocument(docRef);
        DocumentConfig cfg = getDocumentConfigService().getConfig(document.getNode());
        document.setFunction(function);
        document.setSeries(series);
        document.setVolume(volume);
        document.setCase(null);
        document.getNode().getProperties().put(DocumentLocationGenerator.CASE_LABEL_EDITABLE.toString(), caseLabel);
        List<Pair<NodeRef, NodeRef>> updatedRefs = getDocumentDynamicService().updateDocumentGetDocAndNodeRefs(document, cfg.getSaveListenerBeanNames(), true, true)
                .getSecond();
        for (Pair<NodeRef, NodeRef> pair : updatedRefs) {
            updatedNodeRefs.add(pair.getFirst());
            updatedNodeRefs.add(pair.getSecond());
        }
    }

>>>>>>> develop-5.1
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
        BeanHelper.getVisitedDocumentsBean().resetVisitedDocuments(documents);
    }

    private void doInitialSearch() {
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
        documents = setLimited(getChildNodes(parentRef, getLimit()));
<<<<<<< HEAD
=======
        final boolean debugEnabled = LOG.isDebugEnabled();
        if (debugEnabled) {
            LOG.debug("Found " + documents.size() + " document(s) during initial search. Limit: " + getLimit());
        }
>>>>>>> develop-5.1

        // Because documents are fetched from search, the results may not be accurate if indexing is done in background
        // and mass change location was done during the last few seconds
        // Therefore filter out documents, that are not under this volume or case
<<<<<<< HEAD
        for (Iterator<Document> i = documents.iterator(); i.hasNext();) {
            Document document = i.next();
            if (parentCase != null) {
                NodeRef caseRef = (NodeRef) document.getProperties().get(DocumentCommonModel.Props.CASE.toString());
                if (!parentCase.getNode().getNodeRef().equals(caseRef)) {
                    i.remove();
                }
            } else {
                NodeRef volumeRef = (NodeRef) document.getProperties().get(DocumentCommonModel.Props.VOLUME.toString());
                if (!parentVolume.getNode().getNodeRef().equals(volumeRef)) {
                    i.remove();
                }
            }
        }
=======
        List<Document> removedDocs = null;
        if (debugEnabled) {
            removedDocs = new ArrayList<Document>();
        }
        for (Iterator<Document> i = documents.iterator(); i.hasNext();) {
            Document document = i.next();
            QName parentRefProperty = (parentCase != null) ? DocumentCommonModel.Props.CASE : DocumentCommonModel.Props.VOLUME;

            NodeRef documentParentRef = (NodeRef) document.getProperties().get(parentRefProperty.toString());
            if (!parentRef.equals(documentParentRef)) {
                i.remove();
                if (debugEnabled) {
                    removedDocs.add(document);
                }
            }
        }
        if (debugEnabled) {
            LOG.debug("Removed " + removedDocs.size() + " documents from " + parentRef + " children listing during filtering: " + removedDocs.toString());
        }
>>>>>>> develop-5.1

        Collections.sort(documents); // always sort, because at first user gets only limited amount of documents;
        // and if user presses show all, then he/she knows it will take time
        resetModals();
        clearRichList();
    }

    private Pair<List<Document>, Boolean> getChildNodes(NodeRef parentRef, int limit) {
        if (parentRef == null) {
            return Pair.newInstance(Collections.<Document> emptyList(), false);
        }
        return getDocumentService().searchAllDocumentsByParentNodeRef(parentRef, limit);
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
        return ComponentUtil.getDocumentRowFileGenerator(FacesContext.getCurrentInstance().getApplication());
    }

    @Override
    public boolean isShowCheckboxes() {
        if (!new IsAdminOrDocManagerEvaluator().evaluate(getNode())) {
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
        if (propSheet != null) {
            return;
        }
        final FacesContext context = FacesContext.getCurrentInstance();
        final Application application = context.getApplication();

        // Access restriction change reason
        DocumentLocationModalComponent locationModal = new DocumentLocationModalComponent();
        locationModal.setActionListener(application.createMethodBinding("#{DialogManager.bean.massChangeDocLocation}", UIActions.ACTION_CLASS_ARGS));
        List<UIComponent> modalChildren = ComponentUtil.getChildren(locationModal);
        propSheet = generatePropSheet();
        modalChildren.clear();
        modalChildren.add(propSheet);

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
        return propSheet;
    }

    public void setPropSheet(UIPropertySheet propSheet) {
        this.propSheet = propSheet;
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
