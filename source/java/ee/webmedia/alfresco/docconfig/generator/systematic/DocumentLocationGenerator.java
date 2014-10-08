package ee.webmedia.alfresco.docconfig.generator.systematic;

import static ee.webmedia.alfresco.common.web.BeanHelper.getCaseService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getFunctionsService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPropertySheetStateBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSeriesService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getVolumeService;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.CASE;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.FUNCTION;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.REG_NUMBER;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.SERIES;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.VOLUME;

import java.util.ArrayList;
import java.util.Collections;
<<<<<<< HEAD
import java.util.HashSet;
=======
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
>>>>>>> develop-5.1
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

<<<<<<< HEAD
=======
import org.alfresco.repo.security.authentication.AuthenticationUtil;
>>>>>>> develop-5.1
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.collections.Closure;
import org.apache.commons.lang.StringUtils;
import org.hibernate.StaleObjectStateException;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.casefile.service.CaseFile;
import ee.webmedia.alfresco.casefile.service.CaseFileService;
import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.model.DynamicBase;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO.ConfigItemType;
import ee.webmedia.alfresco.common.propertysheet.converter.NodeRefConverter;
import ee.webmedia.alfresco.common.propertysheet.suggester.SuggesterGenerator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.CaseFileType;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
<<<<<<< HEAD
=======
import ee.webmedia.alfresco.docadmin.service.DynamicType;
>>>>>>> develop-5.1
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.generator.BasePropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.BaseSystematicFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;
import ee.webmedia.alfresco.docconfig.generator.systematic.AccessRestrictionGenerator.AccessRestrictionState;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicTypeMenuItemProcessor;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentParentNodesVO;
import ee.webmedia.alfresco.document.search.model.DocumentReportModel;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.document.search.web.DocumentDynamicSearchDialog;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.functions.service.FunctionsService;
<<<<<<< HEAD
=======
import ee.webmedia.alfresco.privilege.model.Privilege;
>>>>>>> develop-5.1
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
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
import ee.webmedia.alfresco.volume.search.model.VolumeReportModel;
import ee.webmedia.alfresco.volume.search.model.VolumeSearchModel;
import ee.webmedia.alfresco.volume.service.VolumeService;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> develop-5.1
public class DocumentLocationGenerator extends BaseSystematicFieldGenerator {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentLocationGenerator.class);
    public static final String[] NODE_REF_FIELD_IDS = new String[] {
            FUNCTION.getLocalName(),
            SERIES.getLocalName(),
            VOLUME.getLocalName(),
            CASE.getLocalName() };
    public static final QName CASE_FILE_TYPE_PROP = RepoUtil.createTransientProp("caseFileType");

    @Override
    protected String[] getOriginalFieldIds() {
        return NODE_REF_FIELD_IDS;
    }

    public static final QName CASE_LABEL_EDITABLE = RepoUtil.createTransientProp(CASE.getLocalName() + "LabelEditable");
    public static final QName UPDATED_ASSOCIATED_DOCUMENTS = RepoUtil.createTransientProp("updatedAssociatedDocuments");
    public static final QName DOCUMENT_TYPE_IDS = RepoUtil.createTransientProp("documentTypeIds");
    private final static NodeRef newCaseFileRef = RepoUtil.createNewUnsavedNodeRef();

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

        BaseObject parent = field.getParent() != null ? field.getParent().getParent() : null;
        BaseObject grandParent = parent != null ? parent.getParent() : null;
        // When parents are null, we should treat these fields like they are under documents (search and reports)
        boolean documentType = parent == null || grandParent == null || parent instanceof DocumentType || grandParent instanceof DocumentType;

        QName functionProp;
        QName seriesProp;
        QName volumeProp = null;
        QName caseProp = null;
        if (field.getParent() != null) {
            Map<String, Field> fieldsByOriginalId = ((FieldGroup) field.getParent()).getFieldsByOriginalId();
            functionProp = getProp(fieldsByOriginalId, FUNCTION);
            seriesProp = getProp(fieldsByOriginalId, SERIES);
            if (documentType) {
                volumeProp = getProp(fieldsByOriginalId, VOLUME);
                caseProp = getProp(fieldsByOriginalId, CASE);
            }
        } else {
            functionProp = FUNCTION;
            seriesProp = SERIES;
            volumeProp = VOLUME;
            caseProp = CASE;
        }
        QName functionLabelProp = getTransientProp(functionProp, "Label");
        QName seriesLabelProp = getTransientProp(seriesProp, "Label");
        QName volumeLabelProp = null;
        QName caseLabelProp = null;
        QName caseLabelEditableProp = null;
        if (documentType) {
            volumeLabelProp = getTransientProp(volumeProp, "Label");
            caseLabelProp = getTransientProp(caseProp, "Label");
            caseLabelEditableProp = getTransientProp(caseProp, "LabelEditable");
        }
        String stateHolderKey = functionProp.getLocalName();

        String labelGenerator = BeanHelper.getSubstitutionBean().isCurrentStructUnitUser() ? "ActionLinkGenerator" : "TextFieldGenerator";

        final ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        if (field.getQName().equals(functionProp)) {
            item.setComponentGenerator("GeneralSelectorGenerator");
            item.setConverter(NodeRefConverter.class.getName());
            item.setSelectionItems(getStateHolderBindingName("getFunctions", stateHolderKey));
            item.setValueChangeListener(getStateHolderBindingName("functionValueChanged", stateHolderKey));
            item.setShowInViewMode(false);

            ItemConfigVO functionLabelItem = generatorResults.generateAndAddViewModeText(functionLabelProp.toString(), field.getName());
            functionLabelItem.setComponentGenerator(labelGenerator);
            functionLabelItem.setAction("dialog:seriesListDialog");
            functionLabelItem.setActionListener("#{SeriesListDialog.showAll}");
            functionLabelItem.setActionListenerParams("functionNodeRef=" + getStateHolderBindingName("function", stateHolderKey));

            // Add stateholder
            generatorResults.addStateHolder(stateHolderKey, new DocumentLocationState(functionProp, seriesProp, caseProp, volumeProp, functionLabelProp, seriesLabelProp,
                    volumeLabelProp, caseLabelProp, caseLabelEditableProp, documentType, useAdditionalStateHolders));
            return;
        } else if (field.getQName().equals(seriesProp)) {
            item.setComponentGenerator("GeneralSelectorGenerator");
            item.setConverter(NodeRefConverter.class.getName());
            item.setSelectionItems(getStateHolderBindingName("getSeries", stateHolderKey));
            item.setValueChangeListener(getStateHolderBindingName("seriesValueChanged", stateHolderKey));
            item.setShowInViewMode(false);

            ItemConfigVO seriesLabelItem = generatorResults.generateAndAddViewModeText(seriesLabelProp.toString(), field.getName());
            seriesLabelItem.setComponentGenerator(labelGenerator);
            seriesLabelItem.setAction("dialog:volumeListDialog");
            seriesLabelItem.setActionListener("#{VolumeListDialog.showAll}");
            seriesLabelItem.setActionListenerParams("seriesNodeRef=" + getStateHolderBindingName("series", stateHolderKey));
            return;
        } else if (field.getQName().equals(volumeProp)) {
            if (!documentType) {
                return;
            }

            final ItemConfigVO caseFileTypeItem = new ItemConfigVO(CASE_FILE_TYPE_PROP.toPrefixString(BeanHelper.getNamespaceService()));
            caseFileTypeItem.setComponentGenerator("GeneralSelectorGenerator");
            caseFileTypeItem.setConfigItemType(ConfigItemType.PROPERTY);
            caseFileTypeItem.setSelectionItems(getStateHolderBindingName("getCaseFileTypes", stateHolderKey));
            caseFileTypeItem.setShowInViewMode(false);
            caseFileTypeItem.setIgnoreIfMissing(false);
            caseFileTypeItem.setRendered(getStateHolderBindingName("showCaseFileTypes", stateHolderKey));
            caseFileTypeItem.setDisplayLabel(MessageUtil.getMessage("docdyn_caseFile_type"));
            caseFileTypeItem.setForcedMandatory(true);
            generatorResults.addItemAfterPregeneratedItem(caseFileTypeItem);

            item.setComponentGenerator("GeneralSelectorGenerator");
            item.setConverter(NodeRefConverter.class.getName());
            item.setSelectionItems(getStateHolderBindingName("getVolumes", stateHolderKey));
            item.setValueChangeListener(getStateHolderBindingName("volumeValueChanged", stateHolderKey));
            item.setShowInViewMode(false);

            ItemConfigVO volumeLabelItem = generatorResults.generateAndAddViewModeText(volumeLabelProp.toString(), field.getName());
            volumeLabelItem.setComponentGenerator(labelGenerator);
            volumeLabelItem.setActionListener("#{VolumeListDialog.showVolumeContents}");
            volumeLabelItem.setActionListenerParams("volumeNodeRef=" + getStateHolderBindingName("volume", stateHolderKey));

            return;
        } else if (field.getQName().equals(caseProp)) {
            if (!documentType) {
                return;
            }

            // Edit mode: non-editable select item
            item.setComponentGenerator("GeneralSelectorGenerator");
            item.setConverter(NodeRefConverter.class.getName());
            item.setSelectionItems(getStateHolderBindingName("getCases", stateHolderKey));
            item.setShowInViewMode(false);
            boolean forSearch = field.isForSearch();
            if (forSearch) {
                item.setShowInEditMode(false);
            }
<<<<<<< HEAD
            if (field.getParent() != null) {
=======
            // second condition is true in case of document location modal component
            if (field.getParent() != null || field.getParent() == null && !forSearch) {
>>>>>>> develop-5.1
                item.setDontRenderIfDisabled(true);
            }

            // Edit mode: editable select item
            ItemConfigVO caseLabelEditableItem = new ItemConfigVO(caseLabelEditableProp.toString());
            caseLabelEditableItem.setConfigItemType(ConfigItemType.PROPERTY);
            caseLabelEditableItem.setIgnoreIfMissing(false);
            caseLabelEditableItem.setDisplayLabel(item.getDisplayLabel());
            caseLabelEditableItem.setReadOnly(item.isReadOnly());
            String readOnlyIf = item.getCustomAttributes().get(BaseComponentGenerator.READONLY_IF);
            if (forSearch) {
                caseLabelEditableItem.setRenderCheckboxAfterLabel(true);
            }
            if (readOnlyIf != null) {
                caseLabelEditableItem.setReadOnlyIf(readOnlyIf);
            }
            if (field.getParent() != null || field instanceof FieldDefinition) {
                caseLabelEditableItem.setDontRenderIfDisabled(true);
            }
            caseLabelEditableItem.setComponentGenerator("SuggesterGenerator");
<<<<<<< HEAD
            caseLabelEditableItem.setSuggesterValues(getStateHolderBindingName("getCasesEditable", stateHolderKey));
=======
            String bindingName = "getCasesEditable";
            if (forSearch) {
                // NB! We also need to be able to search by case label alone. Returning null from getCasesEditable sets the suggester to read-only.
                bindingName += "OrEmptyList";
            }
            caseLabelEditableItem.setSuggesterValues(getBindingName(bindingName, stateHolderKey));
>>>>>>> develop-5.1
            caseLabelEditableItem.setStyleClass("long");
            caseLabelEditableItem.setShowInViewMode(false);
            generatorResults.addItem(caseLabelEditableItem);

            // View mode: text item
<<<<<<< HEAD
            ItemConfigVO caseLabelItem = generatorResults.generateAndAddViewModeText(caseLabelProp.toString(), field.getName());
            caseLabelItem.setComponentGenerator(labelGenerator);
            caseLabelItem.setAction("dialog:documentListDialog");
            caseLabelItem.setActionListener("#{DocumentListDialog.setup}");
            caseLabelItem.setActionListenerParams("caseNodeRef=" + getStateHolderBindingName("case", stateHolderKey));
=======
            NodeRef nodeRef = BeanHelper.getDocumentDialogHelperBean().getNodeRef();
            if (nodeRef == null || nodeService.exists(nodeRef) && BeanHelper.getGeneralService().getAncestorNodeRefWithType(nodeRef, CaseModel.Types.CASE) != null) {
                ItemConfigVO caseLabelItem = generatorResults.generateAndAddViewModeText(caseLabelProp.toString(), field.getName());
                caseLabelItem.setComponentGenerator(labelGenerator);
                caseLabelItem.setAction("dialog:documentListDialog");
                caseLabelItem.setActionListener("#{DocumentListDialog.setup}");
                caseLabelItem.setActionListenerParams("caseNodeRef=" + getBindingName("case", stateHolderKey));
            }
>>>>>>> develop-5.1
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
        private List<SelectItem> caseFileTypes;
        private List<SelectItem> cases;
        private List<String> casesEditable;

        private final boolean documentType;
        private final boolean useAdditionalStateHolder;
        private boolean showCaseFileTypes;

        public DocumentLocationState(QName functionProp, QName seriesProp, QName caseProp, QName volumeProp, QName functionLabelProp, QName seriesLabelProp, QName volumeLabelProp,
                                     QName caseLabelProp, QName caseLabelEditableProp, boolean documentType, boolean useAdditionalStateHolder) {
            this.functionProp = functionProp;
            this.seriesProp = seriesProp;
            this.caseProp = caseProp;
            this.volumeProp = volumeProp;
            this.functionLabelProp = functionLabelProp;
            this.seriesLabelProp = seriesLabelProp;
            this.volumeLabelProp = volumeLabelProp;
            this.caseLabelProp = caseLabelProp;
            this.caseLabelEditableProp = caseLabelEditableProp;
            this.documentType = documentType;
            this.useAdditionalStateHolder = useAdditionalStateHolder;
        }

        @Override
        public void reset(boolean inEditMode) {
            final Node document = dialogDataProvider.getNode();
            Map<String, Object> docProps = document.getProperties();
            NodeRef functionRef = (NodeRef) docProps.get(functionProp);
            NodeRef seriesRef = (NodeRef) docProps.get(seriesProp);
            NodeRef volumeRef = documentType ? (NodeRef) docProps.get(volumeProp) : null;
            NodeRef caseRef = documentType ? (NodeRef) docProps.get(caseProp) : null;

            if (inEditMode) {
                String caseLabel = documentType ? (String) docProps.get(caseLabelEditableProp) : null;
                if (caseRef != null) {
<<<<<<< HEAD
                    String caseLabelByCaseRef = getCaseLabel(getCaseService().getCaseByNoderef(caseRef));
=======
                    String caseLabelByCaseRef = getNodeService().exists(caseRef) ? getCaseLabel(getCaseService().getCaseByNoderef(caseRef)) : null;
>>>>>>> develop-5.1
                    if (StringUtils.isNotBlank(caseLabel)) {
                        if (!StringUtils.equals(caseLabel, caseLabelByCaseRef)) {
                            // reset is using document snapshot data, where user has entered different case name
                            caseRef = null;
                        }
                    } else {
                        caseLabel = caseLabelByCaseRef;
                    }
                }
                DocumentDynamic documentDynamic = dialogDataProvider.getDocument();
                boolean updateAccessRestrictionProps = documentType && documentDynamic != null ? !documentDynamic.isDisableUpdateInitialAccessRestrictionProps() : false;
                updateFnSerVol(functionRef, seriesRef, volumeRef, caseRef, caseLabel, true, updateAccessRestrictionProps, true);
            } else {
                document.getProperties().put(functionLabelProp.toString(), getDocumentListUnitLabel(functionRef));
                document.getProperties().put(seriesLabelProp.toString(), getDocumentListUnitLabel(seriesRef));
                if (documentType) {
                    document.getProperties().put(volumeLabelProp.toString(), getDocumentListUnitLabel(volumeRef));

                    String caseLbl = getDocumentListUnitLabel(caseRef);
                    if (caseLbl != null) {
                        document.getProperties().put(caseLabelProp.toString(), caseLbl);
                    }
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
            return documentType ? (NodeRef) dialogDataProvider.getNode().getProperties().get(volumeProp) : null;
        }

        public NodeRef getCase() {
            return documentType ? (NodeRef) dialogDataProvider.getNode().getProperties().get(caseProp) : null;
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
        public List<SelectItem> getCases(FacesContext context, UIInput selectComponent) {
            return cases;
        }

        public List<SelectItem> getCaseFileTypes(FacesContext context, UIInput selectComponent) {
            return getCaseFileTypes();
        }

        public List<String> getCasesEditable(FacesContext context, UIInput selectComponent) {
            return casesEditable;
        }

<<<<<<< HEAD
=======
        /**
         * Alternative case listing for search dialogs. If we return null for casesEditable, then the Suggester component is rendered as read-only.
         */
        public List<String> getCasesEditableOrEmptyList(FacesContext context, UIInput selectComponent) {
            if (casesEditable == null) {
                return Collections.<String> emptyList();
            }
            return casesEditable;
        }

>>>>>>> develop-5.1
        public void functionValueChanged(ValueChangeEvent event) {
            NodeRef functionRef = (NodeRef) event.getNewValue();
            updateFnSerVol(functionRef, null, null, null, null, false, true, false);
        }

        public void seriesValueChanged(ValueChangeEvent event) {
            Node document = dialogDataProvider.getNode();
            NodeRef functionRef = (NodeRef) document.getProperties().get(functionProp);
            NodeRef seriesRef = (NodeRef) event.getNewValue();
            updateFnSerVol(functionRef, seriesRef, null, null, null, false, true, false);
        }

        public void volumeValueChanged(ValueChangeEvent event) {
            Node document = dialogDataProvider.getNode();
            NodeRef functionRef = (NodeRef) document.getProperties().get(functionProp);
            NodeRef seriesRef = (NodeRef) document.getProperties().get(seriesProp);
            NodeRef volumeRef = (NodeRef) event.getNewValue();
<<<<<<< HEAD
            updateFnSerVol(functionRef, seriesRef, volumeRef, null, null, false, true, false);
=======
            updateFnSerVol(functionRef, seriesRef, volumeRef, null, null, false, false, false);
>>>>>>> develop-5.1
        }

        private void updateFnSerVol(NodeRef functionRef, NodeRef seriesRef, NodeRef volumeRef, NodeRef caseRef, String caseLabel, boolean addIfMissing,
                boolean updateAccessRestrictionProperties, boolean useCaseLabel) {
            Node document = dialogDataProvider.getNode();
            UIPropertySheet ps = dialogDataProvider.getPropertySheet();
            QName docType = document.getType();
            boolean isAssocObjectSearchFilter = DocumentSearchModel.Types.OBJECT_FILTER.equals(docType);
            boolean isSearchFilter = DocumentSearchModel.Types.FILTER.equals(docType)
                    || DocumentReportModel.Types.FILTER.equals(docType)
                    || isAssocObjectSearchFilter
                    || VolumeReportModel.Types.FILTER.equals(docType)
                    || VolumeSearchModel.Types.FILTER.equals(docType);
            boolean isDocTypeDef = DocumentAdminModel.Types.DOCUMENT_TYPE.equals(docType);

            // If this isn't DocumentType then assume that it is a DynamicBase type node
            QName docTypeIdQName = isDocTypeDef ? DocumentAdminModel.Props.ID : DocumentAdminModel.Props.OBJECT_TYPE_ID;
            String documentTypeId = (String) document.getProperties().get(docTypeIdQName);
            @SuppressWarnings("unchecked")
            Set<String> documentTypeIds = (Set<String>) document.getProperties().get(DOCUMENT_TYPE_IDS);
            Set<String> idList;
            if (documentTypeId != null) {
                idList = new HashSet<String>();
                idList.add(documentTypeId);
            } else {
                idList = documentTypeIds;
            }
            boolean originalShowCaseFileTypes = showCaseFileTypes;
            showCaseFileTypes = false;
            boolean docTypeNull = documentTypeId == null;
            boolean isSearchFilterOrDocTypeNull = isSearchFilter || docTypeNull;
            { // Function
                List<Function> allFunctions = getAllFunctions(document, isSearchFilter);
                functions = new ArrayList<SelectItem>(allFunctions.size());
                functions.add(new SelectItem("", ""));
                boolean functionFound = false;
                for (Function function : allFunctions) {
<<<<<<< HEAD
                    List<Series> openSeries = getAllSeries(function.getNodeRef(), isSearchFilterOrDocTypeNull, idList);
=======
                    List<Series> openSeries = getAllSeries(function.getNodeRef(), isSearchFilter, idList);
>>>>>>> develop-5.1
                    if (openSeries.size() == 0) {
                        continue;
                    }
                    functions.add(new SelectItem(function.getNodeRef(), getFunctionLabel(function)));
                    if (functionRef != null && functionRef.equals(function.getNodeRef())) {
                        functionFound = true;
                    }
                }
                if (!functionFound) {
                    if (!isSearchFilter && addIfMissing && functionRef != null && getNodeService().exists(functionRef)) {
                        Function function = getFunctionsService().getFunctionByNodeRef(functionRef);
                        functions.add(1, new SelectItem(function.getNodeRef(), getFunctionLabel(function)));
                    } else {
                        functionRef = null;
                    }
                }
                // If list contains only one value, then select it right away
                if (functions.size() == 2 && !isDocTypeDef && !isAssocObjectSearchFilter) {
                    functions.remove(0);
                    if (functionRef == null) {
                        functionRef = (NodeRef) functions.get(0).getValue();
                    }
                }
            }

            Series currentSeries = null;
            if (functionRef == null) {
                series = null;
                seriesRef = null;
            } else {
                List<Series> allSeries;
<<<<<<< HEAD
                allSeries = getAllSeries(functionRef, isSearchFilterOrDocTypeNull, idList);
=======
                allSeries = getAllSeries(functionRef, isSearchFilter, idList);
>>>>>>> develop-5.1
                series = new ArrayList<SelectItem>(allSeries.size());
                series.add(new SelectItem("", ""));
                for (Series serie : allSeries) {
                    series.add(new SelectItem(serie.getNode().getNodeRef(), getSeriesLabel(serie)));
                    if (seriesRef != null && seriesRef.equals(serie.getNode().getNodeRef())) {
                        currentSeries = serie;
                    }
                }
                if (currentSeries == null) {
                    if (addIfMissing && seriesRef != null && getNodeService().exists(seriesRef)) {
                        currentSeries = getSeriesService().getSeriesByNodeRef(seriesRef);
                        series.add(1, new SelectItem(currentSeries.getNode().getNodeRef(), getSeriesLabel(currentSeries)));
                    } else {
                        seriesRef = null;
                    }
                }
                // If list contains only one value, then select it right away
                if (series.size() == 2 && !isDocTypeDef && !isAssocObjectSearchFilter) {
                    series.remove(0);
                    if (seriesRef == null) {
                        seriesRef = (NodeRef) series.get(0).getValue();
                        currentSeries = getSeriesService().getSeriesByNodeRef(seriesRef);
                    }
                }
            }

            if (seriesRef == null || !documentType) {
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
                if (isSearchFilter) { // Search screens
                    allVolumes = getVolumeService().getAllVolumesBySeries(seriesRef);
<<<<<<< HEAD
                } else if (docTypeNull) { // Mass document relocating
                    allVolumes = getVolumeService().getAllVolumesBySeries(seriesRef, DocListUnitStatus.OPEN);
                } else if (getGeneralService().getStore().equals(seriesRef.getStoreRef())) {
=======
                } else if (docTypeNull || getGeneralService().getStore().equals(seriesRef.getStoreRef())) { // Mass and regular document relocating
>>>>>>> develop-5.1
                    allVolumes = getVolumeService().getAllValidVolumesBySeries(seriesRef, DocListUnitStatus.OPEN);
                } else {
                    allVolumes = Collections.emptyList();
                }
                volumes = new ArrayList<SelectItem>(allVolumes.size());
                volumes.add(new SelectItem("", ""));
                List<String> volTypes = currentSeries.getVolType();
                boolean canAddNewCaseFile = volTypes != null && volTypes.contains(VolumeType.CASE_FILE.name());
                if (canAddNewCaseFile && !isSearchFilter) {
                    volumes.add(new SelectItem(newCaseFileRef, MessageUtil.getMessage("docdyn_start_new_caseFile")));
                }
                boolean volumeFound = false;
                PrivilegeService privilegeService = BeanHelper.getPrivilegeService();
                boolean isDocument = DocumentCommonModel.Types.DOCUMENT.equals(docType);
                for (Volume volume : allVolumes) {
                    NodeRef volRef = volume.getNode().getNodeRef();
<<<<<<< HEAD
                    if (!isDocument || !volume.isDynamic() || privilegeService.hasPermissions(volRef, DocumentCommonModel.Privileges.VIEW_CASE_FILE)) {
=======
                    if (!isDocument || !volume.isDynamic() || privilegeService.hasPermission(volRef, AuthenticationUtil.getRunAsUser(), Privilege.VIEW_CASE_FILE)) {
>>>>>>> develop-5.1
                        volumes.add(new SelectItem(volRef, getVolumeLabel(volume)));
                        if (volumeRef != null && volumeRef.equals(volRef)) {
                            volumeFound = true;
                        }
                    }
                }
                if (canAddNewCaseFile && isNewVolumeSelected(volumeRef)) {
                    volumeFound = true;
                }
                if (!volumeFound) {
                    if (!volumeFound && addIfMissing && volumeRef != null && getNodeService().exists(volumeRef)) {
                        Volume volume = getVolumeService().getVolumeByNodeRef(volumeRef);
                        volumes.add(canAddNewCaseFile ? 2 : 1, new SelectItem(volume.getNode().getNodeRef(), getVolumeLabel(volume)));
                    } else {
                        volumeRef = null;
                    }
                }

                if (volumes.size() == 2 && !isDocTypeDef && !isAssocObjectSearchFilter) {
                    volumes.remove(0);
                    if (volumeRef == null) {
                        volumeRef = (NodeRef) volumes.get(0).getValue();
                    }
                }
                if (isNewVolumeSelected(volumeRef)) {
                    showCaseFileTypes = true;
                }
            }

            boolean casesCreatableByUser = false;
            if (volumeRef == null || RepoUtil.isUnsaved(volumeRef) || !documentType || isAssocObjectSearchFilter) {
                cases = null;
<<<<<<< HEAD
                casesEditable = null;
                caseRef = null;
                caseLabel = null;
=======
                caseRef = null;
                if (!isSearchFilter) {
                    caseLabel = null;
                }
>>>>>>> develop-5.1
            } else {
                Volume volume = getVolumeService().getVolumeByNodeRef(volumeRef);
                if (volume.isContainsCases()) {
                    if (volume.isCasesCreatableByUser() || getUserService().isDocumentManager()) {
                        casesCreatableByUser = true;
                    }
                    List<Case> allCases;
                    if (isSearchFilterOrDocTypeNull) {
                        allCases = getCaseService().getAllCasesByVolume(volumeRef);
                    } else if (getGeneralService().getStore().equals(volumeRef.getStoreRef())) {
                        allCases = getCaseService().getAllCasesByVolume(volumeRef, DocListUnitStatus.OPEN);
                    } else {
                        allCases = Collections.emptyList();
                    }
<<<<<<< HEAD
                    cases = new ArrayList<SelectItem>(allCases.size());
                    casesEditable = new ArrayList<String>(allCases.size());
=======
                    int allCasesSize = allCases.size();
                    cases = new ArrayList<SelectItem>(allCasesSize);
                    casesEditable = new ArrayList<String>(allCasesSize);
>>>>>>> develop-5.1
                    cases.add(new SelectItem("", ""));
                    boolean caseFound = false;
                    for (Case tmpCase : allCases) {
                        String tmpCaseLabel = getCaseLabel(tmpCase);
                        cases.add(new SelectItem(tmpCase.getNode().getNodeRef(), tmpCaseLabel));
                        casesEditable.add(tmpCaseLabel);
                        if (caseRef != null && caseRef.equals(tmpCase.getNode().getNodeRef())) {
                            caseFound = true;
                        }
                    }
                    if (!caseFound) {
                        if (addIfMissing && caseRef != null && getNodeService().exists(caseRef)) {
                            Case tmpCase = getCaseService().getCaseByNoderef(caseRef);
                            cases.add(1, new SelectItem(tmpCase.getNode().getNodeRef(), getCaseLabel(tmpCase)));
                        } else {
                            caseRef = null;
                        }
                    }
<<<<<<< HEAD
                    // If list contains only one value, then select it right away
                    if (!useCaseLabel && StringUtils.isBlank(caseLabel) && casesEditable.size() == 1 && !isDocTypeDef) {
                        caseLabel = StringUtils.trim(casesEditable.get(0));
                    }
                    if (cases.size() == 2 && !isDocTypeDef) {
                        cases.remove(0);
                        if (caseRef == null && !useCaseLabel) {
                            caseRef = (NodeRef) cases.get(0).getValue();
                        }
                    }
                } else {
                    cases = null;
                    casesEditable = null;
                    caseLabel = null;
=======
                } else {
                    cases = null;
                    casesEditable = null;
                    if (!isSearchFilter) {
                        caseLabel = null;
                    }
>>>>>>> develop-5.1
                }
            }
            if (isSearchFilter || casesCreatableByUser && !isDocTypeDef) {
                cases = null;
                caseRef = null;
            } else {
                casesEditable = null;
                if (!useCaseLabel) {
                    caseLabel = null;
                }
            }

            if (ps != null) {
                if (originalShowCaseFileTypes != showCaseFileTypes) {
                    ComponentUtil.executeLater(PhaseId.INVOKE_APPLICATION, ps, new Closure() {
                        @Override
                        public void execute(Object input) {
                            UIPropertySheet propertySheet = dialogDataProvider.getPropertySheet();
                            if (propertySheet != null) {
                                propertySheet.getChildren().clear();
                                propertySheet.getClientValidations().clear();
                            }
                        }
                    });
                } else {
                    List<UIComponent> children = ps.getChildren();
<<<<<<< HEAD
=======
                    FacesContext context = FacesContext.getCurrentInstance();
>>>>>>> develop-5.1
                    for (UIComponent component : children) {
                        List<UIComponent> componentChildren = component.getChildren();
                        if (componentChildren.size() < 2) {
                            continue;
                        }
                        if (component.getId().endsWith("_" + functionProp.getLocalName())) {
<<<<<<< HEAD
                            HtmlSelectOneMenu functionList = (HtmlSelectOneMenu) componentChildren.get(1);
                            ComponentUtil.setSelectItems(FacesContext.getCurrentInstance(), functionList, functions);
                            functionList.setValue(functionRef);
                        } else if (component.getId().endsWith("_" + seriesProp.getLocalName())) {
                            HtmlSelectOneMenu seriesList = (HtmlSelectOneMenu) componentChildren.get(1);
                            ComponentUtil.setSelectItems(FacesContext.getCurrentInstance(), seriesList, series);
                            seriesList.setValue(seriesRef);
                        } else if (documentType && component.getId().endsWith("_" + volumeProp.getLocalName())) {
                            HtmlSelectOneMenu volumeList = (HtmlSelectOneMenu) componentChildren.get(1);
                            ComponentUtil.setSelectItems(FacesContext.getCurrentInstance(), volumeList, volumes);
                            volumeList.setValue(volumeRef);
                        } else if (documentType && component.getId().endsWith("_" + caseProp.getLocalName())) {
                            HtmlSelectOneMenu caseList = (HtmlSelectOneMenu) componentChildren.get(1);
                            ComponentUtil.setSelectItems(FacesContext.getCurrentInstance(), caseList, cases);
                            caseList.setValue(caseRef);
                            component.setRendered(cases != null && !isSearchFilter);
                        } else if (documentType && component.getId().endsWith("_" + caseLabelEditableProp.getLocalName())) {
                            UIInput caseList = (UIInput) componentChildren.get(1);
                            SuggesterGenerator.setValue(caseList, casesEditable);
                            caseList.setValue(caseLabel);
                            component.setRendered(casesEditable != null || isSearchFilter);
=======
                            HtmlSelectOneMenu functionList = (HtmlSelectOneMenu) component.getChildren().get(1);
                            ComponentUtil.setSelectItems(context, functionList, functions);
                            functionList.setValue(functionRef);
                        } else if (component.getId().endsWith("_" + seriesProp.getLocalName())) {
                            HtmlSelectOneMenu seriesList = (HtmlSelectOneMenu) component.getChildren().get(1);
                            ComponentUtil.setSelectItems(context, seriesList, series);
                            seriesList.setValue(seriesRef);
                        } else if (volumeProp != null && component.getId().endsWith("_" + volumeProp.getLocalName())) {
                            HtmlSelectOneMenu volumeList = (HtmlSelectOneMenu) component.getChildren().get(1);
                            ComponentUtil.setSelectItems(context, volumeList, volumes);
                            volumeList.setValue(volumeRef);
                        } else if (caseProp != null && component.getId().endsWith("_" + caseProp.getLocalName())) {
                            HtmlSelectOneMenu caseList = (HtmlSelectOneMenu) component.getChildren().get(1);
                            ComponentUtil.setSelectItems(context, caseList, cases);
                            caseList.setValue(caseRef);
                            component.setRendered(cases != null && !isSearchFilter);
                        } else if (caseLabelEditableProp != null && component.getId().endsWith("_" + caseLabelEditableProp.getLocalName())) {
                            UIInput caseList = (UIInput) component.getChildren().get(1);
                            SuggesterGenerator.setValue(caseList, (isSearchFilter ? getCasesEditableOrEmptyList(context, caseList) : casesEditable));
                            caseList.setValue(caseLabel);
                            component.setRendered(casesEditable != null || isSearchFilter);
                        } else if (component.getId().endsWith(CASE_FILE_TYPE_PROP.getLocalName()) && !showCaseFileTypes) {
                            component.setRendered(showCaseFileTypes);
>>>>>>> develop-5.1
                        }
                    }
                }
            }

            // These only apply when called initially during creation of a new dynamicType object
            // If called from event listener, then model values are updated after and thus overwritten
            document.getProperties().put(functionProp.toString(), functionRef);
            document.getProperties().put(seriesProp.toString(), seriesRef);
            if (documentType) {
                document.getProperties().put(volumeProp.toString(), volumeRef);
                document.getProperties().put(caseProp.toString(), caseRef);
<<<<<<< HEAD
                if (!isSearchFilter) { // do not clear caseLabelEditable prop if this is a search filter node
                    document.getProperties().put(caseLabelEditableProp.toString(), caseLabel);
                }
=======
                document.getProperties().put(caseLabelEditableProp.toString(), caseLabel);
>>>>>>> develop-5.1
            }
        }

        public boolean isNewVolumeSelected(NodeRef volumeRef) {
            return volumeRef != null && RepoUtil.isUnsaved(volumeRef);
        }

        public List<SelectItem> getCaseFileTypes() {
            if (caseFileTypes == null) {
                List<CaseFileType> userCaseFileTypes = BeanHelper.getDocumentAdminService().getUsedCaseFileTypes(DocumentAdminService.DONT_INCLUDE_CHILDREN);
                caseFileTypes = new ArrayList<SelectItem>();
<<<<<<< HEAD
                for (CaseFileType userCaseFileType : userCaseFileTypes) {
                    if (DocumentDynamicTypeMenuItemProcessor
                            .hasPermission(BeanHelper.getPermissionService(), DocumentCommonModel.Privileges.CREATE_CASE_FILE, userCaseFileType)) {
=======
                boolean isDocManager = getUserService().isDocumentManager();
                String userName = AuthenticationUtil.getRunAsUser();
                Set<String> userAuthorities = null;
                Map<String, List<String>> createDocumentPrivileges = null;
                if (!isDocManager) {
                    Map<String, NodeRef> docTypeNodeRefs = new HashMap<String, NodeRef>();
                    for (DynamicType caseFileType : userCaseFileTypes) {
                        NodeRef nodeRef = caseFileType.getNodeRef();
                        docTypeNodeRefs.put(nodeRef.getId(), nodeRef);
                    }
                    userAuthorities = new HashSet<String>(BeanHelper.getAuthorityService().getContainedAuthorities(null, userName, false));
                    userAuthorities.add(userName);
                    createDocumentPrivileges = BeanHelper.getPrivilegeService().getCreateCaseFilePrivileges(docTypeNodeRefs.keySet());
                }
                for (CaseFileType userCaseFileType : userCaseFileTypes) {
                    if (isDocManager || DocumentDynamicTypeMenuItemProcessor
                            .hasCreatePermission(createDocumentPrivileges, userName, userAuthorities, userCaseFileType.getNodeRef())) {
>>>>>>> develop-5.1
                        caseFileTypes.add(new SelectItem(userCaseFileType.getId(), userCaseFileType.getName()));
                    }
                }
            }
            return caseFileTypes;
        }

<<<<<<< HEAD
        public boolean showCaseFileTypes() {
            return showCaseFileTypes;
        }

=======
        public boolean isShowCaseFileTypes() {
            return showCaseFileTypes;
        }

        @SuppressWarnings("unchecked")
>>>>>>> develop-5.1
        private List<Function> getAllFunctions(Node document, boolean isSearchFilter) {
            if (!isSearchFilter) {
                return getFunctionsService().getAllFunctions(DocListUnitStatus.OPEN);
            }

<<<<<<< HEAD
            @SuppressWarnings("unchecked")
            List<NodeRef> selectedStores = (List<NodeRef>) document.getProperties().get(DocumentDynamicSearchDialog.SELECTED_STORES);
            if (selectedStores == null) {
                return getFunctionsService().getAllFunctions();
=======
            List<NodeRef> selectedStores = (List<NodeRef>) document.getProperties().get(DocumentDynamicSearchDialog.SELECTED_STORES);
            if (selectedStores == null || selectedStores.isEmpty()) { // Check if the search filter is already saved
                List<String> storeStrings = (List<String>) document.getProperties().get(DocumentSearchModel.Props.STORE);
                if (storeStrings == null) {
                    return getFunctionsService().getAllFunctions();
                }
                for (Iterator<String> i = storeStrings.iterator(); i.hasNext();) {
                    if (StringUtils.isBlank(i.next())) {
                        i.remove();
                    }
                }
                if (storeStrings.isEmpty()) {
                    return getFunctionsService().getAllFunctions();
                }

                selectedStores = new ArrayList<NodeRef>(storeStrings.size());
                for (String store : storeStrings) {
                    selectedStores.add(new NodeRef(store));
                }
>>>>>>> develop-5.1
            }

            List<Function> allFunctions = new ArrayList<Function>();
            for (NodeRef functionsRootNodeRef : selectedStores) {
                allFunctions.addAll(getFunctionsService().getFunctions(functionsRootNodeRef));
            }
            return allFunctions;
        }

<<<<<<< HEAD
        private List<Series> getAllSeries(NodeRef functionRef, boolean isSearchFilterOrDocTypeNull, Set<String> idList) {
            List<Series> allSeries;
            if (isSearchFilterOrDocTypeNull) {
=======
        private List<Series> getAllSeries(NodeRef functionRef, boolean isSearchFilter, Set<String> idList) {
            List<Series> allSeries;
            if (isSearchFilter || idList == null) {
>>>>>>> develop-5.1
                allSeries = getSeriesService().getAllSeriesByFunction(functionRef);
            } else if (getGeneralService().getStore().equals(functionRef.getStoreRef())) {
                allSeries = documentType
                        ? getSeriesService().getAllSeriesByFunction(functionRef, DocListUnitStatus.OPEN, idList)
                        : getSeriesService().getAllCaseFileSeriesByFunction(functionRef, DocListUnitStatus.OPEN);
            } else {
                allSeries = Collections.emptyList();
            }
            return allSeries;
        }

        private void updateAccessRestrictionProperties(NodeRef seriesRef) {
            if (useAdditionalStateHolder) {
                return;
            }
            String stateHolderKey = DocumentCommonModel.Props.ACCESS_RESTRICTION.getLocalName();
            AccessRestrictionState accessRestrictionState = getPropertySheetStateBean().getStateHolder(stateHolderKey, AccessRestrictionState.class);
            if (accessRestrictionState != null) {
                accessRestrictionState.updateAccessRestrictionProperties(seriesRef);
            }
        }

    }

    public static String getDocumentListUnitLabel(NodeRef nodeRef) {
        if (nodeRef == null || !getNodeService().exists(nodeRef)) {
            return null;
        }
        QName type = getNodeService().getType(nodeRef);
        if (FunctionsModel.Types.FUNCTION.equals(type)) {
            return getFunctionLabel(getFunctionsService().getFunctionByNodeRef(nodeRef));
        }
        if (SeriesModel.Types.SERIES.equals(type)) {
            return getSeriesLabel(getSeriesService().getSeriesByNodeRef(nodeRef));
        }
        if (VolumeModel.Types.VOLUME.equals(type) || CaseFileModel.Types.CASE_FILE.equals(type)) {
            return getVolumeLabel(getVolumeService().getVolumeByNodeRef(nodeRef));
        }
        if (CaseModel.Types.CASE.equals(type)) {
            return getCaseLabel(getCaseService().getCaseByNoderef(nodeRef));
        }

        return null;
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
    private CaseFileService caseFileService;

    @Override
    public void validate(DynamicBase document, ValidationHelper validationHelper) {
        boolean isDocument = document instanceof DocumentDynamic;

        final Map<String, Object> props = document.getNode().getProperties();
        NodeRef functionRef = (NodeRef) props.get(FUNCTION);
        NodeRef seriesRef = (NodeRef) props.get(SERIES);
        NodeRef volumeRef = (NodeRef) props.get(VOLUME);
        if (functionRef == null || seriesRef == null || volumeRef == null && isDocument) {
            validationHelper.addErrorMessage("document_validationMsg_mandatory_functionSeriesVolume");
            return;
        }

        Volume volume = isDocument && RepoUtil.isSaved(volumeRef) ? volumeService.getVolumeByNodeRef(volumeRef) : null;
        String caseLabel = StringUtils.trimToNull((String) props.get(CASE_LABEL_EDITABLE));
        NodeRef caseRef = isDocument ? ((DocumentDynamic) document).getCase() : null;

        if (volume != null && volume.isContainsCases()) {
            boolean casesCreatableByUser = volume.isCasesCreatableByUser() || getUserService().isDocumentManager();
            if (casesCreatableByUser) {
                caseRef = null;
                if (StringUtils.isNotBlank(caseLabel)) {
                    List<Case> allCases = caseService.getAllCasesByVolume(volumeRef);
                    for (Case tmpCase : allCases) {
                        if (StringUtils.equalsIgnoreCase(caseLabel, getCaseLabel(tmpCase))) {
                            caseRef = tmpCase.getNode().getNodeRef();
                            caseLabel = null;
                            break;
                        }
                    }
                    // If case not found, then create it in the save method
                }
            } else {
                caseLabel = null;
            }
        } else {
            caseLabel = null;
            caseRef = null;
        }
        props.put(CASE.toString(), caseRef);
        props.put(CASE_LABEL_EDITABLE.toString(), caseLabel);

        boolean isClosedUnitCheckNeeded = !isDocument || isDocument
                && isClosedUnitCheckNeeded((DocumentDynamic) document, documentService.getAncestorNodesByDocument(document.getNodeRef()), volumeRef, caseRef);

        if (isClosedUnitCheckNeeded && DocListUnitStatus.CLOSED.equals(functionsService.getFunctionByNodeRef(functionRef).getStatus())) {
            validationHelper.addErrorMessage(isDocument ? "document" : "caseFile" + "_validationMsg_closed_function");
        }
        if (isClosedUnitCheckNeeded && DocListUnitStatus.CLOSED.equals(seriesService.getSeriesByNodeRef(seriesRef).getStatus())) {
            validationHelper.addErrorMessage(isDocument ? "document" : "caseFile" + "_validationMsg_closed_series");
        }

        if (!isDocument) {
            return;
        }

        if (isClosedUnitCheckNeeded && volume != null && DocListUnitStatus.CLOSED.equals(volume.getStatus())) {
            validationHelper.addErrorMessage("document_validationMsg_closed_volume");
        }
        if (isClosedUnitCheckNeeded && caseRef != null && DocListUnitStatus.CLOSED.equals(caseService.getCaseByNoderef(caseRef).getStatus())) {
            validationHelper.addErrorMessage("document_validationMsg_closed_case");
        }
    }

    /**
     * NB! This method may change document nodeRef when moving from archive to active store
     */
    @Override
    public void save(DynamicBase document) {
        boolean isDocument = document instanceof DocumentDynamic;

        NodeRef docNodeRef = document.getNodeRef();
        final NodeRef volumeNodeRef = isDocument ? ((DocumentDynamic) document).getVolume() : null;
        Volume volume = isDocument ? volumeService.getVolumeByNodeRef(volumeNodeRef) : null;

        NodeRef caseNodeRef = isDocument ? ((DocumentDynamic) document).getCase() : null; // getCaseNodeRef(document, volumeNodeRef);
        String caseLabel = document.getProp(CASE_LABEL_EDITABLE);
        if (isDocument && StringUtils.isNotBlank(caseLabel)) {
            Assert.isTrue(caseNodeRef == null && volume.isContainsCases() && (volume.isCasesCreatableByUser() || getUserService().isDocumentManager()));
            // create case
            Case tmpCase = caseService.createCase(volumeNodeRef);
            tmpCase.setTitle(caseLabel);
            caseService.saveOrUpdate(tmpCase, false);
            caseNodeRef = tmpCase.getNode().getNodeRef();
            caseLabel = null;
            ((DocumentDynamic) document).setCase(caseNodeRef);
            document.setProp(CASE_LABEL_EDITABLE, caseLabel);
        }

        // Prepare existingParentNode and targetParentRef properties
        final NodeRef targetParentRef;
        Node existingParentNode = null;
        if (caseNodeRef != null) {
            Assert.isTrue(volume.isContainsCases());
            targetParentRef = caseNodeRef;
            existingParentNode = documentService.getCaseByDocument(docNodeRef);
            if (existingParentNode == null) { // moving from volume to case?
                existingParentNode = documentService.getVolumeByDocument(docNodeRef);
            }
        } else if (volumeNodeRef != null) {
            // Documents can also be directly under a volume that contains cases
            targetParentRef = volumeNodeRef;
            existingParentNode = documentService.getVolumeByDocument(docNodeRef);
            if (existingParentNode == null) { // moving from case to volume?
                existingParentNode = documentService.getCaseByDocument(docNodeRef);
            }
        } else {
            // We are dealing with caseFile
            existingParentNode = getGeneralService().getParentWithType(docNodeRef, SeriesModel.Types.SERIES);
            targetParentRef = document.getSeries();
        }

        // Prepare series and function properties
        NodeRef series = isDocument ? nodeService.getPrimaryParent(volumeNodeRef).getParentRef() : document.getSeries();
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
        if (isDocument) {
            ((DocumentDynamic) document).setVolume(volumeNodeRef);
            ((DocumentDynamic) document).setCase(caseNodeRef);
        }

        if (existingParentNode == null || !targetParentRef.equals(existingParentNode.getNodeRef())) {
            boolean needUpdateVolumeShortcuts = false;
            if (document instanceof CaseFile) {
                Node previousFunction = existingParentNode != null ? getGeneralService().getPrimaryParent(existingParentNode.getNodeRef()) : null;
                if ((previousFunction == null || !getFunctionsService().isDraftsFunction(previousFunction.getNodeRef()))
                        && getFunctionsService().isDraftsFunction(document.getFunction())) {
                    needUpdateVolumeShortcuts = true;
                }
            }
            // was not saved (under volume nor case) or saved, but parent (volume or case) must be changed
            Node previousCase = isDocument ? documentService.getCaseByDocument(docNodeRef) : null;
            Node previousVolume = isDocument ? documentService.getVolumeByDocument(docNodeRef, previousCase) : null;
            try {
                // Moving is executed with System user rights, because this is not appropriate to implement in permissions model
                String regNumber = null;
                if (isDocument) {
                    documentService.updateParentNodesContainingDocsCount(docNodeRef, false);
                    regNumber = (String) nodeService.getProperty(docNodeRef, REG_NUMBER);
                    documentService.updateParentDocumentRegNumbers(docNodeRef, regNumber, null);
                }

                QName assocQName = isDocument ? DocumentCommonModel.Assocs.DOCUMENT : CaseFileModel.Assocs.CASE_FILE;
                final NodeRef newDocNodeRef = nodeService.moveNode(docNodeRef, targetParentRef, assocQName, assocQName).getChildRef();
                // don't compare entire nodeRef, but only nodeRef ids to allow inter-store moving
                if (!newDocNodeRef.getId().equals(docNodeRef.getId())) {
                    throw new RuntimeException("NodeRef changed while moving");
                }
                document.getNode().updateNodeRef(newDocNodeRef);
<<<<<<< HEAD
=======
                if (!docNodeRef.equals(newDocNodeRef)) {
                    updateChildAssocNodeRefsRecursively(newDocNodeRef, document.getNode());
                }
                NodeRef oldDocNodeRef = docNodeRef;
>>>>>>> develop-5.1
                docNodeRef = newDocNodeRef;
                if (isDocument) {
                    documentService.updateParentNodesContainingDocsCount(docNodeRef, true);
                    documentService.updateParentDocumentRegNumbers(docNodeRef, null, regNumber);
                }
                if (needUpdateVolumeShortcuts) {
                    getGeneralService().runOnBackground(new RunAsWork<Void>() {
                        @Override
                        public Void doWork() throws Exception {
                            BeanHelper.getMenuService().addVolumeShortcuts(newDocNodeRef, true);
                            return null;
                        }
                    }, "addVolumeShortcuts", true);
                }
                if (existingParentNode != null && !targetParentRef.equals(existingParentNode.getNodeRef())) {
                    if (isDocument) {
                        final String existingRegNr = (String) document.getProp(REG_NUMBER);
<<<<<<< HEAD
=======
                        final Date existingRegDate = (Date) document.getProp(DocumentCommonModel.Props.REG_DATE_TIME);
>>>>>>> develop-5.1
                        if (StringUtils.isNotBlank(existingRegNr)) {
                            // reg. number is changed if function, series or volume is changed
                            if (previousVolume != null && !previousVolume.getNodeRef().equals(volumeNodeRef)) {
                                documentService.registerDocumentRelocating(document.getNode(), previousVolume);
<<<<<<< HEAD
                                BeanHelper.getAdrService().addDeletedDocument(docNodeRef);
=======
                                if (oldDocNodeRef != null && "ArchivalsStore".equals(oldDocNodeRef.getStoreRef().getIdentifier())) {
                                    BeanHelper.getAdrService().addDeletedDocumentFromArchive(oldDocNodeRef, existingRegNr, existingRegDate);
                                } else {
                                    BeanHelper.getAdrService().addDeletedDocument(oldDocNodeRef);
                                }
>>>>>>> develop-5.1
                            }
                        }
                    } else {
                        caseFileService.registerCaseFile(document.getNode(), seriesService.getSeriesNodeByRef(existingParentNode.getNodeRef()), true);
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
        }
    }

<<<<<<< HEAD
=======
    private void updateChildAssocNodeRefsRecursively(final NodeRef newNodeRef, Node node) {
        Map<String, List<Node>> childAssocs = node.getAllChildAssociationsByAssocType();
        if (childAssocs != null) {
            for (List<Node> nodeList : childAssocs.values()) {
                for (Node childAssoc : nodeList) {
                    NodeRef oldRef = childAssoc.getNodeRef();
                    if (nodeService.exists(oldRef) || RepoUtil.isUnsaved(oldRef)) {
                        continue;
                    }
                    NodeRef newRef = new NodeRef(newNodeRef.getStoreRef(), oldRef.getId());
                    childAssoc.updateNodeRef(newRef);
                    updateChildAssocNodeRefsRecursively(newNodeRef, childAssoc);
                }
            }
        }
    }

>>>>>>> develop-5.1
    private boolean isClosedUnitCheckNeeded(DocumentDynamic document, DocumentParentNodesVO parents, NodeRef volumeRef, NodeRef caseRef) {
        return document.isDraftOrImapOrDvk()
                || !(volumeRef.equals(parents.getVolumeNode().getNodeRef())
                && (parents.getCaseNode() == null ? caseRef == null
                        : (caseRef == null ? false
                                : parents.getCaseNode().getNodeRef().equals(caseRef)
<<<<<<< HEAD
                                )
                                )
                        );
=======
                        )
                )
                );
>>>>>>> develop-5.1
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

    public void setCaseFileService(CaseFileService caseFileService) {
        this.caseFileService = caseFileService;
    }

    // END: setters
}