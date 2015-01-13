package ee.webmedia.alfresco.filter.web;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlSelectBooleanCheckbox;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty;
import ee.webmedia.alfresco.common.propertysheet.generator.GeneralSelectorGenerator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.filter.model.FilterVO;
import ee.webmedia.alfresco.filter.service.FilterService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.WebUtil;

/**
 * Base class for dialogs, that have searching functionality using filters <br>
 * that could be saved for later private use (or to all if user has admin rights).
 */
public abstract class AbstractSearchFilterBlockBean<T extends FilterService> extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    protected transient T filterService;
    private transient DocumentSearchService documentSearchService;
    private transient UserService userService;

    protected Node filter;
    private transient UIPropertySheet propertySheet;
    private transient HtmlInputText searchTitleInput;
    private transient HtmlSelectBooleanCheckbox publicCheckBox;
    private transient HtmlSelectOneMenu selectedFilterMenu;
    private String originalFilterName;
    private Set<String /* nodeRef */> publicFilterRefs;
    private NodeRef selectedFilter;
    private List<SelectItem> allFilters;

    @Override
    public void init(Map<String, String> parameters) {
        super.init(parameters);
        reset();
        filter = getNewFilter();
        originalFilterName = null;
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

    protected void loadAllFilters() {
        allFilters = new ArrayList<SelectItem>();
        allFilters.add(new SelectItem("", MessageUtil.getMessage(getNewFilterSelectItemMessageKey())));
        final List<FilterVO> filters = getFilterService().getFilters();
        publicFilterRefs = new HashSet<String>(filters.size());
        for (FilterVO filter : filters) {
            final NodeRef nodeRef = filter.getFilterRef();
            final SelectItem selectItem = new SelectItem(nodeRef, StringUtils.abbreviate(filter.getFilterName(), 75), filter.getFilterName());
            if (!filter.isPrivate()) {
                publicFilterRefs.add(nodeRef.toString());
            }
            allFilters.add(selectItem);
        }
        WebUtil.sort(allFilters);
    }

    /** @param event */
    public void saveFilter(ActionEvent event) {
        final boolean isPrivate = isPrivateFilter();
        final boolean isNew = filter instanceof TransientNode;
        String newFilterName = getNewFilterNameFromInput();
        if (StringUtils.isBlank(newFilterName)) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), getBlankFilterNameMessageKey());
            return;
        }
        Map<String, Object> properties = filter.getProperties();
        properties.put(getFilterNameProperty().toString(), newFilterName);
        Case caseByTitle = null;
        NodeRef volumeRef = (NodeRef) properties.get(DocumentCommonModel.Props.VOLUME.toString());
        if (volumeRef != null) {
            caseByTitle = BeanHelper.getCaseService().getCaseByTitle((String) properties.get(RepoUtil.createTransientProp("caseLabelEditable").toString()), volumeRef, null);
        }
        if (caseByTitle != null) {
            properties.put(DocumentCommonModel.Props.CASE.toString(), caseByTitle.getNode().getNodeRef());
        } else if (properties.containsKey(DocumentCommonModel.Props.CASE.toString())) {
            properties.put(DocumentCommonModel.Props.CASE.toString(), null);
        }
        properties.put(DocumentCommonModel.Props.CASE.toString() + WMUIProperty.AFTER_LABEL_BOOLEAN,
                properties.get(RepoUtil.createTransientProp("caseLabelEditable" + WMUIProperty.AFTER_LABEL_BOOLEAN).toString()));

        if (isNew) {
            filter = getFilterService().createFilter(filter, isPrivate);
        } else {
            try {
                filter = getFilterService().createOrSaveFilter(filter, isPrivate);
            } catch (AccessDeniedException e) {
                MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), getFilterModifyDeniedMessageKey(), originalFilterName);
                return;
            }
        }
        setFilterCaseProps();
        propertySheet.getChildren().clear();
        loadAllFilters();
        selectedFilter = filter.getNodeRef();
    }

    /** @param event */
    public void deleteFilter(ActionEvent event) {
        if (!(filter instanceof TransientNode)) {
            try {
                getFilterService().deleteFilter(filter.getNodeRef());
            } catch (AccessDeniedException e) {
                MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), getFilterDeleteDeniedMessageKey(), originalFilterName);
                return;
            }
        }
        filter = getNewFilter();
        originalFilterName = (String) filter.getProperties().get(getFilterNameProperty());
        searchTitleInput.setValue(originalFilterName);
        propertySheet.getChildren().clear();
        loadAllFilters();
        selectedFilter = null;
        setPublicFilter(null);
    }

    public List<SelectItem> getAllFilters() {
        // Must be called after component is added to component tree
        selectedFilterMenu.setStyleClass(GeneralSelectorGenerator.ONCHANGE_MARKER_CLASS + GeneralSelectorGenerator.ONCHANGE_SCRIPT_START_MARKER
                + Utils.generateFormSubmit(FacesContext.getCurrentInstance(), selectedFilterMenu));
        if (allFilters == null) {
            loadAllFilters();
        }
        return allFilters;
    }

    @Override
    public String cancel() {
        reset();
        return super.cancel();
    }

    public HtmlInputText getSearchTitleInput() {
        if (searchTitleInput == null) {
            searchTitleInput = new HtmlInputText();
            String filterName = (String) filter.getProperties().get(getFilterNameProperty());
            searchTitleInput.setValue(filterName);
        }
        return searchTitleInput;
    }

    public void setSearchTitleInput(HtmlInputText htmlInputText) {
        this.searchTitleInput = htmlInputText;
    }

    public HtmlSelectBooleanCheckbox getPublicCheckBox() {
        if (publicCheckBox == null) {
            publicCheckBox = new HtmlSelectBooleanCheckbox();
        }
        return publicCheckBox;
    }

    public void setPublicCheckBox(HtmlSelectBooleanCheckbox publicCheckBox) {
        this.publicCheckBox = publicCheckBox;
    }

    public NodeRef getSelectedFilter() {
        return selectedFilter;
    }

    public void setSelectedFilter(NodeRef selectedFilter) {
        this.selectedFilter = selectedFilter;
    }

    public void selectedFilterValueChanged(ValueChangeEvent event) {
        NodeRef newValue = (NodeRef) event.getNewValue();
        if (newValue == null) {
            filter = getNewFilter();
        } else {
            filter = getFilterService().getFilter(newValue);
        }
        originalFilterName = (String) filter.getProperties().get(getFilterNameProperty());
        // may be null if filter saving block is not displayed
        if (searchTitleInput != null) {
            searchTitleInput.setValue(originalFilterName);
        }
        propertySheet.getChildren().clear();
        setPublicFilter(newValue);
    }

    protected void setFilterCaseProps() {
        Map<String, Object> filterProps = filter.getProperties();
        if (filterProps.containsKey(DocumentCommonModel.Props.CASE.toString())) {
            // caseLabelEditable is set by DocumentLocationGenerator
            String caseAfterLabelBooleanProp = DocumentCommonModel.Props.CASE.toString() + WMUIProperty.AFTER_LABEL_BOOLEAN;
            filterProps.put(RepoUtil.createTransientProp("caseLabelEditable" + WMUIProperty.AFTER_LABEL_BOOLEAN).toString(), filterProps.get(caseAfterLabelBooleanProp));
            filterProps.remove(caseAfterLabelBooleanProp);
        }
    }

    protected boolean isPrivateFilter() {
        boolean isAdmin = ((UserService) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), UserService.BEAN_NAME)).isAdministrator();
        if (isAdmin) {
            // only administrators can make their filters public(or the other way)
            return !(Boolean) publicCheckBox.getValue();
        }
        Boolean isPublic = publicFilterRefs.contains(filter.getNodeRefAsString());
        return isPublic == null ? true : !isPublic;
    }

    protected String getNewFilterNameFromInput() {
        return (String) searchTitleInput.getValue();
    }

    protected void reset() {
        searchTitleInput = null;
        originalFilterName = null;
        publicCheckBox = null;
        publicFilterRefs = null;

        propertySheet = null;
        selectedFilterMenu = null;
        filter = null;
        allFilters = null;
        selectedFilter = null;
    }

    abstract protected Node getNewFilter();

    abstract protected String getFilterModifyDeniedMessageKey();

    abstract protected String getBlankFilterNameMessageKey();

    abstract protected String getFilterDeleteDeniedMessageKey();

    abstract protected String getNewFilterSelectItemMessageKey();

    abstract protected QName getFilterType();

    public String getManageSavedBlockTitle() {
        return "";
    }

    public String getSavedFilterSelectTitle() {
        return "";
    }

    public String getFilterPanelTitle() {
        return "";
    }

    public boolean isReportSearch() {
        return false;
    }

    public boolean isShowManageSavedDialog() {
        return true;
    }

    private void setPublicFilter(NodeRef newValue) {
        final boolean isPublic = newValue == null ? false : publicFilterRefs.contains(newValue.toString());
        getPublicCheckBox().setValue(isPublic);
    }

    private QName getFilterNameProperty() {
        return getFilterService().getFilterNameProperty();
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

    abstract protected T getFilterService();

    protected DocumentSearchService getDocumentSearchService() {
        if (documentSearchService == null) {
            documentSearchService = (DocumentSearchService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(DocumentSearchService.BEAN_NAME);
        }
        return documentSearchService;
    }

    protected UserService getUserService() {
        if (userService == null) {
            userService = (UserService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(UserService.BEAN_NAME);
        }
        return userService;
    }

    // END: getters / setters

}
