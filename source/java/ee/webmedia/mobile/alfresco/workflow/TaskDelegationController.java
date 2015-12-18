package ee.webmedia.mobile.alfresco.workflow;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocLockService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.UserContactGroupSearchBean;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.web.DelegationBean;
import ee.webmedia.alfresco.workflow.web.DelegationTaskListGenerator.DelegatableTaskType;
import ee.webmedia.mobile.alfresco.workflow.model.DelegationMessage;
import ee.webmedia.mobile.alfresco.workflow.model.LockMessage;
import ee.webmedia.mobile.alfresco.workflow.model.TaskDelegationForm;
import ee.webmedia.mobile.alfresco.workflow.model.TaskDelegationForm.TaskElement;

@Controller
public class TaskDelegationController extends AbstractCompoundWorkflowController {

    private static final long serialVersionUID = 1L;

    private static final String DELEGATION_MAPPING = CompundWorkflowDetailsController.COMPOUND_WORKFLOW_DETAILS_MAPPING
            + "/{compoundWorkflowNodeId}/{taskId}/delegation/{taskType}";

    // Due to a bug in Spring the DELEGATION_MAPPING (3 path variables) does not work when we have a fallback mapping ("**")
    // See https://jira.spring.io/browse/SPR-6741
    private static final String DELEGATION_BASE = CompundWorkflowDetailsController.COMPOUND_WORKFLOW_DETAILS_MAPPING + "/{compoundWorkflowNodeId}/{taskId}/delegation/";
    private static final String INFORMATION_TASK_DELEGATION = DELEGATION_BASE + "information";
    private static final String ASSIGNMENT_TASK_DELEGATION = DELEGATION_BASE + "assignment";
    private static final String OPINION_TASK_DELEGATION = DELEGATION_BASE + "opinion";
    private static final String REVIEW_TASK_DELEGATION = DELEGATION_BASE + "review";

    private static final Map<QName, QName> DELEGABLE_TASK_TO_PARENT_TYPE;
    private static final List<String> ASSIGNMENT_TASK_DELEGATION_OPTIONS;
    private static final String TASK_DELEGATION_FORM = "taskDelegationForm";

    static {
        Map<QName, QName> taskToParent = new HashMap<>();
        taskToParent.put(WorkflowSpecificModel.Types.INFORMATION_TASK, WorkflowSpecificModel.Types.INFORMATION_WORKFLOW);
        taskToParent.put(WorkflowSpecificModel.Types.ASSIGNMENT_TASK, WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW);
        taskToParent.put(WorkflowSpecificModel.Types.OPINION_TASK, WorkflowSpecificModel.Types.OPINION_WORKFLOW);
        taskToParent.put(WorkflowSpecificModel.Types.REVIEW_TASK, WorkflowSpecificModel.Types.REVIEW_WORKFLOW);
        DELEGABLE_TASK_TO_PARENT_TYPE = Collections.unmodifiableMap(taskToParent);

        List<String> dTaskTypes = new ArrayList<>();
        dTaskTypes.add(DelegatableTaskType.ASSIGNMENT_RESPONSIBLE.name());
        dTaskTypes.add(DelegatableTaskType.ASSIGNMENT_NOT_RESPONSIBLE.name());
        dTaskTypes.add(DelegatableTaskType.INFORMATION.name());
        dTaskTypes.add(DelegatableTaskType.OPINION.name());
        ASSIGNMENT_TASK_DELEGATION_OPTIONS = dTaskTypes;
    }

    @RequestMapping(value = INFORMATION_TASK_DELEGATION, method = GET)
    public String informationTaskDelegation(@PathVariable String compoundWorkflowNodeId, @PathVariable String taskId, Model model, HttpServletRequest request) {
        return setupCommon(compoundWorkflowNodeId, taskId, model, request, DelegatableTaskType.INFORMATION);
    }

    @RequestMapping(value = OPINION_TASK_DELEGATION, method = GET)
    public String opinionTaskDelegation(@PathVariable String compoundWorkflowNodeId, @PathVariable String taskId, Model model, HttpServletRequest request) {
        return setupCommon(compoundWorkflowNodeId, taskId, model, request, DelegatableTaskType.OPINION);
    }

    @RequestMapping(value = REVIEW_TASK_DELEGATION, method = GET)
    public String reviewTaskDelegation(@PathVariable String compoundWorkflowNodeId, @PathVariable String taskId, Model model, HttpServletRequest request) {
        return setupCommon(compoundWorkflowNodeId, taskId, model, request, DelegatableTaskType.REVIEW);
    }

    private String setupCommon(String compoundWorkflowNodeId, String taskId, Model model, HttpServletRequest request, DelegatableTaskType dTaskType) {
        super.setup(model, request);
        NodeRef cwfRef = WebUtil.getNodeRefFromNodeId(compoundWorkflowNodeId);
        NodeRef taskRef = new NodeRef(cwfRef.getStoreRef(), taskId);
        return setupTaskDelegation(cwfRef, taskRef, model, dTaskType);
    }

    @RequestMapping(value = ASSIGNMENT_TASK_DELEGATION, method = GET)
    public String assignmentTaskDelegation(@PathVariable String compoundWorkflowNodeId, @PathVariable String taskId, Model model, HttpServletRequest request) {
        super.setup(model, request);
        NodeRef cwfRef = WebUtil.getNodeRefFromNodeId(compoundWorkflowNodeId);
        NodeRef taskRef = new NodeRef(cwfRef.getStoreRef(), taskId);
        return setupAssignmentTaskDelegation(cwfRef, taskRef, model);
    }

    private String setupAssignmentTaskDelegation(NodeRef cwfRef, NodeRef taskRef, Model model) {
        List<ee.webmedia.alfresco.workflow.service.Task> tasks = workflowService.getMyTasksInProgress(Arrays.asList(cwfRef), WorkflowSpecificModel.Types.ASSIGNMENT_TASK);
        Date taskDueDate = null;
        List<ee.webmedia.alfresco.workflow.service.Task> delegationTasks = new ArrayList<>();
        for (ee.webmedia.alfresco.workflow.service.Task t : tasks) {
            delegationTasks.add(t);
            if (taskDueDate == null && taskRef.equals(t.getNodeRef())) {
                taskDueDate = t.getDueDate();
            }
        }
        setupDelegationHistoryBlock(model, delegationTasks);
        TaskDelegationForm form = setupTaskDelegationFrom(taskDueDate, getDelegationTaskChoices(taskRef, ASSIGNMENT_TASK_DELEGATION_OPTIONS));
        form.setCompoundWorkflowRef(cwfRef);
        form.setTaskType("assignment");
        model.addAttribute(TASK_DELEGATION_FORM, form);

        return "compound-workflow/assignment-task-delegation";
    }

    private LinkedHashMap<String, String> getDelegationTaskChoices(NodeRef taskRef, List<String> options) {
        LinkedHashMap<String, String> delegationTaskChoices = new LinkedHashMap<>(); // must preserve order
        int start = hasResponsibleAspect(taskRef) ? 0 : 1;
        for (int i = start; i < options.size(); i++) {
            String delegatable = options.get(i);
            delegationTaskChoices.put(delegatable, translate("workflow.task.delegation.choice." + delegatable));
        }
        return delegationTaskChoices;
    }

    private String setupTaskDelegation(NodeRef cwfRef, NodeRef taskRef, Model model, DelegatableTaskType dTaskType) {
        ee.webmedia.alfresco.workflow.service.Task task = workflowService.getTaskWithoutDueDateData(taskRef);
        TaskDelegationForm form = setupTaskDelegationFrom(task.getDueDate(), null);
        form.setChoice(dTaskType.name());
        form.setCompoundWorkflowRef(cwfRef);
        String taskType = TASK_TYPE_TO_KEY_MAPPING.get(dTaskType.getTaskTypeQName());
        form.setTaskType(taskType);

        setupTranslations(form, dTaskType);
        model.addAttribute(TASK_DELEGATION_FORM, form);
        return "compound-workflow/task-delegation";
    }

    private void setupTranslations(TaskDelegationForm form, DelegatableTaskType dTaskType) {
        String taskType = dTaskType.getTaskTypeQName().getLocalName();
        Map<String, String> translations = form.getTranslations();
        String prefix = "workflow.task.type." + taskType;
        translations.put("pageTitle", prefix + ".delegation.title");
        translations.put("assigneesTitle", prefix + ".assignees");
        translations.put("formTitle", prefix + ".assignee.addition");
        translations.put("okButtonTitle", prefix + ".delegation.ok.title");
        translations.put("missingOwner", prefix + ".delegation.missing.owner");
        translations.put("futureDate", prefix + ".delegation.dueDate.after.original.dueDate");
        translations.put("assignee", prefix + ".assignee");
    }

    private TaskDelegationForm setupTaskDelegationFrom(Date taskDueDate, LinkedHashMap<String, String> delegationTaskChoices) {
        TaskDelegationForm form = new TaskDelegationForm(delegationTaskChoices);
        if (taskDueDate != null) {
            form.setTaskDueDate(DATE_FORMAT.format(taskDueDate));
        }
        return form;
    }

    private boolean hasResponsibleAspect(NodeRef taskRef) {
        return workflowService.getTask(taskRef, false).isResponsible();
    }

    @RequestMapping(value = DELEGATION_MAPPING, method = POST)
    public String processTaskDelegationSubmit(@PathVariable String compoundWorkflowNodeId, @PathVariable String taskId, @PathVariable String taskType,
            @ModelAttribute TaskDelegationForm form, RedirectAttributes redirectAttributes) {
        NodeRef cwfRef = WebUtil.getNodeRefFromNodeId(compoundWorkflowNodeId);
        NodeRef originalTaskRef = new NodeRef(cwfRef.getStoreRef(), taskId);

        QName type = resolveToQName(taskType);
        // set lock
        boolean locked = (compoundWorkflowNodeId != null)?setLock(new NodeRef(cwfRef.getStoreRef(), compoundWorkflowNodeId), "workflow_compond.locked", redirectAttributes):false;
        if (locked) {
	        boolean success = true;
	        
	        try {
		        if (WorkflowSpecificModel.Types.ASSIGNMENT_TASK.equals(type)) {
		            success = delegateMultipleTaskTypes(form, redirectAttributes, originalTaskRef);
		        } else if (WorkflowSpecificModel.Types.INFORMATION_TASK.equals(type)
		                || WorkflowSpecificModel.Types.OPINION_TASK.equals(type)
		                || WorkflowSpecificModel.Types.REVIEW_TASK.equals(type)) {
		            success = delegateSingleTaskTypeAndAddTasksToInitialTaskWorkflow(form, redirectAttributes, originalTaskRef);
		        }
		        if (!success) {
		            return redirectToCompoundWorkflowOrHomePage(originalTaskRef, compoundWorkflowNodeId);
		        }
	        } finally {
	        	// unlock
	        	if (compoundWorkflowNodeId != null) {
	    			getDocLockService().unlockIfOwner(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, compoundWorkflowNodeId));
	    		}
	        }
	        
	        return redirectToTaskList(type);
        } else {
        	return redirectToCompoundWorkflow(compoundWorkflowNodeId) + String.format("/%s/delegation/%s", originalTaskRef.getId(), taskType);
        }

        
    }

    private String redirectToCompoundWorkflowOrHomePage(NodeRef originalTaskRef, String compoundWorkflowNodeId) {
        ee.webmedia.alfresco.workflow.service.Task task = workflowService.getTaskWithoutDueDateData(originalTaskRef);
        if (task != null && task.isStatus(Status.IN_PROGRESS)) {
            return redirectToCompoundWorkflow(compoundWorkflowNodeId);
        }
        return redirectToTaskList();
    }

    private boolean delegateMultipleTaskTypes(TaskDelegationForm form, RedirectAttributes redirectAttributes, NodeRef originalTaskRef) {
        ee.webmedia.alfresco.workflow.service.Task assignmentTask = workflowService.getTaskWithParents(originalTaskRef);
        Workflow originalTaskWorkflow = assignmentTask.getParent();
        DelegationBean bean = BeanHelper.getDelegationBean();
        Map<String, List<TaskElement>> taskMap = form.getTaskElementMap();
        Set<String> keys = taskMap.keySet();
        Set<String> emptyGroups = new HashSet<>();

        for (String key : keys) {
            List<TaskElement> tasks = taskMap.get(key);
            DelegatableTaskType dTaskType = DelegatableTaskType.valueOf(key);
            Set<String> groups = addDelegationTasks(0, tasks, bean, originalTaskWorkflow, dTaskType);
            emptyGroups.addAll(groups);
        }

        boolean success = bean.delegate(assignmentTask, false);
        addRedirectMessages(redirectAttributes, emptyGroups, success, assignmentTask.getType());
        return success;
    }

    private boolean delegateSingleTaskTypeAndAddTasksToInitialTaskWorkflow(TaskDelegationForm form, RedirectAttributes redirectAttributes, NodeRef originalTaskRef) {
        ee.webmedia.alfresco.workflow.service.Task delegableTask = workflowService.getTaskWithParents(originalTaskRef);
        int taskIndex = (delegableTask.getTaskIndexInWorkflow() != null) ? (delegableTask.getTaskIndexInWorkflow() + 1) : 0;
        Workflow originalTaskWorkflow = delegableTask.getParent();
        DelegationBean bean = BeanHelper.getDelegationBean();

        Map<String, List<TaskElement>> taskMap = form.getTaskElementMap();
        Set<String> emptyGroups = new HashSet<>();
        for (String key : taskMap.keySet()) {
            List<TaskElement> tasks = taskMap.get(key);
            Set<String> empty = addTasksToOriginalWorkflow(taskIndex, tasks, bean, originalTaskWorkflow);
            emptyGroups.addAll(empty);
        }
        boolean success = bean.delegate(delegableTask, true);
        addRedirectMessages(redirectAttributes, emptyGroups, success, delegableTask.getType());
        return success;
    }

    private void addRedirectMessages(RedirectAttributes redirectAttributes, Set<String> emptyGroups, boolean success, QName taskType) {
        BeanHelper.getDelegationBean().reset();
        if (!success) {
            addRedirectWarnMsg(redirectAttributes, "workflow.task.delegation.failed");
            return;
        }

        if (!emptyGroups.isEmpty()) {
            addEmptyGroupsMessage(redirectAttributes, emptyGroups);
        }
        if (WorkflowSpecificModel.Types.INFORMATION_TASK.equals(taskType)) {
            addRedirectInfoMsg(redirectAttributes, "workflow.task.delegation.finished.informationTask");
        } else {
            addRedirectInfoMsg(redirectAttributes, "workflow.task.delegation.finished");
        }
    }

    private void addEmptyGroupsMessage(RedirectAttributes redirectAttributes, Set<String> emptyGroups) {
        Set<String> names = new HashSet<String>(emptyGroups.size());
        NodeService nodeService = BeanHelper.getNodeService();
        for (String emptyGroup : emptyGroups) {
            if (NodeRef.isNodeRef(emptyGroup)) {
                names.add((String) nodeService.getProperty(new NodeRef(emptyGroup), AddressbookModel.Props.GROUP_NAME));
            } else {
                names.add(BeanHelper.getAuthorityService().getAuthorityDisplayName(emptyGroup));
            }
        }
        String groups = StringUtils.join(names, ", ");
        addRedirectWarnMsg(redirectAttributes, "workflow.task.delegation.found.empty.groups", groups);
    }

    private Set<String> addDelegationTasks(int taskIndex, List<TaskElement> tasks, DelegationBean bean, Workflow taskWorkflow, DelegatableTaskType dTaskType) {
        AddDelegationTaskCallback addTask = new AddDelegationTaskCallback() {
            @Override
            public void addTask(DelegationBean bean, Workflow taskWorkflow, Integer index, String resolution, Date dueDate, String ownerId, int ownerType,
                    DelegatableTaskType dTaskType) {
                bean.addDelegationTask(taskWorkflow, index, resolution, dueDate, ownerId, ownerType, dTaskType);
            }
        };
        return addTasks(taskIndex, tasks, bean, taskWorkflow, dTaskType, addTask);
    }

    private Set<String> addTasksToOriginalWorkflow(int taskIndex, List<TaskElement> tasks, DelegationBean bean, Workflow taskWorkflow) {
        if (CollectionUtils.isEmpty(tasks)) {
            return Collections.emptySet();
        }
        AddDelegationTaskCallback addToOriginalWorkflow = new AddDelegationTaskCallback() {
            @Override
            public void addTask(DelegationBean bean, Workflow taskWorkflow, Integer index, String resolution, Date dueDate, String ownerId, int ownerType,
                    DelegatableTaskType dTaskType) {
                bean.addDelegationTaskToOriginalWorkflow(taskWorkflow, index, resolution, dueDate, ownerId, ownerType);
            }
        };
        return addTasks(taskIndex, tasks, bean, taskWorkflow, null, addToOriginalWorkflow);
    }

    private interface AddDelegationTaskCallback {
        void addTask(DelegationBean bean, Workflow taskWorkflow, Integer index, String resolution, Date dueDate, String ownerId, int ownerType, DelegatableTaskType dTaskType);
    }

    private Set<String> addTasks(int taskIndex, List<TaskElement> tasks, DelegationBean bean, Workflow taskWorkflow, DelegatableTaskType dTaskType,
            AddDelegationTaskCallback callback) {
        if (CollectionUtils.isEmpty(tasks)) {
            return Collections.emptySet();
        }
        Set<String> emptyGroups = new HashSet<>();
        for (TaskElement task : tasks) {
            if (task.getGroupMembers() != null) { // group
                if (task.getGroupMembers().length() == 0) {
                    emptyGroups.add(task.getOwnerId());
                    continue;
                }
                String[] groupMembers = task.getGroupMembers().split(",");
                for (String member : groupMembers) {
                    if (StringUtils.isBlank(member)) {
                        continue;
                    }
                    callback.addTask(bean, taskWorkflow, taskIndex, task.getResolution(), task.getDueDate(), member, getOwnerType(member), dTaskType);
                    taskIndex++;
                }
                continue;
            }
            String ownerId = task.getOwnerId(); // can be userName or nodeRef
            callback.addTask(bean, taskWorkflow, taskIndex, task.getResolution(), task.getDueDate(), ownerId, getOwnerType(ownerId), dTaskType);
            taskIndex++;
        }
        return emptyGroups;
    }

    private int getOwnerType(String userName) {
        if (NodeRef.isNodeRef(userName)) {
            return UserContactGroupSearchBean.CONTACTS_FILTER;
        }
        return UserContactGroupSearchBean.USERS_FILTER;
    }

    @RequestMapping(value = "/ajax/delegation/confirmation", method = POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public DelegationMessage getDelegationConfirmationMessages(@RequestBody DelegationMessage message) throws ParseException {
        NodeRef cwfRef = message.getCompoundWorkflowRef();
        CompoundWorkflowType type = workflowService.getCompoundWorkflowType(cwfRef);
        List<String> messages = new ArrayList<>(2);
        message.setMessages(messages);
        if (CompoundWorkflowType.INDEPENDENT_WORKFLOW.equals(type)) {
            List<String> dueDateStrings = message.getDueDates();
            if (CollectionUtils.isNotEmpty(dueDateStrings)) {
                List<NodeRef> docRefs = workflowService.getCompoundWorkflowDocumentRefs(cwfRef);
                if (CollectionUtils.isNotEmpty(docRefs)) {
                    QName workflowType = getDelegableTaskParentType(resolveToQName(message.getTaskType()));
                    DelegationBean.addDueDateConfirmation(messages, getMaxDate(dueDateStrings), docRefs, workflowType.getLocalName(), true);
                }
            }
        } else if (CompoundWorkflowType.DOCUMENT_WORKFLOW.equals(type)) {

            List<String> dueDateStrings = message.getDueDates();
            NodeRef docRef = BeanHelper.getNodeService().getPrimaryParent(cwfRef).getParentRef();
            QName taskType = resolveToQName(message.getTaskType());
            QName workflowType = getDelegableTaskParentType(taskType);
            if (CollectionUtils.isNotEmpty(dueDateStrings)) {
                DelegationBean.addDueDateConfirmation(messages, getMaxDate(dueDateStrings), Collections.singletonList(docRef), workflowType.getLocalName(), false);
            }
            Set<String> taskOwners = new HashSet<>(message.getTaskOwners());
            DelegationBean.addDuplicateTaskMessage(messages, docRef, taskType, taskOwners);
        } else {
            throw new RuntimeException("Unexpected compound workflow type: " + type.name());
        }
        return message;
    }
    
    @RequestMapping(value = "/ajax/cwf/lockdelegate", method = POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public LockMessage setCompoundWorkflowLockDelegate(@RequestBody LockMessage message) throws ParseException {
        
        final NodeRef cwfRef = message.getCompoundWorkflowRef();
        List<String> messages = new ArrayList<>(2);
        message.setMessages(messages);
        RetryingTransactionCallback<String> callback = new RetryingTransactionCallback<String>()
        {
           public String execute() throws Throwable
           {
        	   
        	   LockStatus lockStatus = getDocLockService().setLockIfFree(cwfRef);
        	   String result;
               
	           	if (lockStatus == LockStatus.LOCK_OWNER) {
	           		result = null;
	            } else {
	            	String lockOwner = StringUtils.substringBefore(getDocLockService().getLockOwnerIfLocked(cwfRef), "_");
	                String lockOwnerName = getUserService().getUserFullNameAndId(lockOwner);
	                result = translate("workflow_compond.locked", lockOwnerName);
                }
                return result;
           }
        };
        
        String lockError = txnHelper.doInTransaction(callback, false, true);
        if (StringUtils.isNotBlank(lockError)) {
        	messages.add(lockError);
        }
        return message;
    }

    private QName getDelegableTaskParentType(QName taskType) {
        QName workflowType = DELEGABLE_TASK_TO_PARENT_TYPE.get(taskType);
        if (workflowType == null) {
            throw new RuntimeException("Task of type " + taskType.getLocalName() + " is not delegable");
        }
        return workflowType;
    }

    private Date getMaxDate(List<String> dueDateStrings) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        Date maxTaskDueDate = null;
        for (String dueDateStr : dueDateStrings) {
            if (StringUtils.isBlank(dueDateStr)) {
                continue;
            }
            Date temp = format.parse(dueDateStr);
            if (maxTaskDueDate == null || temp.after(maxTaskDueDate)) {
                maxTaskDueDate = temp;
            }
        }
        return maxTaskDueDate;
    }

    private QName resolveToQName(String taskTypeMapping) {
        Set<QName> taskTypes = TASK_TYPE_MAPPING.get(taskTypeMapping);
        if (CollectionUtils.isEmpty(taskTypes)) {
            throw new RuntimeException("Unknown task type: " + taskTypeMapping);
        } else if (taskTypes.size() > 1) {
            throw new RuntimeException("Unable to resolve task type: found " + taskTypes.size() + " matches for " + taskTypeMapping);
        }
        return taskTypes.iterator().next();
    }

}
