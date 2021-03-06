package ee.webmedia.mobile.alfresco.workflow;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocLockService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowDbService;
import static ee.webmedia.alfresco.workflow.web.WorkflowBlockBean.isMobileIdOutcome;
import static ee.webmedia.alfresco.workflow.web.WorkflowBlockBean.isMobileIdOutcomeAndMobileIdDisabled;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.app.servlet.UploadFileBaseServlet;
import org.alfresco.web.bean.FileUploadBean;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ee.webmedia.alfresco.common.listener.ExternalAccessPhaseListener;
import ee.webmedia.alfresco.common.propertysheet.upload.UploadFileInput.FileWithContentType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.web.DocumentLockHelperBean;
import ee.webmedia.alfresco.document.file.model.File;
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
import ee.webmedia.alfresco.workflow.service.SignatureTask;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowConstantsBean;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import ee.webmedia.alfresco.workflow.web.CompoundWorkflowDialog;
import ee.webmedia.alfresco.workflow.web.SigningFlowContainer;
import ee.webmedia.alfresco.workflow.web.WorkflowBlockBean;
import ee.webmedia.mobile.alfresco.common.AbstractBaseController;
import ee.webmedia.mobile.alfresco.workflow.model.DueDateExtensionForm;
import ee.webmedia.mobile.alfresco.workflow.model.InProgressTasksForm;
import ee.webmedia.mobile.alfresco.workflow.model.LockMessage;
import ee.webmedia.mobile.alfresco.workflow.model.MobileIdSignatureAjaxRequest;
import ee.webmedia.mobile.alfresco.workflow.model.Task;
import ee.webmedia.mobile.alfresco.workflow.model.TaskFile;

@Controller
public class CompundWorkflowDetailsController extends AbstractCompoundWorkflowController {

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
    @Resource
    private WorkflowDbService workflowDbService;
    @Resource
    private DocumentTemplateService documentTemplateService;
    @Resource
    private WorkflowConstantsBean workflowConstantsBean;

    @RequestMapping(value = COMPOUND_WORKFLOW_DETAILS_MAPPING + "/{compoundWorkflowNodeId}", method = GET)
    public String setupCompoundWorkflow(@PathVariable String compoundWorkflowNodeId, Model model, HttpServletRequest request, RedirectAttributes redirectAttributes,
            HttpSession session) {
        super.setup(model, request);
        Long signingFlowId = (Long) model.asMap().get(SIGNING_FLOW_ID_ATTR);
        return initCompoundWorkflow(compoundWorkflowNodeId, model, signingFlowId, redirectAttributes, session, request);
    }

    // compoundWorkflow is initialized here, not in @ModelAttribute method, because it may be null
    private String initCompoundWorkflow(String compoundWorkflowNodeId, Model model, Long signingFlowId, RedirectAttributes redirectAttributes, HttpSession session,
            HttpServletRequest request) {
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
        Map<String, Task> myTasksMap = new LinkedHashMap<>(); // Must preserve insertion order
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
            formTask.setTypeStr(TASK_TYPE_TO_KEY_MAPPING.get(taskType));
            formTask.setType(taskType);
            if (!Boolean.TRUE.equals(task.getViewedByOwner())) {
                getWorkflowDbService().updateTaskSingleProperty(task, WorkflowCommonModel.Props.VIEWED_BY_OWNER, Boolean.TRUE, task.getWorkflowNodeRef());
            }
            myTasksMap.put(task.getNodeRef().toString(), formTask);
            if (signatureTask != null && task.getNodeRef().equals(signatureTask.getNodeRef())) {
                formTask.setComment(signatureTask.getComment());
            }
            String saveLabel = buttonLabelPrefix + taskType.getLocalName() + ".save.title";
            String label = buttonLabelPrefix + taskType.getLocalName() + ".outcome.";
            List<Pair<String, String>> taskOutcomeBtnLabels = new ArrayList<>();
            if (task.isType(WorkflowSpecificModel.Types.OPINION_TASK,
                    WorkflowSpecificModel.Types.REVIEW_TASK,
                    WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK,
                    WorkflowSpecificModel.Types.CONFIRMATION_TASK)) {
                taskOutcomeBtnLabels.add(Pair.newInstance(ACTION_SAVE, saveLabel));
            }
            if (task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK, WorkflowSpecificModel.Types.REVIEW_TASK, WorkflowSpecificModel.Types.OPINION_TASK)) {
                formTask.setCommentLabel("workflow.task." + taskType.getLocalName() + ".prop.comment");
            } else {
                formTask.setCommentLabel("workflow.task.prop.comment");
            }
            if (task.isType(WorkflowSpecificModel.Types.OPINION_TASK)) {
                addTaskFiles(session, request, task, formTask);
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
            if (isDelegatableTask(task)) {
                String buttonKey = task.isType(WorkflowSpecificModel.Types.INFORMATION_TASK) ? "workflow.task.delegation.button.delegate.informationTask"
                        : "workflow.task.delegation.button.delegate";
                taskOutcomeBtnLabels.add(Pair.newInstance(DELEGATE_TASK, buttonKey));
            }
            if (task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK)) {
                delegableTasks.add(task);
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
            inProgressTasksForm.setDefaultSigningNumber(signingFlow.isDefaultTelephoneForSigning());
        }
        model.addAttribute("inProgressTasksForm", inProgressTasksForm);
        model.addAttribute("taskOutcomeButtons", taskOutcomeButtons);
        model.addAttribute("compoundWorkflow", compoundWorkflow);
        model.addAttribute("reviewTaskOutcomes", getReviewTaskOutcomes());
        if (workflowConstantsBean.isWorkflowTitleEnabled()) {
            model.addAttribute("compoundWorkflowTitle", compoundWorkflow.getTitle());
        }

        setupComments(model, compoundWorkflowNodeId);
        setupObjects(model, compoundWorkflow);
        setupOpinions(model, compoundWorkflow);
        setupRelatedUrls(model, compoundWorkflowNodeRef);
        setupWorkflowBlock(model, compoundWorkflow);
        if (!delegableTasks.isEmpty()) {
            setupDelegationHistoryBlock(model, delegableTasks);
        }
        BeanHelper.getLogService().addLogEntry(LogEntry.create(LogObject.COMPOUND_WORKFLOW, getUserService(), compoundWorkflowNodeRef, "applog_compoundWorkflow_view"));
        return COMPOUND_WORKFLOW_DETAILS_MAPPING;
    }

    private boolean isDelegatableTask(ee.webmedia.alfresco.workflow.service.Task task) {
        return task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK)
                || task.isType(WorkflowSpecificModel.Types.INFORMATION_TASK) && workflowConstantsBean.isInformationWorkflowDelegationEnabled()
                || task.isType(WorkflowSpecificModel.Types.OPINION_TASK) && workflowConstantsBean.isOpinionWorkflowDelegationEnabled()
                || task.isType(WorkflowSpecificModel.Types.REVIEW_TASK) && workflowConstantsBean.isReviewWorkflowDelegationEnabled();
    }

    private void addTaskFiles(HttpSession session, HttpServletRequest request, ee.webmedia.alfresco.workflow.service.Task task, Task formTask) {
        workflowService.loadTaskFiles(task);
        List<TaskFile> files = new ArrayList<>();
        formTask.setFiles(files);
        for (Object file : task.getFiles()) {
            if (file instanceof File) {
                File f = ((File) file);
                files.add(new TaskFile(f.getName(), f.getNodeRef()));
            }
        }
        FileUploadBean uploadBean = getFileUploadBean(session);
        if (uploadBean != null && CollectionUtils.isNotEmpty(uploadBean.getTaskRefs())) {
            NodeRef taskRef = task.getNodeRef();
            List<NodeRef> taskRefs = uploadBean.getTaskRefs();
            for (int i = 0; i < taskRefs.size(); i++) {
                NodeRef ref = taskRefs.get(i);
                if (taskRef.equals(ref)) {
                    TaskFile f = new TaskFile(uploadBean.getFileNames().get(i), null);
                    String deleteUrl = UploadFileBaseServlet.generateDeleteUrl(request, uploadBean.getFiles().get(i).getName());
                    f.setDeleteUrl(deleteUrl);
                    files.add(f);
                }
            }
        }
    }

    private void setupWorkflowBlock(Model model, CompoundWorkflow compoundWorkflow) {
        List<WorkflowBlockItem> groupedWorkflowBlockItems = getWorkflowDbService().getWorkflowBlockItems(Arrays.asList(compoundWorkflow.getNodeRef()), null, null);
        setMessageSource(groupedWorkflowBlockItems);
        model.addAttribute(WORKFLOW_BLOCK_ITEMS_ATTR, groupedWorkflowBlockItems);
        model.addAttribute(TASK_COUNT_ATTR, WorkflowUtil.getTaskCount(compoundWorkflow));
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

    private void setupOpinions(Model model, CompoundWorkflow compoundWorkflow) {
        List<Task> finishedOpinionTasks = new ArrayList<>();
        Set<NodeRef> taskRefs = new HashSet<>();
        for (Workflow wf : compoundWorkflow.getWorkflows()) {
            if (!wf.isType(WorkflowSpecificModel.Types.OPINION_WORKFLOW)) {
                continue;
            }
            for (ee.webmedia.alfresco.workflow.service.Task task : wf.getTasks()) {
                if (task.isStatus(Status.FINISHED)) {
                    taskRefs.add(task.getNodeRef());
                    Task t = new Task(task);
                    t.setCompletedDateTime(task.getCompletedDateTime());
                    t.setOwnerNameWithSubstitute(task.getOwnerNameWithSubstitute());
                    t.setCommentAndLinks(task.getCommentAndLinks());
                    finishedOpinionTasks.add(t);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(finishedOpinionTasks)) {
            Map<NodeRef, List<File>> taskToFiles = workflowService.loadTaskFilesFromCompoundWorkflow(taskRefs, compoundWorkflow.getNodeRef());
            for (Task task : finishedOpinionTasks) {
                if (taskToFiles.containsKey(task.getNodeRef())) {
                    List<TaskFile> taskFiles = new ArrayList<>();
                    for (File f : taskToFiles.get(task.getNodeRef())) {
                        taskFiles.add(new TaskFile(f));
                    }
                    task.setFiles(taskFiles);
                }
            }
            Collections.sort(finishedOpinionTasks, new TaskDueDateComparator());
        }
        model.addAttribute("opinions", finishedOpinionTasks);
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
    
    @RequestMapping(value = "/ajax/cwf/locktask", method = POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public LockMessage setCompoundWorkflowLockTask(@RequestBody LockMessage message) throws ParseException {
        
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
    
    @RequestMapping(value = COMPOUND_WORKFLOW_DETAILS_MAPPING + "/{compoundWorkflowNodeId}/{taskId}/extension", method = GET)
    public String dueDateExtension(@PathVariable String compoundWorkflowNodeId, @PathVariable String taskId, Model model, HttpServletRequest request) {
        super.setupWithoutSidebarMenu(model, request);
        DueDateExtensionForm form = new DueDateExtensionForm();
        NodeRef cwfRef = WebUtil.getNodeRefFromNodeId(compoundWorkflowNodeId);
        form.setCompoundWorkflowRef(cwfRef);
        NodeRef taskRef = new NodeRef(cwfRef.getStoreRef(), taskId);
        String taskCreatorId = (String) getWorkflowDbService().getTaskProperty(taskRef, WorkflowSpecificModel.Props.CREATOR_ID);
        String currentUser = AuthenticationUtil.getRunAsUser();
        if (currentUser != null && !currentUser.equals(taskCreatorId)) {
            form.setUserId(taskCreatorId);
            String taskCreatorName = getUserService().getUserFullName(taskCreatorId);
            form.setUserName(taskCreatorName);
        }
        Date date = CalendarUtil.addWorkingDaysToDate(new LocalDate(), 2, BeanHelper.getClassificatorService()).toDateTimeAtCurrentTime().toDate();
        form.setInitialExtensionDueDate(DATE_FORMAT.format(date));
        model.addAttribute("dueDateExtensionForm", form);

        return "compound-workflow/due-date-extension";
    }

    @RequestMapping(value = COMPOUND_WORKFLOW_DETAILS_MAPPING + "/{compoundWorkflowNodeId}/{taskId}/extension", method = POST)
    public String processDueDateExtensionSubmit(@PathVariable String compoundWorkflowNodeId, @PathVariable String taskId, @ModelAttribute DueDateExtensionForm form,
            RedirectAttributes redirectAttributes) {
        NodeRef cwfRef = WebUtil.getNodeRefFromNodeId(compoundWorkflowNodeId);
        NodeRef taskRef = new NodeRef(cwfRef.getStoreRef(), taskId);
        String userId = form.getUserId();
        String extenderUserFullname = getUserService().getUserFullName(userId);
        ee.webmedia.alfresco.workflow.service.Task initiatingTask = workflowService.getTaskWithParents(taskRef);
        String extenderEmail = getUserService().getUserEmail(userId);
        
     // set lock
        boolean locked = (compoundWorkflowNodeId != null)?setLock(new NodeRef(cwfRef.getStoreRef(), compoundWorkflowNodeId), "workflow_compond.locked", redirectAttributes):false;
        if (locked) {
	        try {
		        workflowService.createDueDateExtension(
		                form.getReason(),
		                form.getNewDueDate(),
		                form.getExtensionDueDate(),
		                initiatingTask,
		                cwfRef,
		                userId,
		                extenderUserFullname,
                        extenderEmail);
		
		        addRedirectInfoMsg(redirectAttributes, "workflow.task.dueDate.extension.submitted");
	        } finally {
	        	// unlock
	        	if (compoundWorkflowNodeId != null) {
        			getDocLockService().unlockIfOwner(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, compoundWorkflowNodeId));
        		}
	        }
	        
	        return redirectToCompoundWorkflow(compoundWorkflowNodeId);
        } else {
        	return redirectToDueDateExtensionView(compoundWorkflowNodeId, taskId);
        }
        
    }

    @RequestMapping(value = COMPOUND_WORKFLOW_DETAILS_MAPPING + "/{compoundWorkflowNodeId}", method = POST)
    public String processSubmit(@PathVariable String compoundWorkflowNodeId, @ModelAttribute InProgressTasksForm inProgressTasksForm, RedirectAttributes redirectAttributes,
            HttpSession session) {
        if (inProgressTasksForm == null) {
            return redirectToCompoundWorkflow(compoundWorkflowNodeId);
        }
        NodeRef cwfRef = WebUtil.getNodeRefFromNodeId(compoundWorkflowNodeId);
        // set lock
        boolean locked = (compoundWorkflowNodeId != null)?setLock(new NodeRef(cwfRef.getStoreRef(), compoundWorkflowNodeId), "workflow_compond.locked", redirectAttributes):false;
        if (locked) {
	        try {
		        Map<String, String> formActions = inProgressTasksForm.getActions();
		        if (!formActions.isEmpty()) {
		            boolean redirectToTaskList = continueCurrentSigning(inProgressTasksForm, redirectAttributes, formActions, session);
		            if (redirectToTaskList) {
		                return redirectToTaskList(WorkflowSpecificModel.Types.SIGNATURE_TASK);
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
		                        return redirectToTaskDelegation(compoundWorkflowNodeId, task);
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
		            saveTask(taskToFinish, redirectAttributes, session);
		        } else if (extensionTaskRef != null) {
		            return redirectToDueDateExtensionView(compoundWorkflowNodeId, extensionTaskRef.getId());
		        }
		        else {
		            QName taskType = workflowService.getNodeRefType(taskToFinish.getNodeRef());
		            if (isMobileIdOutcome(taskType, outcomeIndex)) {
		                startSigning(inProgressTasksForm, redirectAttributes, taskToFinish, session);
		            } else {
		                boolean finishSuccess = finishTask(taskToFinish, taskType, outcomeIndex, redirectAttributes, session);
		                if (finishSuccess) {
		                    return redirectToTaskList(taskType);
		                }
		            }
		        }
		        
	        } finally {
	        	// unlock
	        	if (compoundWorkflowNodeId != null) {
        			getDocLockService().unlockIfOwner(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, compoundWorkflowNodeId));
        		}
	        }
    	}
        return redirectToCompoundWorkflow(compoundWorkflowNodeId);
    }

    private String redirectToDueDateExtensionView(String compoundWorkflowNodeId, String taskId) {
        return redirectToCompoundWorkflow(compoundWorkflowNodeId) + "/" + taskId + "/extension";
    }

    private String redirectToTaskDelegation(String compoundWorkflowNodeId, Task task) {
        String typeStr = task.getTypeStr();
        if (typeStr == null || !TASK_TYPE_MAPPING.containsKey(typeStr)) {
            return redirectToTaskList();
        }
        // String string = redirectToCompoundWorkflow(compoundWorkflowNodeId) + "/" + task.getNodeRef().getId() + "/delegation/" + typeStr;
        return redirectToCompoundWorkflow(compoundWorkflowNodeId) + String.format("/%s/delegation/%s", task.getNodeRef().getId(), typeStr);
        // return string;
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
                session.setAttribute(SigningFlowContainer.LAST_USED_MOBILE_ID_NUMBER, phoneNumber);
                signingFlow.setDefaultTelephoneForSigning(inProgressTasksForm.isDefaultSigningNumber());
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

    private void saveTask(Task taskToFinish, RedirectAttributes redirectAttributes, HttpSession session) {
        ee.webmedia.alfresco.workflow.service.Task task = workflowService.getTaskWithParents(taskToFinish.getNodeRef());
        if (!task.isStatus(Status.IN_PROGRESS)) {
            addRedirectErrorMsg(redirectAttributes, "workflow.task.finish.error.workflow.task.save.failed");
            return;
        }
        task.setComment(taskToFinish.getComment());
        List<Integer> uploadedFileIndexes = addUploadedFilesAndRemoveDeletedFiles(taskToFinish, session, task);
        try {
            workflowService.saveInProgressTask(task);
            addRedirectInfoMsg(redirectAttributes, "save.success");
            if (CollectionUtils.isNotEmpty(uploadedFileIndexes)) {
                getFileUploadBean(session).removeFiles(uploadedFileIndexes);
                addRedirectInfoMsg(redirectAttributes, "workflow.task.type.opinionTask.files.uploaded");
            }
        } catch (WorkflowChangedException e) {
            addRedirectErrorMsg(redirectAttributes, "workflow.task.finish.error.workflow.task.save.failed");
        }
    }

    private List<Integer> addUploadedFilesAndRemoveDeletedFiles(Task taskToFinish, HttpSession session, ee.webmedia.alfresco.workflow.service.Task task) {
        List<Integer> uploadedFileIndexes = null;
        if (task.isType(WorkflowSpecificModel.Types.OPINION_TASK)) {
            uploadedFileIndexes = addUploadedFiles(session, task);
            removeDeletedFiles(taskToFinish.getFiles(), task);
        }
        return uploadedFileIndexes;
    }

    private void removeDeletedFiles(List<TaskFile> files, ee.webmedia.alfresco.workflow.service.Task task) {
        if (CollectionUtils.isEmpty(files)) {
            return;
        }
        for (TaskFile file : files) {
            if (file.isDeleted()) {
                task.getRemovedFiles().add(file.getNodeRef());
            }
        }
    }

    private List<Integer> addUploadedFiles(HttpSession session, ee.webmedia.alfresco.workflow.service.Task task) {
        FileUploadBean fileBean = getFileUploadBean(session);
        if (fileBean != null && CollectionUtils.isNotEmpty(fileBean.getFiles())) {
            List<java.io.File> files = fileBean.getFiles();
            List<String> contentTypes = fileBean.getContentTypes();
            List<String> names = fileBean.getFileNames();
            List<NodeRef> taskRefs = fileBean.getTaskRefs();
            NodeRef originalTaskRef = task.getNodeRef();
            List<Integer> addedFilesIndexes = new ArrayList<>();
            for (int i = 0; i < files.size(); i++) {
                NodeRef taskRef = taskRefs.get(i);
                if (!originalTaskRef.equals(taskRef)) {
                    continue;
                }
                FileWithContentType file = new FileWithContentType(files.get(i), contentTypes.get(i), names.get(i));
                task.addUploadedFile(file);
                addedFilesIndexes.add(i);
            }
            return addedFilesIndexes;
        }
        return Collections.emptyList();
    }

    private FileUploadBean getFileUploadBean(HttpSession session) {
        return (FileUploadBean) session.getAttribute(FileUploadBean.FILE_UPLOAD_BEAN_NAME);
    }

    private boolean finishTask(Task taskToFinish, QName taskType, int outcomeIndex, RedirectAttributes redirectAttributes, HttpSession session) {
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
                task.setProp(WorkflowSpecificModel.Props.TEMP_OUTCOME, outcomeIndex);
            }
        }

        List<Integer> uploadedFileIndexes = addUploadedFilesAndRemoveDeletedFiles(taskToFinish, session, task);
        if (WorkflowSpecificModel.Types.OPINION_TASK.equals(taskType) && CollectionUtils.isEmpty(uploadedFileIndexes)) {
            workflowService.loadTaskFiles(task);
        }

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
            if (task.isType(WorkflowSpecificModel.Types.OPINION_TASK) && CollectionUtils.isNotEmpty(task.getFiles())) {
                session.removeAttribute(FileUploadBean.FILE_UPLOAD_BEAN_NAME);
                addRedirectInfoMsg(redirectAttributes, "workflow.task.type.opinionTask.files.uploaded.and.visible");
            }
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

    @RequestMapping(value = COMPOUND_WORKFLOW_DETAILS_MAPPING + "/ajax/get-signature", method = POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getMobileIdSignature(HttpServletResponse response, @RequestBody MobileIdSignatureAjaxRequest requestParams) {
        SigningFlowContainer signingFlow = signingFlowHolder.getSigningFlow(requestParams.getSigningFlowId());
        if (signingFlow == null) {
            return SigningFlowContainer.handleInvalidSigantureState();
        }
        return signingFlow.getMobileIdSignature(requestParams.getMobileIdChallengeId());
    }

    private class TaskDueDateComparator implements Comparator<Task> {

        @Override
        public int compare(Task t1, Task t2) {
            if (t1.getCompletedDateTime().before(t2.getCompletedDateTime())) {
                return -1;
            } else if (t2.getCompletedDateTime().before(t1.getCompletedDateTime())) {
                return 1;
            }
            return 0;
        }

    }
    
    

}
