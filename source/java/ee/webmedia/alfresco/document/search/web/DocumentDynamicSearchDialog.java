package ee.webmedia.alfresco.document.search.web;

<<<<<<< HEAD
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentConfigService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentSearchBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentSearchResultsDialog;
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPropertySheetStateBean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlSelectManyListbox;
import javax.faces.context.FacesContext;
<<<<<<< HEAD
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
=======
import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.alfresco.web.config.PropertySheetConfigElement;
<<<<<<< HEAD
=======
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.collections.Closure;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.archivals.model.ArchivalsStoreVO;
<<<<<<< HEAD
import ee.webmedia.alfresco.casefile.service.CaseFile;
import ee.webmedia.alfresco.classificator.constant.FieldType;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docconfig.generator.PropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.systematic.DocumentLocationGenerator.DocumentLocationState;
import ee.webmedia.alfresco.docconfig.service.DocumentConfig;
<<<<<<< HEAD
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchFilterService;
import ee.webmedia.alfresco.filter.web.AbstractSearchFilterBlockBean;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * @author Keit Tehvan
 */
=======
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.document.search.model.DocumentReportModel;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchFilterService;
import ee.webmedia.alfresco.filter.web.AbstractSearchFilterBlockBean;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public class DocumentDynamicSearchDialog extends AbstractSearchFilterBlockBean<DocumentSearchFilterService> implements DialogDataProvider {
    private static final long serialVersionUID = 1L;

    private static final Log LOG = LogFactory.getLog(DocumentDynamicSearchDialog.class);
    private static final List<String> defaultCheckedFields = Arrays.asList(
            "regNumber",
            "regDateTime",
            "senderName",
            "recipientName",
            "docName",
            "dueDate",
<<<<<<< HEAD
            "complienceDate",
            "thesaurus",
            "firstKeywordLevel");
    public static final QName SELECTED_STORES = RepoUtil.createTransientProp("selectedStores");
=======
            "complienceDate");
    public static final QName SELECTED_STORES = RepoUtil.createTransientProp("selectedStores");
    public static final QName SELECTED_REPORT_TYPE = RepoUtil.createTransientProp("selectedReportOutputType");
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    protected List<SelectItem> stores;
    protected DocumentConfig config;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);

        if (stores == null) {
            stores = new ArrayList<SelectItem>();
            stores.add(new SelectItem(BeanHelper.getFunctionsService().getFunctionsRoot(), MessageUtil.getMessage("functions_title")));
            for (ArchivalsStoreVO archivalsStoreVO : getGeneralService().getArchivalsStoreVOs()) {
                stores.add(new SelectItem(archivalsStoreVO.getNodeRef(), archivalsStoreVO.getTitle()));
            }
        }
        loadConfig();
        getPropertySheetStateBean().reset(config.getStateHolders(), this);
        if (LOG.isDebugEnabled()) {
            LOG.debug("config=" + config);
        }

        loadAllFilters();
    }

    protected void loadConfig() {
        config = getDocumentConfigService().getSearchConfig();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // don't call reset, because we don't close this dialog
        isFinished = false;
        String dialog = getDocumentSearchResultsDialog().setup(filter);
        return AlfrescoNavigationHandler.DIALOG_PREFIX + dialog;
    }

    @Override
    public void restored() {
        getPropertySheetStateBean().reset(config.getStateHolders(), this);
    }

    @Override
    public void selectedFilterValueChanged(ValueChangeEvent event) {
        super.selectedFilterValueChanged(event);
        NodeRef newValue = (NodeRef) event.getNewValue();
        if (newValue != null) {
            // remove saved filter properties that are not defined in config any more
            Set<String> currentFilterPropNames = config.getPropertySheetConfigElement().getItems().keySet();
            List<QName> removedProps = new ArrayList<QName>();
            for (Iterator<Entry<String, Object>> i = filter.getProperties().entrySet().iterator(); i.hasNext();) {
                Map.Entry<String, Object> entry = i.next();
                QName savedProp = QName.createQName(entry.getKey());
                if (RepoUtil.isSystemProperty(savedProp) || savedProp.getLocalName().contains("_")) {
                    continue;
                }
                if (!currentFilterPropNames.contains(savedProp.toPrefixString(getNamespaceService()))) {
                    removedProps.add(savedProp);
                    i.remove();
                }
            }
            // for removed properties, also remove checkbox properties
            for (QName removedProp : removedProps) {
                filter.getProperties().remove(removedProp.toString() + WMUIProperty.AFTER_LABEL_BOOLEAN);
            }
<<<<<<< HEAD
            List<FieldDefinition> searchableDocumentFieldDefinitions = getDocumentAdminService().getSearchableDocumentFieldDefinitions();
            searchableDocumentFieldDefinitions.add(createThesaurusField());
            setFilterDefaultValues(filter, searchableDocumentFieldDefinitions, defaultCheckedFields);
        }
        getPropertySheetStateBean().reset(config.getStateHolders(), this);
    }

    public static void setFilterDefaultValues(Node filterNode, List<FieldDefinition> searchableFields, List<String> defaultCheckedFields) {
        long start = System.currentTimeMillis();
        try {
=======
            setFilterDefaultValues(filter);
        }
        setFilterCaseProps();
        getPropertySheetStateBean().reset(config.getStateHolders(), this);
    }

    private void setFilterDefaultValues(Node filterNode) {
        long start = System.currentTimeMillis();
        try {
            List<FieldDefinition> searchableFields = BeanHelper.getDocumentAdminService().getSearchableFieldDefinitions();
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            Map<String, Object> filterProp = filterNode.getProperties();
            for (FieldDefinition fieldDefinition : searchableFields) {
                if (filterProp.containsKey(fieldDefinition.getQName().toString())) {
                    continue;
                }
<<<<<<< HEAD
                DynamicPropertyDefinition def = getDocumentConfigService().getPropertyDefinition(filterNode, fieldDefinition.getQName());
                if (defaultCheckedFields != null && defaultCheckedFields.contains(def.getName().getLocalName())) {
                    filterProp.put(fieldDefinition.getQName().toString() + WMUIProperty.AFTER_LABEL_BOOLEAN, Boolean.TRUE);
                }
                if (def.isMultiValued()) {
                    ArrayList<Object> arrayList = new ArrayList<Object>();
                    if (def.getDataTypeQName().equals(DataTypeDefinition.TEXT) && !FieldType.STRUCT_UNIT.equals(def.getFieldType())) {
                        arrayList.add("");
                    }
                    filterProp.put(fieldDefinition.getQName().toString(), arrayList);
=======
                PropertyDefinition def = getDocumentConfigService().getPropertyDefinition(filterNode, fieldDefinition.getQName());
                if (defaultCheckedFields.contains(def.getName().getLocalName())) {
                    filterProp.put(fieldDefinition.getQName().toString() + WMUIProperty.AFTER_LABEL_BOOLEAN, Boolean.TRUE);
                }
                if (def.isMultiValued()) {
                    filterProp.put(fieldDefinition.getQName().toString(), new ArrayList<Object>());
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
                }
            }
        } finally {
            LOG.info("search filter default values: " + (System.currentTimeMillis() - start) + "ms");
        }
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "search");
    }

    @Override
    protected String getBlankFilterNameMessageKey() {
        return "document_search_save_error_nameIsBlank";
    }

    @Override
    protected String getFilterModifyDeniedMessageKey() {
        return "document_search_filter_modify_accessDenied";
    }

    @Override
    protected String getFilterDeleteDeniedMessageKey() {
        return "document_search_filter_delete_accessDenied";
    }

    @Override
    protected String getNewFilterSelectItemMessageKey() {
        return "document_search_new";
    }

    public PropertySheetConfigElement getPropertySheetConfigElement() {
        return config.getPropertySheetConfigElement();
    }

    public void storeValueChangeListener(ValueChangeEvent event) {
        @SuppressWarnings("unchecked")
        List<NodeRef> selectedStores = (List<NodeRef>) event.getNewValue();
        getNode().getProperties().put(SELECTED_STORES.toString(), selectedStores);
<<<<<<< HEAD

=======
    }

    public void reportTypeChanged(ValueChangeEvent event) {
        final String selectedType = (String) event.getNewValue();

        ComponentUtil.executeLater(PhaseId.INVOKE_APPLICATION, getPropertySheet(), new Closure() {

            @Override
            public void execute(Object arg0) {
                final Map<String, Object> props = getNode().getProperties();
                QName type = DocumentReportModel.Props.REPORT_OUTPUT_TYPE;
                props.put(type.toString(), selectedType);
                props.put(type.getLocalName(), selectedType);

                clearPropertySheet();
            }
        });

    }

    private void clearPropertySheet() {
        UIPropertySheet propertySheet = getPropertySheet();
        if (propertySheet != null) {
            propertySheet.getChildren().clear();
            propertySheet.getClientValidations().clear();
        }
    }

    protected void resetDocumentLocationState() {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        for (PropertySheetStateHolder stateHolder : config.getStateHolders().values()) { // State holder key varies
            if (stateHolder instanceof DocumentLocationState) {
                ((DocumentLocationState) stateHolder).reset(isInEditMode());
                return;
            }
        }
    }

    @Override
<<<<<<< HEAD
=======
    public void saveFilter(ActionEvent event) {
        super.saveFilter(event);
        resetDocumentLocationState();
    }

    @Override
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    protected Node getNewFilter() {
        long start = System.currentTimeMillis();
        try {
            Map<QName, Serializable> data = getMandatoryProps();
            TransientNode transientNode = new TransientNode(getFilterType(), null, data);
            transientNode.getProperties().put(DocumentSearchModel.Props.DOCUMENT_TYPE.toString() + WMUIProperty.AFTER_LABEL_BOOLEAN, Boolean.TRUE);
            transientNode.getProperties().put(DocumentSearchModel.Props.SEND_MODE.toString() + WMUIProperty.AFTER_LABEL_BOOLEAN, Boolean.TRUE);
            transientNode.getProperties().put(DocumentSearchModel.Props.DOCUMENT_CREATED.toString() + WMUIProperty.AFTER_LABEL_BOOLEAN, Boolean.TRUE);
<<<<<<< HEAD
            List<FieldDefinition> searchableDocumentFieldDefinitions = getDocumentAdminService().getSearchableDocumentFieldDefinitions();
            searchableDocumentFieldDefinitions.add(createThesaurusField());
            setFilterDefaultValues(transientNode, searchableDocumentFieldDefinitions, defaultCheckedFields);
=======
            setFilterDefaultValues(transientNode);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            return transientNode;
        } finally {
            LOG.info("New search filter generation: " + (System.currentTimeMillis() - start) + "ms");
        }
    }

<<<<<<< HEAD
    public static FieldDefinition createThesaurusField() {
        FieldDefinition thesaurusDef = BeanHelper.getDocumentAdminService().createNewUnSavedFieldDefinition();
        thesaurusDef.setFieldId(DocumentDynamicModel.Props.THESAURUS.getLocalName());
        thesaurusDef.setOriginalFieldId(DocumentDynamicModel.Props.THESAURUS.getLocalName());
        thesaurusDef.setName(MessageUtil.getMessage("thesaurus"));
        thesaurusDef.setFieldTypeEnum(FieldType.TEXT_FIELD);
        return thesaurusDef;
    }

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    protected Map<QName, Serializable> getMandatoryProps() {
        Map<QName, Serializable> data = new HashMap<QName, Serializable>();
        data.put(DocumentSearchModel.Props.STORE, new ArrayList<Object>());
        data.put(DocumentSearchModel.Props.DOCUMENT_TYPE, new ArrayList<Object>());
        Map<QName, PropertyDefinition> propDefs = BeanHelper.getDictionaryService().getPropertyDefs(getFilterType());
        for (Map.Entry<QName, PropertyDefinition> entry : propDefs.entrySet()) {
            PropertyDefinition propDef = entry.getValue();
            if (propDef.isMultiValued()) {
                data.put(entry.getKey(), new ArrayList<Object>());
            }
        }
        return data;
    }

    @Override
    public QName getFilterType() {
        return DocumentSearchModel.Types.FILTER;
    }

    @Override
    protected void reset() {
        super.reset();
        // searchOutput, stores doesn't need to be set to null, they never change
        getDocumentSearchBean().reset();
        getPropertySheetStateBean().reset(null, null);
    }

    @Override
    public String getManageSavedBlockTitle() {
        return MessageUtil.getMessage("document_search_saved_manage");
    }

    @Override
    public String getSavedFilterSelectTitle() {
        return MessageUtil.getMessage("document_search_saved");
    }

    @Override
    public String getFilterPanelTitle() {
        return MessageUtil.getMessage("document_search");
    }

    // GeneralSelectorGenerator 'selectionItems' method bindings

    /**
     * @param context
     * @param selectComponent
     * @return dropDown items for JSP
     */
    public List<SelectItem> getStores(FacesContext context, UIInput selectComponent) {
        ((HtmlSelectManyListbox) selectComponent).setSize(5);
        return stores;
    }

    // START: getters / setters

    @Override
    protected DocumentSearchFilterService getFilterService() {
        if (filterService == null) {
            filterService = BeanHelper.getDocumentSearchFilterService();
        }
        return filterService;
    }

    // END: getters / setters

    @Override
    public DocumentDynamic getDocument() {
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
        throw new UnsupportedOperationException(); // TODO refactor this method out of this interface
    }

<<<<<<< HEAD
    @Override
    public CaseFile getCaseFile() {
        // Not used.
        return null;
    }

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}
