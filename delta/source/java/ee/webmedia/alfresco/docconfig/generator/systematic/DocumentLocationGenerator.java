package ee.webmedia.alfresco.docconfig.generator.systematic;

import static ee.webmedia.alfresco.common.web.BeanHelper.getCaseService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getFunctionsService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPropertySheetStateBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSeriesService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getVolumeService;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.CASE;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.FUNCTION;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.REG_NUMBER;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.SERIES;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.VOLUME;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesListener;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;
import org.hibernate.StaleObjectStateException;

import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.propertysheet.converter.NodeRefConverter;
import ee.webmedia.alfresco.common.propertysheet.suggester.SuggesterGenerator;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.generator.BasePropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.BaseSystematicFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;
import ee.webmedia.alfresco.docconfig.generator.systematic.AccessRestrictionGenerator.AccessRestrictionState;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentParentNodesVO;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
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
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * @author Alar Kvell
 */
public class DocumentLocationGenerator extends BaseSystematicFieldGenerator {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentLocationGenerator.class);
    public static final String[] NODE_REF_FIELD_IDS = new String[] {
            FUNCTION.getLocalName(),
            SERIES.getLocalName(),
            VOLUME.getLocalName(),
            CASE.getLocalName() };

    @Override
    protected String[] getOriginalFieldIds() {
        return NODE_REF_FIELD_IDS;
    }

    public static final QName CASE_LABEL_EDITABLE = RepoUtil.createTransientProp(CASE.getLocalName() + "LabelEditable");
    public static final QName UPDATED_ASSOCIATED_DOCUMENTS = RepoUtil.createTransientProp("updatedAssociatedDocuments");
    public static final QName DOCUMENT_TYPE_IDS = RepoUtil.createTransientProp("documentTypeIds");

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

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        // Actually these fields cannot be used outside systematic group, because they have onlyInGroup=true
        // But let's leave this check in just in case
        if ((field.getParent() == null && !field.getFieldId().equals(field.getOriginalFieldId()))
                || (field.getParent() != null && (!(field.getParent() instanceof FieldGroup) || !((FieldGroup) field.getParent()).isSystematic()))) {
            generatorResults.getAndAddPreGeneratedItem();
            return;
        }

        QName functionProp;
        QName seriesProp;
        QName volumeProp;
        QName caseProp;
        if (field.getParent() != null) {
            Map<String, Field> fieldsByOriginalId = ((FieldGroup) field.getParent()).getFieldsByOriginalId();
            functionProp = getProp(fieldsByOriginalId, FUNCTION);
            seriesProp = getProp(fieldsByOriginalId, SERIES);
            volumeProp = getProp(fieldsByOriginalId, VOLUME);
            caseProp = getProp(fieldsByOriginalId, CASE);
        } else {
            functionProp = FUNCTION;
            seriesProp = SERIES;
            volumeProp = VOLUME;
            caseProp = CASE;
        }
        QName functionLabelProp = getTransientProp(functionProp, "Label");
        QName seriesLabelProp = getTransientProp(seriesProp, "Label");
        QName volumeLabelProp = getTransientProp(volumeProp, "Label");
        QName caseLabelProp = getTransientProp(caseProp, "Label");
        QName caseLabelEditableProp = getTransientProp(caseProp, "LabelEditable");
        String stateHolderKey = functionProp.getLocalName();

        final ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        if (field.getQName().equals(functionProp)) {
            item.setComponentGenerator("GeneralSelectorGenerator");
            item.setConverter(NodeRefConverter.class.getName());
            item.setSelectionItems(getBindingName("getFunctions", stateHolderKey));
            item.setValueChangeListener(getBindingName("functionValueChanged", stateHolderKey));
            item.setShowInViewMode(false);

            ItemConfigVO functionLabelItem = generatorResults.generateAndAddViewModeText(functionLabelProp.toString(), field.getName());
            functionLabelItem.setComponentGenerator("ActionLinkGenerator");
            functionLabelItem.setAction("dialog:seriesListDialog");
            functionLabelItem.setActionListener("#{SeriesListDialog.showAll}");
            functionLabelItem.setActionListenerParams("functionNodeRef=" + getBindingName("function", stateHolderKey));

            // Add stateholder
            generatorResults.addStateHolder(stateHolderKey, new DocumentLocationState(functionProp, seriesProp, caseProp, volumeProp, functionLabelProp, seriesLabelProp,
                    volumeLabelProp, caseLabelProp, caseLabelEditableProp));
            return;
        } else if (field.getQName().equals(seriesProp)) {
            item.setComponentGenerator("GeneralSelectorGenerator");
            item.setConverter(NodeRefConverter.class.getName());
            item.setSelectionItems(getBindingName("getSeries", stateHolderKey));
            item.setValueChangeListener(getBindingName("seriesValueChanged", stateHolderKey));
            item.setShowInViewMode(false);

            ItemConfigVO seriesLabelItem = generatorResults.generateAndAddViewModeText(seriesLabelProp.toString(), field.getName());
            seriesLabelItem.setComponentGenerator("ActionLinkGenerator");
            seriesLabelItem.setAction("dialog:volumeListDialog");
            seriesLabelItem.setActionListener("#{VolumeListDialog.showAll}");
            seriesLabelItem.setActionListenerParams("seriesNodeRef=" + getBindingName("series", stateHolderKey));
            return;
        } else if (field.getQName().equals(volumeProp)) {
            item.setComponentGenerator("GeneralSelectorGenerator");
            item.setConverter(NodeRefConverter.class.getName());
            item.setSelectionItems(getBindingName("getVolumes", stateHolderKey));
            item.setValueChangeListener(getBindingName("volumeValueChanged", stateHolderKey));
            item.setShowInViewMode(false);

            ItemConfigVO volumeLabelItem = generatorResults.generateAndAddViewModeText(volumeLabelProp.toString(), field.getName());
            volumeLabelItem.setComponentGenerator("ActionLinkGenerator");
            volumeLabelItem.setActionListener("#{VolumeListDialog.showVolumeContents}");
            volumeLabelItem.setActionListenerParams("volumeNodeRef=" + getBindingName("volume", stateHolderKey));
            return;
        } else if (field.getQName().equals(caseProp)) {
            item.setName(caseLabelEditableProp.toString());
            if (field.getParent() != null) {
                item.setForcedMandatory(true);
                item.setDontRenderIfDisabled(true);
            }
            item.setComponentGenerator("SuggesterGenerator");
            item.setSuggesterValues(getBindingName("getCases", stateHolderKey));
            item.setValueChangeListener(getBindingName("volumeValueChanged", stateHolderKey));
            item.setStyleClass("long");
            item.setShowInViewMode(false);

            ItemConfigVO caseLabelItem = generatorResults.generateAndAddViewModeText(caseLabelProp.toString(), field.getName());
            caseLabelItem.setComponentGenerator("ActionLinkGenerator");
            caseLabelItem.setAction("dialog:documentListDialog");
            caseLabelItem.setActionListener("#{DocumentListDialog.setup}");
            caseLabelItem.setActionListenerParams("caseNodeRef=" + getBindingName("case", stateHolderKey));
            return;
        }
        throw new RuntimeException("Unsupported field: " + field);
    }

    private QName getProp(Map<String, Field> fieldsByOriginalId, QName propName) {
        return fieldsByOriginalId.get(propName.getLocalName()).getQName();
    }

    private QName getTransientProp(QName propName, String suffix) {
        return RepoUtil.createTransientProp(propName.getLocalName() + suffix);
    }

    // ===============================================================================================================================

    public static class DocumentLocationState extends BasePropertySheetStateHolder {
        private static final long serialVersionUID = 1L;

        private final QName functionProp;
        private final QName seriesProp;
        private final QName caseProp;
        private final QName volumeProp;
        private final QName functionLabelProp;
        private final QName seriesLabelProp;
        private final QName volumeLabelProp;
        private final QName caseLabelProp;
        private final QName caseLabelEditableProp;

        private List<SelectItem> functions;
        private List<SelectItem> series;
        private List<SelectItem> volumes;
        private List<String> cases;

        public DocumentLocationState(QName functionProp, QName seriesProp, QName caseProp, QName volumeProp, QName functionLabelProp, QName seriesLabelProp, QName volumeLabelProp,
                                     QName caseLabelProp, QName caseLabelEditableProp) {
            this.functionProp = functionProp;
            this.seriesProp = seriesProp;
            this.caseProp = caseProp;
            this.volumeProp = volumeProp;
            this.functionLabelProp = functionLabelProp;
            this.seriesLabelProp = seriesLabelProp;
            this.volumeLabelProp = volumeLabelProp;
            this.caseLabelProp = caseLabelProp;
            this.caseLabelEditableProp = caseLabelEditableProp;
        }

        @Override
        public void reset(boolean inEditMode) {
            final Node document = dialogDataProvider.getNode();
            NodeRef functionRef = (NodeRef) document.getProperties().get(functionProp);
            NodeRef seriesRef = (NodeRef) document.getProperties().get(seriesProp);
            NodeRef volumeRef = (NodeRef) document.getProperties().get(volumeProp);
            NodeRef caseRef = (NodeRef) document.getProperties().get(caseProp);

            if (inEditMode) {
                String caseLabel = null;
                if (caseRef != null) {
                    caseLabel = getCaseLabel(getCaseService().getCaseByNoderef(caseRef));
                }
                DocumentDynamic documentDynamic = dialogDataProvider.getDocument();
                boolean updateAccessRestrictionProps = documentDynamic != null ? !documentDynamic.isDisableUpdateInitialAccessRestrictionProps() : false;
                updateFnSerVol(functionRef, seriesRef, volumeRef, caseLabel, true, updateAccessRestrictionProps);
            } else {
                String functionLbl = functionRef == null ? null : getFunctionLabel(getFunctionsService().getFunctionByNodeRef(functionRef));
                String seriesLbl = seriesRef == null ? null : getSeriesLabel(getSeriesService().getSeriesByNodeRef(seriesRef));
                String volumeLbl = volumeRef == null ? null : getVolumeLabel(getVolumeService().getVolumeByNodeRef(volumeRef));
                String caseLbl = caseRef == null ? null : getCaseLabel(getCaseService().getCaseByNoderef(caseRef));

                document.getProperties().put(functionLabelProp.toString(), functionLbl);
                document.getProperties().put(seriesLabelProp.toString(), seriesLbl);
                document.getProperties().put(volumeLabelProp.toString(), volumeLbl);
                if (caseLbl != null) {
                    document.getProperties().put(caseLabelProp.toString(), caseLbl);
                }
            }
        }

        public NodeRef getFunction() {
            return (NodeRef) dialogDataProvider.getNode().getProperties().get(functionProp);
        }

        public NodeRef getSeries() {
            return (NodeRef) dialogDataProvider.getNode().getProperties().get(seriesProp);
        }

        public NodeRef getVolume() {
            return (NodeRef) dialogDataProvider.getNode().getProperties().get(volumeProp);
        }

        public NodeRef getCase() {
            return (NodeRef) dialogDataProvider.getNode().getProperties().get(caseProp);
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
            updateFnSerVol(functionRef, null, null, null, false, true);
        }

        public void seriesValueChanged(ValueChangeEvent event) {
            Node document = dialogDataProvider.getNode();
            NodeRef functionRef = (NodeRef) document.getProperties().get(functionProp);
            NodeRef seriesRef = (NodeRef) event.getNewValue();
            updateFnSerVol(functionRef, seriesRef, null, null, false, true);
        }

        public void volumeValueChanged(ValueChangeEvent event) {
            Node document = dialogDataProvider.getNode();
            NodeRef functionRef = (NodeRef) document.getProperties().get(functionProp);
            NodeRef seriesRef = (NodeRef) document.getProperties().get(seriesProp);
            NodeRef volumeRef = (NodeRef) event.getNewValue();
            updateFnSerVol(functionRef, seriesRef, volumeRef, null, false, true);
        }

        private void updateFnSerVol(NodeRef functionRef, NodeRef seriesRef, NodeRef volumeRef, String caseLabel, boolean addIfMissing, boolean updateAccessRestrictionProperties) {
            Node document = dialogDataProvider.getNode();
            UIPropertySheet ps = dialogDataProvider.getPropertySheet();
            boolean isSearchFilter = DocumentSearchModel.Types.FILTER.equals(document.getType());

            String documentTypeId = (String) document.getProperties().get(DocumentAdminModel.Props.OBJECT_TYPE_ID);
            @SuppressWarnings("unchecked")
            Set<String> documentTypeIds = (Set<String>) document.getProperties().get(DOCUMENT_TYPE_IDS);
            Set<String> idList;
            if (documentTypeId != null) {
                idList = new HashSet<String>();
                idList.add(documentTypeId);
            } else {
                idList = documentTypeIds;
            }
            boolean isSearchFilterOrDocTypeNull = isSearchFilter || (documentTypeId == null && (documentTypeIds == null || documentTypeIds.isEmpty()));
            { // Function
                List<Function> allFunctions;
                if (isSearchFilter) {
                    allFunctions = getFunctionsService().getAllFunctions();
                } else {
                    allFunctions = getFunctionsService().getAllFunctions(DocListUnitStatus.OPEN);
                }
                functions = new ArrayList<SelectItem>(allFunctions.size());
                functions.add(new SelectItem("", ""));
                boolean functionFound = false;
                for (Function function : allFunctions) {
                    List<Series> openSeries;
                    if (isSearchFilterOrDocTypeNull) {
                        openSeries = getSeriesService().getAllSeriesByFunction(function.getNodeRef());
                    } else {
                        openSeries = getSeriesService().getAllSeriesByFunction(function.getNodeRef(), DocListUnitStatus.OPEN, idList);
                    }
                    if (openSeries.size() == 0) {
                        continue;
                    }
                    functions.add(new SelectItem(function.getNode().getNodeRef(), getFunctionLabel(function)));
                    if (functionRef != null && functionRef.equals(function.getNode().getNodeRef())) {
                        functionFound = true;
                    }
                }
                if (!functionFound) {
                    if (addIfMissing && functionRef != null && getNodeService().exists(functionRef)) {
                        Function function = getFunctionsService().getFunctionByNodeRef(functionRef);
                        functions.add(1, new SelectItem(function.getNode().getNodeRef(), getFunctionLabel(function)));
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
                List<Series> allSeries;
                if (isSearchFilterOrDocTypeNull) {
                    allSeries = getSeriesService().getAllSeriesByFunction(functionRef);
                } else {
                    allSeries = getSeriesService().getAllSeriesByFunction(functionRef, DocListUnitStatus.OPEN, idList);
                }
                series = new ArrayList<SelectItem>(allSeries.size());
                series.add(new SelectItem("", ""));
                boolean serieFound = false;
                for (Series serie : allSeries) {
                    series.add(new SelectItem(serie.getNode().getNodeRef(), getSeriesLabel(serie)));
                    if (seriesRef != null && seriesRef.equals(serie.getNode().getNodeRef())) {
                        serieFound = true;
                    }
                }
                if (!serieFound) {
                    if (addIfMissing && seriesRef != null && getNodeService().exists(seriesRef)) {
                        Series serie = getSeriesService().getSeriesByNodeRef(seriesRef);
                        series.add(1, new SelectItem(serie.getNode().getNodeRef(), getSeriesLabel(serie)));
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
                if (updateAccessRestrictionProperties) {
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
                }

                List<Volume> allVolumes;
                if (isSearchFilterOrDocTypeNull) {
                    allVolumes = getVolumeService().getAllValidVolumesBySeries(seriesRef);
                } else {
                    allVolumes = getVolumeService().getAllValidVolumesBySeries(seriesRef, DocListUnitStatus.OPEN);
                }
                volumes = new ArrayList<SelectItem>(allVolumes.size());
                volumes.add(new SelectItem("", ""));
                boolean volumeFound = false;
                for (Volume volume : allVolumes) {
                    volumes.add(new SelectItem(volume.getNode().getNodeRef(), getVolumeLabel(volume)));
                    if (volumeRef != null && volumeRef.equals(volume.getNode().getNodeRef())) {
                        volumeFound = true;
                    }
                }
                if (!volumeFound) {
                    if (addIfMissing && volumeRef != null && getNodeService().exists(volumeRef)) {
                        Volume volume = getVolumeService().getVolumeByNodeRef(volumeRef);
                        volumes.add(1, new SelectItem(volume.getNode().getNodeRef(), getVolumeLabel(volume)));
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
                if (getVolumeService().getVolumeByNodeRef(volumeRef).isContainsCases()) {
                    List<Case> allCases;
                    if (isSearchFilterOrDocTypeNull) {
                        allCases = getCaseService().getAllCasesByVolume(volumeRef);
                    } else {
                        allCases = getCaseService().getAllCasesByVolume(volumeRef, DocListUnitStatus.OPEN);
                    }
                    cases = new ArrayList<String>(allCases.size());
                    for (Case tmpCase : allCases) {
                        cases.add(getCaseLabel(tmpCase));
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
                    if (component.getId().endsWith("_" + functionProp.getLocalName())) {
                        HtmlSelectOneMenu functionList = (HtmlSelectOneMenu) component.getChildren().get(1);
                        ComponentUtil.setSelectItems(FacesContext.getCurrentInstance(), functionList, functions);
                        functionList.setValue(functionRef);
                    } else if (component.getId().endsWith("_" + seriesProp.getLocalName())) {
                        HtmlSelectOneMenu seriesList = (HtmlSelectOneMenu) component.getChildren().get(1);
                        ComponentUtil.setSelectItems(FacesContext.getCurrentInstance(), seriesList, series);
                        seriesList.setValue(seriesRef);
                    } else if (component.getId().endsWith("_" + volumeProp.getLocalName())) {
                        HtmlSelectOneMenu volumeList = (HtmlSelectOneMenu) component.getChildren().get(1);
                        ComponentUtil.setSelectItems(FacesContext.getCurrentInstance(), volumeList, volumes);
                        volumeList.setValue(volumeRef);
                    } else if (component.getId().endsWith("_" + caseLabelEditableProp.getLocalName())) {
                        UIInput caseList = (UIInput) component.getChildren().get(1);
                        SuggesterGenerator.setValue(caseList, cases);
                        caseList.setValue(caseLabel);
                        component.setRendered(cases != null || isSearchFilter);
                    }
                }
            }

            // These only apply when called initially during creation of a new document
            // If called from eventlistener, then model values are updated after and thus overwritten
            document.getProperties().put(functionProp.toString(), functionRef);
            document.getProperties().put(seriesProp.toString(), seriesRef);
            document.getProperties().put(volumeProp.toString(), volumeRef);
            document.getProperties().put(caseLabelEditableProp.toString(), caseLabel);
        }

        private void updateAccessRestrictionProperties(NodeRef seriesRef) {
            String stateHolderKey = DocumentCommonModel.Props.ACCESS_RESTRICTION.getLocalName();
            AccessRestrictionState accessRestrictionState = getPropertySheetStateBean().getStateHolder(stateHolderKey, AccessRestrictionState.class);
            if (accessRestrictionState != null) {
                accessRestrictionState.updateAccessRestrictionProperties(seriesRef);
            }
        }

    }

    public static String getFunctionLabel(Function function) {
        return function.getMark() + " " + function.getTitle();
    }

    public static String getSeriesLabel(Series serie) {
        return serie.getSeriesIdentifier() + " " + serie.getTitle();
    }

    public static String getVolumeLabel(Volume volume) {
        return volume.getVolumeMark() + " " + volume.getTitle();
    }

    public static String getCaseLabel(Case tmpCase) {
        return StringUtils.trim(tmpCase.getTitle());
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
                if (StringUtils.equalsIgnoreCase(caseLabel, getCaseLabel(tmpCase))) {
                    caseRef = tmpCase.getNode().getNodeRef();
                    docCase = tmpCase;
                    break;
                }
            }
            props.put(CASE.toString(), caseRef);
        }

        boolean isClosedUnitCheckNeeded = isClosedUnitCheckNeeded(document, documentService.getAncestorNodesByDocument(document.getNodeRef()), volumeRef, docCase);

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

    /**
     * NB! This method may change document nodeRef when moving from archive to active store
     */
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
                // don't compare entire nodeRef, but only nodeRef ids to allow inter-store moving
                if (!newDocNodeRef.getId().equals(docNodeRef.getId())) {
                    throw new RuntimeException("NodeRef changed while moving");
                }
                document.getNode().updateNodeRef(newDocNodeRef);
                docNodeRef = newDocNodeRef;
                documentService.updateParentNodesContainingDocsCount(docNodeRef, true);

                if (existingParentNode != null && !targetParentRef.equals(existingParentNode.getNodeRef())) {
                    final String existingRegNr = (String) document.getProp(REG_NUMBER);
                    if (StringUtils.isNotBlank(existingRegNr)) {
                        // reg. number is changed if function, series or volume is changed
                        if (!previousVolume.getNodeRef().equals(volumeNodeRef)) {
                            documentService.registerDocumentRelocating(document.getNode(), previousVolume);
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

            if (!document.isDraftOrImapOrDvk()) {
                documentLogService.addDocumentLog(docNodeRef, MessageUtil.getMessage("document_log_location_changed"));
            }
        }
    }

    private boolean isClosedUnitCheckNeeded(DocumentDynamic document, DocumentParentNodesVO parents, NodeRef volumeRef, Case docCase) {
        return document.isDraftOrImapOrDvk()
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
        String caseLabel = document.getProp(CASE_LABEL_EDITABLE);
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
                if (caseLabel.equalsIgnoreCase(getCaseLabel(tmpCase))) {
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
