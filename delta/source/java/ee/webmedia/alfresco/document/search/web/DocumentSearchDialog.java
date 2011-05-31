package ee.webmedia.alfresco.document.search.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlSelectManyListbox;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.addressbook.web.dialog.AddressbookMainViewDialog;
import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchFilterService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.filter.web.AbstractSearchFilterBlockBean;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * @author Alar Kvell
 */
public class DocumentSearchDialog extends AbstractSearchFilterBlockBean<DocumentSearchFilterService> {

    private static final long serialVersionUID = 1L;

    public static String OUTPUT_EXTENDED = "extended";

    private static String OUTPUT_SIMPLE = "simple";

    private DocumentSearchResultsDialog documentSearchResultsDialog;
    private DocumentSearchBean documentSearchBean;

    private transient FunctionsService functionsService;
    private transient SeriesService seriesService;
    private transient VolumeService volumeService;
    private transient CaseService caseService;
    private transient OrganizationStructureService organizationStructureService;
    private transient DocumentService documentService;
    private transient GeneralService generalService;

    private List<SelectItem> stores;
    private List<SelectItem> searchOutput;
    private List<SelectItem> functions;
    private List<SelectItem> series;
    private List<SelectItem> volumes;
    private List<SelectItem> cases;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);

        // Search output types
        if (searchOutput == null) {
            searchOutput = new ArrayList<SelectItem>(2);
            searchOutput.add(new SelectItem(OUTPUT_SIMPLE, MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_search_output_simple")));
            searchOutput.add(new SelectItem(OUTPUT_EXTENDED, MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_search_output_extended")));
        }

        if (stores == null) {
            stores = new ArrayList<SelectItem>(2);
            stores.add(new SelectItem(getGeneralService().getStore(), MessageUtil.getMessage("functions_title")));
            stores.add(new SelectItem(getGeneralService().getArchivalsStoreRef(), MessageUtil.getMessage("archivals_list")));
        }

        // Functions
        List<Function> allFunctions = getFunctionsService().getAllFunctions();
        functions = new ArrayList<SelectItem>(allFunctions.size() + 1);
        functions.add(new SelectItem("", ""));
        for (Function function : allFunctions) {
            functions.add(new SelectItem(function.getNodeRef(), function.getMark() + " " + function.getTitle()));
        }

        series = Collections.emptyList();
        volumes = Collections.emptyList();
        cases = Collections.emptyList();

        loadAllFilters();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // don't call reset, because we don't close this dialog
        isFinished = false;
        String dialog = documentSearchResultsDialog.setup(filter);
        return AlfrescoNavigationHandler.DIALOG_PREFIX + dialog;
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

    @Override
    protected Node getNewFilter() {
        // New empty filter
        Node node = new TransientNode(DocumentSearchModel.Types.FILTER, null, null);

        // UISelectMany components don't want null as initial value
        node.getProperties().put(DocumentSearchModel.Props.STORE.toString(), new ArrayList<StoreRef>());
        node.getProperties().put(DocumentSearchModel.Props.DOCUMENT_TYPE.toString(), new ArrayList<QName>());
        node.getProperties().put(DocumentSearchModel.Props.DOC_STATUS.toString(), new ArrayList<String>());
        node.getProperties().put(DocumentSearchModel.Props.ACCESS_RESTRICTION.toString(), new ArrayList<String>());
        node.getProperties().put(DocumentSearchModel.Props.STORAGE_TYPE.toString(), new ArrayList<String>());
        node.getProperties().put(DocumentSearchModel.Props.SEND_MODE.toString(), new ArrayList<String>());
        node.getProperties().put(DocumentSearchModel.Props.COST_MANAGER.toString(), new ArrayList<String>());
        node.getProperties().put(DocumentSearchModel.Props.ERRAND_COUNTY.toString(), new ArrayList<String>());
        node.getProperties().put(DocumentSearchModel.Props.PROCUREMENT_TYPE.toString(), new ArrayList<String>());
        node.getProperties().put(DocumentSearchModel.Props.FUND.toString(), new ArrayList<String>());
        node.getProperties().put(DocumentSearchModel.Props.FUNDS_CENTER.toString(), new ArrayList<String>());
        node.getProperties().put(DocumentSearchModel.Props.EA_COMMITMENT_ITEM.toString(), new ArrayList<String>());

        return node;
    }

    @Override
    protected void reset() {
        super.reset();
        // searchOutput, stores doesn't need to be set to null, they never change
        documentSearchBean.reset();
        functions = null;
        series = null;
        volumes = null;
        cases = null;
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

    /**
     * @param context
     * @param selectComponent
     * @return dropdown items for JSP
     */
    public List<SelectItem> getSearchOutput(FacesContext context, UIInput selectComponent) {
        return searchOutput;
    }

    // Functions selector value change event listener
    public void functionValueChanged(ValueChangeEvent event) {
        series = Collections.emptyList();
        volumes = Collections.emptyList();
        cases = Collections.emptyList();

        NodeRef functionRef = (NodeRef) event.getNewValue();
        updateSelections(functionRef, null, null, true);
    }

    public void seriesValueChanged(ValueChangeEvent event) {
        volumes = Collections.emptyList();
        cases = Collections.emptyList();

        NodeRef seriesRef = (NodeRef) event.getNewValue();
        updateSelections(null, seriesRef, null, true);
    }

    public void volumeValueChanged(ValueChangeEvent event) {
        cases = Collections.emptyList();

        NodeRef volumeRef = (NodeRef) event.getNewValue();
        updateSelections(null, null, volumeRef, true);
    }

    @Override
    public void selectedFilterValueChanged(ValueChangeEvent event) {
        super.selectedFilterValueChanged(event);

        series = Collections.emptyList();
        volumes = Collections.emptyList();
        cases = Collections.emptyList();

        Map<String, Object> props = filter.getProperties();
        NodeRef functionRef = (NodeRef) props.get(DocumentSearchModel.Props.FUNCTION.toString());
        NodeRef seriesRef = (NodeRef) props.get(DocumentSearchModel.Props.SERIES.toString());
        NodeRef volumeRef = (NodeRef) props.get(DocumentSearchModel.Props.VOLUME.toString());
        updateSelections(functionRef, seriesRef, volumeRef, false);
    }

    // SearchGenerator 'setterCallback' method bindings

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

    public void setSellerPartyName(String nodeRefStr) {
        NodeRef nodeRef = new NodeRef(nodeRefStr);
        filter.getProperties().put(DocumentSearchModel.Props.SELLER_PARTY_NAME.toString(),
                AddressbookMainViewDialog.getContactFullName(getNodeService().getProperties(nodeRef), getNodeService().getType(nodeRef)));
    }

    // START: getters / setters
    public void setDocumentSearchResultsDialog(DocumentSearchResultsDialog documentSearchResultsDialog) {
        this.documentSearchResultsDialog = documentSearchResultsDialog;
    }

    public void setDocumentSearchBean(DocumentSearchBean documentSearchBean) {
        this.documentSearchBean = documentSearchBean;
    }

    @Override
    protected DocumentSearchFilterService getFilterService() {
        if (filterService == null) {
            filterService = (DocumentSearchFilterService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(DocumentSearchFilterService.BEAN_NAME);
        }
        return filterService;
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

    protected VolumeService getVolumeService() {
        if (volumeService == null) {
            volumeService = (VolumeService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(VolumeService.BEAN_NAME);
        }
        return volumeService;
    }

    protected CaseService getCaseService() {
        if (caseService == null) {
            caseService = (CaseService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(CaseService.BEAN_NAME);
        }
        return caseService;
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

    protected GeneralService getGeneralService() {
        if (generalService == null) {
            generalService = (GeneralService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    GeneralService.BEAN_NAME);
        }
        return generalService;
    }

    // END: getters / setters

    private void updateSelections(NodeRef functionRef, NodeRef seriesRef, NodeRef volumeRef, boolean updateComponents) {
        if (functionRef != null) {
            List<Series> allSeries = getSeriesService().getAllSeriesByFunction(functionRef);
            series = new ArrayList<SelectItem>(allSeries.size());
            series.add(new SelectItem("", ""));
            for (Series serie : allSeries) {
                series.add(new SelectItem(serie.getNode().getNodeRef(), serie.getSeriesIdentifier() + " " + serie.getTitle()));
            }
        }

        if (seriesRef != null) {
            List<Volume> allVolumes = getVolumeService().getAllVolumesBySeries(seriesRef);
            volumes = new ArrayList<SelectItem>(allVolumes.size());
            volumes.add(new SelectItem("", ""));
            for (Volume volume : allVolumes) {
                volumes.add(new SelectItem(volume.getNode().getNodeRef(), volume.getVolumeMark() + " " + volume.getTitle()));
            }
        }

        if (volumeRef != null) {
            List<Case> allCases = getCaseService().getAllCasesByVolume(volumeRef);
            cases = new ArrayList<SelectItem>(allCases.size());
            cases.add(new SelectItem("", ""));
            for (Case tmpCase : allCases) {
                cases.add(new SelectItem(tmpCase.getNode().getNodeRef(), tmpCase.getTitle()));
            }
        }

        if (updateComponents) {
            @SuppressWarnings("unchecked")
            List<UIComponent> children = getPropertySheet().getChildren();
            for (UIComponent component : children) {
                if (component.getId().endsWith("_series")) {
                    HtmlSelectOneMenu seriesList = (HtmlSelectOneMenu) component.getChildren().get(1);
                    ComponentUtil.setSelectItems(FacesContext.getCurrentInstance(), seriesList, series);
                } else if (component.getId().endsWith("_volume")) {
                    HtmlSelectOneMenu volumeList = (HtmlSelectOneMenu) component.getChildren().get(1);
                    ComponentUtil.setSelectItems(FacesContext.getCurrentInstance(), volumeList, volumes);
                } else if (component.getId().endsWith("_case")) {
                    HtmlSelectOneMenu caseList = (HtmlSelectOneMenu) component.getChildren().get(1);
                    ComponentUtil.setSelectItems(FacesContext.getCurrentInstance(), caseList, cases);
                }
            }
        }
    }

}
