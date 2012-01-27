package ee.webmedia.alfresco.document.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentConfigService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDynamicService;
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

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.config.PropertySheetConfigElement;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.propertysheet.component.SimUIPropertySheet;
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
import ee.webmedia.alfresco.document.web.evaluator.IsAdminOrDocManagerEvaluator;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * Form backing bean for Document list. <br>
 * <br>
 * This Class has logic of two diferent, but similar versions of documents(when parent is volume or case). <br>
 * Reason is that we don't have to worry about what the parent of document in jsp files.
 * 
 * @author Ats Uiboupin
 */
public class DocumentListDialog extends BaseDocumentListDialog implements DialogDataProvider {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "DocumentListDialog";

    private static final String VOLUME_NODE_REF = "volumeNodeRef";
    private static final String CASE_NODE_REF = "caseNodeRef";
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

    @Override
    public Object getActionsContext() {
        return parentVolume == null ? parentCase.getNode() : parentVolume.getNode();
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        getPropertySheetStateBean().reset(getConfig().getStateHolders(), this);
    }

    public void init(NodeRef parentRef) {
        QName type = getNodeService().getType(parentRef);
        getPropertySheetStateBean().reset(null, null);
        if (VolumeModel.Types.VOLUME.equals(type)) {
            parentVolume = getVolumeService().getVolumeByNodeRef(parentRef);
            parentCase = null;
        } else if (CaseModel.Types.CASE.equals(type)) {
            parentCase = getCaseService().getCaseByNoderef(parentRef);
            parentVolume = null;
        } else {
            throw new RuntimeException("Unsupported type: " + type);
        }
        restored();
        WebUtil.navigateTo(AlfrescoNavigationHandler.DIALOG_PREFIX + "documentListDialog");
    }

    public void setup(ActionEvent event) {
        final Map<String, String> parameterMap = ((UIActionLink) event.getSource()).getParameterMap();
        final String param;
        if (parameterMap.containsKey(VOLUME_NODE_REF)) {
            param = ActionUtil.getParam(event, VOLUME_NODE_REF);
            parentVolume = getVolumeService().getVolumeByNodeRef(param);
            parentCase = null;
        } else {
            param = ActionUtil.getParam(event, CASE_NODE_REF);
            parentCase = getCaseService().getCaseByNoderef(param);
            parentVolume = null;
        }
        restored();
        WebUtil.navigateTo(AlfrescoNavigationHandler.DIALOG_PREFIX + "documentListDialog");
    }

    public void updateLocationSelect(ActionEvent event) {
        Set<String> documentTypeIds = new HashSet<String>();
        DocumentDynamicService documentDynamicService = BeanHelper.getDocumentDynamicService();
        for (Entry<NodeRef, Boolean> entry : getListCheckboxes().entrySet()) {
            if (entry.getValue()) {
                documentTypeIds.add(documentDynamicService.getDocumentType(entry.getKey()));
            }
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
        if (!isValidLocation(function, series, volume, caseLabel)) {
            return;
        }
        // assume that current document list contains documents from one location, check location for first document only
        if (!getListCheckboxes().isEmpty()) {
            DocumentDynamic document = getDocumentDynamicService().getDocument(getListCheckboxes().keySet().iterator().next());
            if (hasSameLocation(document, function, series, volume, caseLabel)) {
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
        Map<String, Object> locationProps = getLocationNode().getProperties();
        NodeRef function = (NodeRef) locationProps.get(DocumentCommonModel.Props.FUNCTION.toString());
        NodeRef series = (NodeRef) locationProps.get(DocumentCommonModel.Props.SERIES.toString());
        NodeRef volume = (NodeRef) locationProps.get(DocumentCommonModel.Props.VOLUME.toString());
        String caseLabel = (String) locationProps.get(DocumentLocationGenerator.CASE_LABEL_EDITABLE);
        List<NodeRef> updatedNodeRefs = new ArrayList<NodeRef>();
        for (Entry<NodeRef, Boolean> entry : getListCheckboxes().entrySet()) {
            if (!entry.getValue()) {
                continue;
            }
            NodeRef docRef = entry.getKey();
            if (updatedNodeRefs.contains(docRef)) {
                // document was already moved as followup or reply document of some selected document
                continue;
            }
            DocumentDynamic document = getDocumentDynamicService().getDocument(docRef);
            DocumentConfig cfg = getDocumentConfigService().getConfig(document.getNode());
            document.setFunction(function);
            document.setSeries(series);
            document.setVolume(volume);
            document.setCase(null);
            document.getNode().getProperties().put(DocumentLocationGenerator.CASE_LABEL_EDITABLE.toString(), caseLabel);
            List<NodeRef> updatedRefs = getDocumentDynamicService().updateDocumentGetOriginalNodeRefs(document, cfg.getSaveListenerBeanNames());
            updatedNodeRefs.addAll(updatedRefs);
        }
        restored();
    }

    public void resetConfirmation(ActionEvent event) {
        confirmMoveAssociatedDocuments = false;
    }

    public boolean isConfirmMoveAssociatedDocuments() {
        return confirmMoveAssociatedDocuments;
    }

    public boolean isShowDocumentsLocationPopup() {
        return showDocumentsLocationPopup;
    }

    private boolean isValidLocation(NodeRef functionRef, NodeRef seriesRef, NodeRef volumeRef, String caseLabel) {
        if (functionRef == null || seriesRef == null || volumeRef == null) {
            return false;
        }
        Volume volume = BeanHelper.getVolumeService().getVolumeByNodeRef(volumeRef);
        if (volume.isContainsCases() && StringUtils.isBlank(caseLabel)) {
            return false;
        }
        return true;
    }

    private boolean hasSameLocation(DocumentDynamic document, NodeRef function, NodeRef series, NodeRef volume, String caseLabel) {
        NodeRef docFunction = document.getFunction();
        if (docFunction == null || !docFunction.equals(function)) {
            return false;
        }
        NodeRef docSeries = document.getSeries();
        if (docSeries == null || !docSeries.equals(series)) {
            return false;
        }
        NodeRef docVolume = document.getVolume();
        if (docVolume == null || !docVolume.equals(volume)) {
            return false;
        }
        NodeRef caseRef = document.getCase();
        boolean hasCaseLabel = !StringUtils.isBlank(caseLabel);
        if ((caseRef == null && hasCaseLabel) || (caseRef != null && !hasCaseLabel)) {
            return false;
        }
        if (caseRef != null && hasCaseLabel) {
            Case aCase = BeanHelper.getCaseService().getCaseByNoderef(caseRef);
            if (!caseLabel.equalsIgnoreCase(aCase.getTitle())) {
                return false;
            }
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
    public void restored() {
        final NodeRef parentRef;
        locationNode = null;
        setListCheckboxes(new HashMap<NodeRef, Boolean>());
        getPropertySheetStateBean().reset(getConfig().getStateHolders(), this);
        if (parentCase != null) {
            parentRef = parentCase.getNode().getNodeRef();
        } else {// assuming that parentVolume is volume
            parentRef = parentVolume.getNode().getNodeRef();
        }
        documents = getChildNodes(parentRef);
        if (documents.size() <= 2000) {// if sorting takes less than ca 6 sec (ca 15ms per document, 400doc*15ms==6sec)
            Collections.sort(documents);// sorting needs properties to be fetched from repo
        }
        resetModals();
        clearRichList();
    }

    private List<Document> getChildNodes(NodeRef parentRef) {
        List<ChildAssociationRef> childAssocs = getNodeService().getChildAssocs(parentRef, RegexQNamePattern.MATCH_ALL, RegexQNamePattern.MATCH_ALL);
        List<Document> docsOfParent = new ArrayList<Document>(childAssocs.size());
        for (ChildAssociationRef childAssocRef : childAssocs) {
            NodeRef docRef = childAssocRef.getChildRef();
            if (!DocumentCommonModel.Types.DOCUMENT.equals(getNodeService().getType(docRef))) { // XXX DLSeadist filter out old document types
                continue;
            }
            docsOfParent.add(new Document(docRef));
        }
        return docsOfParent;
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
    public String getListTitle() {
        if (parentCase != null) {
            return parentCase.getTitle();
        } else if (parentVolume != null) {
            return parentVolume.getVolumeMark() + " " + parentVolume.getTitle();
        } else {
            return "";
        }
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
        locationModal.setActionListener(application.createMethodBinding("#{DocumentListDialog.massChangeDocLocation}", UIActions.ACTION_CLASS_ARGS));
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
        sheet.setValueBinding("binding", application.createValueBinding("#{DocumentListDialog.propSheet}")); // this is friggin important!!
        sheet.setValueBinding("config", application.createValueBinding("#{DocumentListDialog.locationNodeConfig}"));
        sheet.setValueBinding("value", application.createValueBinding("#{DocumentListDialog.locationNode}"));
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

}
