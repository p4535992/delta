package ee.webmedia.alfresco.document.search.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentConfigService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentSearchBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentSearchResultsDialog;
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPropertySheetStateBean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlSelectManyListbox;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.alfresco.web.config.PropertySheetConfigElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.archivals.model.ArchivalsStoreVO;
import ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docconfig.service.DocumentConfig;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchFilterService;
import ee.webmedia.alfresco.filter.web.AbstractSearchFilterBlockBean;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Keit Tehvan
 */
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
            "complienceDate");

    private List<SelectItem> stores;
    private DocumentConfig config;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);

        if (stores == null) {
            stores = new ArrayList<SelectItem>();
            stores.add(new SelectItem(getGeneralService().getStore(), MessageUtil.getMessage("functions_title")));
            for (ArchivalsStoreVO archivalsStoreVO : getGeneralService().getArchivalsStoreVOs()) {
                stores.add(new SelectItem(archivalsStoreVO.getStoreRef(), archivalsStoreVO.getTitle()));
            }
        }

        config = getDocumentConfigService().getSearchConfig();
        getPropertySheetStateBean().reset(config.getStateHolders(), this);
        LOG.info("config=" + config);

        loadAllFilters();
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

    @Override
    protected Node getNewFilter() {
        long start = System.currentTimeMillis();
        try {
            Map<QName, Serializable> data = new HashMap<QName, Serializable>();
            data.put(DocumentSearchModel.Props.STORE, new ArrayList<Object>());
            data.put(DocumentSearchModel.Props.DOCUMENT_TYPE, new ArrayList<Object>());
            Map<QName, PropertyDefinition> propDefs = BeanHelper.getDictionaryService().getPropertyDefs(DocumentSearchModel.Types.FILTER);
            for (Map.Entry<QName, PropertyDefinition> entry : propDefs.entrySet()) {
                PropertyDefinition propDef = entry.getValue();
                if (propDef.isMultiValued()) {
                    data.put(entry.getKey(), new ArrayList<Object>());
                }
            }

            TransientNode transientNode = new TransientNode(DocumentSearchModel.Types.FILTER, null, data);
            transientNode.getProperties().put(DocumentSearchModel.Props.DOCUMENT_TYPE.toString() + WMUIProperty.AFTER_LABEL_BOOLEAN, Boolean.TRUE);
            transientNode.getProperties().put(DocumentSearchModel.Props.SEND_MODE.toString() + WMUIProperty.AFTER_LABEL_BOOLEAN, Boolean.TRUE);
            List<FieldDefinition> searchableFields = BeanHelper.getDocumentAdminService().getSearchableFieldDefinitions();
            for (FieldDefinition fieldDefinition : searchableFields) {
                PropertyDefinition def = getDocumentConfigService().getPropertyDefinition(transientNode, fieldDefinition.getQName());
                if (defaultCheckedFields.contains(def.getName().getLocalName())) {
                    transientNode.getProperties().put(fieldDefinition.getQName().toString() + WMUIProperty.AFTER_LABEL_BOOLEAN, Boolean.TRUE);
                }
                if (def.isMultiValued()) {
                    transientNode.getProperties().put(fieldDefinition.getQName().toString(), new ArrayList<Object>());
                }
            }
            return transientNode;
        } finally {
            LOG.info("New search filter generation: " + (System.currentTimeMillis() - start) + "ms");
        }
    }

    @Override
    protected void reset() {
        super.reset();
        // searchOutput, stores doesn't need to be set to null, they never change
        getDocumentSearchBean().reset();
        getPropertySheetStateBean().reset(null, null);
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

}
