package ee.webmedia.mobile.alfresco.workflow;

import static ee.webmedia.alfresco.workflow.web.WorkflowBlockBean.isMobileIdOutcome;
import static ee.webmedia.alfresco.workflow.web.WorkflowBlockBean.isMobileIdOutcomeAndMobileIdDisabled;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;
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

import ee.webmedia.alfresco.common.listener.ExternalAccessPhaseListener;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.web.DocumentLockHelperBean;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
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
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import ee.webmedia.alfresco.workflow.web.CompoundWorkflowDialog;
import ee.webmedia.alfresco.workflow.web.SigningFlowContainer;
import ee.webmedia.alfresco.workflow.web.WorkflowBlockBean;
import ee.webmedia.mobile.alfresco.HomeController;
import ee.webmedia.mobile.alfresco.common.AbstractBaseController;
import ee.webmedia.mobile.alfresco.workflow.model.InProgressTasksForm;
import ee.webmedia.mobile.alfresco.workflow.model.MobileIdSignatureAjaxRequest;
import ee.webmedia.mobile.alfresco.workflow.model.Task;

@Controller
public class CompundWorkflowDetailsController extends AbstractBaseController {

    private static final String TASK_COUNT_ATTR = "taskCount";

    private static final String WORKFLOW_BLOCK_ITEMS_ATTR = "workflowBlockItems";

    private static org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(CompundWorkflowDetailsController.class);

    private static final String ACTION_SAVE = "save";
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
        List<CompoundWorkflow> compoundWorkflows = Arrays.asList(compoundWorkflow);
        List<ee.webmedia.alfresco.workflow.service.Task> myTasks = workflowService.getMyTasksInProgress(compoundWorkflows);
        Map<String, Task> myTasksMap = new HashMap<String, Task>();
        Map<NodeRef, List<Pair<String, String>>> taskOutcomeButtons = new HashMap<NodeRef, List<Pair<String, String>>>();
        SigningFlowContainer signingFlow = null;
        SignatureTask signatureTask = null;
        if (signingFlowId != null) {
            signingFlow = signingFlowHolder.getSigningFlow(signingFlowId);
            signatureTask = signingFlow.getSignatureTask();
        }
        String buttonLabelPrefix = "workflow.task.type.";
        for (ee.webmedia.alfresco.workflow.service.Task task : myTasks) {
            ee.webmedia.mobile.alfresco.workflow.model.Task formTask = new ee.webmedia.mobile.alfresco.workflow.model.Task(task);
            if (!task.isType(WorkflowSpecificModel.Types.SIGNATURE_TASK, WorkflowSpecificModel.Types.REVIEW_TASK)) {
                continue;
            }
            if (!Boolean.TRUE.equals(task.getViewedByOwner())) {
                BeanHelper.getWorkflowDbService().updateTaskSingleProperty(task, WorkflowCommonModel.Props.VIEWED_BY_OWNER, Boolean.TRUE);
            }
            myTasksMap.put(task.getNodeRef().toString(), formTask);
            if (signatureTask != null && task.getNodeRef().equals(signatureTask.getNodeRef())) {
                formTask.setComment(signatureTask.getComment());
            }
            QName taskType = task.getType();
            String saveLabel = buttonLabelPrefix + taskType.getLocalName() + ".save.title";
            String label = buttonLabelPrefix + taskType.getLocalName() + ".outcome.";
            List<Pair<String, String>> taskOutcomeBtnLabels = new ArrayList<Pair<String, String>>();
            if (task.isType(WorkflowSpecificModel.Types.OPINION_TASK,
                    WorkflowSpecificModel.Types.REVIEW_TASK,
                    WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK,
                    WorkflowSpecificModel.Types.CONFIRMATION_TASK)) {
                taskOutcomeBtnLabels.add(Pair.newInstance(ACTION_SAVE, saveLabel));
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
        if (BeanHelper.getWorkflowService().isWorkflowTitleEnabled()) {
            model.addAttribute("compoundWorkflowTitle", compoundWorkflow.getTitle());
        }

        setupComments(model, compoundWorkflowNodeId);
        setupObjects(model, compoundWorkflow);
        setupRelatedUrls(model, compoundWorkflowNodeRef);
        setupWorkflowBlock(model, compoundWorkflow);
        return COMPOUND_WORKFLOW_DETAILS_MAPPING;
    }

    private void setupWorkflowBlock(Model model, CompoundWorkflow compoundWorkflow) {
        List<WorkflowBlockItem> groupedWorkflowBlockItems = new ArrayList<WorkflowBlockItem>();
        WorkflowBlockBean.collectWorkflowBlockItems(Arrays.asList(compoundWorkflow), groupedWorkflowBlockItems, null, null);
        setMessageSource(groupedWorkflowBlockItems);
        model.addAttribute(WORKFLOW_BLOCK_ITEMS_ATTR, groupedWorkflowBlockItems);
        model.addAttribute(TASK_COUNT_ATTR, WorkflowUtil.getTaskCount(compoundWorkflow));
    }

    private void setMessageSource(List<WorkflowBlockItem> groupedWorkflowBlockItems) {
        for (WorkflowBlockItem workflowBlockItem : groupedWorkflowBlockItems) {
            workflowBlockItem.setMessageSource(messageSource);
            if (workflowBlockItem.isGroupBlockItem()) {
                setMessageSource(workflowBlockItem.getGroupItems());
            }
        }
    }

    private Map<Integer, String> getReviewTaskOutcomes() {
        if (reviewTaskOutcomes == null) {
            reviewTaskOutcomes = new HashMap<Integer, String>();
            int outcomes = workflowService.getWorkflowTypes().get(WorkflowSpecificModel.Types.REVIEW_WORKFLOW).getTaskOutcomes();
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
        // TODO: optimize loading compound workflow if possible (all data may not be needed in mobile version)
        CompoundWorkflow compoundWorkflow = workflowService.getCompoundWorkflow(compoundWorkflowNodeRef);

        List<WorkflowBlockItem> groupedWorkflowBlockItems = new ArrayList<WorkflowBlockItem>();
        List<WorkflowBlockItem> groupedWorkflowBlockItem = new ArrayList<WorkflowBlockItem>();
        WorkflowBlockBean.collectWorkflowBlockItems(Arrays.asList(compoundWorkflow), groupedWorkflowBlockItems, null, null);
        WorkflowBlockItem currentItem = null;
        OUTER: for (WorkflowBlockItem workflowBlockItem : groupedWorkflowBlockItems) {
            if (!workflowBlockItem.isGroupBlockItem()) {
                String workflowTaskId = workflowBlockItem.getTask().getNodeRef().getId();
                if (workflowTaskId.equals(taskId)) {
                    return redirectToCompoundWorkflow(compoundWorkflowId);
                }
            }
            else {
                for (WorkflowBlockItem taskItem : workflowBlockItem.getGroupItems()) {
                    String workflowTaskId = taskItem.getTask().getNodeRef().getId();
                    if (workflowTaskId.equals(taskId)) {
                        currentItem = workflowBlockItem;
                        groupedWorkflowBlockItem.addAll(workflowBlockItem.getGroupItems());
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
        for (Task task : inProgressTasksForm.getInProgressTasks().values()) {
            for (Map.Entry<String, String> entry : task.getActions().entrySet()) {
                if (StringUtils.isNotBlank(entry.getValue())) {
                    taskToFinish = task;
                    String taskAction = entry.getKey();
                    if (ACTION_SAVE.equals(taskAction)) {
                        saveOnly = true;
                        break;
                    }
                    outcomeIndex = Integer.valueOf(taskAction);
                    break;
                }
            }
        }

        if (taskToFinish == null || (outcomeIndex < 0 && !saveOnly)) {
            addRedirectErrorMsg(redirectAttributes, "workflow.task.finish.error.workflow.task.save.failed");
        } else if (saveOnly) {
            saveTask(taskToFinish, redirectAttributes);
        } else {
            QName taskType = workflowService.getNodeRefType(taskToFinish.getNodeRef());
            if (isMobileIdOutcome(taskType, outcomeIndex)) {
                startSigning(compoundWorkflowNodeId, inProgressTasksForm, redirectAttributes, taskToFinish, session);
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
        return "redirect:/m/tasks/" + getViewNameForTaskType(taskType);
    }

    private String getViewNameForTaskType(QName taskType) {
        // TODO: when TASK_TYPE_MAPPING is improved, use it to get proper view for task type
        return WorkflowSpecificModel.Types.REVIEW_TASK.equals(taskType) ? "review" : "signature";
    }

    public void startSigning(String compoundWorkflowNodeId, InProgressTasksForm inProgressTasksForm, RedirectAttributes redirectAttributes, Task taskToFinish, HttpSession session) {
        ee.webmedia.alfresco.workflow.service.Task task = workflowService.getTask(taskToFinish.getNodeRef(), false);
        if (task == null || !task.isStatus(Status.IN_PROGRESS)) {
            addRedirectErrorMsg(redirectAttributes, "workflow.task.finish.error.workflow.task.save.failed");
            return;
        }
        task.setComment(taskToFinish.getComment());
        MobileSigningFlowContainer signingFlow = new MobileSigningFlowContainer((SignatureTask) task, inProgressTasksForm, inProgressTasksForm.getCompoundWorkflowRef(),
                inProgressTasksForm.getContainerRef());
        boolean signingPrepared = signingFlow.prepareSigning(this, redirectAttributes);
        if (!signingPrepared) {
            return;
        }
        long signingFlowId = signingFlowHolder.addSigningFlow(signingFlow, session);
        redirectAttributes.addFlashAttribute(SIGNING_FLOW_ID_ATTR, signingFlowId);
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
