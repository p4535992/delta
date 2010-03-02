package ee.webmedia.alfresco.document.search.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlSelectManyListbox;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchFilterService;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.type.model.DocumentType;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.utils.WebUtil;

/**
 * @author Alar Kvell
 */
public class DocumentSearchDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    private static String OUTPUT_SIMPLE = "simple";
    private static String OUTPUT_EXTENDED = "extended";

    private transient UIPropertySheet propertySheet;
    private transient HtmlSelectOneMenu selectedFilterMenu;

    private DocumentSearchResultsDialog documentSearchResultsDialog;

    private transient DocumentSearchService documentSearchService;
    private transient DocumentSearchFilterService documentSearchFilterService;
    private transient UserService userService;
    private transient DocumentTypeService documentTypeService;
    private transient FunctionsService functionsService;
    private transient SeriesService seriesService;
    private transient OrganizationStructureService organizationStructureService;
    private transient DocumentService documentService;

    private Node filter;
    private String filterName;
    private List<SelectItem> searchOutput;
    private List<SelectItem> documentTypes;
    private List<SelectItem> functions;
    private List<SelectItem> series;
    private List<SelectItem> allFilters;
    private NodeRef selectedFilter;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        reset();

        filter = getNewFilter();
        filterName = null;

        // Search output types
        if (searchOutput == null) {
            searchOutput = new ArrayList<SelectItem>(2);
            searchOutput.add(new SelectItem(OUTPUT_SIMPLE, MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_search_output_simple")));
            searchOutput.add(new SelectItem(OUTPUT_EXTENDED, MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_search_output_extended")));
        }

        // Document types
        List<DocumentType> types = getDocumentTypeService().getAllDocumentTypes(true);
        documentTypes = new ArrayList<SelectItem>(types.size());
        for (DocumentType documentType : types) {
            documentTypes.add(new SelectItem(documentType.getId(), documentType.getName()));
        }
        WebUtil.sort(documentTypes);

        // Functions
        List<Function> allFunctions = getFunctionsService().getAllFunctions();
        functions = new ArrayList<SelectItem>(allFunctions.size() + 1);
        functions.add(new SelectItem("", ""));
        for (Function function : allFunctions) {
            functions.add(new SelectItem(function.getNodeRef(), function.getMark() + " " + function.getTitle()));
        }
        WebUtil.sort(functions);

        // Series
        series = Collections.emptyList();

        loadAllFilters();
    }

    private Node getNewFilter() {
        // New empty filter
        Node node = new TransientNode(DocumentSearchModel.Types.FILTER, null, null);

        // UISelectMany components don't want null as initial value
        node.getProperties().put(DocumentSearchModel.Props.DOCUMENT_TYPE.toString(), new ArrayList<QName>());
        node.getProperties().put(DocumentSearchModel.Props.SERIES.toString(), new ArrayList<NodeRef>());
        node.getProperties().put(DocumentSearchModel.Props.DOC_STATUS.toString(), new ArrayList<String>());
        node.getProperties().put(DocumentSearchModel.Props.ACCESS_RESTRICTION.toString(), new ArrayList<String>());
        node.getProperties().put(DocumentSearchModel.Props.STORAGE_TYPE.toString(), new ArrayList<String>());
        node.getProperties().put(DocumentSearchModel.Props.SEND_MODE.toString(), new ArrayList<String>());
        node.getProperties().put(DocumentSearchModel.Props.COST_MANAGER.toString(), new ArrayList<String>());
        node.getProperties().put(DocumentSearchModel.Props.ERRAND_COUNTY.toString(), new ArrayList<String>());
        node.getProperties().put(DocumentSearchModel.Props.PROCUREMENT_TYPE.toString(), new ArrayList<String>());

        return node;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // don't call reset, because we don't close this dialog
        isFinished = false;
        List<Document> documents = getDocumentSearchService().searchDocuments(filter);
        String dialog = "documentSearchResultsDialog";
        if (OUTPUT_EXTENDED.equals(filter.getProperties().get(DocumentSearchModel.Props.OUTPUT))) {
            dialog = "documentSearchExtendedResultsDialog";
            documents = getDocumentService().processExtendedSearchResults(documents, filter);
        }
        documentSearchResultsDialog.setup(documents);
        return AlfrescoNavigationHandler.DIALOG_PREFIX + dialog;
    }

    @Override
    public String cancel() {
        reset();
        return super.cancel();
    }

    private void reset() {
        propertySheet = null;
        selectedFilterMenu = null;
        filter = null;
        filterName = null;
        // searchOutput doesn't need to be set to null, it never changes
        documentTypes = null;
        functions = null;
        series = null;
        allFilters = null;
        selectedFilter = null;
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "search");
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

    public void saveFilter(ActionEvent event) {
        if (filterName == null) {
            filterName = "";
        }
        if (filterName.equals(filter.getProperties().get(DocumentSearchModel.Props.NAME))) {
            filter = getDocumentSearchFilterService().createOrSaveFilter(filter);
        } else {
            filter = getDocumentSearchFilterService().createFilter(filter);
        }
        filterName = (String) filter.getProperties().get(DocumentSearchModel.Props.NAME);
        propertySheet.getChildren().clear();
        loadAllFilters();
        selectedFilter = filter.getNodeRef();
    }

    public void deleteFilter(ActionEvent event) {
        if (!(filter instanceof TransientNode)) {
            getDocumentSearchFilterService().deleteFilter(filter.getNodeRef());
        }
        filter = getNewFilter();
        filterName = (String) filter.getProperties().get(DocumentSearchModel.Props.NAME);
        propertySheet.getChildren().clear();
        loadAllFilters();
        selectedFilter = null;
    }

    public List<SelectItem> getAllFilters() {
        // Must be called after component is added to component tree
        selectedFilterMenu.setOnchange(Utils.generateFormSubmit(FacesContext.getCurrentInstance(), selectedFilterMenu));

        if (allFilters == null) {
            loadAllFilters();
        }
        return allFilters;
    }

    private void loadAllFilters() {
        allFilters = new ArrayList<SelectItem>();
        allFilters.add(new SelectItem("", MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_search_new")));
        Set<Entry<NodeRef, String>> entrySet = getDocumentSearchFilterService().getFilters().entrySet();
        for (Entry<NodeRef, String> entry : entrySet) {
            allFilters.add(new SelectItem(entry.getKey(), entry.getValue()));
        }
        WebUtil.sort(allFilters);
    }

    public void selectedFilterValueChanged(ValueChangeEvent event) {
        NodeRef newValue = (NodeRef) event.getNewValue();
        if (newValue == null) {
            filter = getNewFilter();
        } else {
            filter = getDocumentSearchFilterService().getFilter(newValue);
        }
        filterName = (String) filter.getProperties().get(DocumentSearchModel.Props.NAME);
        propertySheet.getChildren().clear();
    }

    public NodeRef getSelectedFilter() {
        return selectedFilter;
    }

    public void setSelectedFilter(NodeRef selectedFilter) {
        this.selectedFilter = selectedFilter;
    }

    // GeneralSelectorGenerator 'selectionItems' method bindings

    public List<SelectItem> getDocumentTypes(FacesContext context, UIInput selectComponent) {
        ((HtmlSelectManyListbox) selectComponent).setSize(5);
        return documentTypes;
    }

    public List<SelectItem> getFunctions(FacesContext context, UIInput selectComponent) {
        return functions;
    }

    public List<SelectItem> getSeries(FacesContext context, UIInput selectComponent) {
        return series;
    }

    public List<SelectItem> getSearchOutput(FacesContext context, UIInput selectComponent) {
        return searchOutput;
    }

    // Functions selector value change event listener
    public void functionValueChanged(ValueChangeEvent event) {
        NodeRef function = (NodeRef) event.getNewValue();
        if (function == null) {
            series = Collections.emptyList();
        } else {
            List<Series> allSeries = getSeriesService().getAllSeriesByFunction(function);
            series = new ArrayList<SelectItem>(allSeries.size());
            for (Series serie : allSeries) {
                series.add(new SelectItem(serie.getNode().getNodeRef(), serie.getSeriesIdentifier() + " " + serie.getTitle()));
            }
        }
        WebUtil.sort(series);

        // Refresh series list
        @SuppressWarnings("unchecked")
        List<UIComponent> children = getPropertySheet().getChildren();
        for (UIComponent component : children) {
            if (component.getId().endsWith("_series")) {
                HtmlSelectManyListbox seriesList = (HtmlSelectManyListbox) component.getChildren().get(1);
                ComponentUtil.setSelectItems(FacesContext.getCurrentInstance(), seriesList, series);
                break;
            }
        }
    }

    // SearchGenerator 'setterCallback' method bindings

    public void setRecipientName(String nodeRefStr) {
        NodeRef nodeRef = new NodeRef(nodeRefStr);

        String name = "";
        Map<QName, Serializable> props = getNodeService().getProperties(nodeRef);
        if (AddressbookModel.Types.ORGANIZATION.equals(getNodeService().getType(nodeRef))) {
            name = (String) props.get(AddressbookModel.Props.ORGANIZATION_NAME);
        } else {
            name = UserUtil.getPersonFullName((String) props.get(AddressbookModel.Props.PERSON_FIRST_NAME), (String) props
                    .get(AddressbookModel.Props.PERSON_LAST_NAME));
        }

        Map<String, Object> filterProps = filter.getProperties();
        filterProps.put(DocumentSearchModel.Props.RECIPIENT_NAME.toString(), name);
    }

    public void setOwner(String userName) {
        Map<QName, Serializable> personProps = getUserService().getUserProperties(userName);

        Map<String, Object> filterProps = filter.getProperties();
        filterProps.put(DocumentSearchModel.Props.OWNER_NAME.toString(), UserUtil.getPersonFullName1(personProps));
        filterProps.put(DocumentSearchModel.Props.OWNER_ORG_STRUCT_UNIT.toString(), getOrganizationStructureService().getOrganizationStructure(
                (String) personProps.get(ContentModel.PROP_ORGID)));
        filterProps.put(DocumentSearchModel.Props.OWNER_JOB_TITLE.toString(), personProps.get(ContentModel.PROP_JOBTITLE));
    }

    public void setSigner(String userName) {
        Map<QName, Serializable> personProps = getUserService().getUserProperties(userName);

        Map<String, Object> filterProps = filter.getProperties();
        filterProps.put(DocumentSearchModel.Props.SIGNER_NAME.toString(), UserUtil.getPersonFullName1(personProps));
        filterProps.put(DocumentSearchModel.Props.SIGNER_JOB_TITLE.toString(), personProps.get(ContentModel.PROP_JOBTITLE));
    }

    // START: getters / setters

    public Node getFilter() {
        return filter;
    }

    public void setPropertySheet(UIPropertySheet propertySheet) {
        this.propertySheet = propertySheet;
    }

    public UIPropertySheet getPropertySheet() {
        return propertySheet;
    }

    public void setSelectedFilterMenu(HtmlSelectOneMenu selectedFilterMenu) {
        this.selectedFilterMenu = selectedFilterMenu;
    }

    public HtmlSelectOneMenu getSelectedFilterMenu() {
        return selectedFilterMenu;
    }

    public void setDocumentSearchResultsDialog(DocumentSearchResultsDialog documentSearchResultsDialog) {
        this.documentSearchResultsDialog = documentSearchResultsDialog;
    }

    protected DocumentSearchService getDocumentSearchService() {
        if (documentSearchService == null) {
            documentSearchService = (DocumentSearchService) FacesContextUtils.getRequiredWebApplicationContext( // 
                    FacesContext.getCurrentInstance()).getBean(DocumentSearchService.BEAN_NAME);
        }
        return documentSearchService;
    }

    protected DocumentSearchFilterService getDocumentSearchFilterService() {
        if (documentSearchFilterService == null) {
            documentSearchFilterService = (DocumentSearchFilterService) FacesContextUtils.getRequiredWebApplicationContext( // 
                    FacesContext.getCurrentInstance()).getBean(DocumentSearchFilterService.BEAN_NAME);
        }
        return documentSearchFilterService;
    }

    protected UserService getUserService() {
        if (userService == null) {
            userService = (UserService) FacesContextUtils.getRequiredWebApplicationContext( // 
                    FacesContext.getCurrentInstance()).getBean(UserService.BEAN_NAME);
        }
        return userService;
    }

    protected DocumentTypeService getDocumentTypeService() {
        if (documentTypeService == null) {
            documentTypeService = (DocumentTypeService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(DocumentTypeService.BEAN_NAME);
        }
        return documentTypeService;
    }

    protected FunctionsService getFunctionsService() {
        if (functionsService == null) {
            functionsService = (FunctionsService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(FunctionsService.BEAN_NAME);
        }
        return functionsService;
    }

    protected SeriesService getSeriesService() {
        if (seriesService == null) {
            seriesService = (SeriesService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(SeriesService.BEAN_NAME);
        }
        return seriesService;
    }

    protected OrganizationStructureService getOrganizationStructureService() {
        if (organizationStructureService == null) {
            organizationStructureService = (OrganizationStructureService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(OrganizationStructureService.BEAN_NAME);
        }
        return organizationStructureService;
    }

    protected DocumentService getDocumentService() {
        if (documentService == null) {
            documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(DocumentService.BEAN_NAME);
        }
        return documentService;
    }

    // END: getters / setters
}
