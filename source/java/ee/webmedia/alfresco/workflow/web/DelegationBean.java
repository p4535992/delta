package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentSearchService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getParametersService;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.TASK_INDEX;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.markAsGeneratedByDelegation;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlInputText;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIGenericPicker;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDate;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.UserContactGroupSearchBean;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageDataWrapper;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;
import ee.webmedia.alfresco.utils.UnableToPerformMultiReasonException;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Task.Action;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import ee.webmedia.alfresco.workflow.web.DelegationTaskListGenerator.DelegatableTaskType;

/**
 * Bean that helps to create controls and manage state related to delegating assignment task
 */
public class DelegationBean implements Serializable {
    private static final String DELEGATION_CONFIRMATION_RENDERED = "delegationConfirmationRendered";
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DelegationBean.class);
    public static final String BEAN_NAME = "DelegationBean";

    /** Index of the assignment task, that could be delegated */
    public static final String ATTRIB_DELEGATABLE_TASK_INDEX = "delegatableTaskIndex";
    public static final String ATTRIB_SINGLE_TASK_TYPE_DELEGATION = "singleTaskTypedDelegation";

    public static final String ATTRIB_GROUP_ID = "groupId";
    public static final String ATTRIB_WF_INDEX = "wfIndex";

    /** if this property added to assignmentTask, then web-client will generate responsible assignment task delegation */
    private static final String TMP_GENERATE_RESPONSIBLE_ASSIGNMENT_TASK_DELEGATION = "{temp}delegateAsResponsibleAssignmentTask";
    /** if this property added to assignmentTask, then web-client will generate opinion task delegation */
    private static final String TMP_GENERATE_OPINION_TASK_DELEGATION = "{temp}delegateAsOpinionTask";
    public static final String DELEGATE = BEAN_NAME + ".delegate";

    private transient NodeService nodeService;
    private transient WorkflowService workflowService;
    private transient UserService userService;
    private WorkflowBlockBean workflowBlockBean;
    private final List<Task> delegatableTasks = new ArrayList<>();
    private List<SelectItem> confirmationMessages;
    // used to keep reference to delegatable task during delegation confirming
    private Task initialTask;
    private final Set<Task> tasksCreatedByDelegation = new HashSet<>();
    private Boolean delegateAndFinishTask;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    private static final Set<DelegatableTaskType> SINGLY_DELEGATABLE_TASK_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            DelegatableTaskType.REVIEW,
            DelegatableTaskType.INFORMATION,
            DelegatableTaskType.OPINION)));

    /**
     * @param event passed to MethodBinding
     */
    public void addDelegationTask(ActionEvent event) {
        Integer taskIndex = null;
        if (ActionUtil.hasParam(event, TASK_INDEX)) {
            taskIndex = Integer.parseInt(ActionUtil.getParam(event, TASK_INDEX));
        }
        DelegatableTaskType delegateTaskType = DelegatableTaskType.valueOf(ActionUtil.getParam(event, DelegationTaskListGenerator.ATTRIB_DELEGATE_TASK_TYPE));
        Workflow workflowForNewTask = getWorkflowByAction(event);
        String defaultResolution = null;
        Date dueDate = null;
        if (ActionUtil.hasParam(event, ATTRIB_DELEGATABLE_TASK_INDEX)) {
            int originalTaskIndex = ActionUtil.getParam(event, ATTRIB_DELEGATABLE_TASK_INDEX, Integer.class);
            Task delegateableTask = delegatableTasks.get(originalTaskIndex);
            defaultResolution = delegateableTask.getResolutionOfTask();
            dueDate = delegateableTask.getDueDate();
        }
        addDelegationTask(delegateTaskType.isResponsibleTask(), workflowForNewTask, taskIndex, defaultResolution, dueDate);
        updatePanelGroup("addDelegationTask");
    }

    public void addDelegationTask(Workflow originalTaskWorkflow, Integer taskIndex, String resolution, Date dueDate, String owner, int filterIndex, DelegatableTaskType dTaskType) {
        Workflow workflow = getOrCreateWorkflow(originalTaskWorkflow, dTaskType);
        addDelegationTask(dTaskType.isResponsibleTask(), workflow, taskIndex, resolution, dueDate);
        addOwners(filterIndex, taskIndex, workflow, owner);
    }

    public void addDelegationTaskToOriginalWorkflow(Workflow originalTaskWorkflow, Integer taskIndex, String resolution, Date dueDate, String owner, int filterIndex) {
        addDelegationTask(false, originalTaskWorkflow, taskIndex, resolution, dueDate, true);
        addOwners(filterIndex, taskIndex, originalTaskWorkflow, owner);
    }

    private void addDelegationTask(boolean hasResponsibleAspect, Workflow workflow, Integer taskIndex, String defaultResolution, Date dueDate) {
        addDelegationTask(hasResponsibleAspect, workflow, taskIndex, defaultResolution, dueDate, false);
    }

    private void addDelegationTask(boolean hasResponsibleAspect, Workflow workflow, Integer taskIndex, String resolution, Date dueDate, boolean forceAddResolution) {
        Task task = taskIndex != null ? workflow.addTask(taskIndex) : workflow.addTask();
        markAsGeneratedByDelegation(task);
        task.setParallel(true);
        if (hasResponsibleAspect) {
            task.setResponsible(true);
            task.setActive(true);
        }
        if (forceAddResolution || task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK, WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK)) {
            task.setResolution(resolution);
        }
        task.setDueDate(dueDate);
    }

    private void updatePanelGroup(String action) {
        workflowBlockBean.constructTaskPanelGroup(action);
    }

    public void updatePanelGroup() {
        workflowBlockBean.constructTaskPanelGroup("updateGeneric");
    }

    public void removeDelegationTask(ActionEvent event) {
        Workflow workflow = getWorkflowByAction(event);
        int taskIndex = ActionUtil.getParam(event, TASK_INDEX, Integer.class);
        workflow.removeTask(taskIndex);
        updatePanelGroup("removeDelegationTask");
    }

    public void removeDelegationGroup(ActionEvent event) {
        Workflow workflow = getWorkflowByAction(event);
        String groupId = ActionUtil.getParam(event, ATTRIB_GROUP_ID);
        TaskGroup group = getTaskGroups().getByGroupId().get(groupId);
        getTaskGroups().removeGroup(workflow.getIndexInCompoundWorkflow(), groupId);

        List<Task> tasksToRemove = getTasksInGroup(workflow, group);
        workflow.removeTasks(tasksToRemove);
        updatePanelGroup();
    }

    public void calculateTaskGroupDueDate(ActionEvent event) {
        Workflow workflow = getWorkflowByAction(event);
        String groupId = ActionUtil.getParam(event, ATTRIB_GROUP_ID);

        String datePickerId = ActionUtil.getParam(event, DelegationTaskListGenerator.ATTRIB_GROUP_DATE_PICKER_ID);
        UIComponent datePicker = ComponentUtil.findComponentById(FacesContext.getCurrentInstance(), event.getComponent().getParent(), datePickerId);
        Date dueDate = (Date) ((HtmlInputText) datePicker).getValue();

        if (dueDate == null) {
            return;
        }
        TaskGroup group = getTaskGroups().getByGroupId().get(groupId);
        group.setDueDate(dueDate);
        List<Task> groupTasks = getTasksInGroup(workflow, group);
        for (Task t : groupTasks) {
            t.setDueDate(dueDate);
        }
    }

    private List<Task> getTasksInGroup(Workflow workflow, TaskGroup group) {
        Set<Integer> taskIds = group.getTaskIds();
        if (CollectionUtils.isEmpty(taskIds)) {
            return Collections.emptyList();
        }
        List<Task> wfTasks = workflow.getTasks();
        List<Task> tasksInGroup = new ArrayList<>();
        for (int i = 0; i < wfTasks.size(); i++) {
            if (taskIds.contains(i) && WorkflowUtil.isGeneratedByDelegation(wfTasks.get(i))) {
                tasksInGroup.add(wfTasks.get(i));
            }
        }
        return tasksInGroup;
    }

    public void resetDelegationTask(ActionEvent event) {
        Task task = getTaskByActionParams(event);
        Map<String, Object> props = task.getNode().getProperties();
        props.put(WorkflowCommonModel.Props.OWNER_NAME.toString(), null);
        props.put(WorkflowSpecificModel.Props.RESOLUTION.toString(), null);
        props.put(WorkflowSpecificModel.Props.DUE_DATE.toString(), null);
    }

    /**
     * @param task
     * @return pair(delegatableTaskIndex, delegatableTask). delegatableTask == assignmentTask when task with the same noderef hasn't been added yet
     */
    public Pair<Integer, Task> initDelegatableTask(Task task) {
        NodeRef delegatableTaskRef = task.getNodeRef();
        int delegatableTaskIndex = 0;
        for (Task t : delegatableTasks) {
            if (delegatableTaskRef.equals(t.getNodeRef())) {
                // don't add new delegatable task if this is yet another clone of existing task or this method was called after update.
                return new Pair<>(delegatableTaskIndex, t);
            }
            delegatableTaskIndex++;
        }
        DelegatableTaskType dTaskType = DelegatableTaskType.getTypeByTask(task);
        delegatableTasks.add(task);
        delegatableTaskIndex = delegatableTasks.size() - 1;
        Workflow workflow = task.getParent();

        if (dTaskType.isOrderAssignmentOrAssignmentWorkflow()) {
            createOpininAndInformationWorkflows(task, workflow);
        } else {
            getOrCreateWorkflow(workflow, dTaskType, true);
        }
        return new Pair<>(delegatableTaskIndex, task);
    }

    private void createOpininAndInformationWorkflows(Task task, Workflow workflow) {
        String resolutionOfTask = task.getResolutionOfTask();
        if (task.isResponsible()) {
            task.getNode().getProperties().put(TMP_GENERATE_RESPONSIBLE_ASSIGNMENT_TASK_DELEGATION, Boolean.TRUE);
            addDelegationTask(true, workflow, null, resolutionOfTask, task.getDueDate());
        } else {
            addDelegationTask(false, workflow, null, resolutionOfTask, task.getDueDate());
        }
        // create information and opinion workflows under the compoundWorkflow of the assignment task in case user adds corresponding task.
        // If no tasks are added to following workflow, then that workflows is not saved when saving compound workflow
        NodeRef docRef = task.getParent().getParent().getParent();
        String docStatus = (String) getNodeService().getProperty(docRef, DocumentCommonModel.Props.DOC_STATUS);
        if (!DocumentStatus.FINISHED.getValueName().equals(docStatus)) {
            Node taskNode = task.getNode();
            taskNode.getProperties().put(TMP_GENERATE_OPINION_TASK_DELEGATION, Boolean.TRUE);
            getOrCreateWorkflow(workflow, DelegatableTaskType.OPINION);
        }
        getOrCreateWorkflow(workflow, DelegatableTaskType.INFORMATION);
    }

    public void reset() {
        delegatableTasks.clear();
        confirmationMessages = null;
        initialTask = null;
        delegateAndFinishTask = null;
        tasksCreatedByDelegation.clear();
        taskGroups = null;
    }

    private Workflow getWorkflowByAction(ActionEvent event) {
        UIComponent component = event.getComponent();
        DelegatableTaskType dTaskType;
        Workflow originalTaskWorkflow;
        if (component instanceof UIActionLink && ActionUtil.hasParam(event, ATTRIB_DELEGATABLE_TASK_INDEX)) {
            originalTaskWorkflow = getWorkflowByOriginalTask(ActionUtil.getParam(event, ATTRIB_DELEGATABLE_TASK_INDEX, Integer.class));
            dTaskType = DelegatableTaskType.valueOf(ActionUtil.getParam(event, DelegationTaskListGenerator.ATTRIB_DELEGATE_TASK_TYPE));
        } else { // when component is for example picker
            Map<String, Object> attributes = ComponentUtil.getAttributes(component);
            int delegatableTaskIndex = (Integer) attributes.get(ATTRIB_DELEGATABLE_TASK_INDEX);
            originalTaskWorkflow = getWorkflowByOriginalTask(delegatableTaskIndex);
            dTaskType = (DelegatableTaskType) attributes.get(DelegationTaskListGenerator.ATTRIB_DELEGATE_TASK_TYPE);
        }
        boolean singleTaskType = BooleanUtils.toBoolean(ActionUtil.getEventParamOrAttirbuteValue(event, ATTRIB_SINGLE_TASK_TYPE_DELEGATION, Boolean.class));
        return getOrCreateWorkflow(originalTaskWorkflow, dTaskType, singleTaskType);
    }

    private Workflow getOrCreateWorkflow(Workflow originalTaskWorkflow, DelegatableTaskType dTaskType, boolean isSingleTaskTypeDelegation) {
        if (isAddTasksToOriginalWorkflow(dTaskType, isSingleTaskTypeDelegation)) {
            return originalTaskWorkflow;
        }
        CompoundWorkflow compoundWorkflow = originalTaskWorkflow.getParent();
        QName workflowTypeQName = dTaskType.getWorkflowTypeQName();
        int lastInprogressWfIndex = 0;
        int i = 0;
        for (Workflow otherWorkflow : compoundWorkflow.getWorkflows()) {
            boolean isGeneratedByDelegation = WorkflowUtil.isGeneratedByDelegation(otherWorkflow);
            if (isGeneratedByDelegation) {
                if (workflowTypeQName.equals(otherWorkflow.getType())) {
                    return otherWorkflow;
                }
            }
            if (WorkflowUtil.isStatus(otherWorkflow, Status.IN_PROGRESS)) {
                lastInprogressWfIndex = i;
            }
            i++;
        }
        Workflow newWorkflow = getWorkflowService().addNewWorkflow(compoundWorkflow, workflowTypeQName, lastInprogressWfIndex + 1, false);
        markAsGeneratedByDelegation(newWorkflow);
        return newWorkflow;
    }

    private Workflow getOrCreateWorkflow(Workflow originalTaskWorkflow, DelegatableTaskType dTaskType) {
        return getOrCreateWorkflow(originalTaskWorkflow, dTaskType, false);
    }

    private boolean isAddTasksToOriginalWorkflow(DelegatableTaskType dTaskType, boolean isSingleTaskTypeDelegation) {
        return dTaskType.isOrderAssignmentOrAssignmentWorkflow()
                || isSingleTaskTypeDelegation && SINGLY_DELEGATABLE_TASK_TYPES.contains(dTaskType);
    }

    private Task getTaskByActionParams(ActionEvent event) {
        Workflow workflow = getWorkflowByAction(event);
        int taskIndex = ActionUtil.getParam(event, TASK_INDEX, Integer.class);
        return workflow.getTasks().get(taskIndex);
    }

    private Workflow getWorkflowByOriginalTask(int delegatableTaskIndex) {
        return delegatableTasks.get(delegatableTaskIndex).getParent();
    }

    /**
     * If <code>!dTaskType.isAssignmentWorkflow()</code> then result should be equivalent to
     * <code>getNewWorkflowTasksFetchers().get(delegatableTaskIndex).getNonAssignmentTasksByType().get(dTaskType.name());</code><br>
     * that is slower, but used for value binding of non-assignment tasks.
     *
     * @param delegatableTaskIndex
     * @param dTaskType
     * @return tasks of type <code>dTaskType</code> that should be displayed for delegating when showing assigment task <code>delegatableTaskIndex</code>
     */
    public List<Task> getTasks(int delegatableTaskIndex, DelegatableTaskType dTaskType, boolean isSingleTaskDelegation) {
        Workflow workflow = getWorkflowByOriginalTask(delegatableTaskIndex);
        List<Task> tasks;
        if (isAddTasksToOriginalWorkflow(dTaskType, isSingleTaskDelegation)) {
            tasks = workflow.getTasks();
        } else {
            tasks = getNonAssignmentTasks(dTaskType, workflow.getParent().getWorkflows());
        }
        return tasks;
    }

    /**
     * Used for JSF binding in {@link DelegationTaskListGenerator#setPickerBindings()}
     *
     * @throws Exception
     */
    public void delegate(ActionEvent event) throws Exception {
        delegate(event, null);
    }

    @SuppressWarnings("unchecked")
    void delegate(ActionEvent event, Integer taskIndex) {
        delegateAndFinishTask = taskIndex != null;
        Integer delegatableTaskIndex = delegateAndFinishTask ? taskIndex : ActionUtil.getEventParamOrAttirbuteValue(event, ATTRIB_DELEGATABLE_TASK_INDEX, Integer.class);
        List<Pair<String, Object>> params = new ArrayList<>();
        params.add(new Pair<String, Object>(ATTRIB_DELEGATABLE_TASK_INDEX, delegatableTaskIndex));
        // Save all changes to independent workflow before updating task.
        if (!workflowBlockBean.saveIfIndependentWorkflow(params, DELEGATE, event)) {
            return;
        }
        Pair<Boolean, Date> validationResult = prepareAndValidateDelegation(delegatableTaskIndex);
        if (validationResult.getFirst() && initialTask != null) {
            boolean isReviewOrOpinionTask = initialTask.isType(WorkflowSpecificModel.Types.REVIEW_TASK, WorkflowSpecificModel.Types.OPINION_TASK);
            if (delegateAndFinishTask || isReviewOrOpinionTask) {
                getAndPopulateConfirmationMessages(validationResult.getSecond(), isReviewOrOpinionTask);
            } else {
                populateConfirmationMessage();
            }
            if (CollectionUtils.isNotEmpty(confirmationMessages)) {
                FacesContext.getCurrentInstance().getExternalContext().getRequestMap().put(DELEGATION_CONFIRMATION_RENDERED, Boolean.TRUE);
                return;
            }
            setInitialTaskFinished();
            delegateTaskConfirmed();
        }
    }

    private Pair<Boolean, Date> prepareAndValidateDelegation(Integer taskIndex) {
        boolean temp = delegateAndFinishTask; // reloading workflow resets this value
        initialTask = reloadWorkflow(taskIndex);
        delegateAndFinishTask = temp;
        if (initialTask == null) {
            return Pair.newInstance(true, null);
        }
        boolean validateDueDates = delegateAndFinishTask || initialTask.isType(WorkflowSpecificModel.Types.REVIEW_TASK, WorkflowSpecificModel.Types.OPINION_TASK);
        Pair<Boolean, Date> validationResult = validate(initialTask.getParent().getParent(), validateDueDates);
        return validationResult;
    }

    private void getAndPopulateConfirmationMessages(Date maxTaskDueDate, boolean checkForPastDate) {
        List<String> messages = getConfirmationMessages(maxTaskDueDate, checkForPastDate);
        populateConfirmationMessages(messages);
    }

    private void populateConfirmationMessages(List<String> messages) {
        if (CollectionUtils.isNotEmpty(messages)) {
            confirmationMessages = new ArrayList<>();
            for (String message : messages) {
                confirmationMessages.add(new SelectItem(message));
            }
        }
    }

    private List<String> getConfirmationMessages(Date maxTaskDueDate, boolean checkForPastDate) {
        CompoundWorkflow compoundWorkflow = initialTask.getParent().getParent();
        CompoundWorkflowType type = compoundWorkflow.getTypeEnum();
        List<String> messages = new ArrayList<>();
        NodeRef cwfRef = compoundWorkflow.getNodeRef();
        if (checkForPastDate) {
            addDueDateInPastMessage(messages);
        }
        if (CompoundWorkflowType.INDEPENDENT_WORKFLOW.equals(type) && maxTaskDueDate != null) {
            List<NodeRef> docRefs = workflowService.getCompoundWorkflowDocumentRefs(cwfRef);
            if (CollectionUtils.isNotEmpty(docRefs)) {
                addDueDateConfirmation(messages, maxTaskDueDate, docRefs, initialTask.getParent().getType().getLocalName(), true);
            }
        } else if (CompoundWorkflowType.DOCUMENT_WORKFLOW.equals(type)) {
            NodeRef docRef = getNodeService().getPrimaryParent(cwfRef).getParentRef();
            if (maxTaskDueDate != null) {
                addDueDateConfirmation(messages, maxTaskDueDate, Collections.singletonList(docRef), initialTask.getParent().getType().getLocalName(), false);
            }

            Set<String> taskOwners = new HashSet<>();
            for (Task task : tasksCreatedByDelegation) {
                taskOwners.add(task.getOwnerId());
            }
            addDuplicateTaskMessage(messages, docRef, initialTask.getType(), taskOwners);
        }
        return messages;
    }

    private void addDueDateInPastMessage(List<String> messages) {
        Date now = new Date();
        for (Task task : tasksCreatedByDelegation) {
            Date dueDate = task.getDueDate();
            if (dueDate != null && now.after(dueDate)) {
                messages.add(MessageUtil.getMessage("task_confirm_due_date_in_past"));
                break;
            }
        }
    }

    public static void addDuplicateTaskMessage(List<String> messages, NodeRef docRef, QName taskType, Set<String> addedOwnerIds) {
        List<Map<QName, Serializable>> existingTasks = BeanHelper.getWorkflowService()
                .getDocumentCompoundWorkflowTaskOwnerNamesAndIds(docRef, Collections.singleton(taskType), Status.getAllExept(Status.UNFINISHED));

        for (Map<QName, Serializable> task : existingTasks) {
            String ownerId = (String) task.get(WorkflowCommonModel.Props.OWNER_ID);
            if (addedOwnerIds.contains(ownerId)) {
                String ownerName = (String) task.get(WorkflowCommonModel.Props.OWNER_NAME);
                String msg = MessageUtil.getMessage("workflow_compound_confirm_same_task", ownerName, MessageUtil.getMessage("task_title_" + taskType.getLocalName()));
                messages.add(msg + " " + MessageUtil.getMessage("workflow_compound_confirm_continue"));
            }
        }
    }

    /**
     * This method assumes that compound workflow has been validated and initialTask has been set correctly
     * by delegate method
     */
    public void delegateConfirmed(@SuppressWarnings("unused") ActionEvent event) {
        setInitialTaskFinished();
        delegateTaskConfirmed();
    }

    private void setInitialTaskFinished() {
        if (BooleanUtils.toBoolean(delegateAndFinishTask)) {
            initialTask.setAction(Action.FINISH);
        } else if (initialTask.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK, WorkflowSpecificModel.Types.REVIEW_TASK, WorkflowSpecificModel.Types.OPINION_TASK)) {
            initialTask.setAction(Action.FINISH);
        }
    }

    private void delegateTaskConfirmed() {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            CompoundWorkflow newCWF = getWorkflowService().delegate(initialTask);
            QName type = initialTask.getType();
            String taskType = type.getLocalName();
            workflowBlockBean.refreshCompoundWorkflowAndRestore(newCWF, "delegate");
            workflowBlockBean.notifyDialogsIfNeeded();
            if (WorkflowSpecificModel.Types.INFORMATION_TASK.equals(type)) {
                MessageUtil.addInfoMessage("task_finish_success_defaultMsg");
            }
            MessageUtil.addInfoMessage("delegated_successfully_" + taskType);
        } catch (UnableToPerformMultiReasonException e) {
            MessageUtil.addStatusMessages(context, e.getMessageDataWrapper());
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(context, e);
        } catch (NodeLockedException e) {
            LOG.debug("Compound workflow action failed: document locked!", e);
            MessageUtil.addErrorMessage(context, "workflow_compound_save_failed_docLocked");
        } catch (WorkflowChangedException e) {
            CompoundWorkflowDialog.handleException(e, null);
        } catch (Exception e) {
            LOG.error("Compound workflow action failed!", e);
            MessageUtil.addErrorMessage(context, "workflow_compound_save_failed_general");
        }
    }

    public boolean delegate(Task task, boolean delegateAndFinish) {
        initialTask = task;
        delegateAndFinishTask = delegateAndFinish;
        if (!validate(initialTask.getParent().getParent(), delegateAndFinish).getFirst()) {
            return false;
        }
        try {
            setInitialTaskFinished();
            getWorkflowService().delegate(initialTask);
        } catch (WorkflowChangedException e) {
            return false;
        } catch (Exception e) {
            LOG.warn("Delegating task failed due to unknown reason", e);
            return false;
        }
        return true;
    }

    private void populateConfirmationMessage() {
        Workflow parentWorkflow = initialTask.getParent();
        if (!parentWorkflow.getParent().isDocumentWorkflow()) {
            confirmationMessages = null;
            return;
        }
        NodeRef docRef = parentWorkflow.getParent().getParent();
        Serializable documentDueDate = getNodeService().getProperty(docRef, DocumentSpecificModel.Props.DUE_DATE);
        if (!(documentDueDate instanceof Date)) {
            confirmationMessages = null;
            return;
        }
        List<String> messages = new ArrayList<String>();
        for (Workflow workflow : initialTask.getParent().getParent().getWorkflows()) {
            for (Task task : workflow.getTasks()) {
                if (!WorkflowUtil.isEmptyTask(task) && WorkflowUtil.isGeneratedByDelegation(task)) {
                    Date taskDueDate = task.getDueDate();
                    if (taskDueDate != null) {
                        WorkflowUtil.getDocmentDueDateMessage((Date) documentDueDate, messages, workflow, taskDueDate);
                    }
                }
            }
        }
        populateConfirmationMessages(messages);
    }

    public List<SelectItem> getConfirmationMessages() {
        return confirmationMessages;
    }

    public boolean isConfirmationRendered() {
        boolean isConfirmationRequest = Boolean.TRUE.equals(FacesContext.getCurrentInstance().getExternalContext().getRequestMap().get(DELEGATION_CONFIRMATION_RENDERED));
        return isConfirmationRequest && confirmationMessages != null && !confirmationMessages.isEmpty();
    }

    private Pair<Boolean, Date> validate(CompoundWorkflow compoundWorkflow, boolean validateDueDates) {
        MessageDataWrapper feedback = new MessageDataWrapper();
        boolean searchResponsibleTask = WorkflowUtil.isActiveResponsible(initialTask);
        boolean isAssignmentWorkflow = initialTask.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK);
        Date initialTaskDueDate = validateDueDates ? initialTask.getDueDate() : null;
        Task newMandatoryTask = null;
        boolean hasAtLeastOneDelegationTask = false;
        Date maxTaskDueDate = null;
        QName maxDueDatetaskType = null;
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            for (Task task : workflow.getTasks()) {
                if (!WorkflowUtil.isEmptyTask(task) && WorkflowUtil.isGeneratedByDelegation(task)) {
                    hasAtLeastOneDelegationTask = true;
                    delegationTaskMandatoryFieldsFilled(task, feedback, initialTaskDueDate);
                    Date dueDate = task.getDueDate();
                    if (dueDate != null) {
                        dueDate.setHours(23);
                        dueDate.setMinutes(59);
                        if (maxTaskDueDate == null || dueDate.after(maxTaskDueDate)) {
                            maxTaskDueDate = dueDate;
                            maxDueDatetaskType = task.getType();
                        }
                    }
                    if (isAssignmentWorkflow && workflow.isType(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW) && newMandatoryTask == null) {
                        if (!searchResponsibleTask) {
                            newMandatoryTask = task;
                        } else if (WorkflowUtil.isActiveResponsible(task)) {
                            newMandatoryTask = task;
                        }
                    }
                    if (validateDueDates) {
                        tasksCreatedByDelegation.add(task);
                    }
                }
            }
        }
        if (validateDueDates && maxTaskDueDate != null && initialTaskDueDate != null && maxTaskDueDate.after(initialTaskDueDate)) {
            feedback.addFeedbackItem(new MessageDataImpl(MessageSeverity.ERROR, "delegate_error_dueDate_after_original_task_dueDate_" + maxDueDatetaskType.getLocalName()));
        }
        if (isAssignmentWorkflow) {
            if (newMandatoryTask == null) {
                if (searchResponsibleTask) {
                    feedback.addFeedbackItem(new MessageDataImpl(MessageSeverity.ERROR, "delegate_error_noNewResponsibleTask"));
                } else {
                    feedback.addFeedbackItem(new MessageDataImpl(MessageSeverity.ERROR, "delegate_error_noNewTask"));
                }
            }
        } else if (!hasAtLeastOneDelegationTask) {
            if (initialTask != null && initialTask.isType(WorkflowSpecificModel.Types.OPINION_TASK)) {
                feedback.addFeedbackItem(new MessageDataImpl(MessageSeverity.ERROR, "delegate_error_noDelegationTask_opinionTask"));
            } else {
                feedback.addFeedbackItem(new MessageDataImpl(MessageSeverity.ERROR, "delegate_error_noDelegationTask"));
            }
        }
        if (feedback.hasErrors()) {
            MessageUtil.addStatusMessages(FacesContext.getCurrentInstance(), feedback);
            return Pair.newInstance(false, maxTaskDueDate);
        }
        return Pair.newInstance(true, maxTaskDueDate);
    }

    public static void addDueDateConfirmation(List<String> messages, Date maxTaskDueDate, List<NodeRef> docRefs, String workflowTypeName, boolean isIndependentWF) {
        Set<QName> propsToLoad = new HashSet<>(Arrays.asList(DocumentSpecificModel.Props.DUE_DATE, DocumentAdminModel.Props.OBJECT_TYPE_ID, DocumentCommonModel.Props.DOC_NAME));
        Collection<Node> documents = BeanHelper.getBulkLoadNodeService().loadNodes(docRefs, propsToLoad).values();
        LocalDate taskDueDate = LocalDate.fromDateFields(maxTaskDueDate);
        for (Node doc : documents) {
            Map<String, Object> props = doc.getProperties();
            if (SystematicDocumentType.INVOICE.isSameType((String) props.get(DocumentAdminModel.Props.OBJECT_TYPE_ID))) {
                continue;
            }
            Date docDueDate = (Date) props.get(DocumentSpecificModel.Props.DUE_DATE);
            if (docDueDate != null) {
                LocalDate docDue = LocalDate.fromDateFields(docDueDate);
                if (taskDueDate.isAfter(docDue)) {
                    String workflowType = MessageUtil.getMessage("workflow_" + workflowTypeName);
                    if (isIndependentWF) {
                        String docName = (String) props.get(DocumentCommonModel.Props.DOC_NAME);
                        messages.add(MessageUtil.getMessage("delegate_confirm_task_dueDate_after_workflow_document_dueDate",
                                workflowType, DATE_FORMAT.format(maxTaskDueDate), docName, DATE_FORMAT.format(docDueDate)));
                    } else {
                        messages.add(MessageUtil.getMessage("delegate_confirm_task_dueDate_after_document_dueDate",
                                workflowType, DATE_FORMAT.format(maxTaskDueDate), DATE_FORMAT.format(docDueDate)));
                    }
                }
            }
        }
    }

    private void delegationTaskMandatoryFieldsFilled(Task task, MessageDataWrapper feedback, Date initialDateDueDate) {
        boolean noOwner = StringUtils.isBlank(task.getOwnerName());
        QName taskType = task.getType();
        String key = "delegate_error_taskMandatory_" + taskType.getLocalName();
        if (taskType.equals(WorkflowSpecificModel.Types.INFORMATION_TASK)) {
            if (noOwner) {
                feedback.addFeedbackItem(new MessageDataImpl(MessageSeverity.ERROR, key));
            }
            if (initialDateDueDate != null && task.getDueDate() == null) {
                feedback.addFeedbackItem(new MessageDataImpl(MessageSeverity.ERROR, key + "_dueDate"));
            }
        } else if (noOwner || task.getDueDate() == null) {
            if (taskType.equals(WorkflowSpecificModel.Types.OPINION_TASK)) {
                feedback.addFeedbackItem(new MessageDataImpl(MessageSeverity.ERROR, key));
            } else {
                if (task.isResponsible()) {
                    key += "_responsible";
                }
                feedback.addFeedbackItem(new MessageDataImpl(MessageSeverity.ERROR, key));
            }

        }
    }

    public boolean hasTasksForDelegation(NodeRef taskRef) {
        if (CollectionUtils.isEmpty(delegatableTasks) || RepoUtil.isUnsaved(taskRef)) {
            return false;
        }
        Task task = null;
        for (Task t : delegatableTasks) {
            if (t.getNodeRef().equals(taskRef)) {
                task = t;
                break;
            }
        }
        if (task == null) {
            return false;
        }

        for (Task t : task.getParent().getTasks()) {
            if (WorkflowUtil.isGeneratedByDelegation(t) && !WorkflowUtil.isEmptyTask(t)) {
                return true;
            }
        }
        List<Workflow> workflows = task.getParent().getParent().getWorkflows();
        for (Workflow wf : workflows) {
            if (WorkflowUtil.isGeneratedByDelegation(wf) && CollectionUtils.isNotEmpty(wf.getTasks())) {
                return true;
            }
        }
        return false;
    }

    // reload workflow and inject delegation data
    private Task reloadWorkflow(Integer index) {
        // get task that has not been saved
        Task task = delegatableTasks.get(index);
        CompoundWorkflow unsavedCompoundWorkflow = task.getParent().getParent();
        if (!unsavedCompoundWorkflow.isIndependentWorkflow()) {
            return task;
        }

        if (unsavedCompoundWorkflow.isIndependentWorkflow() && task.isSaved()) {
            List<Task> tasksCreatedByDelegation = new ArrayList<>();
            List<Workflow> workflowsCreatedByDelegation = new ArrayList<>();

            // For performance reasons, preserve only needed properties, not whole workflow hierarchy
            Map<String, Object> changedProps = getWorkflowService().getTaskChangedProperties(task);
            NodeRef taskRef = task.getNodeRef();
            Status status = Status.of(task.getStatus());
            for (Task workflowTask : task.getParent().getTasks()) {
                if (WorkflowUtil.isGeneratedByDelegation(workflowTask)) {
                    tasksCreatedByDelegation.add(workflowTask);
                }
            }
            NodeRef lastInProgressWorkflow = null;
            for (Workflow workflow : unsavedCompoundWorkflow.getWorkflows()) {
                if (workflow.isStatus(Status.IN_PROGRESS)) {
                    lastInProgressWorkflow = workflow.getNodeRef();
                }
                else if (WorkflowUtil.isGeneratedByDelegation(workflow)) {
                    workflowsCreatedByDelegation.add(workflow);
                }
            }
            task = null;

            // init this bean with compoundWorkflowDialog data
            BeanHelper.getCompoundWorkflowDialog().initWorkflowBlockBean();

            Task updatedTask = null;
            for (Task updated : delegatableTasks) {
                if (taskRef.equals(updated.getNodeRef())) {
                    updatedTask = updated;
                    break;
                }
            }
            if (updatedTask != null) {
                if (!updatedTask.isStatus(status)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.error("Task status has changed during saving compound workflow, finishing task is not possible. Original status=" + status +
                                ", saved task=\n" + task);
                    }
                    MessageUtil.addErrorMessage("workflow_task_finish_failed");
                } else {
                    task = updatedTask;
                    CompoundWorkflow updatedCompoundWorkflow = task.getParent().getParent();
                    task.getNode().getProperties().putAll(changedProps);

                    WorkflowUtil.removeEmptyTasks(updatedCompoundWorkflow);
                    WorkflowUtil.removeTasksGeneratedByDelegation(updatedCompoundWorkflow);
                    WorkflowUtil.removeEmptyWorkflowsGeneratedByDelegation(updatedCompoundWorkflow);

                    int indexToInsertWorkflows = 0;
                    if (lastInProgressWorkflow != null) {
                        for (Workflow workflow : updatedCompoundWorkflow.getWorkflows()) {
                            indexToInsertWorkflows++;
                            if (workflow.getNodeRef().equals(lastInProgressWorkflow)) {
                                break;
                            }
                        }
                    }

                    workflowService.injectWorkflows(updatedCompoundWorkflow, indexToInsertWorkflows, workflowsCreatedByDelegation);
                    List<Task> workflowTasks = task.getParent().getTasks();
                    int indexOfTaskInsert = workflowTasks.indexOf(task) + 1;
                    workflowService.injectTasks(task.getParent(), indexOfTaskInsert, tasksCreatedByDelegation);
                }
            } else {
                // should never actually happen
                if (LOG.isDebugEnabled()) {
                    LOG.error("Task with nodeRef=" + taskRef + " not found after saving compound workflow with nodeRef=" + unsavedCompoundWorkflow.getNodeRef());
                }
                MessageUtil.addErrorMessage("workflow_task_finish_failed");
            }
        }
        return task;
    }

    /** Used for JSF binding in {@link DelegationTaskListGenerator#setPickerBindings()} */
    public void processResponsibleOwnerSearchResults(ActionEvent event) {
        UIGenericPicker picker = (UIGenericPicker) event.getComponent();
        int filterIndex = picker.getFilterIndex();
        if (filterIndex == UserContactGroupSearchBean.USER_GROUPS_FILTER) {
            filterIndex = UserContactGroupSearchBean.CONTACTS_FILTER;
        }
        processOwnerSearchResults(event, filterIndex);
    }

    /** Used for JSF binding in {@link DelegationTaskListGenerator#setPickerBindings()} */
    public void processOwnerSearchResults(ActionEvent event) {
        UIGenericPicker picker = (UIGenericPicker) event.getComponent();
        int filterIndex = picker.getFilterIndex();
        processOwnerSearchResults(event, filterIndex);
    }

    /** Used for JSF binding in {@link DelegationTaskListGenerator#createTaskPropValueBinding()} */
    public List<Task> getDelegatableTasks() {
        return delegatableTasks;
    }

    /**
     * Used for value binding in {@link DelegationTaskListGenerator#createTaskPropValueBinding()} <br>
     * (key must be Long, as numeric map keys are converted to Long when resolving ValueBinding)
     */
    public Map<Long/* delegatableTaskIndex */, NewWorkflowTasksFetcher> getNewWorkflowTasksFetchers() {
        Map<Long, DelegationBean.NewWorkflowTasksFetcher> wfWrappersByDelegatableTaskIndex = new LinkedHashMap<Long, DelegationBean.NewWorkflowTasksFetcher>();
        for (long delegatableTaskIndex = 0; delegatableTaskIndex < delegatableTasks.size(); delegatableTaskIndex++) {
            wfWrappersByDelegatableTaskIndex.put(delegatableTaskIndex, new NewWorkflowTasksFetcher((int) delegatableTaskIndex));
        }
        return wfWrappersByDelegatableTaskIndex;
    }

    public class NewWorkflowTasksFetcher {
        private final int delegatableTaskIndex;

        public NewWorkflowTasksFetcher(int delegatableTaskIndex) {
            this.delegatableTaskIndex = delegatableTaskIndex;
        }

        /**
         * Used for value binding in {@link DelegationTaskListGenerator#createTaskPropValueBinding()} <br>
         * (key must be Long, as numeric map keys are converted to Long when resolving ValueBinding)
         */
        public Map<String/* delegatableTaskType.name() */, List<Task>> getNonAssignmentTasksByType() {
            List<Workflow> workflows = getWorkflow().getParent().getWorkflows();
            HashMap<String, List<Task>> results = new HashMap<String, List<Task>>();
            results.put(DelegatableTaskType.INFORMATION.name(), getNonAssignmentTasks(DelegatableTaskType.INFORMATION, workflows));
            results.put(DelegatableTaskType.OPINION.name(), getNonAssignmentTasks(DelegatableTaskType.OPINION, workflows));
            return results;
        }

        public Map<String/* delegatableTaskType.name() */, Workflow> getNonAssignmentWorkflowsByType() {
            List<Workflow> workflows = getWorkflow().getParent().getWorkflows();
            HashMap<String, Workflow> results = new HashMap<String, Workflow>();
            for (Workflow otherWorkflow : workflows) {
                if (WorkflowUtil.isGeneratedByDelegation(otherWorkflow)) {
                    if (DelegatableTaskType.INFORMATION.getWorkflowTypeQName().equals(otherWorkflow.getType())) {
                        results.put(DelegatableTaskType.INFORMATION.name(), otherWorkflow);
                    } else if (DelegatableTaskType.OPINION.getWorkflowTypeQName().equals(otherWorkflow.getType())) {
                        results.put(DelegatableTaskType.OPINION.name(), otherWorkflow);
                    }
                }
            }
            return results;
        }

        /**
         * Used for value binding in {@link DelegationTaskListGenerator#createWorkflowPropValueBinding(DelegatableTaskType, int, QName, javax.faces.application.Application)}
         */
        private Workflow getWorkflow() {
            return getWorkflowByOriginalTask(delegatableTaskIndex);
        }
    }

    private List<Task> getNonAssignmentTasks(DelegatableTaskType dTaskType, List<Workflow> workflows) {
        for (Workflow otherWorkflow : workflows) {
            if (WorkflowUtil.isGeneratedByDelegation(otherWorkflow) && dTaskType.getWorkflowTypeQName().equals(otherWorkflow.getType())) {
                return otherWorkflow.getTasks();
            }
        }
        return Collections.<Task> emptyList();
    }

    private TaskGroupHolder taskGroups;

    public TaskGroupHolder getTaskGroups() {
        if (taskGroups == null) {
            taskGroups = new TaskGroupHolder();
        }
        return taskGroups;
    }

    public void updateTaskListPageSize(ValueChangeEvent event) {
        String pageSize = (String) event.getNewValue();
        if (StringUtils.isBlank(pageSize) || !StringUtils.isNumeric(pageSize)) {
            return;
        }
        int size = Integer.parseInt(pageSize);
        if (size >= 0) {
            BeanHelper.getBrowseBean().setPageSizeContent(size);
            updatePanelGroup();
        }
    }

    private void processOwnerSearchResults(ActionEvent event, int filterIndex) {
        UIGenericPicker picker = (UIGenericPicker) event.getComponent();

        Map attributes = picker.getAttributes();
        int taskIndex = Integer.parseInt((String) attributes.get(Search.OPEN_DIALOG_KEY));
        String[] results = picker.getSelectedResults();
        if (results == null) {
            return;
        }
        Workflow workflow = getWorkflowByAction(event);
        for (String result : results) {
            taskIndex = addOwners(filterIndex, taskIndex, workflow, result);
        }
        updatePanelGroup("processOwnerSearchResults");
    }

    private int addOwners(int filterIndex, int taskIndex, Workflow workflow, String result) {
        // users
        if (filterIndex == UserContactGroupSearchBean.USERS_FILTER) {
            setPersonPropsToTask(workflow, taskIndex, result, null);
        }
        // user groups
        else if (filterIndex == UserContactGroupSearchBean.USER_GROUPS_FILTER) {
            Set<String> children = UserUtil.getUsersInGroup(result, getNodeService(), getUserService(), getParametersService(), getDocumentSearchService());
            String groupName = BeanHelper.getAuthorityService().getAuthorityDisplayName(result);
            int j = 0;
            Task task = workflow.getTasks().get(taskIndex);
            DelegatableTaskType delegateTaskType = DelegatableTaskType.getTypeByTask(task);
            String resolution = task.getResolution();
            Date dueDate = task.getDueDate();
            for (String userName : children) {
                if (j > 0) {
                    addDelegationTask(delegateTaskType.isResponsibleTask(), workflow, ++taskIndex, resolution, dueDate);
                }
                setPersonPropsToTask(workflow, taskIndex, userName, groupName);
                j++;
            }
        }
        // contacts
        else if (filterIndex == UserContactGroupSearchBean.CONTACTS_FILTER) {
            setContactPropsToTask(workflow, taskIndex, new NodeRef(result), null);
        }
        // contact groups
        else if (filterIndex == UserContactGroupSearchBean.CONTACT_GROUPS_FILTER) {
            NodeRef contactGroupRef = new NodeRef(result);
            List<NodeRef> contacts = BeanHelper.getAddressbookService().getContactGroupContents(contactGroupRef);
            String groupName = (String) getNodeService().getProperty(contactGroupRef, AddressbookModel.Props.GROUP_NAME);
            taskIndex = addContactGroupTasks(taskIndex, workflow, contacts, groupName);
        } else {
            throw new RuntimeException("Unknown filter index value: " + filterIndex);
        }
        return taskIndex;
    }

    private void setPersonPropsToTask(Workflow workflow, int taskIndex, String userName, String groupName) {
        Map<QName, Serializable> resultProps = getUserService().getUserProperties(userName);
        String name = UserUtil.getPersonFullName1(resultProps);
        Serializable id = resultProps.get(ContentModel.PROP_USERNAME);
        Serializable email = resultProps.get(ContentModel.PROP_EMAIL);
        Serializable orgName = (Serializable) BeanHelper.getOrganizationStructureService().getOrganizationStructurePaths((String) resultProps.get(ContentModel.PROP_ORGID));
        Serializable jobTitle = resultProps.get(ContentModel.PROP_JOBTITLE);
        setPropsToTask(workflow, taskIndex, name, id, email, orgName, jobTitle, groupName);
    }

    private void setPropsToTask(Workflow workflow, int taskIndex, String name, Serializable id, Serializable email, Serializable orgName,
            Serializable jobTitle, String ownerGroup) {
        Task task = workflow.getTasks().get(taskIndex);
        task.setOwnerName(name);
        task.setOwnerId((String) id);
        task.setOwnerEmail((String) email);
        if (StringUtils.isNotBlank(ownerGroup)) {
            task.setOwnerGroup(ownerGroup);
        }
        Map<String, Object> props = task.getNode().getProperties();
        props.put(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME.toString(), orgName);
        props.put(WorkflowCommonModel.Props.OWNER_JOB_TITLE.toString(), jobTitle);
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

    public int addContactGroupTasks(int taskIndex, Workflow block, List<NodeRef> contacts, String groupName) {
        int taskCounter = 0;
        Task task = block.getTasks().get(taskIndex);
        Date dueDate = task.getDueDate();
        DelegatableTaskType delegateTaskType = DelegatableTaskType.getTypeByTask(task);
        String resolution = task.getResolution();
        for (int j = 0; j < contacts.size(); j++) {
            Map<QName, Serializable> contactProps = getNodeService().getProperties(contacts.get(j));
            if (getNodeService().hasAspect(contacts.get(j), AddressbookModel.Aspects.ORGANIZATION_PROPERTIES)
                    && Boolean.TRUE.equals(contactProps.get(AddressbookModel.Props.TASK_CAPABLE))) {
                if (taskCounter > 0) {
                    addDelegationTask(delegateTaskType.isResponsibleTask(), block, ++taskIndex, resolution, dueDate);
                }
                setContactPropsToTask(block, taskIndex, contacts.get(j), groupName);
                taskCounter++;
            }
        }
        return taskIndex;
    }

    private NodeService getNodeService() {
        if (nodeService == null) {
            nodeService = BeanHelper.getNodeService();
        }
        return nodeService;
    }

    private WorkflowService getWorkflowService() {
        if (workflowService == null) {
            workflowService = BeanHelper.getWorkflowService();
        }
        return workflowService;
    }

    private UserService getUserService() {
        if (userService == null) {
            userService = BeanHelper.getUserService();
        }
        return userService;
    }

    public void setWorkflowBlockBean(WorkflowBlockBean workflowBlockBean) {
        this.workflowBlockBean = workflowBlockBean;
    }

}
