package ee.webmedia.mobile.alfresco.workflow;

import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowDbService;
import static ee.webmedia.alfresco.workflow.web.WorkflowBlockBean.isMobileIdOutcome;
import static ee.webmedia.alfresco.workflow.web.WorkflowBlockBean.isMobileIdOutcomeAndMobileIdDisabled;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.common.listener.ExternalAccessPhaseListener;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.UserContactGroupSearchBean;
import ee.webmedia.alfresco.docdynamic.web.DocumentLockHelperBean;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.utils.CalendarUtil;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.workflow.exception.WorkflowActiveResponsibleTaskException;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException;
import ee.webmedia.alfresco.workflow.model.Comment;
import ee.webmedia.alfresco.workflow.model.RelatedUrl;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowBlockItem;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.DelegationHistoryUtil;
import ee.webmedia.alfresco.workflow.service.SignatureTask;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowConstantsBean;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import ee.webmedia.alfresco.workflow.web.CompoundWorkflowDialog;
import ee.webmedia.alfresco.workflow.web.DelegationBean;
import ee.webmedia.alfresco.workflow.web.SigningFlowContainer;
import ee.webmedia.alfresco.workflow.web.WorkflowBlockBean;
import ee.webmedia.mobile.alfresco.HomeController;
import ee.webmedia.mobile.alfresco.common.AbstractBaseController;
import ee.webmedia.mobile.alfresco.workflow.model.DelegationHistoryItem;
import ee.webmedia.mobile.alfresco.workflow.model.DueDateExtensionForm;
import ee.webmedia.mobile.alfresco.workflow.model.InProgressTasksForm;
import ee.webmedia.mobile.alfresco.workflow.model.MobileIdSignatureAjaxRequest;
import ee.webmedia.mobile.alfresco.workflow.model.Task;
import ee.webmedia.mobile.alfresco.workflow.model.TaskDelegationForm;
import ee.webmedia.mobile.alfresco.workflow.model.TaskDelegationForm.TaskElement;

@Controller
public class CompundWorkflowDetailsController extends AbstractBaseController {

    private static final String TASK_COUNT_ATTR = "taskCount";

    private static final String WORKFLOW_BLOCK_ITEMS_ATTR = "workflowBlockItems";

    private static org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(CompundWorkflowDetailsController.class);

    private static final String ACTION_SAVE = "save";

    private static final String DELEGATE_TASK = "delegate";
    private static final String EXTEND_DUE_DATE = "extendDueDate";
    private static final String SIGNING_FLOW_ID_ATTR = "signingFlowId";
    private static final long serialVersionUID = 1L;
    public static final String COMPOUND_WORKFLOW_NODE_ID = "compoundWorkflowNodeId";
    public static final String COMPOUND_WORKFLOW_DETAILS_MAPPING = "compound-workflow/details";
    private static final List<QName> SUPPORTED_TASK_TYPES = new ArrayList<QName>(Arrays.asList(AbstractBaseController.TASK_TYPES));
    private static final String TITLE_SUFFIX = ".title";
    private static Map<Integer, String> reviewTaskOutcomes;
    @Resource
    private SigningFlowHolder signingFlowHolder;

    @Resource(name = "WmWorkflowService")
    private WorkflowService workflowService;
    @Resource
    private WorkflowDbService workflowDbService;
    @Resource
    private DocumentTemplateService documentTemplateService;
    @Resource
    private WorkflowConstantsBean workflowConstantsBean;

    @RequestMapping(value = COMPOUND_WORKFLOW_DETAILS_MAPPING + "/{compoundWorkflowNodeId}", method = RequestMethod.GET)
    public String setupCompoundWorkflow(@PathVariable String compoundWorkflowNodeId, Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        super.setup(model, request);
        Long signingFlowId = (Long) model.asMap().get(SIGNING_FLOW_ID_ATTR);
        return initCompoundWorkflow(compoundWorkflowNodeId, model, signingFlowId, redirectAttributes);
    }

    // compoundWorkflow is initialized here, not in @ModelAttribute method, because it may be null
    private String initCompoundWorkflow(String compoundWorkflowNodeId, Model model, Long signingFlowId, RedirectAttributes redirectAttributes) {
        NodeRef compoundWorkflowNodeRef = WebUtil.getNodeRefFromNodeId(compoundWorkflowNodeId);
        if (compoundWorkflowNodeRef == null) {
            addRedirectErrorMsg(redirectAttributes, "workflow_compound_edit_error_docDeleted");
            return "redirect:/m/tasks";
        }
        // TODO: optimize loading compound workflow if possible (all data may not be needed in mobile version)
        CompoundWorkflow compoundWorkflow = workflowService.getCompoundWorkflow(compoundWorkflowNodeRef);
        if (!(compoundWorkflow.isIndependentWorkflow() && WorkflowUtil.hasInProgressTaskOfType(compoundWorkflow, AuthenticationUtil.getRunAsUser(), SUPPORTED_TASK_TYPES))) {
            addRedirectErrorMsg(redirectAttributes, "redirect.unavailable." + ExternalAccessPhaseListener.OUTCOME_COMPOUND_WORKFLOW_NODEREF);
            return "redirect:/m/tasks";
        }
        List<ee.webmedia.alfresco.workflow.service.Task> myTasks = workflowService.getMyTasksInProgress(Arrays.asList(compoundWorkflow.getNodeRef()));
        Map<String, Task> myTasksMap = new HashMap<>();
        Map<NodeRef, List<Pair<String, String>>> taskOutcomeButtons = new HashMap<>();
        SigningFlowContainer signingFlow = null;
        SignatureTask signatureTask = null;
        if (signingFlowId != null) {
            signingFlow = signingFlowHolder.getSigningFlow(signingFlowId);
            signatureTask = signingFlow.getSignatureTask();
        }
        String buttonLabelPrefix = "workflow.task.type.";
        List<ee.webmedia.alfresco.workflow.service.Task> delegableTasks = new ArrayList<>();
        for (ee.webmedia.alfresco.workflow.service.Task task : myTasks) {
            ee.webmedia.mobile.alfresco.workflow.model.Task formTask = new ee.webmedia.mobile.alfresco.workflow.model.Task(task);
            formTask.setSignTogether(isSignTogether(task));
            QName taskType = task.getType();
            if (!SUPPORTED_TASK_TYPES.contains(taskType)) {
                continue;
            }
            if (!Boolean.TRUE.equals(task.getViewedByOwner())) {
                getWorkflowDbService().updateTaskSingleProperty(task, WorkflowCommonModel.Props.VIEWED_BY_OWNER, Boolean.TRUE, task.getWorkflowNodeRef());
            }
            myTasksMap.put(task.getNodeRef().toString(), formTask);
            if (signatureTask != null && task.getNodeRef().equals(signatureTask.getNodeRef())) {
                formTask.setComment(signatureTask.getComment());
            }
            String saveLabel = buttonLabelPrefix + taskType.getLocalName() + ".save.title";
            String label = buttonLabelPrefix + taskType.getLocalName() + ".outcome.";
            List<Pair<String, String>> taskOutcomeBtnLabels = new ArrayList<Pair<String, String>>();
            if (task.isType(WorkflowSpecificModel.Types.OPINION_TASK,
                    WorkflowSpecificModel.Types.REVIEW_TASK,
                    WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK,
                    WorkflowSpecificModel.Types.CONFIRMATION_TASK)) {
                taskOutcomeBtnLabels.add(Pair.newInstance(ACTION_SAVE, saveLabel));
            }
            if (task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK, WorkflowSpecificModel.Types.REVIEW_TASK)) {
                formTask.setCommentLabel("workflow.task." + taskType.getLocalName() + ".prop.comment");
            } else {
                formTask.setCommentLabel("workflow.task.prop.comment");
            }

            for (int outcomeIndex = 0; outcomeIndex < task.getOutcomes(); outcomeIndex++) {
                if (isIdCardOutcome(taskType, outcomeIndex) || isMobileIdOutcomeAndMobileIdDisabled(taskType, outcomeIndex)) {
                    continue;
                }
                taskOutcomeBtnLabels.add(Pair.newInstance(Integer.valueOf(outcomeIndex).toString(), label + outcomeIndex + TITLE_SUFFIX));
                if (WorkflowSpecificModel.Types.REVIEW_TASK.equals(taskType)
                        || WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK.equals(taskType)) {
                    break;
                }
            }
            if (task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK)) {
                delegableTasks.add(task);
                taskOutcomeBtnLabels.add(Pair.newInstance(DELEGATE_TASK, "workflow.task.type.assignmentTask.delegate"));
                taskOutcomeBtnLabels.add(Pair.newInstance(EXTEND_DUE_DATE, "workflow.task.type.assignmentTask.dueDateExtension"));
            }
            taskOutcomeButtons.put(task.getNodeRef(), taskOutcomeBtnLabels);
        }
        InProgressTasksForm inProgressTasksForm = new InProgressTasksForm(myTasksMap, compoundWorkflowNodeRef, BeanHelper.getGeneralService()
                .getPrimaryParent(compoundWorkflowNodeRef).getNodeRef());
        if (signingFlow != null) {
            inProgressTasksForm.setSigningFlowId(signingFlowId);
            inProgressTasksForm.setSigningFlowView(signingFlow.getSigningFlowView().name());
            inProgressTasksForm.setMobileIdChallengeId(signingFlow.getChallengeId());
            inProgressTasksForm.setPhoneNumber(signingFlow.getPhoneNumber());
        }
        model.addAttribute("inProgressTasksForm", inProgressTasksForm);
        model.addAttribute("taskOutcomeButtons", taskOutcomeButtons);
        model.addAttribute("compoundWorkflow", compoundWorkflow);
        model.addAttribute("reviewTaskOutcomes", getReviewTaskOutcomes());
        if (BeanHelper.getWorkflowConstantsBean().isWorkflowTitleEnabled()) {
            model.addAttribute("compoundWorkflowTitle", compoundWorkflow.getTitle());
        }

        setupComments(model, compoundWorkflowNodeId);
        setupObjects(model, compoundWorkflow);
        setupRelatedUrls(model, compoundWorkflowNodeRef);
        setupWorkflowBlock(model, compoundWorkflow);
        if (!delegableTasks.isEmpty()) {
            setupDelegationHistoryBlock(model, delegableTasks);
        }
        BeanHelper.getLogService().addLogEntry(LogEntry.create(LogObject.COMPOUND_WORKFLOW, getUserService(), compoundWorkflowNodeRef, "applog_compoundWorkflow_view"));
        return COMPOUND_WORKFLOW_DETAILS_MAPPING;
    }

    private void setupWorkflowBlock(Model model, CompoundWorkflow compoundWorkflow) {
        List<WorkflowBlockItem> groupedWorkflowBlockItems = getWorkflowDbService().getWorkflowBlockItems(Arrays.asList(compoundWorkflow.getNodeRef()), null, null);
        setMessageSource(groupedWorkflowBlockItems);
        model.addAttribute(WORKFLOW_BLOCK_ITEMS_ATTR, groupedWorkflowBlockItems);
        model.addAttribute(TASK_COUNT_ATTR, WorkflowUtil.getTaskCount(compoundWorkflow));
    }

    private void setupDelegationHistoryBlock(Model model, List<ee.webmedia.alfresco.workflow.service.Task> delegationTasks) {
        if (CollectionUtils.isEmpty(delegationTasks)) {
            return;
        }
        List<ee.webmedia.alfresco.workflow.service.Task> delegationHistory = new ArrayList<>();
        List<NodeRef> delegationTaskRefs = new ArrayList<>();
        delegationHistory.addAll(workflowService.getTasks4DelegationHistory(delegationTasks.get(0).getNode()));
        for (ee.webmedia.alfresco.workflow.service.Task task : delegationTasks) {
            delegationTaskRefs.add(task.getNodeRef());
        }
        List<Node> delegationHistoryNodes = DelegationHistoryUtil.getDelegationNodes(delegationTaskRefs, delegationHistory);
        List<DelegationHistoryItem> items = new ArrayList<>(delegationHistoryNodes.size());
        for (Node node : delegationHistoryNodes) {
            items.add(new DelegationHistoryItem(node.getProperties()));
        }
        model.addAttribute("delegationHistoryTaskCount", items.size());
        model.addAttribute("delegationHistoryBlockItems", items);
    }

    private void setMessageSource(List<WorkflowBlockItem> groupedWorkflowBlockItems) {
        for (WorkflowBlockItem workflowBlockItem : groupedWorkflowBlockItems) {
            workflowBlockItem.setMessageSource(messageSource);
        }
    }

    private Map<Integer, String> getReviewTaskOutcomes() {
        if (reviewTaskOutcomes == null) {
            reviewTaskOutcomes = new HashMap<Integer, String>();
            int outcomes = workflowConstantsBean.getWorkflowTypes().get(WorkflowSpecificModel.Types.REVIEW_WORKFLOW).getTaskOutcomes();
            for (int i = 0; i < outcomes; i++) {
                reviewTaskOutcomes.put(i, translate("workflow.task.type.reviewTask.outcome." + i + TITLE_SUFFIX));
            }
        }
        return reviewTaskOutcomes;
    }

    private void setupComments(Model model, String compundWorkflowId) {
        List<Comment> compoundWorkflowComments = workflowDbService.getCompoundWorkflowComments(compundWorkflowId);
        if (!compoundWorkflowComments.isEmpty()) {
            model.addAttribute("comments", compoundWorkflowComments);
        }
    }

    private void setupObjects(Model model, CompoundWorkflow compoundWorkflow) {
        List<Document> compoundWorkflowDocuments = workflowService.getCompoundWorkflowDocuments(compoundWorkflow.getNodeRef());

        if (!compoundWorkflowDocuments.isEmpty()) {
            // Set up object properties
            List<String> documentsToSignRefId = compoundWorkflow.getDocumentsToSignNodeRefIds();
            for (Document document : compoundWorkflowDocuments) {
                String nodeRefId = document.getNodeRef().getId();
                if (documentsToSignRefId.contains(nodeRefId)) {
                    document.setDocumentToSign(Boolean.TRUE);
                }
                // TODO: After implementing document details view, add permission check.
                // if (document.getNode().hasPermission(DocumentCommonModel.Privileges.VIEW_DOCUMENT_META_DATA)) {
                // document.setShowLink(true);
                // }
            }

            model.addAttribute("objects", compoundWorkflowDocuments);
        }
    }

    private void setupRelatedUrls(Model model, NodeRef compoundWorkflowNodeRef) {
        List<RelatedUrl> relatedUrls = workflowService.getRelatedUrls(compoundWorkflowNodeRef);
        if (!relatedUrls.isEmpty()) {
            WebUtil.toggleSystemUrlTarget(documentTemplateService.getServerUrl(), relatedUrls);

            model.addAttribute("relatedUrls", relatedUrls);
        }
    }

    private boolean isIdCardOutcome(QName taskType, int outcomeIndex) {
        return WorkflowSpecificModel.Types.SIGNATURE_TASK.equals(taskType)
                && WorkflowSpecificModel.SignatureTaskOutcome.SIGNED_IDCARD.equals(outcomeIndex);
    }

    /** If given task belongs to some task group in workflow block, show all tasks in this group. Otherwise redirect to compound workflow details view */
    @RequestMapping(value = "compound-workflow/task-group-details/{compoundWorkflowId}/{taskId}")
    public String getTaskGroupDetails(@PathVariable String compoundWorkflowId, @PathVariable String taskId, Model model) {
        NodeRef compoundWorkflowNodeRef = WebUtil.getNodeRefFromNodeId(compoundWorkflowId);
        if (compoundWorkflowNodeRef == null) {
            addErrorMessage("workflow_compound_edit_error_docDeleted");
            return "home";
        }

        List<WorkflowBlockItem> groupedWorkflowBlockItems = getWorkflowDbService().getWorkflowBlockItems(Arrays.asList(compoundWorkflowNodeRef), null, null);
        List<WorkflowBlockItem> groupedWorkflowBlockItem = new ArrayList<WorkflowBlockItem>();
        WorkflowBlockItem currentItem = null;
        OUTER: for (WorkflowBlockItem workflowBlockItem : groupedWorkflowBlockItems) {
            if (!workflowBlockItem.isGroupBlockItem()) {
                String workflowTaskId = workflowBlockItem.getTaskNodeRef().getId();
                if (workflowTaskId.equals(taskId)) {
                    return redirectToCompoundWorkflow(compoundWorkflowId);
                }
            }
            else {
                final List<WorkflowBlockItem> groupItems = getWorkflowDbService().getWorkflowBlockItemGroup(workflowBlockItem);
                for (WorkflowBlockItem taskItem : groupItems) {
                    String workflowTaskId = taskItem.getTaskNodeRef().getId();
                    if (workflowTaskId.equals(taskId)) {
                        currentItem = workflowBlockItem;
                        groupedWorkflowBlockItem.addAll(groupItems);
                        break OUTER;
                    }
                }

            }
        }
        if (groupedWorkflowBlockItem.isEmpty()) {
            return redirectToCompoundWorkflow(compoundWorkflowId);
        }
        setMessageSource(groupedWorkflowBlockItem);
        model.addAttribute(WORKFLOW_BLOCK_ITEMS_ATTR, groupedWorkflowBlockItem);
        model.addAttribute("groupName", currentItem.getGroupName());
        return "compound-workflow/task-group-details";
    }

    @RequestMapping(value = COMPOUND_WORKFLOW_DETAILS_MAPPING + "/{compoundWorkflowNodeId}/{taskId}/extension", method = RequestMethod.GET)
    public String dueDateExtension(@PathVariable String compoundWorkflowNodeId, @PathVariable String taskId, Model model, HttpServletRequest request) {
        super.setupWithoutSidebarMenu(model, request);
        DueDateExtensionForm form = new DueDateExtensionForm();
        NodeRef cwfRef = WebUtil.getNodeRefFromNodeId(compoundWorkflowNodeId);
        NodeRef taskRef = new NodeRef(cwfRef.getStoreRef(), taskId);
        String taskCreatorId = (String) getWorkflowDbService().getTaskProperty(taskRef, WorkflowSpecificModel.Props.CREATOR_ID);
        String currentUser = AuthenticationUtil.getRunAsUser();
        if (currentUser != null && !currentUser.equals(taskCreatorId)) {
            form.setUserId(taskCreatorId);
            String taskCreatorName = getUserService().getUserFullName(taskCreatorId);
            form.setUserName(taskCreatorName);
        }
        Date date = CalendarUtil.addWorkingDaysToDate(new LocalDate(), 2, BeanHelper.getClassificatorService()).toDateTimeAtCurrentTime().toDate();
        form.setInitialExtensionDueDate(new SimpleDateFormat("dd.MM.yyyy").format(date));
        model.addAttribute("dueDateExtensionForm", form);

        return "compound-workflow/due-date-extension";
    }

    @RequestMapping(value = COMPOUND_WORKFLOW_DETAILS_MAPPING + "/{compoundWorkflowNodeId}/{taskId}/extension", method = RequestMethod.POST)
    public String processDueDateExtensionSubmit(@PathVariable String compoundWorkflowNodeId, @PathVariable String taskId, @ModelAttribute DueDateExtensionForm form,
            RedirectAttributes redirectAttributes) {
        NodeRef cwfRef = WebUtil.getNodeRefFromNodeId(compoundWorkflowNodeId);
        NodeRef taskRef = new NodeRef(cwfRef.getStoreRef(), taskId);
        String userId = form.getUserId();
        String extenderUserFullname = getUserService().getUserFullName(userId);
        ee.webmedia.alfresco.workflow.service.Task initiatingTask = workflowService.getTaskWithParents(taskRef);
        workflowService.createDueDateExtension(
                form.getReason(),
                form.getNewDueDate(),
                form.getExtensionDueDate(),
                initiatingTask,
                cwfRef,
                userId,
                extenderUserFullname);

        addRedirectInfoMsg(redirectAttributes, "workflow.task.dueDate.extension.submitted");
        return redirectToCompoundWorkflow(compoundWorkflowNodeId);
    }

    @RequestMapping(value = COMPOUND_WORKFLOW_DETAILS_MAPPING + "/{compoundWorkflowNodeId}/{taskId}/delegation", method = RequestMethod.GET)
    public String taskDelegation(@PathVariable String compoundWorkflowNodeId, @PathVariable String taskId, Model model, HttpServletRequest request) {
        super.setup(model, request);
        NodeRef cwfRef = WebUtil.getNodeRefFromNodeId(compoundWorkflowNodeId);
        List<ee.webmedia.alfresco.workflow.service.Task> tasks = workflowService.getMyTasksInProgress(Arrays.asList(cwfRef),
                WorkflowSpecificModel.Types.ASSIGNMENT_TASK);
        List<ee.webmedia.alfresco.workflow.service.Task> delegationTasks = new ArrayList<>();
        for (ee.webmedia.alfresco.workflow.service.Task t : tasks) {
            delegationTasks.add(t);
        }
        setupDelegationHistoryBlock(model, delegationTasks);
        NodeRef taskRef = new NodeRef(cwfRef.getStoreRef(), taskId);
        TaskDelegationForm form = setupTaskDelegationFrom(taskRef);
        model.addAttribute("taskDelegationForm", form);

        return "compound-workflow/task-delegation";
    }

    private TaskDelegationForm setupTaskDelegationFrom(NodeRef taskRef) {
        Map<Integer, String> delegationTaskChoices = new HashMap<>();
        int start = hasResponsibleAspect(taskRef) ? 0 : 1;
        for (int i = start; i < DelegationBean.DELEGATION_TASK_CHOICE_COUNT; i++) {
            delegationTaskChoices.put(i, translate("workflow.task.delegation.choice." + i));
        }
        return new TaskDelegationForm(delegationTaskChoices);
    }

    private boolean hasResponsibleAspect(NodeRef taskRef) {
        return workflowService.getTask(taskRef, false).isResponsible();
    }

    @RequestMapping(value = COMPOUND_WORKFLOW_DETAILS_MAPPING + "/{compoundWorkflowNodeId}/{taskId}/delegation", method = RequestMethod.POST)
    public String processTaskDelegationSubmit(@PathVariable String compoundWorkflowNodeId, @PathVariable String taskId, @ModelAttribute TaskDelegationForm form,
            RedirectAttributes redirectAttributes) {

        NodeRef cwfRef = WebUtil.getNodeRefFromNodeId(compoundWorkflowNodeId);
        NodeRef originalTaskRef = new NodeRef(cwfRef.getStoreRef(), taskId);

        ee.webmedia.alfresco.workflow.service.Task assignmentTask = workflowService.getTaskWithParents(originalTaskRef);
        Workflow originalTaskWorkflow = assignmentTask.getParent();
        DelegationBean bean = BeanHelper.getDelegationBean();
        Map<String, List<TaskElement>> taskMap = form.getTaskElementMap();
        Set<String> keys = taskMap.keySet();
        Set<String> emptyGroups = new HashSet<>();
        for (String key : keys) {
            List<TaskElement> tasks = taskMap.get(key);
            int taskIndex = 0;
            for (TaskElement task : tasks) {
                if (task.getGroupMembers() != null) { // group
                    if (task.getGroupMembers().length() == 0) {
                        emptyGroups.add(task.getOwnerId());
                        continue;
                    }
                    String[] groupMembers = task.getGroupMembers().split(",");
                    for (String member : groupMembers) {
                        if (StringUtils.isEmpty(member)) {
                            continue;
                        }
                        bean.addDelegationTask(originalTaskWorkflow, taskIndex, task.getResolution(), task.getDueDate(), Integer.valueOf(key), member, getOwnerType(member));
                        taskIndex++;
                    }
                    continue;
                }
                String ownerId = task.getOwnerId(); // can be userName or nodeRef
                bean.addDelegationTask(originalTaskWorkflow, taskIndex, task.getResolution(), task.getDueDate(), Integer.valueOf(key), ownerId, getOwnerType(ownerId));
                taskIndex++;
            }
        }
        boolean success = BeanHelper.getDelegationBean().delegate(assignmentTask);
        BeanHelper.getDelegationBean().reset();
        if (!success) { // oh nos!
            addRedirectWarnMsg(redirectAttributes, "workflow.task.delegation.failed");
            return redirectToCompoundWorkflow(compoundWorkflowNodeId);
        }

        if (!emptyGroups.isEmpty()) {
            addEmptyGroupsMessage(redirectAttributes, emptyGroups);
        }
        addRedirectInfoMsg(redirectAttributes, "workflow.task.delegation.finished");
        return redirectToTaskList(redirectAttributes, WorkflowSpecificModel.Types.ASSIGNMENT_TASK);
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

    private int getOwnerType(String userName) {
        if (NodeRef.isNodeRef(userName)) {
            return UserContactGroupSearchBean.CONTACTS_FILTER;
        }
        return UserContactGroupSearchBean.USERS_FILTER;

    }

    @RequestMapping(value = COMPOUND_WORKFLOW_DETAILS_MAPPING + "/{compoundWorkflowNodeId}", method = RequestMethod.POST)
    public String processSubmit(@PathVariable String compoundWorkflowNodeId, @ModelAttribute InProgressTasksForm inProgressTasksForm, RedirectAttributes redirectAttributes,
            HttpSession session) {
        if (inProgressTasksForm == null) {
            return redirectToCompoundWorkflow(compoundWorkflowNodeId);
        }
        Map<String, String> formActions = inProgressTasksForm.getActions();
        if (!formActions.isEmpty()) {
            boolean redirectToTaskList = continueCurrentSigning(inProgressTasksForm, redirectAttributes, formActions, session);
            if (redirectToTaskList) {
                return redirectToTaskList(redirectAttributes, WorkflowSpecificModel.Types.SIGNATURE_TASK);
            }
            return redirectToCompoundWorkflow(compoundWorkflowNodeId);
        }
        Task taskToFinish = null;
        int outcomeIndex = -1;
        boolean saveOnly = false;
        NodeRef extensionTaskRef = null;
        for (Task task : inProgressTasksForm.getInProgressTasks().values()) {
            for (Map.Entry<String, String> entry : task.getActions().entrySet()) {
                if (StringUtils.isNotBlank(entry.getValue())) {
                    taskToFinish = task;
                    String taskAction = entry.getKey();
                    if (ACTION_SAVE.equals(taskAction)) {
                        saveOnly = true;
                        break;
                    }
                    if (DELEGATE_TASK.equals(taskAction)) {
                        return redirectToTaskDelegation(compoundWorkflowNodeId, task.getNodeRef().getId());
                    }
                    if (EXTEND_DUE_DATE.equals(taskAction)) {
                        extensionTaskRef = task.getNodeRef();
                        break;
                    }
                    outcomeIndex = Integer.valueOf(taskAction);
                    break;
                }
            }
        }

        if (taskToFinish == null || (outcomeIndex < 0 && !saveOnly && extensionTaskRef == null)) {
            addRedirectErrorMsg(redirectAttributes, "workflow.task.finish.error.workflow.task.save.failed");
        } else if (saveOnly) {
            saveTask(taskToFinish, redirectAttributes);
        } else if (extensionTaskRef != null) {
            return redirectToDueDateExtensionView(compoundWorkflowNodeId, extensionTaskRef.getId());
        }
        else {
            QName taskType = workflowService.getNodeRefType(taskToFinish.getNodeRef());
            if (isMobileIdOutcome(taskType, outcomeIndex)) {
                startSigning(inProgressTasksForm, redirectAttributes, taskToFinish, session);
            } else {
                boolean finishSuccess = finishTask(taskToFinish, taskType, outcomeIndex, redirectAttributes);
                if (finishSuccess) {
                    return redirectToTaskList(redirectAttributes, taskType);
                }
            }
        }
        return redirectToCompoundWorkflow(compoundWorkflowNodeId);
    }

    private String redirectToTaskList(RedirectAttributes redirectAttributes, QName taskType) {
        redirectAttributes.addFlashAttribute(HomeController.REDIRECT_FROM_FINISH_TASK_ATTR, Boolean.TRUE);
        return "redirect:/m/tasks/" + TASK_TYPE_TO_KEY_MAPPING.get(taskType);
    }

    private String redirectToDueDateExtensionView(String compoundWorkflowNodeId, String taskId) {
        return redirectToCompoundWorkflow(compoundWorkflowNodeId) + "/" + taskId + "/extension";
    }

    private String redirectToTaskDelegation(String compoundWorkflowNodeId, String taskId) {
        return redirectToCompoundWorkflow(compoundWorkflowNodeId) + "/" + taskId + "/delegation";
    }

    public void startSigning(InProgressTasksForm inProgressTasksForm, RedirectAttributes redirectAttributes, Task taskToFinish, HttpSession session) {
        ee.webmedia.alfresco.workflow.service.Task task = workflowService.getTask(taskToFinish.getNodeRef(), false);
        if (task == null || !task.isStatus(Status.IN_PROGRESS)) {
            addRedirectErrorMsg(redirectAttributes, "workflow.task.finish.error.workflow.task.save.failed");
            return;
        }
        task.setComment(taskToFinish.getComment());
        MobileSigningFlowContainer signingFlow = new MobileSigningFlowContainer((SignatureTask) task, isSignTogether(task), inProgressTasksForm,
                inProgressTasksForm.getCompoundWorkflowRef(), inProgressTasksForm.getContainerRef());
        boolean signingPrepared = signingFlow.prepareSigning(this, redirectAttributes);
        if (!signingPrepared) {
            return;
        }
        long signingFlowId = signingFlowHolder.addSigningFlow(signingFlow, session);
        redirectAttributes.addFlashAttribute(SIGNING_FLOW_ID_ATTR, signingFlowId);
    }

    private boolean isSignTogether(ee.webmedia.alfresco.workflow.service.Task task) {
        NodeRef worklowRef = task.getWorkflowNodeRef();
        return WorkflowUtil.isSignTogetherType((String) BeanHelper.getNodeService().getProperty(worklowRef, WorkflowSpecificModel.Props.SIGNING_TYPE));
    }

    public boolean continueCurrentSigning(InProgressTasksForm inProgressTasksForm, RedirectAttributes redirectAttributes, Map<String, String> formActions, HttpSession session) {
        Long signingFlowId = inProgressTasksForm.getSigningFlowId();
        MobileSigningFlowContainer signingFlow = getSigningFlow(inProgressTasksForm, signingFlowId);
        if (formActions.containsKey("mobileNumberInserted")) {
            if (signingFlow != null) {
                String phoneNumber = inProgressTasksForm.getPhoneNumber();
                signingFlow.setPhoneNumber(phoneNumber);
                session.setAttribute(SigningFlowHolder.LAST_USED_MOBILE_ID_NUMBER, phoneNumber);
                boolean signingStarted = signingFlow.startMobileIdSigning(this, redirectAttributes);
                if (!signingStarted) {
                    signingFlowHolder.removeSigningFlow(signingFlowId);
                } else {
                    redirectAttributes.addFlashAttribute(SIGNING_FLOW_ID_ATTR, signingFlowId);
                }
            }
        } else if (formActions.containsKey("finishMobileIdSigning")) {
            if (signingFlow != null) {
                boolean signingFinished = signingFlow.finishMobileIdSigning(this, redirectAttributes);
                if (!signingFinished || signingFlow.isSigningQueueEmpty()) {
                    signingFlowHolder.removeSigningFlow(signingFlowId);
                    return true;
                }
                redirectAttributes.addFlashAttribute(SIGNING_FLOW_ID_ATTR, signingFlowId);
            }
        } else if (formActions.containsKey("signingCancelled")) {
            if (signingFlow != null) {
                signingFlowHolder.removeSigningFlow(signingFlowId);
            }
        } else {
            LOG.warn("Unknown compound workflow action");
        }
        return false;
    }

    private void saveTask(Task taskToFinish, RedirectAttributes redirectAttributes) {
        ee.webmedia.alfresco.workflow.service.Task task = workflowService.getTaskWithParents(taskToFinish.getNodeRef());
        if (!task.isStatus(Status.IN_PROGRESS)) {
            addRedirectErrorMsg(redirectAttributes, "workflow.task.finish.error.workflow.task.save.failed");
            return;
        }
        task.setComment(taskToFinish.getComment());
        try {
            workflowService.saveInProgressTask(task);
            addRedirectInfoMsg(redirectAttributes, "save.success");
        } catch (WorkflowChangedException e) {
            addRedirectErrorMsg(redirectAttributes, "workflow.task.finish.error.workflow.task.save.failed");
        }
    }

    private boolean finishTask(Task taskToFinish, QName taskType, int outcomeIndex, RedirectAttributes redirectAttributes) {
        ee.webmedia.alfresco.workflow.service.Task task = workflowService.getTaskWithParents(taskToFinish.getNodeRef());
        if (task == null || !task.isStatus(Status.IN_PROGRESS)) {
            addRedirectErrorMsg(redirectAttributes, "workflow.task.finish.error.workflow.task.save.failed");
            return false;
        }
        task.setComment(taskToFinish.getComment());

        if (WorkflowSpecificModel.Types.REVIEW_TASK.equals(taskType) || WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK.equals(taskType)) {
            Integer nodeOutcome = taskToFinish.getReviewTaskOutcome();
            if (nodeOutcome != null) {
                outcomeIndex = nodeOutcome;
            }
        }

        // TODO: implement
        // else if (task.isType(WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK) && outcomeIndex == DueDateExtensionWorkflowType.DUE_DATE_EXTENSION_OUTCOME_NOT_ACCEPTED) {
        // task.setConfirmedDueDate(null);
        // }

        List<Pair<String, String>> validationMsgs = null;
        if ((validationMsgs = WorkflowBlockBean.validate(task, outcomeIndex)) != null) {
            for (Pair<String, String> validationMsg : validationMsgs) {
                if (validationMsg.getSecond() == null) {
                    addRedirectErrorMsg(redirectAttributes, validationMsg.getFirst());
                } else {
                    addRedirectErrorMsg(redirectAttributes, validationMsg.getFirst(), validationMsg.getSecond());
                }

            }
            return false;
        }
        // finish the task
        try {
            workflowService.finishInProgressTask(task, outcomeIndex);
            addRedirectInfoMsg(redirectAttributes, "workflow.task.finish.success");
            return true;
        } catch (InvalidNodeRefException e) {
            addRedirectErrorMsg(redirectAttributes, "workflow.task.finish.error.docDeleted");
        } catch (NodeLockedException e) {
            LOG.error("Finishing task failed", e);
            Pair<String, Object[]> messageKeyAndValueHolders = DocumentLockHelperBean.getErrorMessageKeyAndValueHolders(
                    "workflow.task.finish.error.document.registerDoc.docLocked",
                    e.getNodeRef());
            addRedirectErrorMsg(redirectAttributes, messageKeyAndValueHolders.getFirst(), messageKeyAndValueHolders.getSecond());
        } catch (WorkflowChangedException e) {
            CompoundWorkflowDialog.logWorkflowChangedException(e, "Finishing task failed", LOG);
            addRedirectErrorMsg(redirectAttributes, "workflow.task.finish.error.workflow.task.save.failed");
        } catch (WorkflowActiveResponsibleTaskException e) {
            LOG.debug("Finishing task failed: more than one active responsible task!", e);
            addRedirectErrorMsg(redirectAttributes, "workflow.compound.save.failed.responsible");
        }
        return false;
    }

    private MobileSigningFlowContainer getSigningFlow(InProgressTasksForm inProgressTasksForm, Long signingFlowId) {
        if (inProgressTasksForm != null) {
            return signingFlowHolder.getSigningFlow(signingFlowId);
        }
        return null;
    }

    @RequestMapping(value = COMPOUND_WORKFLOW_DETAILS_MAPPING + "/ajax/get-signature", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getMobileIdSignature(HttpServletResponse response, @RequestBody MobileIdSignatureAjaxRequest requestParams) {
        SigningFlowContainer signingFlow = signingFlowHolder.getSigningFlow(requestParams.getSigningFlowId());
        if (signingFlow == null) {
            return SigningFlowContainer.handleInvalidSigantureState();
        }
        return signingFlow.getMobileIdSignature(requestParams.getMobileIdChallengeId());
    }

    public static String redirectToCompoundWorkflow(String compoundWorkflowNodeId) {
        return "redirect:/m/" + COMPOUND_WORKFLOW_DETAILS_MAPPING + "/" + compoundWorkflowNodeId;
    }
}
