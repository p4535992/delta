package ee.webmedia.alfresco.docconfig.generator.systematic;

import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.CASE;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.FUNCTION;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.REG_NUMBER;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.SERIES;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.VOLUME;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesListener;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;
import org.hibernate.StaleObjectStateException;

import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.propertysheet.converter.NodeRefConverter;
import ee.webmedia.alfresco.common.propertysheet.suggester.SuggesterGenerator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.generator.BasePropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.BaseSystematicFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.FieldGroupGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;
import ee.webmedia.alfresco.docconfig.generator.systematic.AccessRestrictionGenerator.AccessRestrictionState;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentParentNodesVO;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * @author Alar Kvell
 */
public class DocumentLocationGenerator extends BaseSystematicFieldGenerator implements FieldGroupGenerator {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentLocationGenerator.class);

    // TODO DLSeadist: ensure that everything works according to originalFieldId<-->fieldId

    @Override
    protected String[] getOriginalFieldIds() {
        return new String[] {
                FUNCTION.getLocalName(),
                SERIES.getLocalName(),
                VOLUME.getLocalName(),
                CASE.getLocalName() };
    }

    /*
     * In EDIT_MODE we use properties:
     * docdyn:function - NodeRef
     * docdyn:series - NodeRef
     * docdyn:volume - NodeRef
     * {temp}caseLabelEditable - String
     * .
     * In VIEW_MODE we use properties:
     * {temp}functionLabel - String
     * {temp}seriesLabel - String
     * {temp}volumeLabel - String
     * {temp}caseLabel - String
     */

    public static final QName FUNCTION_LABEL = RepoUtil.createTransientProp("functionLabel");
    public static final QName SERIES_LABEL = RepoUtil.createTransientProp("seriesLabel");
    public static final QName VOLUME_LABEL = RepoUtil.createTransientProp("volumeLabel");
    public static final QName CASE_LABEL = RepoUtil.createTransientProp("caseLabel");
    public static final QName CASE_LABEL_EDITABLE = RepoUtil.createTransientProp("caseLabelEditable");

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        final ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        if (field.getOriginalFieldId().equals(FUNCTION.getLocalName())) {
            item.setComponentGenerator("GeneralSelectorGenerator");
            item.setConverter(NodeRefConverter.class.getName());
            item.setSelectionItems(getBindingName("getFunctions"));
            item.setValueChangeListener(getBindingName("functionValueChanged"));
            item.setShowInViewMode(false);

            generatorResults.generateAndAddViewModeText(FUNCTION_LABEL.toString(), field.getName());
            return;
        } else if (field.getOriginalFieldId().equals(SERIES.getLocalName())) {
            item.setComponentGenerator("GeneralSelectorGenerator");
            item.setConverter(NodeRefConverter.class.getName());
            item.setSelectionItems(getBindingName("getSeries"));
            item.setValueChangeListener(getBindingName("seriesValueChanged"));
            item.setShowInViewMode(false);

            generatorResults.generateAndAddViewModeText(SERIES_LABEL.toString(), field.getName());
            return;
        } else if (field.getOriginalFieldId().equals(VOLUME.getLocalName())) {
            item.setComponentGenerator("GeneralSelectorGenerator");
            item.setConverter(NodeRefConverter.class.getName());
            item.setSelectionItems(getBindingName("getVolumes"));
            item.setValueChangeListener(getBindingName("volumeValueChanged"));
            item.setShowInViewMode(false);

            generatorResults.generateAndAddViewModeText(VOLUME_LABEL.toString(), field.getName());
            return;
        } else if (field.getOriginalFieldId().equals(CASE.getLocalName())) {
            item.setName(CASE_LABEL_EDITABLE.toString());
            item.setForcedMandatory(true);
            item.setComponentGenerator("SuggesterGenerator");
            item.setSuggesterValues(getBindingName("getCases"));
            item.setValueChangeListener(getBindingName("volumeValueChanged"));
            item.setStyleClass("long");
            item.setDontRenderIfDisabled(true);
            item.setShowInViewMode(false);

            generatorResults.generateAndAddViewModeText(CASE_LABEL.toString(), field.getName());
            return;
        }
        throw new RuntimeException("Unsupported field: " + field);
    }

    @Override
    public void generateFieldGroup(FieldGroup fieldGroup, GeneratorResults generatorResults) {
        generatorResults.addStateHolder(getStateHolderKey(), new DocumentLocationState());
    }

    // ===============================================================================================================================

    public static class DocumentLocationState extends BasePropertySheetStateHolder {
        private static final long serialVersionUID = 1L;

        private List<SelectItem> functions;
        private List<SelectItem> series;
        private List<SelectItem> volumes;
        private List<String> cases;

        @Override
        public void reset(boolean inEditMode) {
            final Node document = dialogDataProvider.getNode();
            if (inEditMode) {
                NodeRef functionRef = (NodeRef) document.getProperties().get(FUNCTION);
                NodeRef seriesRef = (NodeRef) document.getProperties().get(SERIES);
                NodeRef volumeRef = (NodeRef) document.getProperties().get(VOLUME);
                NodeRef caseRef = (NodeRef) document.getProperties().get(CASE);
                String caseLabel = null;
                if (caseRef != null) {
                    caseLabel = BeanHelper.getCaseService().getCaseByNoderef(caseRef).getTitle();
                }
                updateFnSerVol(functionRef, seriesRef, volumeRef, caseLabel, true);
            } else {
                final DocumentParentNodesVO documentParentNodesVO = BeanHelper.getDocumentService().getAncestorNodesByDocument(document.getNodeRef());
                Node functionNode = documentParentNodesVO.getFunctionNode();
                Node seriesNode = documentParentNodesVO.getSeriesNode();
                Node caseNode = documentParentNodesVO.getCaseNode();
                Node volumeNode = documentParentNodesVO.getVolumeNode();
                String functionLbl = functionNode != null ? functionNode.getProperties().get(FunctionsModel.Props.MARK).toString() //
                        + " " + functionNode.getProperties().get(FunctionsModel.Props.TITLE).toString() : null;
                String seriesLbl = seriesNode != null ? seriesNode.getProperties().get(SeriesModel.Props.SERIES_IDENTIFIER).toString() //
                        + " " + seriesNode.getProperties().get(SeriesModel.Props.TITLE).toString() : null;
                String volumeLbl = volumeNode != null ? volumeNode.getProperties().get(VolumeModel.Props.MARK).toString() //
                        + " " + volumeNode.getProperties().get(VolumeModel.Props.TITLE).toString() : null;
                String caseLbl = caseNode != null ? caseNode.getProperties().get(CaseModel.Props.TITLE).toString() : null;
                document.getProperties().put(FUNCTION_LABEL.toString(), functionLbl);
                document.getProperties().put(SERIES_LABEL.toString(), seriesLbl);
                document.getProperties().put(VOLUME_LABEL.toString(), volumeLbl);
                if (caseLbl != null) {
                    document.getProperties().put(CASE_LABEL.toString(), caseLbl);
                }
            }
        }

        /**
         * @param context
         * @param selectComponent
         * @return dropdown items for JSP
         */
        public List<SelectItem> getFunctions(FacesContext context, UIInput selectComponent) {
            return functions;
        }

        /**
         * @param context
         * @param selectComponent
         * @return dropdown items for JSP
         */
        public List<SelectItem> getSeries(FacesContext context, UIInput selectComponent) {
            return series;
        }

        /**
         * @param context
         * @param selectComponent
         * @return dropdown items for JSP
         */
        public List<SelectItem> getVolumes(FacesContext context, UIInput selectComponent) {
            return volumes;
        }

        /**
         * @param context
         * @param selectComponent
         * @return dropdown items for JSP
         */
        public List<String> getCases(FacesContext context, UIInput selectComponent) {
            return cases;
        }

        public void functionValueChanged(ValueChangeEvent event) {
            NodeRef functionRef = (NodeRef) event.getNewValue();
            updateFnSerVol(functionRef, null, null, null, false);
        }

        public void seriesValueChanged(ValueChangeEvent event) {
            Node document = dialogDataProvider.getNode();
            NodeRef functionRef = (NodeRef) document.getProperties().get(FUNCTION);
            NodeRef seriesRef = (NodeRef) event.getNewValue();
            updateFnSerVol(functionRef, seriesRef, null, null, false);
        }

        public void volumeValueChanged(ValueChangeEvent event) {
            Node document = dialogDataProvider.getNode();
            NodeRef functionRef = (NodeRef) document.getProperties().get(FUNCTION);
            NodeRef seriesRef = (NodeRef) document.getProperties().get(SERIES);
            NodeRef volumeRef = (NodeRef) event.getNewValue();
            updateFnSerVol(functionRef, seriesRef, volumeRef, null, false);
        }

        private void updateFnSerVol(NodeRef functionRef, NodeRef seriesRef, NodeRef volumeRef, String caseLabel, boolean addIfMissing) {
            Node document = dialogDataProvider.getNode();
            UIPropertySheet ps = dialogDataProvider.getPropertySheet();

            { // Function
                List<Function> allFunctions = BeanHelper.getFunctionsService().getAllFunctions(DocListUnitStatus.OPEN);
                functions = new ArrayList<SelectItem>(allFunctions.size());
                functions.add(new SelectItem("", ""));
                boolean functionFound = false;
                for (Function function : allFunctions) {
                    List<Series> openSeries = BeanHelper.getSeriesService().getAllSeriesByFunction(function.getNodeRef(), DocListUnitStatus.OPEN, document.getType());
                    if (openSeries.size() == 0) {
                        continue;
                    }
                    functions.add(new SelectItem(function.getNode().getNodeRef(), function.getMark() + " " + function.getTitle()));
                    if (functionRef != null && functionRef.equals(function.getNode().getNodeRef())) {
                        functionFound = true;
                    }
                }
                if (!functionFound) {
                    if (addIfMissing && functionRef != null && BeanHelper.getNodeService().exists(functionRef)) {
                        Function function = BeanHelper.getFunctionsService().getFunctionByNodeRef(functionRef);
                        functions.add(1, new SelectItem(function.getNode().getNodeRef(), function.getMark() + " " + function.getTitle()));
                    } else {
                        functionRef = null;
                    }
                }
                // If list contains only one value, then select it right away
                if (functions.size() == 2) {
                    functions.remove(0);
                    if (functionRef == null) {
                        functionRef = (NodeRef) functions.get(0).getValue();
                    }
                }
            }

            if (functionRef == null) {
                series = null;
                seriesRef = null;
            } else {
                List<Series> allSeries = BeanHelper.getSeriesService().getAllSeriesByFunction(functionRef, DocListUnitStatus.OPEN, document.getType());
                series = new ArrayList<SelectItem>(allSeries.size());
                series.add(new SelectItem("", ""));
                boolean serieFound = false;
                for (Series serie : allSeries) {
                    series.add(new SelectItem(serie.getNode().getNodeRef(), serie.getSeriesIdentifier() + " " + serie.getTitle()));
                    if (seriesRef != null && seriesRef.equals(serie.getNode().getNodeRef())) {
                        serieFound = true;
                    }
                }
                if (!serieFound) {
                    if (addIfMissing && seriesRef != null && BeanHelper.getNodeService().exists(seriesRef)) {
                        Series serie = BeanHelper.getSeriesService().getSeriesByNodeRef(seriesRef);
                        series.add(1, new SelectItem(serie.getNode().getNodeRef(), serie.getSeriesIdentifier() + " " + serie.getTitle()));
                    } else {
                        seriesRef = null;
                    }
                }
                // If list contains only one value, then select it right away
                if (series.size() == 2) {
                    series.remove(0);
                    if (seriesRef == null) {
                        seriesRef = (NodeRef) series.get(0).getValue();
                    }
                }
            }

            if (seriesRef == null) {
                volumes = null;
                volumeRef = null;
            } else {
                if (ps == null) { // when metadata block is first rendered
                    updateAccessRestrictionProperties(seriesRef);
                } else { // when value change event is fired
                    final NodeRef finalSeriesRef = seriesRef;
                    ActionEvent event = new ActionEvent(ps) {
                        private static final long serialVersionUID = 1L;

                        boolean notExecuted = true;

                        @Override
                        public void processListener(FacesListener faceslistener) {
                            notExecuted = false;
                            updateAccessRestrictionProperties(finalSeriesRef);
                        }

                        @Override
                        public boolean isAppropriateListener(FacesListener faceslistener) {
                            return notExecuted;
                        }
                    };
                    event.setPhaseId(PhaseId.INVOKE_APPLICATION);
                    ps.queueEvent(event);
                }

                List<Volume> allVolumes = BeanHelper.getVolumeService().getAllValidVolumesBySeries(seriesRef, DocListUnitStatus.OPEN);
                volumes = new ArrayList<SelectItem>(allVolumes.size());
                volumes.add(new SelectItem("", ""));
                boolean volumeFound = false;
                for (Volume volume : allVolumes) {
                    volumes.add(new SelectItem(volume.getNode().getNodeRef(), volume.getVolumeMark() + " " + volume.getTitle()));
                    if (volumeRef != null && volumeRef.equals(volume.getNode().getNodeRef())) {
                        volumeFound = true;
                    }
                }
                if (!volumeFound) {
                    if (addIfMissing && volumeRef != null && BeanHelper.getNodeService().exists(volumeRef)) {
                        Volume volume = BeanHelper.getVolumeService().getVolumeByNodeRef(volumeRef);
                        volumes.add(1, new SelectItem(volume.getNode().getNodeRef(), volume.getVolumeMark() + " " + volume.getTitle()));
                    } else {
                        volumeRef = null;
                    }
                }
                // If list contains only one value, then select it right away
                if (volumes.size() == 2) {
                    volumes.remove(0);
                    if (volumeRef == null) {
                        volumeRef = (NodeRef) volumes.get(0).getValue();
                    }
                }
            }

            if (volumeRef == null) {
                cases = null;
                caseLabel = null;
            } else {
                if (BeanHelper.getVolumeService().getVolumeByNodeRef(volumeRef).isContainsCases()) {
                    List<Case> allCases = BeanHelper.getCaseService().getAllCasesByVolume(volumeRef, DocListUnitStatus.OPEN);
                    cases = new ArrayList<String>(allCases.size());
                    for (Case tmpCase : allCases) {
                        cases.add(StringUtils.trim(tmpCase.getTitle()));
                    }
                    if (StringUtils.isBlank(caseLabel) && cases.size() == 1) {
                        caseLabel = StringUtils.trim(cases.get(0));
                    }
                } else {
                    cases = null;
                    caseLabel = null;
                }
            }

            if (ps != null) {
                @SuppressWarnings("unchecked")
                List<UIComponent> children = ps.getChildren();
                for (UIComponent component : children) {
                    if (component.getId().endsWith("_function")) {
                        HtmlSelectOneMenu functionList = (HtmlSelectOneMenu) component.getChildren().get(1);
                        ComponentUtil.setSelectItems(FacesContext.getCurrentInstance(), functionList, functions);
                        functionList.setValue(functionRef);
                    } else if (component.getId().endsWith("_series")) {
                        HtmlSelectOneMenu seriesList = (HtmlSelectOneMenu) component.getChildren().get(1);
                        ComponentUtil.setSelectItems(FacesContext.getCurrentInstance(), seriesList, series);
                        seriesList.setValue(seriesRef);
                    } else if (component.getId().endsWith("_volume")) {
                        HtmlSelectOneMenu volumeList = (HtmlSelectOneMenu) component.getChildren().get(1);
                        ComponentUtil.setSelectItems(FacesContext.getCurrentInstance(), volumeList, volumes);
                        volumeList.setValue(volumeRef);
                    } else if (component.getId().endsWith("_caseLabelEditable")) {
                        UIInput caseList = (UIInput) component.getChildren().get(1);
                        SuggesterGenerator.setValue(caseList, cases);
                        caseList.setValue(caseLabel);
                        component.setRendered(cases != null);
                    }
                }
            }

            // These only apply when called initially during creation of a new document
            // If called from eventlistener, then model values are updated after and thus overwritten
            document.getProperties().put(FUNCTION.toString(), functionRef);
            document.getProperties().put(SERIES.toString(), seriesRef);
            document.getProperties().put(VOLUME.toString(), volumeRef);
            document.getProperties().put(CASE_LABEL_EDITABLE.toString(), caseLabel);
        }

        private void updateAccessRestrictionProperties(NodeRef seriesRef) {
            AccessRestrictionState accessRestrictionState = dialogDataProvider.getStateHolder(AccessRestrictionGenerator.class.getName(), AccessRestrictionState.class);
            if (accessRestrictionState != null) {
                accessRestrictionState.updateAccessRestrictionProperties(seriesRef);
            }
        }

    }

    // ===============================================================================================================================

    private DocumentService documentService;
    private NodeService nodeService;
    private FunctionsService functionsService;
    private SeriesService seriesService;
    private VolumeService volumeService;
    private CaseService caseService;
    private DocumentLogService documentLogService;

    @Override
    public void validate(DocumentDynamic document, ValidationHelper validationHelper) {
        final Map<String, Object> props = document.getNode().getProperties();
        NodeRef functionRef = (NodeRef) props.get(FUNCTION);
        NodeRef seriesRef = (NodeRef) props.get(SERIES);
        NodeRef volumeRef = (NodeRef) props.get(VOLUME);
        if (functionRef == null || seriesRef == null || volumeRef == null) {
            if (log.isDebugEnabled()) {
                log.warn("validation failed: document_validationMsg_mandatory_functionSeriesVolume");
            }
            validationHelper.addErrorMessage("document_validationMsg_mandatory_functionSeriesVolume");
            return;
        }

        final List<String> messages = new ArrayList<String>(4);
        Volume volume = volumeService.getVolumeByNodeRef(volumeRef);

        String caseLabel = StringUtils.trimToNull((String) props.get(CASE_LABEL_EDITABLE));
        if (volume.isContainsCases() && StringUtils.isBlank(caseLabel)) {
            // client-side validation prevents it; but it reaches here if in-between rendering and submitting, someone else changes volume's containsCases=true
            if (log.isDebugEnabled()) {
                log.warn("validation failed: document_validationMsg_mandatory_case");
            }
            messages.add("document_validationMsg_mandatory_case");
        } else if (!volume.isContainsCases() && StringUtils.isNotBlank(caseLabel)) {
            caseLabel = null;
        }
        Case docCase = null;
        if (volume.isContainsCases() && StringUtils.isNotBlank(caseLabel)) {
            List<Case> allCases = caseService.getAllCasesByVolume(volumeRef);
            NodeRef caseRef = null;
            for (Case tmpCase : allCases) {
                if (StringUtils.equalsIgnoreCase(caseLabel, tmpCase.getTitle())) {
                    caseRef = tmpCase.getNode().getNodeRef();
                    docCase = tmpCase;
                    break;
                }
            }
            props.put(CASE.toString(), caseRef);
        }

        boolean isClosedUnitCheckNeeded = isClosedUnitCheckNeeded(document.getNodeRef(), documentService.getAncestorNodesByDocument(document.getNodeRef()), volumeRef, docCase);

        if (isClosedUnitCheckNeeded && DocListUnitStatus.CLOSED.equals(functionsService.getFunctionByNodeRef(functionRef).getStatus())) {
            messages.add("document_validationMsg_closed_function");
        }
        if (isClosedUnitCheckNeeded && DocListUnitStatus.CLOSED.equals(seriesService.getSeriesByNodeRef(seriesRef).getStatus())) {
            messages.add("document_validationMsg_closed_series");
        }
        if (isClosedUnitCheckNeeded && DocListUnitStatus.CLOSED.equals(volume.getStatus())) {
            messages.add("document_validationMsg_closed_volume");
        }
        if (isClosedUnitCheckNeeded && docCase != null && docCase.isClosed()) {
            if (log.isDebugEnabled()) {
                log.warn("validation failed: document_validationMsg_closed_case");
            }
            messages.add("document_validationMsg_closed_case");
        }

        props.put(CASE_LABEL_EDITABLE.toString(), caseLabel);
    }

    @Override
    public void save(DocumentDynamic document) {
        NodeRef docNodeRef = document.getNodeRef();

        // Prepare caseNodeRef
        final NodeRef volumeNodeRef = document.getVolume();
        NodeRef caseNodeRef = getCaseNodeRef(document, volumeNodeRef);

        // Prepare existingParentNode and targetParentRef properties
        final NodeRef targetParentRef;
        Node existingParentNode = null;
        if (caseNodeRef != null) {
            targetParentRef = caseNodeRef;
            existingParentNode = documentService.getCaseByDocument(docNodeRef);
            if (existingParentNode == null) { // moving from volume to case?
                existingParentNode = documentService.getVolumeByDocument(docNodeRef);
            }
        } else {
            targetParentRef = volumeNodeRef;
            final Volume volume = volumeService.getVolumeByNodeRef(targetParentRef);
            if (volume.isContainsCases()) {
                throw new RuntimeException("Selected volume '" + volume.getTitle() + "' must contain cases, not directly documents. Invalid caseNodeRef: '"
                        + caseNodeRef + "'");
            }
            existingParentNode = documentService.getVolumeByDocument(docNodeRef);
            if (existingParentNode == null) { // moving from case to volume?
                existingParentNode = documentService.getCaseByDocument(docNodeRef);
            }
        }

        // Prepare series and function properties
        NodeRef series = nodeService.getPrimaryParent(volumeNodeRef).getParentRef();
        if (series == null) {
            throw new RuntimeException("Volume parent is null: " + volumeNodeRef);
        }
        QName seriesType = nodeService.getType(series);
        if (!seriesType.equals(SeriesModel.Types.SERIES)) {
            throw new RuntimeException("Volume parent is not series, but " + seriesType + " - " + series);
        }
        NodeRef function = nodeService.getPrimaryParent(series).getParentRef();
        if (function == null) {
            throw new RuntimeException("Series parent is null: " + series);
        }
        QName functionType = nodeService.getType(function);
        if (!functionType.equals(FunctionsModel.Types.FUNCTION)) {
            throw new RuntimeException("Series parent is not function, but " + functionType + " - " + function);
        }
        document.setFunction(function);
        document.setSeries(series);
        document.setVolume(volumeNodeRef);
        document.setCase(caseNodeRef);

        if (existingParentNode == null || !targetParentRef.equals(existingParentNode.getNodeRef())) {
            // was not saved (under volume nor case) or saved, but parent (volume or case) must be changed
            Node previousCase = documentService.getCaseByDocument(docNodeRef);
            Node previousVolume = documentService.getVolumeByDocument(docNodeRef, previousCase);
            try {
                // Moving is executed with System user rights, because this is not appropriate to implement in permissions model
                documentService.updateParentNodesContainingDocsCount(docNodeRef, false);
                NodeRef newDocNodeRef = nodeService.moveNode(docNodeRef, targetParentRef //
                        , DocumentCommonModel.Assocs.DOCUMENT, DocumentCommonModel.Assocs.DOCUMENT).getChildRef();
                if (!newDocNodeRef.equals(docNodeRef)) {
                    throw new RuntimeException("NodeRef changed while moving");
                }
                documentService.updateParentNodesContainingDocsCount(docNodeRef, true);

                if (existingParentNode != null && !targetParentRef.equals(existingParentNode.getNodeRef())) {
                    if (documentService.isReplyOrFollowupDoc(docNodeRef, null)) {
                        throw new UnableToPerformException(MessageSeverity.ERROR, "document_errorMsg_register_movingNotEnabled_isReplyOrFollowUp");
                    }
                    final boolean isInitialDocWithRepliesOrFollowUps //
                    = nodeService.getSourceAssocs(docNodeRef, DocumentCommonModel.Assocs.DOCUMENT_REPLY).size() > 0 //
                            || nodeService.getSourceAssocs(docNodeRef, DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP).size() > 0;
                    if (isInitialDocWithRepliesOrFollowUps) {
                        throw new UnableToPerformException(MessageSeverity.ERROR, "document_errorMsg_register_movingNotEnabled_hasReplyOrFollowUp");
                    }
                    final String existingRegNr = (String) document.getProp(REG_NUMBER);
                    if (StringUtils.isNotBlank(existingRegNr)) {
                        // reg. number is changed if function, series or volume is changed
                        if (!previousVolume.getNodeRef().equals(volumeNodeRef)) {
                            documentService.registerDocumentRelocating(document.getNode());
                        }
                    }
                } else {
                    // Make sure that the node's volume is same as it's followUp's or reply's
                    List<AssociationRef> replies = nodeService.getTargetAssocs(docNodeRef, DocumentCommonModel.Assocs.DOCUMENT_REPLY);
                    List<AssociationRef> followUps = nodeService.getTargetAssocs(docNodeRef, DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP);
                    AssociationRef assoc = replies.size() > 0 ? replies.get(0) : followUps.size() > 0 ? followUps.get(0) : null;
                    if (assoc != null) {
                        NodeRef baseRef = assoc.getTargetRef();
                        Node baseCase = documentService.getCaseByDocument(baseRef);
                        Node baseVol = documentService.getVolumeByDocument(baseRef, baseCase);

                        if (!baseVol.getNodeRef().equals(volumeNodeRef)) {
                            throw new UnableToPerformException(MessageSeverity.ERROR, "document_errorMsg_register_movingNotEnabled_isReplyOrFollowUp");
                        }
                    }
                }
            } catch (UnableToPerformException e) {
                throw e;
            } catch (StaleObjectStateException e) {
                log.error("Failed to move document to volumes folder", e);
                throw new UnableToPerformException(MessageSeverity.ERROR, e.getMessage(), e);// NOT translated - occurs sometimes while debugging
            } catch (RuntimeException e) {
                log.error("Failed to move document to volumes folder", e);
                throw new UnableToPerformException(MessageSeverity.ERROR, "document_errorMsg_register_movingNotEnabled_isReplyOrFollowUp", e);
            }

            if (!document.isDraft()) {
                documentLogService.addDocumentLog(docNodeRef, MessageUtil.getMessage("document_log_location_changed"));
            }
        }
    }

    private boolean isClosedUnitCheckNeeded(NodeRef docRef, DocumentParentNodesVO parents, NodeRef volumeRef, Case docCase) {
        final QName parentType = nodeService.getType(nodeService.getPrimaryParent(docRef).getParentRef());
        final boolean isDraft = DocumentCommonModel.Types.DRAFTS.equals(parentType);
        return isDraft
                || !(volumeRef.equals(parents.getVolumeNode().getNodeRef())
                     && (parents.getCaseNode() == null ? docCase == null
                             : (docCase == null ? false
                                     : parents.getCaseNode().getNodeRef().equals(docCase.getNode().getNodeRef())
                              )
                         )
                     );
    }

    private NodeRef getCaseNodeRef(final DocumentDynamic document, final NodeRef volumeNodeRef) {
        NodeRef caseNodeRef = document.getCase();
        String caseLabel = document.getCaseLabelEditable();
        if (StringUtils.isBlank(caseLabel)) {
            caseNodeRef = null;
        }
        if (caseNodeRef != null) {
            return caseNodeRef;
        }
        if (StringUtils.isNotBlank(caseLabel)) {
            // find case by casLabel
            List<Case> allCases = caseService.getAllCasesByVolume(volumeNodeRef);
            for (Case tmpCase : allCases) {
                if (caseLabel.equalsIgnoreCase(tmpCase.getTitle())) {
                    caseNodeRef = tmpCase.getNode().getNodeRef();
                    break;
                }
            }
            if (caseNodeRef == null) {
                // create case
                Case tmpCase = caseService.createCase(volumeNodeRef);
                tmpCase.setTitle(caseLabel);
                caseService.saveOrUpdate(tmpCase, false);
                caseNodeRef = tmpCase.getNode().getNodeRef();
            }
        }
        document.setCase(caseNodeRef);
        return caseNodeRef;
    }

    // START: setters
    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setFunctionsService(FunctionsService functionsService) {
        this.functionsService = functionsService;
    }

    public void setSeriesService(SeriesService seriesService) {
        this.seriesService = seriesService;
    }

    public void setVolumeService(VolumeService volumeService) {
        this.volumeService = volumeService;
    }

    public void setCaseService(CaseService caseService) {
        this.caseService = caseService;
    }

    public void setDocumentLogService(DocumentLogService documentLogService) {
        this.documentLogService = documentLogService;
    }
    // END: setters

}
