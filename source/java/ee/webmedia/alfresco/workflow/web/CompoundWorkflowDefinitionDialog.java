package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.addressbook.util.AddressbookUtil.transformAddressbookNodesToSelectItems;
import static ee.webmedia.alfresco.common.web.BeanHelper.getAddressbookService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentSearchService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDvkService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getEInvoiceService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getOrganizationStructureService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getParametersService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserContactGroupSearchBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserListDialog;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowService;
import static ee.webmedia.alfresco.utils.ComponentUtil.addChildren;
import static ee.webmedia.alfresco.utils.ComponentUtil.addFacet;
import static ee.webmedia.alfresco.utils.ComponentUtil.createUIParam;
import static ee.webmedia.alfresco.utils.ComponentUtil.putAttribute;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.TASK_INDEX;
import static ee.webmedia.alfresco.workflow.web.TaskListGenerator.ATTR_RESPONSIBLE;
import static ee.webmedia.alfresco.workflow.web.TaskListGenerator.WF_INDEX;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.UISelectItem;
import javax.faces.component.html.HtmlCommandButton;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.component.PickerSearchParams;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIGenericPicker;
import org.alfresco.web.ui.common.component.UIMenu;
import org.alfresco.web.ui.common.component.UIPanel;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.collections.Closure;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.shared_impl.renderkit.RendererUtils;
import org.apache.myfaces.shared_impl.renderkit.html.HTML;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;
import ee.webmedia.alfresco.common.propertysheet.generator.GeneralSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.UserContactGroupSearchBean;
import ee.webmedia.alfresco.document.einvoice.model.Transaction;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.CalendarUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.AssignmentWorkflow;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflowDefinition;
import ee.webmedia.alfresco.workflow.service.OrderAssignmentWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import ee.webmedia.alfresco.workflow.service.type.WorkflowType;

/**
 * Dialog bean for working with one compound workflow definition.
 * 
 * @author Erko Hansar
 */
public class CompoundWorkflowDefinitionDialog extends BaseDialogBean {

    public static final String WORKFLOW_AFTER_CONFIRMATION_LINK = "workflow-after-confirmation-link";
    public static final String WORKFLOW_CONFIRMATION_MESSAGES = "workflow-confirmation-messages";
    private static final String WORKFLOW_PANEL_ID_PREFIX = "workflow-panel-";

    private static final String COMPOUND_WORKFLOW_PANEL_GROUP_ID = "compound-workflow-panel-group";

    private static final long serialVersionUID = 1L;

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(CompoundWorkflowDefinitionDialog.class);

    protected static final String COMP_WORKFLOW_DEFINITION_SELECTOR_ID = "comp-workflow-definition-selector";
    protected static final String COMP_WORKFLOW_DEFINITION_INPUT_ID = "comp-workflow-definition-input";

    protected transient HtmlPanelGroup panelGroup;
    private transient HtmlPanelGroup saveAsGroup;
    private transient HtmlPanelGroup commonDataGroup;
    protected Map<String, QName> sortedTypes;

    private OwnerSearchBean ownerSearchBean;
    private List<SelectItem> parallelSelections;
    private SelectItem[] externalReviewSearchFilters;

    protected CompoundWorkflow compoundWorkflow;
    protected List<Map<String, List<TaskGroup>>> taskGroups = new ArrayList<Map<String, List<TaskGroup>>>();

    protected boolean fullAccess;
    protected boolean isUnsavedWorkFlow;
    protected List<Boolean> workflowBlockExpandedStatuses;

    @Override
    public void init(Map<String, String> parameters) {
        super.init(parameters);

        if (parallelSelections == null) {
            parallelSelections = new ArrayList<SelectItem>(3);
            parallelSelections.add(new SelectItem("", MessageUtil.getMessage("workflow_choose")));
            parallelSelections.add(new SelectItem(true, MessageUtil.getMessage("reviewWorkflow_parallel_true")));
            parallelSelections.add(new SelectItem(false, MessageUtil.getMessage("reviewWorkflow_parallel_false")));
        }

        ownerSearchBean.init();

        if (externalReviewSearchFilters == null) {
            externalReviewSearchFilters = new SelectItem[] {
                    new SelectItem(0, MessageUtil.getMessage("task_owner_contacts")),
                    new SelectItem(1, MessageUtil.getMessage("task_owner_contactgroups"))
            };
        }
        addInfoMessageIfNeeded();
    }

    @Override
    public String cancel() {
        resetState();
        return super.cancel();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        try {
            preprocessWorkflow();
            preprocessWorkflowDefinition();
            if (validate()) {
                getWorkflowService().saveCompoundWorkflowDefinition((CompoundWorkflowDefinition) compoundWorkflow);
                BeanHelper.getMenuService().menuUpdated();
                MessageUtil.addInfoMessage("save_success");
            } else {
                return null;
            }
        } catch (Exception e) {
            log.debug("Failed to save " + compoundWorkflow, e);
            throw e;
        }
        resetState();
        return outcome;
    }

    private boolean validate() {
        if (compoundWorkflow.isDocumentWorkflow()) {
            return true;
        }
        boolean isCaseFileWorkflow = compoundWorkflow.isCaseFileWorkflow();
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            if (!isCaseFileWorkflow && workflow.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW)) {
                MessageUtil.addErrorMessage("compoundWorkflow_external_review_not_allowed");
                return false;
            }
            if (isCaseFileWorkflow
                    && workflow.isType(WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW, WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW,
                            WorkflowSpecificModel.Types.OPINION_WORKFLOW, WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW)) {
                MessageUtil.addErrorMessage("compoundWorkflow_caseFile_workflow_type_not_allowed");
                return false;
            }
            QName workflowType = workflow.getType();
            if (isNotAllowedConfirmationWorkflow(workflowType) || isNotAllowedGroupAssignmentWorkflow(workflowType) || isNotAllowedOrderAssignmentWorkflow(workflowType)) {
                MessageUtil.addErrorMessage("compoundWorkflow_workflow_not_allowed",
                        MessageUtil.getTypeName(getWorkflowService().getWorkflowTypes().get(workflowType).getTaskType()));
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return compoundWorkflow == null;
    }

    /**
     * Action listener for JSP.
     */
    public void setupWorkflow(ActionEvent event) {
        resetState();
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        compoundWorkflow = getWorkflowService().getCompoundWorkflowDefinition(nodeRef);
        addLargeWorkflowWarning();
        updateFullAccess();
        initExpandedStatuses();
    }

    protected void initExpandedStatuses() {
        workflowBlockExpandedStatuses = new ArrayList<Boolean>();
        Boolean isInitiallyExpanded = isWorkflowBlockInitiallyExpanded();
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            if (WorkflowUtil.isGeneratedByDelegation(workflow)) {
                continue;
            }
            workflowBlockExpandedStatuses.add(isInitiallyExpanded);
        }
    }

    /**
     * Action listener for JSP.
     */
    public void setupNewWorkflow(@SuppressWarnings("unused") ActionEvent event) {
        resetState();
        compoundWorkflow = getWorkflowService().getNewCompoundWorkflowDefinition();
        if (!isShowCompoundWorkflowDefinitionType()) {
            compoundWorkflow.setTypeEnum(CompoundWorkflowType.DOCUMENT_WORKFLOW);
        }
        updateFullAccess();
        initExpandedStatuses();
    }

    /**
     * Action listener for JSP.
     */
    public void addWorkflowBlock(ActionEvent event) {
        if (!getNodeService().exists(compoundWorkflow.getParent())) {
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addErrorMessage(context, "workflow_compound_add_block_error_docDeleted");
            WebUtil.navigateTo(getDefaultCancelOutcome(), context);
            return;
        }
        QName workflowType = QName.createQName(ActionUtil.getParam(event, "workflowType"));
        int wfIndex = ActionUtil.getParam(event, WF_INDEX, Integer.class);
        log.debug("addWorkflowBlock: " + wfIndex + ", " + workflowType.getLocalName());
        Workflow workflow = getWorkflowService().addNewWorkflow(compoundWorkflow, workflowType, wfIndex, true);
        List<Map<String, List<TaskGroup>>> groups = getTaskGroups();
        HashMap<String, List<TaskGroup>> emptyGroup = new HashMap<String, List<TaskGroup>>();
        if (groups.size() > wfIndex) { // If workflow was added in the middle
            groups.add(wfIndex, emptyGroup);
        } else {
            groups.add(emptyGroup);
        }
        if (!(compoundWorkflow instanceof CompoundWorkflowDefinition) && isCostManagerWorkflow(wfIndex)) {
            addCostManagerTasks(workflow);
        }
        retrieveExpandedStatuses();
        workflowBlockExpandedStatuses.add(wfIndex, true);
        updatePanelGroup(null, null, true, false, null, false);
    }

    private boolean isWorkflowBlockInitiallyExpanded() {
        return compoundWorkflow != null && (!compoundWorkflow.isIndependentWorkflow() || compoundWorkflow.isStatus(Status.NEW));
    }

    protected void addCostManagerTasks(Workflow workflow) {
        NodeRef docRef = compoundWorkflow.getParent();
        if (!compoundWorkflow.isDocumentWorkflow() || docRef == null) {
            return;
        }
        List<Transaction> transactions = getEInvoiceService().getInvoiceTransactions(docRef);
        Map<NodeRef, Node> taskOwners = new HashMap<NodeRef, Node>();
        for (Transaction transaction : transactions) {
            String fundsCenter = transaction.getFundsCenter();
            if (StringUtils.isNotBlank(fundsCenter)) {
                List<NodeRef> users = getDocumentSearchService().searchUsersByRelatedFundsCenter(fundsCenter);
                for (NodeRef userRef : users) {
                    taskOwners.put(userRef, new Node(userRef));
                }
            }
        }
        for (Node taskOwner : taskOwners.values()) {
            Task task = workflow.addTask();
            setPersonPropsToTask(task, RepoUtil.toQNameProperties(taskOwner.getProperties()));
        }
    }

    private boolean isCostManagerWorkflow(int wfIndex) {
        NodeRef docRef = compoundWorkflow.getParent();
        if (docRef == null || !DocumentSubtypeModel.Types.INVOICE.equals(getNodeService().getType(docRef))) {
            return false;
        }

        Long costManagerWfIndex = getParametersService().getLongParameter(Parameters.REVIEW_WORKFLOW_COST_MANAGER_WORKFLOW_NUMBER);
        if (costManagerWfIndex == null) {
            return false;
        }
        int generalWfIndex = 0;
        int reviewWfIndex = 0;
        for (Workflow wf : compoundWorkflow.getWorkflows()) {
            if (wf.isType(WorkflowSpecificModel.Types.REVIEW_WORKFLOW)) {
                // parameter workflow index is 1-based (not 0-based)
                if (reviewWfIndex == costManagerWfIndex - 1) {
                    return true;
                }
                reviewWfIndex++;
            }
            if (generalWfIndex++ >= wfIndex) {
                break;
            }
        }
        return false;
    }

    /**
     * Action listener for JSP.
     */
    public void removeWorkflowBlock(ActionEvent event) {
        int wfIndex = ActionUtil.getParam(event, WF_INDEX, Integer.class);
        log.debug("removeWorkflow: " + wfIndex);
        getTaskGroups().remove(wfIndex);
        compoundWorkflow.removeWorkflow(wfIndex);
        workflowBlockExpandedStatuses.remove(wfIndex);
        updatePanelGroupWithoutWorkflowBlockUpdate();
    }

    /**
     * Action listener for JSP.
     */
    public void addWorkflowTask(ActionEvent event) {
        int wfIndex = ActionUtil.getParam(event, WF_INDEX, Integer.class);
        int taskIndex = -1;
        if (ActionUtil.hasParam(event, TASK_INDEX)) {
            taskIndex = Integer.parseInt(ActionUtil.getParam(event, TASK_INDEX));
        }
        log.debug("addWorkflowTask: blockIndex=" + wfIndex + "; " + TASK_INDEX + "=" + taskIndex);
        boolean responsible = false;
        if (ActionUtil.hasParam(event, ATTR_RESPONSIBLE)) {
            responsible = Boolean.parseBoolean(ActionUtil.getParam(event, ATTR_RESPONSIBLE));
        }
        Workflow workflow = compoundWorkflow.getWorkflows().get(wfIndex);
        if (taskIndex > -1) {
            if (!responsible) {
                workflow.addTask(taskIndex);
            } else {
                ((OrderAssignmentWorkflow) workflow).addResponsibleTask(taskIndex);
            }
        } else {
            if (!responsible) {
                workflow.addTask();
            } else {
                ((OrderAssignmentWorkflow) workflow).addResponsibleTask();
            }
        }
        updatePanelGroupWithoutWorkflowBlockUpdate();
    }

    /**
     * Action listener for JSP.
     */
    public void removeWorkflowTask(ActionEvent event) {
        int wfIndex = ActionUtil.getParam(event, WF_INDEX, Integer.class);
        int taskIndex = Integer.parseInt(ActionUtil.getParam(event, TASK_INDEX));
        removeWorkflowTask(wfIndex, taskIndex, true);
    }

    public void removeWorkflowTask(Integer wfIndex, Integer taskIndex, boolean updatePanelGroup) {
        log.debug("removeWorkflowTask: " + wfIndex + ", " + taskIndex);
        Workflow block = compoundWorkflow.getWorkflows().get(wfIndex);
        Task delTask = block.getTasks().get(taskIndex);
        if (delTask.getNode().getType().equals(WorkflowSpecificModel.Types.ASSIGNMENT_TASK) && WorkflowUtil.isActiveResponsible(delTask)) {
            for (Task task : block.getTasks()) {
                if (WorkflowUtil.isInactiveResponsible(task) && !Status.FINISHED.equals(task.getStatus()) && !Status.UNFINISHED.equals(task.getStatus())) {
                    task.getNode().getProperties().put(WorkflowSpecificModel.Props.ACTIVE.toString(), Boolean.TRUE);
                }
            }
        }
        block.removeTask(taskIndex);
        updateTaskGroupsAfterTaskRemoval(wfIndex, taskIndex);
        if (updatePanelGroup) { // Regenerate component only if needed
            updatePanelGroupWithoutWorkflowBlockUpdate();
        }
    }

    /**
     * Action listener for JSP.
     */
    public void compoundWorkflowDefinitionTypeChanged(ValueChangeEvent event) {
        ComponentUtil.executeLater(PhaseId.INVOKE_APPLICATION, ComponentUtil.getAncestorComponent(event.getComponent(), UIPropertySheet.class), new Closure() {
            @Override
            public void execute(Object input) {
                updatePanelGroup();
            }
        });
    }

    public boolean isShowDocumentTypes() {
        return isType(CompoundWorkflowType.DOCUMENT_WORKFLOW);
    }

    public boolean isShowCaseFileTypes() {
        return isType(CompoundWorkflowType.CASE_FILE_WORKFLOW);
    }

    public boolean isShowCompoundWorkflowDefinitionType() {
        return BeanHelper.getWorkflowService().isIndependentWorkflowEnabled() || BeanHelper.getVolumeService().isCaseVolumeEnabled();
    }

    public boolean isShowSigningType() {
        return isType(CompoundWorkflowType.INDEPENDENT_WORKFLOW);
    }

    private boolean isType(CompoundWorkflowType requiredType) {
        String type = compoundWorkflow.getProp(WorkflowCommonModel.Props.TYPE);
        if (StringUtils.isBlank(type)) {
            return false;
        }
        return requiredType == CompoundWorkflowType.valueOf(type);
    }

    private void updateTaskGroupsAfterTaskRemoval(Integer wfIndex, Integer taskIndex) {
        if (taskGroups == null || taskGroups.size() <= wfIndex) {
            return;
        }

        Map<String, List<TaskGroup>> wfTaskGroup = taskGroups.get(wfIndex);
        if (wfTaskGroup == null) {
            return;
        }

        for (List<TaskGroup> taskGroupList : wfTaskGroup.values()) {
            for (TaskGroup taskGroup : taskGroupList) {
                List<Integer> taskIds = taskGroup.getTaskIds();
                taskIds.remove(taskIndex);
                for (int i = 0; i < taskIds.size(); i++) {
                    Integer taskId = taskIds.get(i);
                    if (taskId > taskIndex) {
                        taskIds.set(i, taskId - 1);
                    }
                }
            }
        }
    }

    public TaskGroup findTaskGroup(ActionEvent event) {
        String groupName = ActionUtil.getParam(event, "groupName");
        String groupId = ActionUtil.getParam(event, "groupId");
        Integer wfIndex = ActionUtil.getParam(event, WF_INDEX, Integer.class);

        for (TaskGroup group : getTaskGroups().get(wfIndex).get(groupName)) {
            if (!group.getGroupId().equals(groupId)) {
                continue;
            }
            return group;
        }

        return null;
    }

    // merge this into UserContactGroupSearchBean#searchAll
    private SelectItem[] executeOwnerSearch(PickerSearchParams params, boolean orgOnly, boolean taskCapableOnly, boolean dvkCapableOnly, String institutionToRemove) {
        log.debug("executeOwnerSearch: " + params.getFilterIndex() + ", " + params.getSearchString());
        List<Node> nodes = null;
        SelectItem[] results = new SelectItem[0];

        if (params.isFilterIndex(UserContactGroupSearchBean.USERS_FILTER)) {
            if (this instanceof CompoundWorkflowDialog) {
                results = (SelectItem[]) ArrayUtils.addAll(results, getUserListDialog().searchUsers(params));
            } else {
                results = (SelectItem[]) ArrayUtils.addAll(results, getUserListDialog().searchUsersWithoutSubstitutionInfoShown(params));
            }
        }

        if (params.isFilterIndex(UserContactGroupSearchBean.USER_GROUPS_FILTER)) {
            results = (SelectItem[]) ArrayUtils.addAll(results, getUserContactGroupSearchBean().searchGroups(params, false));
        }

        if (params.isFilterIndex(UserContactGroupSearchBean.CONTACTS_FILTER)) {
            if (taskCapableOnly) {
                nodes = getAddressbookService().searchTaskCapableContacts(params.getSearchString(), orgOnly, dvkCapableOnly, institutionToRemove, params.getLimit());
            } else {
                nodes = getAddressbookService().search(params.getSearchString(), params.getLimit());
            }
            results = (SelectItem[]) ArrayUtils.addAll(results, transformAddressbookNodesToSelectItems(nodes));
        }

        if (params.isFilterIndex(UserContactGroupSearchBean.CONTACT_GROUPS_FILTER)) {
            if (taskCapableOnly) {
                nodes = getAddressbookService().searchTaskCapableContactGroups(params.getSearchString(), orgOnly, taskCapableOnly, institutionToRemove, params.getLimit());
            } else {
                nodes = getAddressbookService().searchContactGroups(params.getSearchString(), true, false, params.getLimit());
            }
            results = (SelectItem[]) ArrayUtils.addAll(results, transformAddressbookNodesToSelectItems(nodes));
        }
        return results;
    }

    /**
     * Action listener for JSP.
     */
    public SelectItem[] executeOwnerSearch(PickerSearchParams params) {
        return executeOwnerSearch(params, false, false, false, null);
    }

    /**
     * Action listener for JSP.
     */
    public SelectItem[] executeTaskOwnerSearch(PickerSearchParams params) {
        return executeOwnerSearch(params, false, true, false, null);
    }

    /**
     * Action listener for JSP.
     */
    public SelectItem[] executeResponsibleOwnerSearch(PickerSearchParams params) {
        if (params.isFilterIndex(UserContactGroupSearchBean.USER_GROUPS_FILTER)) {
            params.setFilterIndex(UserContactGroupSearchBean.CONTACTS_FILTER);
        }
        return executeOwnerSearch(params, false, true, false, null);
    }

    /**
     * Action listener for JSP.
     */
    public SelectItem[] executeExternalReviewOwnerSearch(PickerSearchParams params) {
        if (params.isFilterIndex(UserContactGroupSearchBean.USERS_FILTER)) {
            params.setFilterIndex(UserContactGroupSearchBean.CONTACTS_FILTER);
        } else {
            params.setFilterIndex(UserContactGroupSearchBean.CONTACT_GROUPS_FILTER);
        }
        if (getWorkflowService().isInternalTesting()) {
            return executeOwnerSearch(params, true, true, true, null);
        }
        return executeOwnerSearch(params, true, true, true, getDvkService().getInstitutionCode());
    }

    /**
     * Action listener for JSP.
     */
    public SelectItem[] executeDueDateExtensionOwnerSearch(PickerSearchParams params) {
        return executeOwnerSearch(params, false, false, false, null);
    }

    /**
     * Action listener for JSP.
     */
    public void processExternalReviewOwnerSearchResults(ActionEvent event) {
        UIGenericPicker picker = (UIGenericPicker) event.getComponent();
        int filterIndex = picker.getFilterIndex();
        if (filterIndex == UserContactGroupSearchBean.USERS_FILTER) {
            filterIndex = UserContactGroupSearchBean.CONTACTS_FILTER;
        } else {
            filterIndex = UserContactGroupSearchBean.CONTACT_GROUPS_FILTER;
        }
        processOwnerSearchResults(event, filterIndex);
    }

    /**
     * Action listener for JSP.
     */
    public void processResponsibleOwnerSearchResults(ActionEvent event) {
        UIGenericPicker picker = (UIGenericPicker) event.getComponent();
        int filterIndex = picker.getFilterIndex();
        if (filterIndex == UserContactGroupSearchBean.USER_GROUPS_FILTER) {
            filterIndex = UserContactGroupSearchBean.CONTACTS_FILTER;
        }
        processOwnerSearchResults(event, filterIndex);
    }

    /**
     * Action listener for JSP.
     */
    public void processOwnerSearchResults(ActionEvent event) {
        UIGenericPicker picker = (UIGenericPicker) event.getComponent();
        int filterIndex = picker.getFilterIndex();
        processOwnerSearchResults(event, filterIndex);
    }

    private void processOwnerSearchResults(ActionEvent event, int filterIndex) {
        UIGenericPicker picker = (UIGenericPicker) event.getComponent();
        int wfIndex = (Integer) picker.getAttributes().get(TaskListGenerator.ATTR_WORKFLOW_INDEX);
        int taskIndex = Integer.parseInt((String) picker.getAttributes().get(Search.OPEN_DIALOG_KEY));
        picker.getAttributes().remove(Search.OPEN_DIALOG_KEY);
        boolean addOrderAssignmentResponsibleTask = false;
        Workflow block = compoundWorkflow.getWorkflows().get(wfIndex);
        Date originalTaskDueDate = null;
        if (taskIndex >= 0) {
            Task originalTask = block.getTasks().get(taskIndex);
            addOrderAssignmentResponsibleTask = originalTask.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK) && originalTask.isResponsible();
            originalTaskDueDate = originalTask.getDueDate();
        }
        String[] results = picker.getSelectedResults();
        if (results == null) {
            return;
        }
        log.debug("processOwnerSearchResults: " + picker.getId() + ", " + wfIndex + ", " //
                + taskIndex + ", " + filterIndex + " = " + StringUtils.join(results, ","));

        long addTasksStart = System.nanoTime();
        long addingTaskTotal = 0;
        long getUserNamesInGroupTotal = 0;
        long retrieveUserPropsTotal = 0;
        long retrieveOrgPropsTotal = 0;
        long userPropsSet = 0;
        boolean isUserGroupFilter = filterIndex == UserContactGroupSearchBean.USER_GROUPS_FILTER;
        for (int i = 0; i < results.length; i++) {
            if (i > 0 && !isUserGroupFilter) {
                long addingTaskStart = System.nanoTime();
                taskIndex = addTask(taskIndex, addOrderAssignmentResponsibleTask, block, originalTaskDueDate);
                addingTaskTotal += System.nanoTime() - addingTaskStart;
            }

            // users
            if (filterIndex == UserContactGroupSearchBean.USERS_FILTER) {
                Pair<Long, Long> userAndOrgRetrieveTime = setPersonPropsToTask(block, taskIndex, results[i], null);
                retrieveUserPropsTotal += userAndOrgRetrieveTime.getFirst();
                retrieveOrgPropsTotal += userAndOrgRetrieveTime.getSecond();
                userPropsSet++;
            }
            // user groups
            else if (isUserGroupFilter) {
                long getUserNamesInGroupStart = System.nanoTime();
                Set<String> children = UserUtil.getUsersInGroup(results[i], getNodeService(), getUserService(), getParametersService(), getDocumentSearchService());
                getUserNamesInGroupTotal += System.nanoTime() - getUserNamesInGroupStart;

                String groupName = BeanHelper.getAuthorityService().getAuthorityDisplayName(results[i]);
                int j = 0;
                for (String userName : children) {
                    if (i > 0 || j++ > 0) {
                        long addingTaskStart = System.nanoTime();
                        taskIndex = addTask(taskIndex, addOrderAssignmentResponsibleTask, block, originalTaskDueDate);
                        addingTaskTotal += System.nanoTime() - addingTaskStart;
                    }
                    Pair<Long, Long> userAndOrgRetrieveTime = setPersonPropsToTask(block, taskIndex, userName, groupName);
                    retrieveUserPropsTotal += userAndOrgRetrieveTime.getFirst();
                    retrieveOrgPropsTotal += userAndOrgRetrieveTime.getSecond();
                    userPropsSet++;
                }
            }
            // contacts
            else if (filterIndex == UserContactGroupSearchBean.CONTACTS_FILTER) {
                if (block.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW)) {
                    setExternalReviewPropsToTask(block, taskIndex, new NodeRef(results[i]), null);
                } else {
                    setContactPropsToTask(block, taskIndex, new NodeRef(results[i]), null);
                }
            }
            // contact groups
            else if (filterIndex == UserContactGroupSearchBean.CONTACT_GROUPS_FILTER) {
                taskIndex = addContactGroupTasks(taskIndex, block, new NodeRef(results[i]), addOrderAssignmentResponsibleTask, originalTaskDueDate);
            } else {
                throw new RuntimeException("Unknown filter index value: " + filterIndex);
            }
        }
        long totalDuration = CalendarUtil.duration(addTasksStart);
        if (log.isTraceEnabled() || totalDuration > 2000) {
            boolean isUserFilter = filterIndex == UserContactGroupSearchBean.USERS_FILTER;
            StringBuffer sb = new StringBuffer("Adding tasks statistics:\n");
            sb.append("Processed " + results.length + " search results"
                    + (isUserFilter || isUserGroupFilter ? ", used filter " + (isUserFilter ? " Kasutajad " : "Kasutajagrupid") : "") + "\n");
            sb.append("Processing tasks total time: " + totalDuration).append(" ms\n");
            sb.append("Set user properties on " + userPropsSet + " tasks");
            if (isUserFilter || isUserGroupFilter) {
                sb.append(" of that\n");
                sb.append("   adding tasks: " + (addingTaskTotal / 1000000L) + " ms\n");
                sb.append("   retrieving usernames in group: " + (getUserNamesInGroupTotal / 1000000L) + " ms\n");
                sb.append("   retrieving user props: " + (retrieveUserPropsTotal / 1000000L) + " ms\n");
                sb.append("   retrieving org props: " + (retrieveOrgPropsTotal / 1000000L) + " ms\n");
            }
            log.trace(sb.toString());
        }
        updatePanelGroupWithoutWorkflowBlockUpdate();
    }

    private int addTask(int taskIndex, boolean addOrderAssignmentResponsibleTask, Workflow block, Date originalTaskDueDate) {
        Task addedTask;
        if (addOrderAssignmentResponsibleTask) {
            addedTask = ((OrderAssignmentWorkflow) block).addResponsibleTask(++taskIndex);
        } else {
            addedTask = block.addTask(++taskIndex);
        }

        if (addedTask != null && originalTaskDueDate != null) {
            addedTask.setDueDate(originalTaskDueDate);
        }
        return taskIndex;
    }

    private int addContactGroupTasks(int taskIndex, Workflow block, NodeRef contactGroup, boolean addOrderAssignmentResponsibleTask, Date originalTaskDueDate) {
        int taskCounter = 0;
        boolean isExternalReviewTask = block.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW);
        List<NodeRef> contacts = getAddressbookService().getContactGroupContents(contactGroup);
        String groupName = (String) getNodeService().getProperty(contactGroup, AddressbookModel.Props.GROUP_NAME);
        for (int j = 0; j < contacts.size(); j++) {
            Map<QName, Serializable> contactProps = getNodeService().getProperties(contacts.get(j));
            if (getNodeService().hasAspect(contacts.get(j), AddressbookModel.Aspects.ORGANIZATION_PROPERTIES)
                    && Boolean.TRUE.equals(contactProps.get(AddressbookModel.Props.TASK_CAPABLE))
                    && (!isExternalReviewTask || Boolean.TRUE.equals(contactProps.get(AddressbookModel.Props.DVK_CAPABLE)))) {
                if (taskCounter > 0) {
                    taskIndex = addTask(taskIndex, addOrderAssignmentResponsibleTask, block, originalTaskDueDate);
                }
                if (isExternalReviewTask) {
                    setExternalReviewProps(block, taskIndex, contactProps, groupName);
                } else {
                    setContactPropsToTask(block, taskIndex, contacts.get(j), groupName);
                }
                taskCounter++;
            }
        }
        return taskIndex;
    }

    private void setExternalReviewPropsToTask(Workflow block, int index, NodeRef contact, String groupName) {
        Map<QName, Serializable> resultProps = getNodeService().getProperties(contact);
        setExternalReviewProps(block, index, resultProps, groupName);
    }

    private void setExternalReviewProps(Workflow block, int index, Map<QName, Serializable> resultProps, String groupName) {
        Task task = block.getTasks().get(index);
        task.setInstitutionName((String) resultProps.get(AddressbookModel.Props.ORGANIZATION_NAME));
        task.setInstitutionCode((String) resultProps.get(AddressbookModel.Props.ORGANIZATION_CODE));
        task.setOwnerGroup(groupName);
    }

    /**
     * Binding for JSP.
     */
    public HtmlPanelGroup getPanelGroup() {
        return panelGroup;
    }

    /**
     * Binding for JSP.
     */
    public HtmlPanelGroup getSaveAsGroup() {
        return saveAsGroup;
    }

    /**
     * Binding for JSP.
     */
    public HtmlPanelGroup getCommonDataGroup() {
        return commonDataGroup;
    }

    public void setPanelGroup(HtmlPanelGroup panelGroup) {
        if (this.panelGroup == null) {
            this.panelGroup = panelGroup;
            updatePanelGroup(null, null, false, false, null, true);
        } else {
            this.panelGroup = panelGroup;
        }
    }

    public void setSaveAsGroup(HtmlPanelGroup saveAsGroup) {
        if (this.saveAsGroup == null && isShowSaveAsGroup()) {
            this.saveAsGroup = saveAsGroup;
            updateSaveAsGroup();
        } else {
            this.saveAsGroup = saveAsGroup;
        }
    }

    public void setCommonDataGroup(HtmlPanelGroup commonDataGroup) {
        if (this.commonDataGroup == null) {
            this.commonDataGroup = commonDataGroup;
            updateCommonDataGroup();
        } else {
            this.commonDataGroup = commonDataGroup;
        }
    }

    /**
     * Getter for form input bindings.
     */
    public CompoundWorkflow getWorkflow() {
        return compoundWorkflow;
    }

    /**
     * Getter for parallel checkbox values.
     */
    public List<SelectItem> getParallelSelections(@SuppressWarnings("unused") FacesContext context, @SuppressWarnings("unused") UIInput selectComponent) {
        return parallelSelections;
    }

    /**
     * Getter for the task owner search picker filter.
     */
    public SelectItem[] getOwnerSearchFilters() {
        return ownerSearchBean.getOwnerSearchFilters();
    }

    /**
     * Getter for the task owner search picker filter.
     */
    public SelectItem[] getExternalReviewOwnerSearchFilters() {
        return externalReviewSearchFilters;
    }

    public void setOwnerSearchBean(OwnerSearchBean ownerSearchBean) {
        this.ownerSearchBean = ownerSearchBean;
    }

    public boolean getFullAccess() {
        return fullAccess;
    }

    public boolean isShowUserFullName() {
        return compoundWorkflow instanceof CompoundWorkflowDefinition && StringUtils.isNotBlank(((CompoundWorkflowDefinition) compoundWorkflow).getUserId());
    }

    // /// PROTECTED & PRIVATE METHODS /////

    protected void resetState() {
        resetState(true);
    }

    protected void resetState(boolean resetPanelGroup) {
        compoundWorkflow = null;
        if (resetPanelGroup) {
            commonDataGroup = null;
            panelGroup = null;
            saveAsGroup = null;
        }
        sortedTypes = null;
        isUnsavedWorkFlow = false;
        taskGroups = null;
        workflowBlockExpandedStatuses = null;
    }

    /**
     * Exclude workflow types not available within entire compound workflow.
     * Additional filtering may be performed for each workflow "Lisa töövoog" button.
     * NB! CompoundWorkflowDialog also uses this method as start point for overriden getSortedTypes method.
     */
    protected Map<String, QName> getSortedTypes() {
        if (sortedTypes == null) {
            sortedTypes = new TreeMap<String, QName>();
            Map<QName, WorkflowType> workflowTypes = getWorkflowService().getWorkflowTypes();
            for (QName tmpType : workflowTypes.keySet()) {
                if (tmpType.equals(WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_WORKFLOW)) {
                    continue;
                }
                if (tmpType.equals(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW) && !getWorkflowService().externalReviewWorkflowEnabled()) {
                    continue;
                }
                if (isNotAllowedConfirmationWorkflow(tmpType)) {
                    continue;
                }
                if (isNotAllowedOrderAssignmentWorkflow(tmpType)) {
                    continue;
                }
                if (isNotAllowedGroupAssignmentWorkflow(tmpType)) {
                    continue;
                }
                String tmpName = MessageUtil.getMessage(tmpType.getLocalName());
                sortedTypes.put(tmpName, tmpType);
            }
        }
        return sortedTypes;
    }

    protected boolean isNotAllowedConfirmationWorkflow(QName tmpType) {
        return tmpType.equals(WorkflowSpecificModel.Types.CONFIRMATION_WORKFLOW) && !getWorkflowService().isConfirmationWorkflowEnabled();
    }

    protected boolean isNotAllowedOrderAssignmentWorkflow(QName tmpType) {
        return tmpType.equals(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW) && !getWorkflowService().isOrderAssignmentWorkflowEnabled();
    }

    protected boolean isNotAllowedGroupAssignmentWorkflow(QName tmpType) {
        return tmpType.equals(WorkflowSpecificModel.Types.GROUP_ASSIGNMENT_WORKFLOW) && !getWorkflowService().isGroupAssignmentWorkflowEnabled();
    }

    protected String getConfigArea() {
        return "workflow-settings";
    }

    protected void updatePanelGroup(boolean retrieveExpandedStatuses) {
        updatePanelGroup(null, null, true, retrieveExpandedStatuses, null, true);
    }

    protected void updatePanelGroup() {
        updatePanelGroup(null, null, true, true, null, true);
    }

    protected void updatePanelGroupWithoutWorkflowBlockUpdate() {
        updatePanelGroup(null, null, true, true, null, false);
    }

    protected void updatePanelGroup(List<String> confirmationMessages, String validatedAction, boolean updateAllGroups, boolean retrieveExpandedStatuses,
            List<Pair<String, Object>> confirmationMessageParams, boolean updateWorkflowBlockBean) {
        FacesContext context = FacesContext.getCurrentInstance();
        Application application = context.getApplication();

        resetPanelGroup(retrieveExpandedStatuses);

        if (compoundWorkflow == null) {
            return;
        }

        updateFullAccess();
        ensureResponsibleTaskExists();

        boolean dontShowAddActions = dontShowAddActions();

        Document document = getParentDocument();

        if (updateAllGroups) {
            updateCommonDataGroup(application, dontShowAddActions, document);
            addInfoMessageIfNeeded();
            addLargeWorkflowWarning();
        }

        // render every workflow block
        int wfCounter = 1;
        boolean firstLoading = taskGroups == null; // Check if we are loading pre-saved definition, where document registration WFs are not processed by TaskListGenerator
        for (Workflow block : compoundWorkflow.getWorkflows()) {
            // block actions
            if (WorkflowUtil.isGeneratedByDelegation(block)) {
                wfCounter++;
                continue;
            }
            HtmlPanelGroup facetGroup = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
            facetGroup.setId("action-group-" + wfCounter);
            if (firstLoading) {
                getTaskGroups().add(new HashMap<String, List<TaskGroup>>());
            }
            if (!dontShowAddActions && fullAccess && showAddActions(wfCounter)) {
                // block add workflow actions
                UIMenu addActionsMenu = buildAddActions(application, wfCounter, document);
                addChildren(facetGroup, addActionsMenu);
            }

            String blockStatus = block.getStatus();
            if (fullAccess && Status.NEW.equals(blockStatus) && !block.isMandatory()
                    || !(this instanceof CompoundWorkflowDialog)) {
                // block remove workflow actions
                HtmlPanelGroup deleteActions = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
                deleteActions.setId("action-remove-" + wfCounter);
                addChildren(facetGroup, deleteActions);

                UIActionLink deleteLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
                deleteLink.setId("action-remove-link-" + wfCounter);
                deleteLink.setRendererType(UIActions.RENDERER_ACTIONLINK);
                deleteLink.setImage("/images/icons/delete.gif");
                deleteLink.setValue(MessageUtil.getMessage("workflow_compound_remove_block"));
                deleteLink.setActionListener(application.createMethodBinding("#{DialogManager.bean.removeWorkflowBlock}", UIActions.ACTION_CLASS_ARGS));
                deleteLink.setShowLink(false);
                addChildren(deleteActions, deleteLink);

                addChildren(deleteLink, createUIParam(WF_INDEX, wfCounter - 1, application));
            }

            // block data panel
            UIPanel panelW = (UIPanel) application.createComponent("org.alfresco.faces.Panel");
            panelW.setId(WORKFLOW_PANEL_ID_PREFIX + wfCounter);
            putAttribute(panelW, "styleClass", "panel-100 ie7-workflow workflow-panel");
            String panelLabel = MessageUtil.getMessage(block.getNode().getType().getLocalName() + "_title");
            if (StringUtils.isBlank(getConfigArea())) {
                String workflowDescription = (String) block.getNode().getProperties().get(WorkflowSpecificModel.Props.DESCRIPTION);
                panelLabel = (StringUtils.isBlank(workflowDescription)) ? panelLabel : panelLabel + " - " + workflowDescription;
            }
            panelW.setLabel(panelLabel);
            panelW.setProgressive(true);
            panelW.setExpanded(workflowBlockExpandedStatuses.get(wfCounter - 1));
            if (facetGroup.getChildCount() > 0) {
                addFacet(panelW, "title", facetGroup);
            }
            addChildren(panelGroup, panelW);

            // block data properties
            UIPropertySheet sheetW = (UIPropertySheet) application.createComponent("org.alfresco.faces.PropertySheet");
            sheetW.setId("workflow-" + wfCounter);
            sheetW.setVar("nodeW" + wfCounter);
            putAttribute(sheetW, "workFlowIndex", wfCounter - 1);
            sheetW.setNode(block.getNode());
            putAttribute(sheetW, "labelStyleClass", "propertiesLabel");
            putAttribute(sheetW, "externalConfig", Boolean.TRUE);
            putAttribute(sheetW, "columns", 1);
            putAttribute(sheetW, HTML.WIDTH_ATTR, "100%");
            sheetW.setConfigArea(getConfigArea());
            putAttribute(sheetW, TaskListGenerator.ATTR_WORKFLOW_INDEX, wfCounter - 1);
            if (!fullAccess) {
                sheetW.setMode(UIPropertySheet.VIEW_MODE);
            }
            addChildren(panelW, sheetW);

            wfCounter++;
        }
        addConfirmationMessages(confirmationMessages, "#{" + validatedAction + "}", context, application, confirmationMessageParams);

        if (this instanceof CompoundWorkflowDialog) {
            if (updateAllGroups) {
                updateSaveAsGroup();
            }
            if (updateWorkflowBlockBean && !compoundWorkflow.isDocumentWorkflow()) {
                BeanHelper.getWorkflowBlockBean().initIndependentWorkflow(compoundWorkflow, (CompoundWorkflowDialog) this);
            }
        }
        ComponentUtil.setAjaxEnabledOnActionLinksRecursive(panelGroup, -1);
    }

    public void resetPanelGroup(boolean retrieveExpandedStatuses) {
        if (retrieveExpandedStatuses) {
            retrieveExpandedStatuses();
        }
        panelGroup.getChildren().clear();
    }

    protected void addConfirmationMessages(List<String> confirmationMessages, String methodBinding, FacesContext context, Application application, List<Pair<String, Object>> params) {
        if (confirmationMessages != null && !confirmationMessages.isEmpty()) {
            HtmlSelectOneMenu messageInput = (HtmlSelectOneMenu) application.createComponent(HtmlSelectOneMenu.COMPONENT_TYPE);
            messageInput.setId(WORKFLOW_CONFIRMATION_MESSAGES);
            messageInput.setStyleClass(WORKFLOW_CONFIRMATION_MESSAGES);
            for (String message : confirmationMessages) {
                UISelectItem selectItem = (UISelectItem) application.createComponent(UISelectItem.COMPONENT_TYPE);
                selectItem.setItemValue(RendererUtils.getConvertedUIOutputValue(context, messageInput, message));
                addChildren(messageInput, selectItem);
            }
            messageInput.setStyle("display: none;");
            addChildren(commonDataGroup, messageInput);

            // hidden link for submitting form when OK is clicked in js confirmation alert
            HtmlCommandLink workflowConfirmationLink = new HtmlCommandLink();
            workflowConfirmationLink.setId(WORKFLOW_AFTER_CONFIRMATION_LINK);
            workflowConfirmationLink.setStyleClass(WORKFLOW_AFTER_CONFIRMATION_LINK);
            workflowConfirmationLink.setActionListener(application.createMethodBinding(methodBinding, UIActions.ACTION_CLASS_ARGS));
            workflowConfirmationLink.setStyle("display: none;");
            if (params != null) {
                for (Pair<String, Object> param : params) {
                    workflowConfirmationLink.getChildren().add(ComponentUtil.createUIParam(param.getFirst(), param.getSecond(), application));
                }
            }
            addChildren(commonDataGroup, workflowConfirmationLink);
        }
    }

    private void retrieveExpandedStatuses() {
        if (panelGroup == null) {
            return;
        }
        List<UIComponent> children = ComponentUtil.getChildren(panelGroup);
        if (children == null) {
            return;
        }
        workflowBlockExpandedStatuses = new ArrayList<Boolean>();
        for (UIComponent component : children) {
            if (!(component instanceof UIPanel)) {
                continue;
            }
            UIPanel panel = (UIPanel) component;
            if (panel.getId().startsWith(WORKFLOW_PANEL_ID_PREFIX)) {
                workflowBlockExpandedStatuses.add(panel.getExpandedState());
            }
        }
    }

    private void addInfoMessageIfNeeded() {
        if (this instanceof CompoundWorkflowDialog && compoundWorkflow != null && compoundWorkflow.isDocumentWorkflow()
                && getWorkflowService().hasTwoInProgressOrStoppedCWorkflowsWithMultipleWorkflows(compoundWorkflow, false)) {
            String paramText = BeanHelper.getParametersService().getStringParameter(Parameters.ONE_STEP_ALLOWED_IN_COMPOUND_WORKFLOW_MESSAGE);
            if (StringUtils.isNotBlank(paramText)) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(paramText));
            }
        }
    }

    private boolean dontShowAddActions() {
        boolean dontShowAddActions = getSortedTypes().isEmpty();
        if (!dontShowAddActions && this instanceof CompoundWorkflowDialog && !compoundWorkflow.getWorkflows().isEmpty()) {
            List<CompoundWorkflow> otherCompoundWorkflows = getWorkflowService().getOtherCompoundWorkflows(compoundWorkflow);
            for (CompoundWorkflow cwf : otherCompoundWorkflows) {
                if (cwf.isStatus(Status.IN_PROGRESS, Status.STOPPED) && cwf.getWorkflows().size() > 1 && !cwf.getWorkflows().isEmpty()) {
                    return true;
                }
            }
        }
        return dontShowAddActions;
    }

    private void updateCommonDataGroup() {
        updateCommonDataGroup(FacesContext.getCurrentInstance().getApplication(), dontShowAddActions(), getParentDocument());
    }

    private void updateCommonDataGroup(Application application, boolean dontShowAddActions, Document document) {
        if (compoundWorkflow == null) {
            return;
        }
        // common data panel
        commonDataGroup.getChildren().clear();
        commonDataGroup.setId("compound-workflow-group");

        UIPanel commonDataPanel = (UIPanel) application.createComponent("org.alfresco.faces.Panel");
        commonDataPanel.setId("compound-workflow-panel");
        putAttribute(commonDataPanel, "styleClass", "panel-100 ie7-workflow");
        commonDataPanel.setLabel(MessageUtil.getMessage("workflow_compound_data"));
        commonDataPanel.setProgressive(true);
        addChildren(commonDataPanel, commonDataPanel);

        if (!dontShowAddActions && fullAccess && showAddActions(0)) {
            // common data add workflow actions
            UIMenu addActionsMenuC = buildAddActions(application, 0, document);
            addFacet(commonDataPanel, "title", addActionsMenuC);
        }

        // common data properties
        UIPropertySheet sheetC = (UIPropertySheet) application.createComponent("org.alfresco.faces.PropertySheet");
        sheetC.setId("compound");
        sheetC.setVar("nodeC");
        sheetC.setNode(compoundWorkflow.getNode());
        putAttribute(sheetC, "labelStyleClass", "propertiesLabel");
        putAttribute(sheetC, "styleClass", "panel-100");
        putAttribute(sheetC, "externalConfig", Boolean.TRUE);
        putAttribute(sheetC, "columns", 1);
        // sheetC.getAttributes().put(HTML.WIDTH_ATTR, "100%");
        sheetC.setConfigArea(getConfigArea());
        if (!fullAccess) {
            sheetC.setMode(UIPropertySheet.VIEW_MODE);
        }
        addChildren(commonDataPanel, sheetC);
        addChildren(commonDataGroup, commonDataPanel);
    }

    protected void addLargeWorkflowWarning() {
        if (compoundWorkflow == null) {
            return;
        }
        Long largeWorkflowTaskLimit = BeanHelper.getParametersService().getLongParameter(Parameters.LARGE_WORKFLOW_WARNING_TASK_COUNT);
        int taskCount = 0;
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            taskCount += workflow.getTasks().size();
            if (taskCount > largeWorkflowTaskLimit) {
                MessageUtil.addInfoMessage("large_workflow_warning", largeWorkflowTaskLimit);
                break;
            }
        }
    }

    private void updateSaveAsGroup() {
        if (compoundWorkflow == null) {
            return;
        }
        FacesContext context = FacesContext.getCurrentInstance();
        Application application = context.getApplication();
        saveAsGroup.getChildren().clear();
        saveAsGroup.setId("compound-workflow-saveas-group");

        UIPanel saveAsPanel = (UIPanel) application.createComponent("org.alfresco.faces.Panel");
        saveAsPanel.setId("compound-workflow-saveas-panel");
        putAttribute(saveAsPanel, "styleClass", "panel-100");
        saveAsPanel.setLabel(MessageUtil.getMessage("workflow_compound_saveas"));
        saveAsPanel.setProgressive(true);
        saveAsPanel.setExpanded(false);
        saveAsPanel.setFacetsId("dialog:dialog-body:compound-workflow-saveas-panel");
        addChildren(saveAsGroup, saveAsPanel);

        final HtmlPanelGrid saveasGrid = (HtmlPanelGrid) application.createComponent(HtmlPanelGrid.COMPONENT_TYPE);
        saveasGrid.setId("compound-workflow-saveas-grid");
        saveasGrid.setWidth("100%");
        saveasGrid.setColumns(2);
        saveasGrid.setCellpadding("3");
        saveasGrid.setCellspacing("3");
        saveasGrid.setBorder(0);
        saveasGrid.setColumnClasses("propertiesLabel,");
        saveasGrid.setWidth("100%");

        UIOutput compWorkflowDefinitionLabel = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
        compWorkflowDefinitionLabel.setValue(MessageUtil.getMessage("compoundWorkflow_definition_change") + ": ");
        addChildren(saveasGrid, compWorkflowDefinitionLabel);

        GeneralSelectorGenerator selectorGenerator = new GeneralSelectorGenerator();
        HtmlSelectOneMenu compoundWorkflowDefinitionSelector = (HtmlSelectOneMenu) selectorGenerator.generateSelectComponent(context, COMP_WORKFLOW_DEFINITION_SELECTOR_ID, false);
        selectorGenerator.getCustomAttributes().put("selectionItems", "#{CompoundWorkflowDialog.getUserCompoundWorkflowDefinitions}");
        selectorGenerator.setupSelectComponent(context, null, null, null, compoundWorkflowDefinitionSelector, false);
        compoundWorkflowDefinitionSelector.setValueBinding("value", application.createValueBinding("#{CompoundWorkflowDialog.existingUserCompoundWorkflowDefinition}"));
        addChildren(saveasGrid, compoundWorkflowDefinitionSelector);

        UIOutput newCompWorkflowDefinitionLabel = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
        newCompWorkflowDefinitionLabel.setValue(MessageUtil.getMessage("compoundWorkflow_definition_add") + ": ");
        addChildren(saveasGrid, newCompWorkflowDefinitionLabel);

        HtmlInputText compoundWorkflowDefinitionInput = new HtmlInputText();
        compoundWorkflowDefinitionInput.setId(COMP_WORKFLOW_DEFINITION_INPUT_ID);
        compoundWorkflowDefinitionInput.setValueBinding("value", application.createValueBinding("#{CompoundWorkflowDialog.newUserCompoundWorkflowDefinition}"));
        addChildren(saveasGrid, compoundWorkflowDefinitionInput);

        UIOutput dummy = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
        dummy.setValue("");
        addChildren(saveasGrid, dummy);

        final HtmlPanelGrid saveasButtonGrid = (HtmlPanelGrid) application.createComponent(HtmlPanelGrid.COMPONENT_TYPE);
        saveasButtonGrid.setId("compound-workflow-saveas-button-grid");
        saveasButtonGrid.setWidth("30%");
        saveasButtonGrid.setColumns(3);

        HtmlCommandButton saveAsButton = new HtmlCommandButton();
        saveAsButton.setId("comp-workflow-def-saveas-button");
        saveAsButton.setActionListener(application.createMethodBinding("#{CompoundWorkflowDialog.saveasCompoundWorkflowDefinition}", new Class[] { ActionEvent.class }));
        saveAsButton.setValue(MessageUtil.getMessage("compoundWorkflow_definition_saveas_button"));
        saveAsButton.setOnclick("setPageScrollY();");
        addChildren(saveasButtonGrid, saveAsButton);

        HtmlCommandButton deleteButton = new HtmlCommandButton();
        deleteButton.setId("comp-workflow-def-delete-button");
        deleteButton.setActionListener(application.createMethodBinding("#{CompoundWorkflowDialog.deleteCompoundWorkflowDefinition}", new Class[] { ActionEvent.class }));
        deleteButton.setValue(MessageUtil.getMessage("compoundWorkflow_definition_delete_button"));
        deleteButton.setOnclick("setPageScrollY();");
        addChildren(saveasButtonGrid, deleteButton);

        addChildren(saveasGrid, saveasButtonGrid);

        addChildren(saveAsPanel, saveasGrid);

    }

    public boolean isShowSaveAsGroup() {
        return this instanceof CompoundWorkflowDialog;
    }

    @SuppressWarnings("unchecked")
    protected UIMenu buildAddActions(Application application, int counter, Document doc) {
        HtmlPanelGroup addActions = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
        addActions.setId("action-add-" + counter);

        MethodBinding actionListener = application.createMethodBinding("#{DialogManager.bean.addWorkflowBlock}", UIActions.ACTION_CLASS_ARGS);
        for (Entry<String, QName> entry : getSortedTypes().entrySet()) {
            QName workflowType = entry.getValue();
            if (isAddLinkForWorkflow(doc, workflowType)) {
                UIActionLink addLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
                addLink.setId("action-add-" + workflowType.getLocalName() + "-" + counter);
                addLink.setRendererType(UIActions.RENDERER_ACTIONLINK);
                addLink.setImage("/images/icons/add.gif");
                addLink.setValue(entry.getKey());
                addLink.setActionListener(actionListener);
                addActions.getChildren().add(addLink);
                addChildren(addLink, createUIParam("workflowType", entry.getValue().toString(), application), createUIParam(WF_INDEX, counter, application));
            }
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

    protected Document getParentDocument() {
        return null;
    }

    protected boolean isAddLinkForWorkflow(Document nill, QName workflowType) {
        if (WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW.equals(workflowType) && !compoundWorkflow.isDocumentWorkflow()) {
            return false;
        }
        if (compoundWorkflow.isCaseFileWorkflow()
                && (WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW.equals(workflowType)
                        || WorkflowSpecificModel.Types.OPINION_WORKFLOW.equals(workflowType)
                        || WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW.equals(workflowType))) {
            return false;
        }
        return true;
    }

    private boolean showAddActions(int index) {
        boolean result = false;
        if (index < compoundWorkflow.getWorkflows().size()) {
            Workflow workflow = compoundWorkflow.getWorkflows().get(index);
            String nextBlockStatus = workflow.getStatus();
            result = Status.NEW.equals(nextBlockStatus) && !workflow.isType(WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_WORKFLOW);
        } else {
            String compoundWorkflowStatus = compoundWorkflow.getStatus();
            boolean isLastAllowedType = true;
            if (!compoundWorkflow.getWorkflows().isEmpty()) {
                Workflow lastWorkflow = compoundWorkflow.getWorkflows().get(index - 1);
                isLastAllowedType = !lastWorkflow.isType(WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_WORKFLOW);
            }
            result = !Status.FINISHED.equals(compoundWorkflowStatus) && isLastAllowedType;
        }
        return result;
    }

    private Pair<Long, Long> setPersonPropsToTask(Workflow block, int taskIndex, String userName, String groupName) {
        long retrieveUserPropsStart = System.nanoTime();
        Map<QName, Serializable> resultProps = getUserService().getUserProperties(userName);
        long retrieveUserTime = System.nanoTime() - retrieveUserPropsStart;
        String name = UserUtil.getPersonFullName1(resultProps);
        Serializable id = resultProps.get(ContentModel.PROP_USERNAME);
        Serializable email = resultProps.get(ContentModel.PROP_EMAIL);
        long retrieveOrgPropsStart = System.nanoTime();
        Serializable orgName = (Serializable) getOrganizationStructureService().getOrganizationStructurePaths((String) resultProps.get(ContentModel.PROP_ORGID));
        long retrieveOrgTime = System.nanoTime() - retrieveOrgPropsStart;
        Serializable jobTitle = resultProps.get(ContentModel.PROP_JOBTITLE);
        setPropsToTask(block, taskIndex, name, id, email, orgName, jobTitle, groupName);
        return Pair.newInstance(retrieveUserTime, retrieveOrgTime);
    }

    private void setPersonPropsToTask(Task task, Map<QName, Serializable> personProps) {
        String name = UserUtil.getPersonFullName1(personProps);
        Serializable id = personProps.get(ContentModel.PROP_USERNAME);
        Serializable email = personProps.get(ContentModel.PROP_EMAIL);
        Serializable orgName = (Serializable) getOrganizationStructureService().getOrganizationStructurePaths((String) personProps.get(ContentModel.PROP_ORGID));
        Serializable jobTitle = personProps.get(ContentModel.PROP_JOBTITLE);
        setPropsToTask(task, name, id, email, orgName, jobTitle, null);
    }

    private void setContactPropsToTask(Workflow block, int index, NodeRef contact, String groupName) {
        Map<QName, Serializable> resultProps = getNodeService().getProperties(contact);
        QName resultType = getNodeService().getType(contact);

        String name = null;
        if (resultType.equals(Types.ORGANIZATION)) {
            name = (String) resultProps.get(AddressbookModel.Props.ORGANIZATION_NAME);
        } else {
            name = UserUtil.getPersonFullName((String) resultProps.get(AddressbookModel.Props.PERSON_FIRST_NAME) //
                    , (String) resultProps.get(AddressbookModel.Props.PERSON_LAST_NAME));
        }
        setPropsToTask(block, index, name, null, resultProps.get(AddressbookModel.Props.EMAIL), null, null, groupName);
    }

    private void setPropsToTask(Workflow block, int index, String name, Serializable id, Serializable email, Serializable orgName, Serializable jobTitle, Serializable groupName) {
        Task task = block.getTasks().get(index);
        if (task.getNode().hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE) && StringUtils.isNotBlank(task.getOwnerName())
                && task.getNodeRef() != null && !(compoundWorkflow instanceof CompoundWorkflowDefinition) && !Status.NEW.equals(task.getStatus())) {
            if (block.isType(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW)) {
                task = ((AssignmentWorkflow) block).addResponsibleTask();
            } else {
                task = ((OrderAssignmentWorkflow) block).addResponsibleTask();
            }
        }
        setPropsToTask(task, name, id, email, orgName, jobTitle, groupName);
    }

    private void setPropsToTask(Task task, String name, Serializable id, Serializable email, Serializable orgName, Serializable jobTitle, Serializable groupName) {
        @SuppressWarnings("unchecked")
        List<String> orgStructUnit = (List<String>) orgName;

        task.setOwnerName(name);
        task.setOwnerId((String) id);
        task.setOwnerEmail((String) email);
        task.setOwnerGroup((String) groupName);
        task.setOwnerOrgStructUnitProp(orgStructUnit);
        task.setOwnerJobTitle((String) jobTitle);
    }

    /**
     * Override in child classes.
     */
    protected void updateFullAccess() {
        fullAccess = true;
    }

    private void ensureResponsibleTaskExists() {
        class TaskInfHolder {
            AssignmentWorkflow assignmentWorkflow;
            boolean hasRespTask;
            boolean hasNonRespTask;

            public TaskInfHolder(AssignmentWorkflow assignmentWorkflow) {
                this.assignmentWorkflow = assignmentWorkflow;
            }
        }

        boolean respTaskInSomeBlock = false;
        int i = -1;

        List<TaskInfHolder> wfThatNeedTask = new ArrayList<TaskInfHolder>();
        for (Workflow block : compoundWorkflow.getWorkflows()) {
            i++;
            String blockStatus = block.getStatus();
            if (Status.NEW.equals(blockStatus)) {
                QName blockType = block.getNode().getType();
                if (blockType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW)) {
                    final TaskInfHolder inf = new TaskInfHolder((AssignmentWorkflow) block);
                    for (Task task : block.getTasks()) {
                        if (WorkflowUtil.isActiveResponsible(task)) {
                            inf.hasRespTask = true;
                            respTaskInSomeBlock = true;
                        } else if (!task.getNode().hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE)) {
                            inf.hasNonRespTask = true;
                        }
                    }
                    wfThatNeedTask.add(inf);
                } else {
                    if (block.getTasks().size() == 0 && !blockType.equals(WorkflowSpecificModel.Types.DOC_REGISTRATION_WORKFLOW)) {
                        if (block.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW)) {
                            ((OrderAssignmentWorkflow) block).addResponsibleTask();
                        } else {
                            block.addTask();
                        }
                    }
                }
            }
        }
        if (wfThatNeedTask.size() > 0) {
            boolean docHasRespTask = respTaskInSomeBlock || ((this instanceof CompoundWorkflowDialog) ?
                    isActiveResponsibleAssignedForDocument(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW, false) : false);
            for (TaskInfHolder infHolder : wfThatNeedTask) {
                final AssignmentWorkflow assignmentWorkflow = infHolder.assignmentWorkflow;
                if (!docHasRespTask) {
                    assignmentWorkflow.addResponsibleTask();
                    docHasRespTask = true;
                } else {
                    if (!infHolder.hasRespTask && !infHolder.hasNonRespTask) {
                        assignmentWorkflow.addTask();
                    }
                }
            }
        }
    }

    protected boolean isActiveResponsibleAssignedForDocument(QName workflowType, boolean allowFinished) {
        try {
            return 0 < getWorkflowService().getConnectedActiveResponsibleTasksCount(compoundWorkflow, workflowType, allowFinished,
                    allowFinished ? compoundWorkflow.getNodeRef() : null);
        } catch (InvalidNodeRefException e) {
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addErrorMessage(context, "workflow_compound_add_block_error_docDeleted");
            throw e;
        }
    }

    /** Actions performed on both compoundWorkflow and compundWorkflowDefinition object */
    protected void preprocessWorkflow() {
        WorkflowUtil.removeEmptyTasks(compoundWorkflow);
        WorkflowUtil.setGroupTasksDueDates(compoundWorkflow, getTaskGroups());
    }

    /** Actions performed on compundWorkflowDefinition object only (i.e. not on compoundWorkflow object) */
    private void preprocessWorkflowDefinition() {
        if (!compoundWorkflow.isDocumentWorkflow()) {
            compoundWorkflow.setProp(WorkflowCommonModel.Props.DOCUMENT_TYPES, null);
        }
        if (!compoundWorkflow.isCaseFileWorkflow()) {
            compoundWorkflow.setProp(WorkflowCommonModel.Props.CASE_FILE_TYPES, null);
        }
        if (compoundWorkflow.isDocumentWorkflow()) {
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                if (workflow.isType(WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW)) {
                    workflow.setSigningType(null);
                }
            }
        }
    }

    public List<Map<String, List<TaskGroup>>> getTaskGroups() {
        if (taskGroups == null) {
            taskGroups = new ArrayList<Map<String, List<TaskGroup>>>();
        }
        return taskGroups;
    }

    public void setTaskGroups(List<Map<String, List<TaskGroup>>> taskGroups) {
        this.taskGroups = taskGroups;
    }

}
