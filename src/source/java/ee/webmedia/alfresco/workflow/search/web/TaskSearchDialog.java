package ee.webmedia.alfresco.workflow.search.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlSelectManyListbox;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;
import ee.webmedia.alfresco.addressbook.service.AddressbookService;
import ee.webmedia.alfresco.addressbook.web.dialog.AddressbookMainViewDialog;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.user.web.UserListDialog;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.search.model.TaskInfo;
import ee.webmedia.alfresco.workflow.search.model.TaskSearchModel;
import ee.webmedia.alfresco.workflow.search.service.TaskSearchFilterService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.type.WorkflowType;

/**
 * @author Erko Hansar
 */
public class TaskSearchDialog extends BaseDialogBean {
    
    private static final long serialVersionUID = 1L;

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(TaskSearchDialog.class);

    private transient WorkflowService workflowService;
    private transient UserService userService;
    private transient AddressbookService addressbookService;
    private transient TaskSearchFilterService taskSearchFilterService;
    private transient DocumentSearchService documentSearchService;

    private transient UIPropertySheet propertySheet;
    private transient HtmlSelectOneMenu selectedFilterMenu;

    private TaskSearchResultsDialog taskSearchResultsDialog;
    private UserListDialog userListDialog;

    private Node filter;
    private List<SelectItem> taskTypes;
    private List<SelectItem> taskStatuses;
    private SelectItem[] ownerSearchFilters;
    private NodeRef selectedFilter;
    private String filterName;
    private List<SelectItem> allFilters;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        resetState();
        filter = getNewFilter();

        // Task types
        if (taskTypes == null) {
            Map<QName, WorkflowType> workflowTypes = getWorkflowService().getWorkflowTypes();
            taskTypes = new ArrayList<SelectItem>(workflowTypes.size());
            for (WorkflowType workflowType : workflowTypes.values()) {
                QName taskType = workflowType.getTaskType();
                if (taskType != null) {
                    taskTypes.add(new SelectItem(taskType, MessageUtil.getMessage(workflowType.getWorkflowType().getLocalName())));
                }
            }
            WebUtil.sort(taskTypes);
        }

        // Task statuses
        if (taskStatuses == null) {
            taskStatuses = new ArrayList<SelectItem>(Status.values().length);
            for (Status tmp : Status.values()) {
                taskStatuses.add(new SelectItem(tmp.getName(), tmp.getName()));
            }
        }

        if (ownerSearchFilters == null) {
            ownerSearchFilters = new SelectItem[] { new SelectItem(0, MessageUtil.getMessage("task_owner_users")),
                    new SelectItem(1, MessageUtil.getMessage("task_owner_contacts")), };
        }

        loadAllFilters();
    }

    @Override
    public String cancel() {
        resetState();
        return super.cancel();
    }
    
    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        List<TaskInfo> tasks = getDocumentSearchService().searchTasks(filter);
        taskSearchResultsDialog.setup(tasks);
        super.isFinished = false;
        return AlfrescoNavigationHandler.DIALOG_PREFIX + "taskSearchResultsDialog";
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "search");
    }

    public void saveFilter(ActionEvent event) {
        if (filterName == null) {
            filterName = "";
        }
        if (filterName.equals(filter.getProperties().get(TaskSearchModel.Props.NAME))) {
            filter = getTaskSearchFilterService().createOrSaveFilter(filter);
        } else {
            filter = getTaskSearchFilterService().createFilter(filter);
        }
        filterName = (String) filter.getProperties().get(TaskSearchModel.Props.NAME);
        propertySheet.getChildren().clear();
        loadAllFilters();
        selectedFilter = filter.getNodeRef();
    }

    public void deleteFilter(ActionEvent event) {
        if (!(filter instanceof TransientNode)) {
            getTaskSearchFilterService().deleteFilter(filter.getNodeRef());
        }
        filter = getNewFilter();
        filterName = (String) filter.getProperties().get(TaskSearchModel.Props.NAME);
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

    public void selectedFilterValueChanged(ValueChangeEvent event) {
        NodeRef newValue = (NodeRef) event.getNewValue();
        if (newValue == null) {
            filter = getNewFilter();
        } else {
            filter = getTaskSearchFilterService().getFilter(newValue);
        }
        filterName = (String) filter.getProperties().get(TaskSearchModel.Props.NAME);
        propertySheet.getChildren().clear();
    }

    /**
     * Action listener for JSP.
     */
    public SelectItem[] executeOwnerSearch(int filterIndex, String contains) {
        log.debug("executeOwnerSearch: " + filterIndex + ", " + contains);
        if (filterIndex == 0) { // users
            return userListDialog.searchUsers(-1, contains);
        } else if (filterIndex == 1) { // contacts
            final String personLabel = MessageUtil.getMessage("addressbook_private_person").toLowerCase();
            final String organizationLabel = MessageUtil.getMessage("addressbook_org").toLowerCase();
            List<Node> nodes = getAddressbookService().search(contains);
            return AddressbookMainViewDialog.transformNodesToSelectItems(nodes, personLabel, organizationLabel);
        } else {
            throw new RuntimeException("Unknown filter index value: " + filterIndex);
        }
    }

    /**
     * Action listener for JSP.
     */
    public void processOwnerSearchResults(String searchResult) {
        log.debug("processOwnerSearchResults: " + searchResult);
        if (StringUtils.isBlank(searchResult)) {
            return;
        }
        Serializable name = null;
        if (searchResult.indexOf('/') > -1) { // contact
            NodeRef contact = new NodeRef(searchResult);
            Map<QName, Serializable> resultProps = getNodeService().getProperties(contact);
            QName resultType = getNodeService().getType(contact);
            if (resultType.equals(Types.ORGANIZATION)) {
                name = resultProps.get(AddressbookModel.Props.ORGANIZATION_NAME);
            } else {
                name = UserUtil.getPersonFullName((String) resultProps.get(AddressbookModel.Props.PERSON_FIRST_NAME), (String) resultProps
                        .get(AddressbookModel.Props.PERSON_LAST_NAME));
            }
        } else { // user
            Map<QName, Serializable> personProps = getUserService().getUserProperties(searchResult);
            name = UserUtil.getPersonFullName1(personProps);
        }
        filter.getProperties().put(TaskSearchModel.Props.OWNER_NAME.toString(), name);
    }

    /**
     * Action listener for JSP.
     */
    public void processCreatorSearchResults(String username) {
        log.debug("processCreatorSearchResults: " + username);
        Map<QName, Serializable> personProps = getUserService().getUserProperties(username);
        filter.getProperties().put(TaskSearchModel.Props.CREATOR_NAME.toString(), UserUtil.getPersonFullName1(personProps));
    }

    /**
     * Getter for the task owner search picker filter.
     */
    public SelectItem[] getOwnerSearchFilters() {
        return ownerSearchFilters;
    }

    /**
     * GeneralSelectorGenerator 'selectionItems' method bindings
     */
    public List<SelectItem> getTaskTypes(FacesContext context, UIInput selectComponent) {
        ((HtmlSelectManyListbox) selectComponent).setSize(5);
        return taskTypes;
    }

    /**
     * GeneralSelectorGenerator 'selectionItems' method bindings
     */
    public List<SelectItem> getTaskStatuses(FacesContext context, UIInput selectComponent) {
        ((HtmlSelectManyListbox) selectComponent).setSize(5);
        return taskStatuses;
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

    public NodeRef getSelectedFilter() {
        return selectedFilter;
    }

    public void setSelectedFilter(NodeRef selectedFilter) {
        this.selectedFilter = selectedFilter;
    }

    public void setTaskSearchResultsDialog(TaskSearchResultsDialog taskSearchResultsDialog) {
        this.taskSearchResultsDialog = taskSearchResultsDialog;
    }

    public void setUserListDialog(UserListDialog userListDialog) {
        this.userListDialog = userListDialog;
    }

    protected WorkflowService getWorkflowService() {
        if (workflowService == null) {
            workflowService = (WorkflowService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    WorkflowService.BEAN_NAME);
        }
        return workflowService;
    }

    protected AddressbookService getAddressbookService() {
        if (addressbookService == null) {
            addressbookService = (AddressbookService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    AddressbookService.BEAN_NAME);
        }
        return addressbookService;
    }
    
    protected DocumentSearchService getDocumentSearchService() {
        if (documentSearchService == null) {
            documentSearchService = (DocumentSearchService) FacesContextUtils.getRequiredWebApplicationContext( // 
                    FacesContext.getCurrentInstance()).getBean(DocumentSearchService.BEAN_NAME);
        }
        return documentSearchService;
    }

    protected TaskSearchFilterService getTaskSearchFilterService() {
        if (taskSearchFilterService == null) {
            taskSearchFilterService = (TaskSearchFilterService) FacesContextUtils.getRequiredWebApplicationContext( // 
                    FacesContext.getCurrentInstance()).getBean(TaskSearchFilterService.BEAN_NAME);
        }
        return taskSearchFilterService;
    }

    protected UserService getUserService() {
        if (userService == null) {
            userService = (UserService) FacesContextUtils.getRequiredWebApplicationContext( // 
                    FacesContext.getCurrentInstance()).getBean(UserService.BEAN_NAME);
        }
        return userService;
    }

    // END: getters / setters

    private void resetState() {
        propertySheet = null;
        selectedFilterMenu = null;
        filter = null;
        selectedFilter = null;
        filterName = null;
        allFilters = null;
    }

    private Node getNewFilter() {
        // New empty filter
        Node node = new TransientNode(TaskSearchModel.Types.FILTER, null, null);
        // UISelectMany components don't want null as initial value
        node.getProperties().put(TaskSearchModel.Props.TASK_TYPE.toString(), new ArrayList<QName>());
        node.getProperties().put(TaskSearchModel.Props.STATUS.toString(), new ArrayList<String>());
        return node;
    }

    private void loadAllFilters() {
        allFilters = new ArrayList<SelectItem>();
        allFilters.add(new SelectItem("", MessageUtil.getMessage("task_search_new")));
        Set<Entry<NodeRef, String>> entrySet = getTaskSearchFilterService().getFilters().entrySet();
        for (Entry<NodeRef, String> entry : entrySet) {
            allFilters.add(new SelectItem(entry.getKey(), entry.getValue()));
        }
        WebUtil.sort(allFilters);
    }

}
