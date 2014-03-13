package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.parameters.model.Parameters.MAX_ATTACHED_FILE_SIZE;
import static ee.webmedia.alfresco.workflow.web.TaskListCommentComponent.TASK_INDEX;
import static ee.webmedia.alfresco.workflow.web.TaskListGenerator.WF_INDEX;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.config.DialogsConfigElement.DialogButtonConfig;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.einvoice.model.Transaction;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceUtil;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.web.DocumentDialog;
import ee.webmedia.alfresco.notification.exception.EmailAttachmentSizeLimitException;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.workflow.exception.WorkflowActiveResponsibleTaskException;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Task.Action;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import ee.webmedia.alfresco.workflow.service.type.WorkflowType;
import ee.webmedia.alfresco.workflow.web.TaskListCommentComponent.CommentEvent;
import ee.webmedia.alfresco.workflow.web.evaluator.WorkflowNewEvaluator;

/**
 * Dialog bean for working with one compound workflow instance which is tied to a document.
 */
public class CompoundWorkflowDialog extends CompoundWorkflowDefinitionDialog {

    private static final String CONTINUE_VALIDATED_WORKFLOW = "continueValidatedWorkflow";
    private static final String START_VALIDATED_WORKFLOW = "startValidatedWorkflow";
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "CompoundWorkflowDialog";

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(CompoundWorkflowDialog.class);

    private transient UserService userService;
    private transient DocumentService documentService;
    private transient DocumentLogService documentLogService;
    private transient ParametersService parametersService;
    private DocumentDialog documentDialog;

    private static final List<QName> knownWorkflowTypes = Arrays.asList(//
            WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW
            , WorkflowSpecificModel.Types.OPINION_WORKFLOW
            , WorkflowSpecificModel.Types.REVIEW_WORKFLOW
            , WorkflowSpecificModel.Types.INFORMATION_WORKFLOW
            , WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW
            );

    /**
     * @param propSheet
     * @return true if "{temp}workflowTasks" property should be shown on given propertySheet
     */
    public boolean showAssignmentWorkflowWorkflowTasks(UIPropertySheet propSheet) {
        final int index = (Integer) propSheet.getAttributes().get(TaskListGenerator.ATTR_WORKFLOW_INDEX);
        final Workflow workflow2 = getWorkflow().getWorkflows().get(index);
        final List<Task> tasks = workflow2.getTasks();
        for (Task task : tasks) {
            if (WorkflowUtil.isActiveResponsible(task)) {
                return true; // this workflow has at least one active responsibility task
            }
        }
        return false; // this workflow has no active responsibility tasks
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        boolean checkFinished = WorkflowUtil.isStatus(compoundWorkflow, Status.IN_PROGRESS);
        if (validate(context, checkFinished, false, false)) {
            try {
                removeEmptyTasks();
                getWorkflowService().saveCompoundWorkflow(compoundWorkflow);
                if (isUnsavedWorkFlow) {
                    getDocumentLogService().addDocumentLog(compoundWorkflow.getParent(), MessageUtil.getMessage("document_log_status_workflow"));
                    isUnsavedWorkFlow = false;
                }
                resetState();
                MessageUtil.addInfoMessage("save_success");
                return outcome;
            } catch (NodeLockedException e) {
                log.debug("Compound workflow action failed: document locked!", e);
                MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "workflow_compound_save_failed_docLocked");
            } catch (WorkflowChangedException e) {
                handleException(e, null);
            } catch (WorkflowActiveResponsibleTaskException e) {
                log.debug("Compound workflow action failed: more than one active responsible task!", e);
                MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "workflow_compound_save_failed_responsible");
            } catch (Exception e) {
                throw e;
            }
        }
        super.isFinished = false;
        return null;
    }

    /**
     * Action listener for JSP.
     */
    @Override
    public void setupWorkflow(ActionEvent event) {
        resetState();
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        if (!getNodeService().exists(nodeRef)) {
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addErrorMessage(context, "workflow_compound_edit_error_docDeleted");
            context.getApplication().getNavigationHandler().handleNavigation(context, null, getDefaultCancelOutcome());
            return;
        }
        compoundWorkflow = getWorkflowService().getCompoundWorkflow(nodeRef);
        updateFullAccess();
    }

    /**
     * Action listener for JSP.
     */
    @Override
    public void setupNewWorkflow(ActionEvent event) {
        resetState();
        NodeRef compoundWorkflowDefinition = new NodeRef(ActionUtil.getParam(event, "compoundWorkflowDefinitionNodeRef"));
        NodeRef document = getDocumentDialog().getNode().getNodeRef();
        try {
            compoundWorkflow = getWorkflowService().getNewCompoundWorkflow(compoundWorkflowDefinition, document);
            Workflow costManagerWorkflow = getCostManagerForkflow();
            if (costManagerWorkflow != null) {
                addCostManagerTasks(costManagerWorkflow);
            }
            updateFullAccess();
            isUnsavedWorkFlow = true;
        } catch (InvalidNodeRefException e) {
            log.warn("Failed to create a new compound workflow instance because someone has probably deleted the compound workflow definition.");
        }
    }

    private Workflow getCostManagerForkflow() {
        NodeRef docRef = compoundWorkflow.getParent();
        if (docRef == null || !DocumentSubtypeModel.Types.INVOICE.equals(BeanHelper.getNodeService().getType(docRef))) {
            return null;
        }
        Long costManagerWfIndex = BeanHelper.getParametersService().getLongParameter(Parameters.REVIEW_WORKFLOW_COST_MANAGER_WORKFLOW_NUMBER);
        if (costManagerWfIndex == null) {
            return null;
        }
        int reviewWfIndex = 0;
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            if (workflow.isType(WorkflowSpecificModel.Types.REVIEW_WORKFLOW)) {
                // parameter workflow index is 1-based (not 0-based)
                if (reviewWfIndex == costManagerWfIndex - 1) {
                    return workflow;
                }
                reviewWfIndex++;
            }
        }
        return null;
    }

    public void startWorkflow() {
        log.debug("startWorkflow");
        if (validate(FacesContext.getCurrentInstance(), true, false, true)) {
            List<String> confirmationMessages = getConfirmationMessages();
            if (confirmationMessages != null && !confirmationMessages.isEmpty()) {
                updatePanelGroup(confirmationMessages, START_VALIDATED_WORKFLOW);
                return;
            }
            startValidatedWorkflow(null);
        }
    }

    /**
     * This method assumes that workflows has been validated
     */
    public void startValidatedWorkflow(ActionEvent event) {
        try {
            removeEmptyTasks();
            if (isUnsavedWorkFlow) {
                getDocumentLogService().addDocumentLog(compoundWorkflow.getParent(), MessageUtil.getMessage("document_log_status_workflow"));
            }
            compoundWorkflow = getWorkflowService().saveAndStartCompoundWorkflow(compoundWorkflow);
            if (isUnsavedWorkFlow) {
                isUnsavedWorkFlow = false;
            }
            MessageUtil.addInfoMessage("workflow_compound_start_success");
        } catch (Exception e) {
            handleException(e, "workflow_compound_start_workflow_failed");
        }
        updatePanelGroup();
    }

    private List<String> getConfirmationMessages() {
        NodeService nodeService = BeanHelper.getNodeService();
        NodeRef docRef = compoundWorkflow.getParent();
        if (!DocumentSubtypeModel.Types.INVOICE.equals(nodeService.getType(docRef))) {
            return null;
        }
        Date invoiceDueDate = (Date) nodeService.getProperty(docRef, DocumentSpecificModel.Props.INVOICE_DUE_DATE);
        if (invoiceDueDate == null) {
            return null;
        }
        List<String> messages = new ArrayList<String>();
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            for (Task task : workflow.getTasks()) {
                Date taskDueDate = task.getDueDate();
                if (taskDueDate != null && invoiceDueDate != null) {
                    Date invoiceDueDateMinus3Days = DateUtils.addDays(invoiceDueDate, -3);
                    if (!DateUtils.isSameDay(invoiceDueDateMinus3Days, taskDueDate) && taskDueDate.after(invoiceDueDateMinus3Days)) {
                        FacesContext fc = FacesContext.getCurrentInstance();
                        DateFormat dateFormat = Utils.getDateFormat(fc);
                        String invoiceTaskDueDateConfirmationMsg = MessageUtil.getMessage("task_confirm_invoice_task_due_date",
                                MessageUtil.getMessage(workflow.getType().getLocalName()),
                                dateFormat.format(taskDueDate), dateFormat.format(invoiceDueDate));
                        messages.add(invoiceTaskDueDateConfirmationMsg);
                    }
                }
            }
        }
        return messages;
    }

    /**
     * Action listener for JSP.
     */
    public void stopWorkflow(ActionEvent event) {
        log.debug("stopWorkflow");
        try {
            removeEmptyTasks();
            if (validate(FacesContext.getCurrentInstance(), false, true, false)) {
                compoundWorkflow = getWorkflowService().saveAndStopCompoundWorkflow(compoundWorkflow);
                MessageUtil.addInfoMessage("workflow_compound_stop_success");
            }
        } catch (Exception e) {
            handleException(e, "workflow_compound_stop_workflow_failed");
        }
        updatePanelGroup();
    }

    /**
     * Action listener for JSP.
     */
    public void continueWorkflow(ActionEvent event) {
        log.debug("continueWorkflow");
        try {
            removeEmptyTasks();
            if (validate(FacesContext.getCurrentInstance(), true, false, true)) {
                List<String> confirmationMessages = getConfirmationMessages();
                if (confirmationMessages != null && !confirmationMessages.isEmpty()) {
                    updatePanelGroup(confirmationMessages, CONTINUE_VALIDATED_WORKFLOW);
                    return;
                }
                continueValidatedWorkflow(true);
            }
        } catch (Exception e) {
            handleException(e, "workflow_compound_continue_workflow_failed");
        }
    }

    /**
     * This method assumes that compound workflow has been validated
     */
    public void continueValidatedWorkflow(ActionEvent event) {
        continueValidatedWorkflow(false);
    }

    private void continueValidatedWorkflow(boolean throwException) {
        try {
            compoundWorkflow = getWorkflowService().saveAndContinueCompoundWorkflow(compoundWorkflow);
            MessageUtil.addInfoMessage("workflow_compound_continue_success");
        } catch (Exception e) {
            // let calling method handle error
            if (throwException) {
                throw new RuntimeException(e);
            }
            handleException(e, "workflow_compound_continue_workflow_failed");
        }
        updatePanelGroup();
    }

    /**
     * Action listener for JSP.
     */
    public void finishWorkflow(@SuppressWarnings("unused") ActionEvent event) {
        log.debug("finishWorkflow");
        try {
            removeEmptyTasks();
            compoundWorkflow = getWorkflowService().saveAndFinishCompoundWorkflow(compoundWorkflow);
            MessageUtil.addInfoMessage("workflow_compound_finish_success");
        } catch (Exception e) {
            handleException(e, "workflow_compound_finish_workflow_failed");
        }
        updatePanelGroup();
    }

    /**
     * Action listener for JSP.
     */
    public void copyWorkflow(@SuppressWarnings("unused") ActionEvent event) {
        log.debug("copyWorkflow");
        if (validate(FacesContext.getCurrentInstance(), false, false, false)) {
            try {
                removeEmptyTasks();
                compoundWorkflow = getWorkflowService().saveAndCopyCompoundWorkflow(compoundWorkflow);
            } catch (Exception e) {
                handleException(e, "workflow_compound_copy_workflow_failed");
            }
            updatePanelGroup();
        }
    }

    /**
     * Action for JSP.
     */
    public String deleteWorkflow() {
        log.debug("deleteWorkflow");
        try {
            removeEmptyTasks();
            getWorkflowService().deleteCompoundWorkflow(compoundWorkflow.getNode().getNodeRef());
            resetState();
            MessageUtil.addInfoMessage("workflow_compound_delete_compound_success");
            return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
        } catch (Exception e) {
            handleException(e, "workflow_compound_delete_workflow_failed");
            return null;
        }
    }

    /**
     * Callback method for workflow owner Search component.
     */
    public void setWorkfowOwner(String username) {
        compoundWorkflow.setOwnerId(username);
        compoundWorkflow.setOwnerName(getUserService().getUserFullName(username));
    }

    @Override
    public Object getActionsContext() {
        return getWorkflow();
    }

    /**
     * Action listener for JSP.
     */
    public void cancelWorkflowTask(ActionEvent event) {
        int wfIndex = ActionUtil.getParam(event, WF_INDEX, Integer.class);
        int taskIndex = ActionUtil.getParam(event, TASK_INDEX, Integer.class);
        log.debug("cancelWorkflowTask: " + wfIndex + ", " + taskIndex);
        Workflow block = compoundWorkflow.getWorkflows().get(wfIndex);
        Task task = block.getTasks().get(taskIndex);
        task.setAction(Action.UNFINISH);
        updatePanelGroup();
    }

    /**
     * Action listener for JSP.
     */
    public void finishWorkflowTask(ActionEvent event) {
        CommentEvent commentEvent = (CommentEvent) event;
        int index = (Integer) event.getComponent().getAttributes().get(TaskListGenerator.ATTR_WORKFLOW_INDEX);
        int taskIndex = commentEvent.taskIndex;
        String comment = commentEvent.comment;
        log.debug("finishWorkflowTask: " + index + ", " + taskIndex + ", " + comment);
        if (StringUtils.isBlank(comment)) {
            return;
        }

        Workflow block = compoundWorkflow.getWorkflows().get(index);
        Task task = block.getTasks().get(taskIndex);
        task.setAction(Action.FINISH);
        task.setComment(comment);
        updatePanelGroup();
    }

    // /// PROTECTED & PRIVATE METHODS /////

    @Override
    public List<DialogButtonConfig> getAdditionalButtons() {
        if (new WorkflowNewEvaluator().evaluate(compoundWorkflow)) {
            return Arrays.asList(new DialogButtonConfig("compound_workflow_start", null, "workflow_compound_start",
                    "#{CompoundWorkflowDialog.startWorkflow}", "false", null));
        }
        return Collections.<DialogButtonConfig> emptyList();
    }

    @Override
    protected TreeMap<String, QName> getSortedTypes() {
        if (sortedTypes == null) {
            NodeRef docRef = compoundWorkflow.getParent();
            Document doc = getDocumentService().getDocumentByNodeRef(docRef);
            WorkflowService workflowService = getWorkflowService();
            boolean isDocOwnerOrManager = StringUtils.equals(doc.getOwnerId(), AuthenticationUtil.getRunAsUser()) || getUserService().isDocumentManager();
            boolean isOwnerOfInProgressAssignmentTask = workflowService.isOwnerOfInProgressAssignmentTask(compoundWorkflow);
            boolean isOwnerOfInProgressExternalReviewTask = workflowService.isOwnerOfInProgressExternalReviewTask(compoundWorkflow);

            sortedTypes = new TreeMap<String, QName>();
            Map<QName, WorkflowType> workflowTypes = workflowService.getWorkflowTypes();
            for (QName wfType : workflowTypes.keySet()) {
                boolean addType = false;
                if (wfType.equals(WorkflowSpecificModel.Types.OPINION_WORKFLOW) || wfType.equals(WorkflowSpecificModel.Types.INFORMATION_WORKFLOW)) {
                    addType = true;
                } else if ((wfType.equals(WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW) || wfType.equals(WorkflowSpecificModel.Types.DOC_REGISTRATION_WORKFLOW)
                        || wfType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW))
                        && (isDocOwnerOrManager || isOwnerOfInProgressAssignmentTask)) {
                    addType = true;
                } else if (wfType.equals(WorkflowSpecificModel.Types.REVIEW_WORKFLOW)
                        && (isDocOwnerOrManager || isOwnerOfInProgressAssignmentTask || isOwnerOfInProgressExternalReviewTask)) {
                    addType = true;
                } else if (wfType.equals(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW) && workflowService.externalReviewWorkflowEnabled()) {
                    addType = true;
                }
                if (addType) {
                    String tmpName = MessageUtil.getMessage(wfType.getLocalName());
                    sortedTypes.put(tmpName, wfType);
                }
            }
        }
        return sortedTypes;
    }

    @Override
    protected String getConfigArea() {
        return null;
    }

    protected ParametersService getParametersService() {
        if (parametersService == null) {
            parametersService = (ParametersService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(ParametersService.BEAN_NAME);
        }
        return parametersService;
    }

    protected UserService getUserService() {
        if (userService == null) {
            userService = (UserService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(UserService.BEAN_NAME);
        }
        return userService;
    }

    protected DocumentService getDocumentService() {
        if (documentService == null) {
            documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(DocumentService.BEAN_NAME);
        }
        return documentService;
    }

    protected DocumentLogService getDocumentLogService() {
        if (documentLogService == null) {
            documentLogService = (DocumentLogService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(DocumentLogService.BEAN_NAME);
        }
        return documentLogService;
    }

    public DocumentDialog getDocumentDialog() {
        if (documentDialog == null) {
            documentDialog = (DocumentDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), DocumentDialog.BEAN_NAME);
        }
        return documentDialog;
    }

    @Override
    protected void updateFullAccess() {
        fullAccess = false;

        if (getUserService().isDocumentManager()) {
            fullAccess = true;
        } else if (getDocumentService().isDocumentOwner(compoundWorkflow.getParent(), AuthenticationUtil.getRunAsUser())) {
            fullAccess = true;
        } else if (StringUtils.equals(compoundWorkflow.getOwnerId(), AuthenticationUtil.getRunAsUser())) {
            fullAccess = true;
        } else if (StringUtils.equals(compoundWorkflow.getOwnerId(), AuthenticationUtil.getFullyAuthenticatedUser())) {
            // user is probably substituting someone else (but workFlow owner is still user that logged in)
            fullAccess = true;
        } else if (hasTask(AuthenticationUtil.getRunAsUser(), true)) {
            fullAccess = true;
        } else if (hasTask(AuthenticationUtil.getRunAsUser(), false)) {
            fullAccess = false;
        } else {
            throw new RuntimeException("Unknown user rights! Please check the condition rules in code!");
        }
    }

    private boolean hasTask(String user, boolean responsible) {
        for (Workflow block : compoundWorkflow.getWorkflows()) {
            for (Task task : block.getTasks()) {
                if (responsible && task.getNode().hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE)
                        && (Boolean) task.getNode().getProperties().get(WorkflowSpecificModel.Props.ACTIVE)) {
                    if (StringUtils.equals(task.getOwnerId(), user)) {
                        return true;
                    }
                }
                if (!responsible && !task.getNode().hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE)) {
                    if (WorkflowSpecificModel.Types.ASSIGNMENT_TASK.equals(task.getNode().getType())
                            && StringUtils.equals(task.getOwnerId(), user)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void handleException(Exception e, String failMsg) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (e instanceof WorkflowChangedException) {
            log.debug("Compound workflow action failed: data changed!", e);
            MessageUtil.addErrorMessage(context, "workflow_compound_save_failed");
        } else if (e instanceof WorkflowActiveResponsibleTaskException) {
            log.debug("Compound workflow action failed: more than one active responsible task!", e);
            MessageUtil.addErrorMessage(context, "workflow_compound_save_failed_responsible");
        } else if (e instanceof EmailAttachmentSizeLimitException) {
            log.debug("Compound workflow action failed: email attachment exceeded size limit set in parameter!", e);
            MessageUtil.addErrorMessage(context, "notification_zip_size_too_large", BeanHelper.getParametersService().getLongParameter(MAX_ATTACHED_FILE_SIZE));
        } else if (e instanceof NodeLockedException) {
            log.debug("Compound workflow action failed: document is locked!", e);
            MessageUtil.addErrorMessage(context, new String[] { failMsg, "document_error_docLocked" });
        } else if (e instanceof InvalidNodeRefException) {
            MessageUtil.addErrorMessage(context, "workflow_task_save_failed_docDeleted");
            context.getApplication().getNavigationHandler().handleNavigation(context, null, AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME);
        } else if (e instanceof UnableToPerformException) {
            MessageUtil.addStatusMessage(context, (UnableToPerformException) e);
        } else {
            log.error("Compound workflow action failed!", e);
            MessageUtil.addErrorMessage(context, failMsg);
        }
    }

    private boolean validate(FacesContext context, boolean checkFinished, boolean allowInactiveResponsibleTask, boolean checkInvoice) {
        boolean valid = true;
        boolean activeResponsibleAssignedInSomeWorkFlow = false;
        boolean missingOwnerAssignment = false;
        Boolean missingInformationTasks = null;
        Set<String> missingOwnerMessageKeys = null;
        boolean hasForbiddenFlowsForFinished = false;
        DueDateRegressionHelper regressionTest = new DueDateRegressionHelper();
        for (Workflow block : compoundWorkflow.getWorkflows()) {
            boolean foundOwner = false;
            QName blockType = block.getNode().getType();
            // isActiveResponsible check needs to be done only for ASSIGNMENT_WORKFLOW
            boolean activeResponsibleAssigneeNeeded = blockType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW)
                    && !activeResponsibleAssignedInSomeWorkFlow && !isActiveResponsibleAssignedForDocument(false);
            boolean activeResponsibleAssigneeAssigned = !activeResponsibleAssigneeNeeded;

            if (WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW.equals(blockType) ||
                    WorkflowSpecificModel.Types.REVIEW_WORKFLOW.equals(blockType) ||
                    WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW.equals(blockType) ||
                    WorkflowSpecificModel.Types.OPINION_WORKFLOW.equals(blockType)) {
                if (WorkflowUtil.isStatus(block, Status.NEW, Status.STOPPED)) {
                    hasForbiddenFlowsForFinished = true;
                }
            }
            if (block.isType(WorkflowSpecificModel.Types.REVIEW_WORKFLOW) && block.getNode().getProperties().get(WorkflowCommonModel.Props.PARALLEL_TASKS) == null) {
                valid = false;
                MessageUtil.addErrorMessage(context, "workflow_save_error_missingParallelOrNot");
            }

            for (Task task : block.getTasks()) {
                final boolean activeResponsible = WorkflowUtil.isActiveResponsible(task);
                boolean inactiveResponsible = false;
                if (allowInactiveResponsibleTask) {
                    inactiveResponsible = WorkflowUtil.isInactiveResponsible(task);
                }
                if (activeResponsibleAssigneeNeeded
                        && StringUtils.isNotBlank(task.getOwnerName())
                        && (activeResponsible || inactiveResponsible)) {
                    activeResponsibleAssignedInSomeWorkFlow = true;
                    activeResponsibleAssigneeAssigned = true;
                    missingOwnerAssignment = false;
                }
                if (!foundOwner) {
                    foundOwner = StringUtils.isNotBlank(task.getOwnerName());
                }
                if (activeResponsible) {
                    // both fields must be empty or filled
                    if (StringUtils.isBlank(task.getOwnerName()) != (task.getDueDate() == null)) {
                        valid = false;
                        String taskOwnerMsg = MessageUtil.getMessage(block.getNode().getType().getLocalName() + "_tasks");
                        MessageUtil.addErrorMessage(context, "task_name_and_due_required", taskOwnerMsg);
                        break;
                    }
                } else {
                    QName taskType = task.getNode().getType();
                    // only name is required for information tasks
                    if (taskType.equals(WorkflowSpecificModel.Types.INFORMATION_TASK)) {
                        if (StringUtils.isBlank(task.getOwnerName())) {
                            if (missingInformationTasks == null) {
                                missingInformationTasks = true; // delay showing error message
                            }
                        } else {
                            missingInformationTasks = false;
                        }
                    }
                    // institutionName and dueDate are required for externalReviewTask
                    else if (taskType.equals(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK)) {
                        if (StringUtils.isBlank(task.getInstitutionName()) != (task.getDueDate() == null)) {
                            String taskOwnerMsg = MessageUtil.getMessage(block.getNode().getType().getLocalName() + "_tasks");
                            MessageUtil.addErrorMessage(context, "task_name_and_due_required", taskOwnerMsg);
                            break;
                        }
                    }
                    // both fields must be filled
                    else if (StringUtils.isBlank(task.getOwnerName()) != (task.getDueDate() == null)) {
                        valid = false;
                        String suffix = "";
                        if (taskType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_TASK)) {
                            suffix = "_co";
                        }
                        String taskOwnerMsg = MessageUtil.getMessage(block.getNode().getType().getLocalName() + "_tasks" + suffix);
                        MessageUtil.addErrorMessage(context, "task_name_and_due_required", taskOwnerMsg);
                        break;
                    }
                }
                regressionTest.checkDueDate(task);
            }
            if (activeResponsibleAssigneeNeeded && !activeResponsibleAssigneeAssigned) {
                missingOwnerAssignment = true;
                if (!foundOwner) {
                    valid = false;
                    final String missingOwnerMessageKey = getMissingOwnerMessageKey(blockType);
                    if (missingOwnerMessageKeys == null) {
                        missingOwnerMessageKeys = new HashSet<String>(2);
                    }
                    missingOwnerMessageKeys.add(missingOwnerMessageKey);
                }
                continue;
            }
            if (Boolean.TRUE.equals(missingInformationTasks)) {
                valid = false;
                String taskOwnerMsg = MessageUtil.getMessage(block.getNode().getType().getLocalName() + "_tasks");
                MessageUtil.addErrorMessage(context, "task_name_required", taskOwnerMsg);
                break;
            }
            if (!foundOwner) {
                String missingOwnerMsgKey = getMissingOwnerMessageKey(blockType);
                if (missingOwnerMsgKey != null) {
                    MessageUtil.addErrorMessage(context, missingOwnerMsgKey);
                    valid = false;
                }
            }
        }
        valid &= regressionTest.valid;
        if (missingOwnerAssignment) {
            valid = false;
            MessageUtil.addErrorMessage(context, "workflow_save_error_missingOwner_assignmentWorkflow1");
        } else if (missingOwnerMessageKeys != null) {
            for (String msgKey : missingOwnerMessageKeys) {
                MessageUtil.addErrorMessage(context, msgKey);
            }
        }

        if (checkFinished && hasForbiddenFlowsForFinished) {
            String docStatus = (String) getDocumentDialog().getNode().getProperties().get(DocumentCommonModel.Props.DOC_STATUS);
            if (DocumentStatus.FINISHED.getValueName().equals(docStatus)) {
                valid = false;
                MessageUtil.addErrorMessage(context, "workflow_start_failed_docFinished");
            }
        }

        if (checkInvoice) {
            valid = valid && validateInvoice();
        }

        return valid;
    }

    /**
     * Helper class that validates that dueDates are not getting smaller for consecutive tasks(that run after each other)
     * Exceptions to this rule are tasks inside blocks that are started at the same time:
     * 1) tasks inside one parallel workflow
     * 2) tasks inside assignment, opinion and information workflows immediately following each other in any number and any order
     * Tasks inside each of these blocks are not compared to each other, BUT must still be compared to tasks outside these blocks
     */
    private static class DueDateRegressionHelper {
        boolean valid = true;
        Date earliestAllowedDueDate;
        Date latestDueDateParallel;
        int workflowIndex = -1;
        boolean insideParallelBlock = false; // task is in block of assignment, opinion and information workflows following each other
        boolean insideParallelWorkflow = false; // task is in workflow with parallel property set to true and NOT insideParallelBlock

        private void checkDueDate(Task task) {

            setParallelCheckDates(task);

            Date taskDueDate = task.getDueDate();
            if (taskDueDate == null) {
                return;
            }
            if (earliestAllowedDueDate == null && !(insideParallelBlock || insideParallelWorkflow)) {
                earliestAllowedDueDate = taskDueDate;
                return;
            }

            if (insideParallelBlock || insideParallelWorkflow) {
                // collect maximum date of the current block
                if (latestDueDateParallel == null || latestDueDateParallel.before(taskDueDate)) {
                    latestDueDateParallel = taskDueDate;
                }
            } else if (earliestAllowedDueDate == null || taskDueDate.after(earliestAllowedDueDate)) {
                earliestAllowedDueDate = taskDueDate;
            }

            if (earliestAllowedDueDate != null && taskDueDate.before(earliestAllowedDueDate)) {
                invalid("workflow_save_error_dueDate_decreaseNotAllowed");
            }
        }

        private void setParallelCheckDates(Task task) {
            int indexInCompoundWorkflow = task.getParent().getIndexInCompoundWorkflow();
            if (parallelBlockEnded(task, indexInCompoundWorkflow)) {
                setCheckDate();
            }
            insideParallelBlock = task.getParent().isType(WorkflowSpecificModel.CAN_START_PARALLEL);
            insideParallelWorkflow = !insideParallelBlock && task.getParent().isParallelTasks();
            workflowIndex = indexInCompoundWorkflow;
        }

        private boolean parallelBlockEnded(Task task, int indexInCompoundWorkflow) {
            return indexInCompoundWorkflow != workflowIndex
                    && (insideParallelWorkflow
                            || (insideParallelBlock && !task.getParent().isType(WorkflowSpecificModel.CAN_START_PARALLEL)));
        }

        private void setCheckDate() {
            earliestAllowedDueDate = latestDueDateParallel;
            latestDueDateParallel = null;
        }

        private void invalid(String msg) {
            if (valid) {
                MessageUtil.addErrorMessage(msg);
            }
            valid = false;
        }
    }

    private boolean validateInvoice() {
        NodeRef docRef = compoundWorkflow.getParent();
        if (docRef == null || !DocumentSubtypeModel.Types.INVOICE.equals(BeanHelper.getNodeService().getType(docRef))) {
            return true;
        }
        Map<QName, Serializable> docProps = BeanHelper.getNodeService().getProperties(docRef);
        List<Transaction> transactions = BeanHelper.getEInvoiceService().getInvoiceTransactions(docRef);
        if (transactions.isEmpty()) {
            return true;
        }
        List<String> mandatoryForOwner = BeanHelper.getEInvoiceService().getOwnerMandatoryFields();
        if (mandatoryForOwner.isEmpty()) {
            return true;
        }
        boolean valid = true;
        List<Pair<String, String>> errorMessages = new ArrayList<Pair<String, String>>();
        List<String> addedErrorKeys = new ArrayList<String>();
        for (Transaction transaction : transactions) {
            Map<String, Object> props = transaction.getNode().getProperties();
            for (Map.Entry<String, Object> entry : props.entrySet()) {
                EInvoiceUtil.checkTransactionMandatoryFields(mandatoryForOwner, errorMessages, addedErrorKeys, transaction);
            }
        }

        if (!errorMessages.isEmpty()) {
            valid = false;
            for (Pair<String, String> validationMsg : errorMessages) {
                // override validation message, use only object value
                MessageUtil.addErrorMessage("workflow_start_failed_transaction_mandatory_not_filled", validationMsg.getSecond());
            }
        }
        List<String> errorMessageKeys = new ArrayList<String>();
        Double totalSum = (Double) docProps.get(DocumentSpecificModel.Props.INVOICE_SUM);
        EInvoiceUtil.checkTotalSum(errorMessageKeys, "workflow_start_failed_", totalSum, transactions, null, false);
        if (!errorMessageKeys.isEmpty()) {
            valid = false;
            for (String validationMsg : errorMessageKeys) {
                MessageUtil.addErrorMessage(validationMsg);
            }
        }
        return valid;
    }

    private String getMissingOwnerMessageKey(QName blockType) {
        String missingOwnerMsgKey = null;
        if (knownWorkflowTypes.contains(blockType)) {
            missingOwnerMsgKey = "workflow_save_error_missingOwner_" + blockType.getLocalName();
        }
        return missingOwnerMsgKey;
    }

}
