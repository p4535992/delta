package ee.webmedia.alfresco.volume.search.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentConfigService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPropertySheetStateBean;
import static ee.webmedia.alfresco.document.search.web.DocumentDynamicSearchDialog.createThesaurusField;
import static ee.webmedia.alfresco.document.search.web.DocumentDynamicSearchDialog.setFilterDefaultValues;

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
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.alfresco.web.config.PropertySheetConfigElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.casefile.service.CaseFile;
import ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.CaseFileType;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docconfig.generator.PropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.systematic.DocumentLocationGenerator.DocumentLocationState;
import ee.webmedia.alfresco.docconfig.service.DocumentConfig;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.filter.web.AbstractSearchFilterBlockBean;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.volume.search.model.VolumeSearchModel;
import ee.webmedia.alfresco.volume.search.service.VolumeSearchFilterService;

<<<<<<< HEAD
/**
 * @author Keit Tehvan
 */
=======
>>>>>>> develop-5.1
public class VolumeDynamicSearchDialog extends AbstractSearchFilterBlockBean<VolumeSearchFilterService> implements DialogDataProvider {
    private static final long serialVersionUID = 1L;

    private static final Log LOG = LogFactory.getLog(VolumeDynamicSearchDialog.class);
    private static final List<String> defaultCheckedFields = Arrays.asList(
            "regNumber",
            "caseVolumeType",
            "thesaurus",
            "firstLevelKeyword");
    public static final QName SELECTED_STORES = RepoUtil.createTransientProp("selectedStores");

    protected List<SelectItem> stores;
    protected List<SelectItem> caseFileTypes;
    protected DocumentConfig config;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);

        if (stores == null) {
            stores = getVolumeSearchStores();
        }
        caseFileTypes = new ArrayList<SelectItem>();
        List<CaseFileType> volTypes = getDocumentAdminService().getUsedCaseFileTypes(DocumentAdminService.DONT_INCLUDE_CHILDREN);
        for (CaseFileType caseFileType : volTypes) {
            caseFileTypes.add(new SelectItem(caseFileType.getId(), caseFileType.getName()));
        }
        WebUtil.sort(caseFileTypes);
        loadConfig();
        getPropertySheetStateBean().reset(config.getStateHolders(), this);
        if (LOG.isDebugEnabled()) {
            LOG.debug("config=" + config);
        }

        loadAllFilters();
    }

    public static ArrayList<SelectItem> getVolumeSearchStores() {
        ArrayList<SelectItem> stores = new ArrayList<SelectItem>();
        List<Pair<NodeRef, String>> allStoresWithTitle = BeanHelper.getDocumentSearchService().getAllVolumeSearchStores();
        for (Pair<NodeRef, String> storeAndName : allStoresWithTitle) {
            stores.add(new SelectItem(storeAndName.getFirst(), storeAndName.getSecond()));
        }
        return stores;
    }

    protected void loadConfig() {
        config = getDocumentConfigService().getVolumeSearchFilterConfig(true);
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // don't call reset, because we don't close this dialog
        isFinished = false;
        BeanHelper.getVolumeSearchResultsDialog().setup(filter);
        return AlfrescoNavigationHandler.DIALOG_PREFIX + "volumeSearchResultsDialog";
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
            List<FieldDefinition> searchableVolumeFieldDefinitions = BeanHelper.getDocumentAdminService().getSearchableVolumeFieldDefinitions();
            searchableVolumeFieldDefinitions.add(createThesaurusField());
            setFilterDefaultValues(filter, searchableVolumeFieldDefinitions, defaultCheckedFields);
        }
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "search");
    }

    @Override
    protected String getBlankFilterNameMessageKey() {
        return "volume_search_save_error_nameIsBlank";
    }

    @Override
    protected String getFilterModifyDeniedMessageKey() {
        return "volume_search_filter_modify_accessDenied";
    }

    @Override
    protected String getFilterDeleteDeniedMessageKey() {
        return "volume_search_filter_delete_accessDenied";
    }

    @Override
    protected String getNewFilterSelectItemMessageKey() {
        return "volume_search_new";
    }

    public PropertySheetConfigElement getPropertySheetConfigElement() {
        return config.getPropertySheetConfigElement();
    }

<<<<<<< HEAD
    public void storeValueChangeListener(ValueChangeEvent event) {
=======
    public void storeValueChanged(ValueChangeEvent event) {
>>>>>>> develop-5.1
        @SuppressWarnings("unchecked")
        List<NodeRef> selectedStores = (List<NodeRef>) event.getNewValue();
        getNode().getProperties().put(SELECTED_STORES.toString(), selectedStores);

        for (PropertySheetStateHolder stateHolder : config.getStateHolders().values()) { // State holder key varies
            if (stateHolder instanceof DocumentLocationState) {
                ((DocumentLocationState) stateHolder).reset(isInEditMode());
                return;
            }
        }
    }

    @Override
    protected Node getNewFilter() {
        long start = System.currentTimeMillis();
        try {
            Map<QName, Serializable> data = getMandatoryProps();
            TransientNode transientNode = new TransientNode(getFilterType(), null, data);
            transientNode.getProperties().put(VolumeSearchModel.Props.CASE_FILE_TYPE.toString() + WMUIProperty.AFTER_LABEL_BOOLEAN, Boolean.TRUE);
            List<FieldDefinition> searchableVolumeFieldDefinitions = BeanHelper.getDocumentAdminService().getSearchableVolumeFieldDefinitions();
            searchableVolumeFieldDefinitions.add(createThesaurusField());
            setFilterDefaultValues(transientNode, searchableVolumeFieldDefinitions, defaultCheckedFields);
            return transientNode;
        } finally {
            LOG.info("New search filter generation: " + (System.currentTimeMillis() - start) + "ms");
        }
    }

    protected Map<QName, Serializable> getMandatoryProps() {
        Map<QName, Serializable> data = new HashMap<QName, Serializable>();
        data.put(VolumeSearchModel.Props.STORE, new ArrayList<Object>());
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
        return VolumeSearchModel.Types.FILTER;
    }

    @Override
    protected void reset() {
        super.reset();
        getPropertySheetStateBean().reset(null, null);
    }

    @Override
    public String getManageSavedBlockTitle() {
        return MessageUtil.getMessage("volume_search_saved_manage");
    }

    @Override
    public String getSavedFilterSelectTitle() {
        return MessageUtil.getMessage("volume_search_saved");
    }

    @Override
    public String getFilterPanelTitle() {
        return MessageUtil.getMessage("volume_search");
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

    public List<SelectItem> getCaseFileTypes(FacesContext context, UIInput selectComponent) {
        ((HtmlSelectManyListbox) selectComponent).setSize(5);
        return caseFileTypes;
    }

    // START: getters / setters

    @Override
    protected VolumeSearchFilterService getFilterService() {
        if (filterService == null) {
            filterService = BeanHelper.getVolumeSearchFilterService();
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
        throw new UnsupportedOperationException();
    }

    @Override
    public CaseFile getCaseFile() {
        // Not used.
        return null;
    }

}
