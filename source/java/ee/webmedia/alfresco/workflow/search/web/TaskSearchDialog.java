package ee.webmedia.alfresco.workflow.search.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowConstantsBean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlSelectManyListbox;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.alfresco.web.ui.common.component.PickerSearchParams;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;
import ee.webmedia.alfresco.addressbook.util.AddressbookUtil;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.UserContactGroupSearchBean;
import ee.webmedia.alfresco.filter.web.AbstractSearchFilterBlockBean;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.search.model.TaskSearchModel;
import ee.webmedia.alfresco.workflow.search.service.TaskSearchFilterService;
import ee.webmedia.alfresco.workflow.service.type.WorkflowType;

public class TaskSearchDialog extends AbstractSearchFilterBlockBean<TaskSearchFilterService> {

    public static final String BEAN_NAME = "TaskSearchDialog";

    private static final long serialVersionUID = 1L;

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(TaskSearchDialog.class);

    private List<SelectItem> taskTypes;
    private List<SelectItem> taskStatuses;
    private SelectItem[] ownerSearchFilters;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        // Task types
        if (taskTypes == null) {
            Map<QName, WorkflowType> workflowTypes = BeanHelper.getWorkflowConstantsBean().getWorkflowTypesByTask();
            taskTypes = new ArrayList<SelectItem>(workflowTypes.size());
            for (WorkflowType workflowType : workflowTypes.values()) {
                QName taskType = workflowType.getTaskType();
                if (taskType != null && taskTypeEnabled(taskType)) {
                    QName type = workflowType.getWorkflowType();
                    taskTypes.add(new SelectItem(taskType, MessageUtil.getMessage(type == null ? taskType.getLocalName() : type.getLocalName())));
                }
            }
            WebUtil.sort(taskTypes);
        }

        // Task statuses
        if (taskStatuses == null) {
            taskStatuses = new ArrayList<SelectItem>(Status.values().length);
            for (Status tmp : Status.values()) {
                if (tmp != Status.DELETED) {
                    taskStatuses.add(new SelectItem(tmp.getName(), tmp.getName()));
                }
            }
        }

        if (ownerSearchFilters == null) {
            ownerSearchFilters = new SelectItem[] { new SelectItem(UserContactGroupSearchBean.USERS_FILTER, MessageUtil.getMessage("task_owner_users")),
                    new SelectItem(UserContactGroupSearchBean.CONTACTS_FILTER, MessageUtil.getMessage("task_owner_contacts")), };
        }
        loadAllFilters();
        
        searchPanelTitlePart = null;
    }

    @Override
    public void clean() {
        taskTypes = null;
        taskStatuses = null;
        ownerSearchFilters = null;
        super.clean();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        BeanHelper.getTaskSearchResultsDialog().setup(filter);
        super.isFinished = false;
        return AlfrescoNavigationHandler.DIALOG_PREFIX + "taskSearchResultsDialog";
    }
    
    @Override
    public void selectedFilterValueChanged(ValueChangeEvent event) {
    	super.selectedFilterValueChanged(event);
        NodeRef newValue = (NodeRef) event.getNewValue();             
        searchPanelTitlePart = null;

        if(!getUserService().isAdministrator()){
        	NodeRef oldValue = newValue;
        	newValue = savePublicFilterAsNewLocal(event);
        	if(oldValue != null && newValue == null){
        		Map<String, Object> publicSearchFilterParams = filter.getProperties();
                super.selectedFilterValueChanged(newValue);
                for(Entry<String, Object> parameter : publicSearchFilterParams.entrySet()){
                	if(!parameter.getKey().contains("{http://www.alfresco.org/") && !parameter.getKey().equals(TaskSearchModel.Props.NAME)){
                		filter.getProperties().put(parameter.getKey(), parameter.getValue());
                	}
                }
        	}
    	}
        
    }


    @Override
    public String getManageSavedBlockTitle() {
        return MessageUtil.getMessage("task_search_saved_manage");
    }

    @Override
    public String getSavedFilterSelectTitle() {
        return MessageUtil.getMessage("task_search_saved");
    }

    @Override
    public String getFilterPanelTitle() {
    	if(searchPanelTitlePart == null){
    		return MessageUtil.getMessage("task_search");
    	}
        return String.format("%s: %s", MessageUtil.getMessage("task_search"), searchPanelTitlePart);
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage("search");
    }

    @Override
    protected String getBlankFilterNameMessageKey() {
        return "task_search_save_error_nameIsBlank";
    }

    @Override
    protected String getFilterModifyDeniedMessageKey() {
        return "document_search_filter_modify_accessDenied";
    }

    @Override
    protected String getFilterDeleteDeniedMessageKey() {
        return "task_search_filter_delete_accessDenied";
    }

    @Override
    protected String getNewFilterSelectItemMessageKey() {
        return "task_search_new";
    }

    @Override
    protected Node getNewFilter() {
        // New empty filter
        Node node = new TransientNode(getFilterType(), null, null);
        // UISelectMany components don't want null as initial value
        Map<String, Object> properties = node.getProperties();
        properties.put(TaskSearchModel.Props.TASK_TYPE.toString(), new ArrayList<QName>());
        properties.put(TaskSearchModel.Props.STATUS.toString(), new ArrayList<String>());
        properties.put(TaskSearchModel.Props.DOC_TYPE.toString(), new ArrayList<String>());
        List<String> ownerNames = new ArrayList<String>();
        ownerNames.add("");
        properties.put(TaskSearchModel.Props.OWNER_NAME.toString(), ownerNames);
        properties.put(TaskSearchModel.Props.OUTCOME.toString(), new ArrayList<String>());
        return node;
    }

    @Override
    protected QName getFilterType() {
        return TaskSearchModel.Types.FILTER;
    }

    /**
     * Action listener for JSP.
     */
    public SelectItem[] executeOwnerSearch(PickerSearchParams params) {
        log.debug("executeOwnerSearch: " + params.getFilterIndex() + ", " + params.getSearchString());
        SelectItem[] results = new SelectItem[0];
        if (params.isFilterIndex(UserContactGroupSearchBean.USERS_FILTER)) {
            results = (SelectItem[]) ArrayUtils.addAll(results, BeanHelper.getUserListDialog().searchUsers(params));
        }
        if (params.isFilterIndex(UserContactGroupSearchBean.CONTACTS_FILTER)) {
            List<Node> nodes = BeanHelper.getAddressbookService().search(params.getSearchString(), params.getLimit());
            results = (SelectItem[]) ArrayUtils.addAll(results, AddressbookUtil.transformAddressbookNodesToSelectItems(nodes));
        }

        return results;
    }

    /**
     * Action listener for JSP.
     */
    public List<String> processOwnerSearchResults(String searchResult) {
        log.debug("processOwnerSearchResults: " + searchResult);
        if (StringUtils.isBlank(searchResult)) {
            return null;
        }
        String name = null;
        if (searchResult.indexOf('/') > -1) { // contact
            NodeRef contact = new NodeRef(searchResult);
            Map<QName, Serializable> resultProps = getNodeService().getProperties(contact);
            QName resultType = getNodeService().getType(contact);
            if (resultType.equals(Types.ORGANIZATION)) {
                name = (String) resultProps.get(AddressbookModel.Props.ORGANIZATION_NAME);
            } else {
                name = UserUtil.getPersonFullName((String) resultProps.get(AddressbookModel.Props.PERSON_FIRST_NAME), (String) resultProps
                        .get(AddressbookModel.Props.PERSON_LAST_NAME));
            }
        } else { // user
            Map<QName, Serializable> personProps = getUserService().getUserProperties(searchResult);
            name = UserUtil.getPersonFullName1(personProps);
        }
        return Collections.singletonList(name);
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
     * @param context
     *            GeneralSelectorGenerator 'selectionItems' method bindings
     */
    public List<SelectItem> getTaskTypes(FacesContext context, UIInput selectComponent) {
        ((HtmlSelectManyListbox) selectComponent).setSize(5);
        return taskTypes;
    }

    /**
     * @param context
     *            GeneralSelectorGenerator 'selectionItems' method bindings
     */
    public List<SelectItem> getTaskStatuses(FacesContext context, UIInput selectComponent) {
        ((HtmlSelectManyListbox) selectComponent).setSize(5);
        return taskStatuses;
    }

    private boolean taskTypeEnabled(QName taskType) {
        if (WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK.equals(taskType)) {
            return getWorkflowConstantsBean().isOrderAssignmentWorkflowEnabled();
        } else if (WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK.equals(taskType)) {
            return getWorkflowConstantsBean().isExternalReviewWorkflowEnabled();
        } else if (WorkflowSpecificModel.Types.CONFIRMATION_TASK.equals(taskType)) {
            return getWorkflowConstantsBean().isConfirmationWorkflowEnabled();
        } else if (WorkflowSpecificModel.Types.GROUP_ASSIGNMENT_TASK.equals(taskType)) {
            return getWorkflowConstantsBean().isGroupAssignmentWorkflowEnabled();
        } else if (WorkflowSpecificModel.Types.LINKED_REVIEW_TASK.equals(taskType)) {
            return getWorkflowConstantsBean().isReviewToOtherOrgEnabled();
        }
        return true;
    }

    // START: getters / setters

    @Override
    protected TaskSearchFilterService getFilterService() {
        if (filterService == null) {
            filterService = BeanHelper.getTaskSearchFilterService();
        }
        return filterService;
    }

    // END: getters / setters

}
