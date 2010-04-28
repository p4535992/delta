package ee.webmedia.alfresco.workflow.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.faces.application.Application;
import javax.faces.component.UIInput;
import javax.faces.component.UIParameter;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIGenericPicker;
import org.alfresco.web.ui.common.component.UIMenu;
import org.alfresco.web.ui.common.component.UIPanel;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;
import ee.webmedia.alfresco.addressbook.service.AddressbookService;
import ee.webmedia.alfresco.addressbook.web.dialog.AddressbookMainViewDialog;
import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.user.web.PermissionsAddDialog;
import ee.webmedia.alfresco.user.web.UserListDialog;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.AssignmentWorkflow;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflowDefinition;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import ee.webmedia.alfresco.workflow.service.type.WorkflowType;

/**
 * Dialog bean for working with one compound workflow definition.
 * 
 * @author Erko Hansar
 */
public class CompoundWorkflowDefinitionDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;
    
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(CompoundWorkflowDefinitionDialog.class);

    private transient WorkflowService workflowService;
    private transient PersonService personService;
    private transient AddressbookService addressbookService;
    private transient AuthorityService authorityService;
    private transient OrganizationStructureService organizationStructureService;
    
    private transient HtmlPanelGroup panelGroup;
    private transient TreeMap<String, QName> sortedTypes;    

    private UserListDialog userListDialog;
    private PermissionsAddDialog permissionsAddDialog;
    
    private SelectItem[] ownerSearchFilters;
    private SelectItem[] responsibleOwnerSearchFilters;
    private List<SelectItem> parallelSelections;
    
    protected CompoundWorkflow workflow;
    protected boolean fullAccess;
    protected boolean isUnsavedWorkFlow;

    @Override
    public void init(Map<String, String> parameters) {
        super.init(parameters);
        
        if (parallelSelections == null) {
            parallelSelections = new ArrayList<SelectItem>(3);
            parallelSelections.add(new SelectItem("", MessageUtil.getMessage("workflow_choose")));
            parallelSelections.add(new SelectItem(true, MessageUtil.getMessage("reviewWorkflow_parallel_true")));
            parallelSelections.add(new SelectItem(false, MessageUtil.getMessage("reviewWorkflow_parallel_false")));
        }
        
        if (ownerSearchFilters == null) {
            ownerSearchFilters = new SelectItem[] {
                new SelectItem(0, MessageUtil.getMessage("task_owner_users")),
                new SelectItem(1, MessageUtil.getMessage("task_owner_usergroups")),
                new SelectItem(2, MessageUtil.getMessage("task_owner_contacts")),
                new SelectItem(3, MessageUtil.getMessage("task_owner_contactgroups"))
            };
        }
        
        if (responsibleOwnerSearchFilters == null) {
            responsibleOwnerSearchFilters = new SelectItem[] {
                new SelectItem(0, MessageUtil.getMessage("task_owner_users")),
                new SelectItem(1, MessageUtil.getMessage("task_owner_contacts")),
            };
        }
    }
    
    @Override
    public String cancel() {
        resetState();
        return super.cancel();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        try {
            removeEmptyResponsibleTasks();
            getWorkflowService().saveCompoundWorkflowDefinition((CompoundWorkflowDefinition)workflow);
        } catch (Exception e) {
            log.debug("Failed to save " + workflow, e);
            throw e;
        }
        resetState();
        return outcome;
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return workflow == null;
    }
    
    /**
     * Action listener for JSP.
     */
    public void setupWorkflow(ActionEvent event) {
        resetState();
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        workflow = getWorkflowService().getCompoundWorkflowDefinition(nodeRef);
        updateFullAccess();
    }

    /**
     * Action listener for JSP.
     */
    public void setupNewWorkflow(ActionEvent event) {
        resetState();
        workflow = getWorkflowService().getNewCompoundWorkflowDefinition();
        updateFullAccess();
    }

    /**
     * Action listener for JSP.
     */
    public void addWorkflowBlock(ActionEvent event) {
        QName workflowType = QName.createQName(ActionUtil.getParam(event, "workflowType"));
        int index = Integer.parseInt(ActionUtil.getParam(event, "index"));
        log.debug("addWorkflowBlock: " + index + ", " + workflowType.getLocalName());
        getWorkflowService().addNewWorkflow(workflow, workflowType, index);
        updatePanelGroup();
    }

    /**
     * Action listener for JSP.
     */
    public void removeWorkflowBlock(ActionEvent event) {
        int index = Integer.parseInt(ActionUtil.getParam(event, "index"));
        log.debug("removeWorkflow: " + index);
        workflow.removeWorkflow(index);
        updatePanelGroup();
    }

    /**
     * Action listener for JSP.
     */
    public void addWorkflowTask(ActionEvent event) {
        int index = Integer.parseInt(ActionUtil.getParam(event, "index"));
        log.debug("addWorkflowTask: " + index);
        workflow.getWorkflows().get(index).addTask();
        updatePanelGroup();
    }

    /**
     * Action listener for JSP.
     */
    public void removeWorkflowTask(ActionEvent event) {
        int index = Integer.parseInt(ActionUtil.getParam(event, "index"));
        int taskIndex = Integer.parseInt(ActionUtil.getParam(event, "taskIndex"));
        log.debug("removeWorkflowTask: " + index + ", " + taskIndex);
        Workflow block = workflow.getWorkflows().get(index);
        Task delTask = block.getTasks().get(taskIndex);
        if (delTask.getNode().getType().equals(WorkflowSpecificModel.Types.ASSIGNMENT_TASK) && WorkflowUtil.isActiveResponsible(delTask)) {
            for (Task task : block.getTasks()) {
                if (WorkflowUtil.isInactiveResponsible(task) && !Status.FINISHED.equals(task.getStatus()) && !Status.UNFINISHED.equals(task.getStatus())) {
                    task.getNode().getProperties().put(WorkflowSpecificModel.Props.ACTIVE.toString(), Boolean.TRUE);
                }
            }
        }
        block.removeTask(taskIndex);
        updatePanelGroup();
    }
    
    /**
     * Action listener for JSP.
     */
    public SelectItem[] executeOwnerSearch(int filterIndex, String contains) {
        log.debug("executeOwnerSearch: " + filterIndex + ", " + contains);
        if (filterIndex == 0) { // users
            return userListDialog.searchUsers(-1, contains);
        }
        else if (filterIndex == 1) { // user groups
            return permissionsAddDialog.searchGroups(-1, contains);
        }
        else if (filterIndex == 2) { // contacts
            final String personLabel = MessageUtil.getMessage("addressbook_private_person").toLowerCase();
            final String organizationLabel = MessageUtil.getMessage("addressbook_org").toLowerCase();
            List<Node> nodes = getAddressbookService().search(contains);
            return AddressbookMainViewDialog.transformNodesToSelectItems(nodes, personLabel, organizationLabel);            
        }
        else if (filterIndex == 3) { // contact groups
            final String personLabel = MessageUtil.getMessage("addressbook_private_person").toLowerCase();
            final String organizationLabel = MessageUtil.getMessage("addressbook_org").toLowerCase();
            List<Node> nodes = getAddressbookService().searchContactGroups(contains);
            return AddressbookMainViewDialog.transformNodesToSelectItems(nodes, personLabel, organizationLabel);            
        }
        else {
            throw new RuntimeException("Unknown filter index value: " + filterIndex);
        }
    }

    /**
     * Action listener for JSP.
     */
    public SelectItem[] executeResponsibleOwnerSearch(int filterIndex, String contains) {
        int newIndex = (filterIndex == 1) ? 2 : filterIndex;
        return executeOwnerSearch(newIndex, contains);
    }
    
    /**
     * Action listener for JSP.
     */
    public void processOwnerSearchResults(ActionEvent event) {
        UIGenericPicker picker = (UIGenericPicker)event.getComponent();
        int index = (Integer)picker.getAttributes().get(TaskListGenerator.ATTR_WORKFLOW_INDEX);
        int taskIndex = Integer.parseInt((String)picker.getAttributes().get(Search.OPEN_DIALOG_KEY));
        int filterIndex = picker.getFilterIndex();
        if (filterIndex == 1 && picker.getFilterOptions().length == 2) {
            filterIndex = 2;
        }
        String[] results = picker.getSelectedResults();
        if (results == null) {
            return;
        }
        log.debug("processOwnerSearchResults: " + picker.getId() + ", " + index + ", " + taskIndex + ", " + filterIndex + " = " + StringUtils.join(results, ","));

        Workflow block = workflow.getWorkflows().get(index);
        for (int i = 0; i < results.length; i++) {
            if (i > 0) {
                block.addTask(++taskIndex);
            }

            // users
            if (filterIndex == 0) {
                setPersonPropsToTask(block, taskIndex, results[i]);
            }
            // user groups
            else if (filterIndex == 1) {
                Set<String> children = getAuthorityService().getContainedAuthorities(AuthorityType.USER, results[i], true);
                Iterator<String> childrenIterator = children.iterator();
                int j = 0;
                while (childrenIterator.hasNext()) {
                    if (j++ > 0) {
                        block.addTask(++taskIndex);
                    }
                    setPersonPropsToTask(block, taskIndex, childrenIterator.next());
                }
            }
            // contacts
            else if (filterIndex == 2) {
                setContactPropsToTask(block, taskIndex, new NodeRef(results[i]));
            }
            // contact groups
            else if (filterIndex == 3) {
                List<AssociationRef> assocs = getNodeService().getTargetAssocs(new NodeRef(results[i]), RegexQNamePattern.MATCH_ALL);
                for (int j = 0; j < assocs.size(); j++) {
                    if (j > 0) {
                        block.addTask(++taskIndex);
                    }
                    setContactPropsToTask(block, taskIndex, assocs.get(j).getTargetRef());
                }
            }
            else {
                throw new RuntimeException("Unknown filter index value: " + filterIndex);
            }
        }
        
        updatePanelGroup();
    }
    
    /**
     * Binding for JSP.
     */
    public HtmlPanelGroup getPanelGroup() {
        return panelGroup;
    }

    public void setPanelGroup(HtmlPanelGroup panelGroup) {
        if (this.panelGroup == null) {
            this.panelGroup = panelGroup;
            updatePanelGroup();
        }
        else {
            this.panelGroup = panelGroup;
        }
    }

    /**
     * Getter for form input bindings.
     */
    public CompoundWorkflow getWorkflow() {
        return workflow;
    }

    /**
     * Getter for parallel checkbox values.
     */
    public List<SelectItem> getParallelSelections(FacesContext context, UIInput selectComponent) {
        return parallelSelections;
    }
    
    /**
     * Getter for the task owner search picker filter.
     */
    public SelectItem[] getOwnerSearchFilters() {
        return ownerSearchFilters;
    }

    /**
     * Getter for the task owner search picker filter.
     */
    public SelectItem[] getResponsibleOwnerSearchFilters() {
        return responsibleOwnerSearchFilters;
    }
    
    public void setUserListDialog(UserListDialog userListDialog) {
        this.userListDialog = userListDialog;
    }
    
    public void setPermissionsAddDialog(PermissionsAddDialog permissionsAddDialog) {
        this.permissionsAddDialog = permissionsAddDialog;
    }

    public boolean getFullAccess() {
        return fullAccess;
    }
    
    ///// PROTECTED & PRIVATE METHODS /////

    protected WorkflowService getWorkflowService() {
        if (workflowService == null) {
            workflowService = (WorkflowService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    WorkflowService.BEAN_NAME);
        }
        return workflowService;
    }

    protected PersonService getPersonService() {
        if (personService == null) {
            personService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getPersonService();
        }
        return personService;
    }

    protected AddressbookService getAddressbookService() {
        if (addressbookService == null) {
            addressbookService = (AddressbookService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    AddressbookService.BEAN_NAME);
        }
        return addressbookService;
    }

    protected AuthorityService getAuthorityService() {
        if (authorityService == null) {
            authorityService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getAuthorityService();
        }
        return authorityService;
    }
    
    protected OrganizationStructureService getOrganizationStructureService() {
        if (organizationStructureService == null) {
            organizationStructureService = (OrganizationStructureService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(OrganizationStructureService.BEAN_NAME);
        }
        return organizationStructureService;
    }

    protected void resetState() {
        workflow = null;
        panelGroup = null;
        sortedTypes = null;
        isUnsavedWorkFlow = false;
    }

    protected TreeMap<String, QName> getSortedTypes() {
        if (sortedTypes == null) {
            sortedTypes = new TreeMap<String, QName>();
            Map<QName, WorkflowType> workflowTypes = workflowService.getWorkflowTypes();
            for (QName tmpType : workflowTypes.keySet()) {
                String tmpName = MessageUtil.getMessage(tmpType.getLocalName());
                sortedTypes.put(tmpName, tmpType);
            }
        }
        return sortedTypes;
    }
    
    protected String getConfigArea() {
        return "workflow-settings";
    }

    @SuppressWarnings("unchecked")
    protected void updatePanelGroup() {
        Application application = FacesContext.getCurrentInstance().getApplication();
        
        panelGroup.getChildren().clear();
        
        if (workflow == null) {
            return;
        }
        
        updateFullAccess();
        ensureResponsibleTaskExists();

        // common data panel
        UIPanel panelC = (UIPanel) application.createComponent("org.alfresco.faces.Panel");
        panelC.setId("compound-workflow-panel");
        panelC.getAttributes().put("styleClass", "panel-100 ie7-workflow");
        panelC.setLabel(MessageUtil.getMessage("workflow_compound_data"));
        panelC.setProgressive(true);
        panelC.setFacetsId("dialog:dialog-body:compound-workflow-panel");
        panelGroup.getChildren().add(panelC);
        
        if (fullAccess && showAddActions(0)) {
            // common data add workflow actions
            UIMenu addActionsMenuC = buildAddActions(application, 0);
            panelC.getFacets().put("title", addActionsMenuC);
        }
        
        // common data properties
        UIPropertySheet sheetC = (UIPropertySheet) application.createComponent("org.alfresco.faces.PropertySheet");
        sheetC.setId("compound");
        sheetC.setVar("nodeC");
        sheetC.setNode(workflow.getNode());
        sheetC.getAttributes().put("labelStyleClass", "propertiesLabel");
        sheetC.getAttributes().put("externalConfig", Boolean.TRUE);
        sheetC.getAttributes().put("columns", 1);
        sheetC.setConfigArea(getConfigArea());
        if (!fullAccess) {
            sheetC.setMode(UIPropertySheet.VIEW_MODE);
        }
        panelC.getChildren().add(sheetC);
        
        // render every workflow block
        int counter = 1;
        for (Workflow block : workflow.getWorkflows()) {
            // block actions
            HtmlPanelGroup facetGroup = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
            facetGroup.setId("action-group-" + counter);
            
            if (fullAccess && showAddActions(counter)) {
                // block add workflow actions
                UIMenu addActionsMenu = buildAddActions(application, counter);
                facetGroup.getChildren().add(addActionsMenu);
            }
            
            String blockStatus = block.getStatus();
            if (fullAccess && Status.NEW.equals(blockStatus)) {
                // block remove workflow actions
                HtmlPanelGroup deleteActions = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
                deleteActions.setId("action-remove-" + counter);
                facetGroup.getChildren().add(deleteActions);
                
                UIActionLink deleteLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
                deleteLink.setId("action-remove-link-" + counter);
                deleteLink.setRendererType(UIActions.RENDERER_ACTIONLINK);
                deleteLink.setImage("/images/icons/delete.gif");
                deleteLink.setValue(MessageUtil.getMessage("workflow_compound_remove_block"));
                deleteLink.setActionListener(application.createMethodBinding("#{DialogManager.bean.removeWorkflowBlock}", UIActions.ACTION_CLASS_ARGS));
                deleteLink.setShowLink(false);
                deleteActions.getChildren().add(deleteLink);
                
                UIParameter deleteIndex = (UIParameter) application.createComponent(UIParameter.COMPONENT_TYPE);
                deleteIndex.setName("index");
                deleteIndex.setValue(counter - 1);
                deleteLink.getChildren().add(deleteIndex);
            }

            // block data panel
            UIPanel panelW = (UIPanel) application.createComponent("org.alfresco.faces.Panel");
            panelW.setId("workflow-panel-" + counter);
            panelW.getAttributes().put("styleClass", "panel-100 ie7-workflow");
            panelW.setLabel(MessageUtil.getMessage(block.getNode().getType().getLocalName() + "_title"));
            panelW.setProgressive(true);
            panelW.setFacetsId("dialog:dialog-body:workflow-panel-" + counter);
            if (facetGroup.getChildCount() > 0) {
                panelW.getFacets().put("title", facetGroup);
            }
            panelGroup.getChildren().add(panelW);
            
            // block data properties
            UIPropertySheet sheetW = (UIPropertySheet) application.createComponent("org.alfresco.faces.PropertySheet");
            sheetW.setId("workflow-" + counter);
            sheetW.setVar("nodeW" + counter);
            sheetW.getAttributes().put("workFlowIndex", counter-1);
            sheetW.setNode(block.getNode());
            sheetW.getAttributes().put("labelStyleClass", "propertiesLabel");
            sheetW.getAttributes().put("externalConfig", Boolean.TRUE);
            sheetW.getAttributes().put("columns", 1);
            sheetW.setConfigArea(getConfigArea());
            sheetW.getAttributes().put(TaskListGenerator.ATTR_WORKFLOW_INDEX, counter - 1);
            if (!fullAccess) {
                sheetW.setMode(UIPropertySheet.VIEW_MODE);
            }
            panelW.getChildren().add(sheetW);

            counter++;
        }
    }

    @SuppressWarnings("unchecked")
    protected UIMenu buildAddActions(Application application, int counter) {
        HtmlPanelGroup addActions = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
        addActions.setId("action-add-" + counter);
        
        MethodBinding actionListener = application.createMethodBinding("#{DialogManager.bean.addWorkflowBlock}", UIActions.ACTION_CLASS_ARGS);
        for (Entry<String, QName> entry : getSortedTypes().entrySet()) {
            UIActionLink addLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
            addLink.setId("action-add-" + entry.getValue().getLocalName() + "-" + counter);
            addLink.setRendererType(UIActions.RENDERER_ACTIONLINK);
            addLink.setImage("/images/icons/add.gif");
            addLink.setValue(entry.getKey());
            addLink.setActionListener(actionListener);
            addActions.getChildren().add(addLink);

            UIParameter addWorkflowType = (UIParameter) application.createComponent(UIParameter.COMPONENT_TYPE);
            addWorkflowType.setName("workflowType");
            addWorkflowType.setValue(entry.getValue().toString());
            addLink.getChildren().add(addWorkflowType);                
            
            UIParameter addIndex = (UIParameter) application.createComponent(UIParameter.COMPONENT_TYPE);
            addIndex.setName("index");
            addIndex.setValue(counter);
            addLink.getChildren().add(addIndex);
        }

        UIMenu addActionsMenu = (UIMenu) application.createComponent("org.alfresco.faces.Menu");
        addActionsMenu.setId("action-add-menu-" + counter);
        addActionsMenu.getAttributes().put("style", "white-space:nowrap");
        addActionsMenu.getAttributes().put("menuStyleClass", "dropdown-menu in-title");
        addActionsMenu.setLabel(MessageUtil.getMessage("workflow_compound_add_block"));
        addActionsMenu.getAttributes().put("image", "/images/icons/arrow-down.png");
        addActionsMenu.getChildren().add(addActions);
        
        return addActionsMenu;
    }
    
    private boolean showAddActions(int index) {
        boolean result = false;
        if (index < workflow.getWorkflows().size()) {
            String nextBlockStatus = workflow.getWorkflows().get(index).getStatus();
            result = Status.NEW.equals(nextBlockStatus);
        }
        else {
            String compoundWorkflowStatus = workflow.getStatus();
            result = !Status.FINISHED.equals(compoundWorkflowStatus);
        }
        return result;
    }
    
    private void setPersonPropsToTask(Workflow block, int taskIndex, String userName) {
        NodeRef person = getPersonService().getPerson(userName);
        Map<QName, Serializable> resultProps = getNodeService().getProperties(person);
        Serializable name = UserUtil.getPersonFullName1(resultProps);
        Serializable id = resultProps.get(ContentModel.PROP_USERNAME);
        Serializable email = resultProps.get(ContentModel.PROP_EMAIL);
        Serializable orgName = getOrganizationStructureService().getOrganizationStructure((String) resultProps.get(ContentModel.PROP_ORGID));
        Serializable jobTitle = resultProps.get(ContentModel.PROP_JOBTITLE);
        setPropsToTask(block, taskIndex, name, id, email, orgName, jobTitle);
    }

    private void setContactPropsToTask(Workflow block, int index, NodeRef contact) {
        Map<QName, Serializable> resultProps = getNodeService().getProperties(contact);
        QName resultType = getNodeService().getType(contact);
        
        Serializable name = null;
        if (resultType.equals(Types.ORGANIZATION)) {
            name = resultProps.get(AddressbookModel.Props.ORGANIZATION_NAME);
        } else {
            name = UserUtil.getPersonFullName((String) resultProps.get(AddressbookModel.Props.PERSON_FIRST_NAME), (String) resultProps.get(AddressbookModel.Props.PERSON_LAST_NAME));
        }
        setPropsToTask(block, index, name, null, resultProps.get(AddressbookModel.Props.EMAIL), null, null);
    }
    
    private void setPropsToTask(Workflow block, int index, Serializable name, Serializable id, Serializable email, Serializable orgName, Serializable jobTitle) {
        Task task = block.getTasks().get(index);
        if (task.getNode().hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE) && StringUtils.isNotBlank(task.getOwnerName())
                && task.getNode().getNodeRef() != null && !(workflow instanceof CompoundWorkflowDefinition) && !Status.NEW.equals(task.getStatus())) {
            task = ((AssignmentWorkflow) block).addResponsibleTask();
        }
        task.setOwnerName((String) name);
        task.setOwnerId((String) id);
        task.setOwnerEmail((String) email);
        task.getNode().getProperties().put(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME.toString(), orgName);
        task.getNode().getProperties().put(WorkflowCommonModel.Props.OWNER_JOB_TITLE.toString(), jobTitle);
    }
    
    /**
     * Override in child classes. 
     */
    protected void updateFullAccess() {
        fullAccess = true;
    }

    private void ensureResponsibleTaskExists() {
        for (Workflow block : workflow.getWorkflows()) {
            String blockStatus = block.getStatus();
            if (Status.NEW.equals(blockStatus)) {
                QName blockType = block.getNode().getType();
                if (blockType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW)) {
                    boolean hasRespTask = false;
                    for (Task task : block.getTasks()) {
                        if (WorkflowUtil.isActiveResponsible(task)) {
                            hasRespTask = true;
                            break;
                        }
                    }
                    if (!hasRespTask) {
                        ((AssignmentWorkflow) block).addResponsibleTask();
                    }
                } else {
                    if(block.getTasks().size() == 0 && !blockType.equals(WorkflowSpecificModel.Types.DOC_REGISTRATION_WORKFLOW)) {
                        block.addTask();
                    }
                }
            }
        }
    }

    protected void removeEmptyResponsibleTasks() {
        for (Workflow block : workflow.getWorkflows()) {
            QName blockType = block.getNode().getType();
            if (blockType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW)) {
                ArrayList<Integer> emptyTaskIndexes = new ArrayList<Integer>();
                int index = 0;
                for (Task task : block.getTasks()) {
                    if (task.getNode().hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE)
                            && Boolean.TRUE.equals(task.getNode().getProperties().get(WorkflowSpecificModel.Props.ACTIVE))) {
                        if (StringUtils.isBlank(task.getOwnerName()) && task.getDueDate() == null) {
                            emptyTaskIndexes.add(index);
                        }
                    }
                    index++;
                }
                for (int taskIndex : emptyTaskIndexes) {
                    block.removeTask(taskIndex);
                }
            }            
        }        
    }

}
