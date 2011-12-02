package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDynamicService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowService;
import static ee.webmedia.alfresco.parameters.model.Parameters.MAX_ATTACHED_FILE_SIZE;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.TASK_INDEX;
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

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.config.DialogsConfigElement.DialogButtonConfig;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.propertysheet.modalLayer.ModalLayerComponent.ModalLayerSubmitEvent;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.Confirmable;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.document.einvoice.model.Transaction;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceUtil;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.dvk.service.ExternalReviewException;
import ee.webmedia.alfresco.notification.exception.EmailAttachmentSizeLimitException;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.CalendarUtil;
import ee.webmedia.alfresco.utils.MessageData;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.workflow.exception.WorkflowActiveResponsibleTaskException;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.OrderAssignmentWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Task.Action;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.WorkflowServiceImpl;
import ee.webmedia.alfresco.workflow.service.WorkflowServiceImpl.DialogAction;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import ee.webmedia.alfresco.workflow.service.type.WorkflowType;
import ee.webmedia.alfresco.workflow.web.evaluator.WorkflowNewEvaluator;

/**
 * Dialog bean for working with one compound workflow instance which is tied to a document.
 * 
 * @author Erko Hansar
 */
public class CompoundWorkflowDialog extends CompoundWorkflowDefinitionDialog implements Confirmable {

    private static final String CONTINUE_VALIDATED_WORKFLOW = "continueValidatedWorkflow";
    private static final String START_VALIDATED_WORKFLOW = "startValidatedWorkflow";
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "CompoundWorkflowDialog";

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(CompoundWorkflowDialog.class);

    private transient UserService userService;
    private transient DocumentService documentService;
    private transient DocumentLogService documentLogService;
    private transient ParametersService parametersService;

    private static final List<QName> knownWorkflowTypes = Arrays.asList(//
            WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW
            , WorkflowSpecificModel.Types.OPINION_WORKFLOW
            , WorkflowSpecificModel.Types.REVIEW_WORKFLOW
            , WorkflowSpecificModel.Types.INFORMATION_WORKFLOW
            , WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW
            , WorkflowSpecificModel.Types.CONFIRMATION_WORKFLOW
            );
    public static final String MODAL_KEY_ENTRY_COMMENT = "popup_comment";

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
        boolean isInProgress = WorkflowUtil.isStatus(compoundWorkflow, Status.IN_PROGRESS);
        preprocessWorkflow();
        if (isInProgress && hasOwnerWithNoEmail("workflow_compound_save_failed_owner_without_email")) {
            return null;
        }
        if (validate(context, isInProgress, false, false)) {
            if (!askConfirmIfHasSameTask(MessageUtil.getMessage("workflow_compound_save"), DialogAction.SAVING)) {
                saveCompWorkflow();
                return outcome;
            }
        }
        return null;
    }

    private boolean hasOwnerWithNoEmail(String messageKey) {
        List<String> ownersWithNoEmail = WorkflowUtil.getOwnersWithNoEmail(compoundWorkflow);
        if (!ownersWithNoEmail.isEmpty()) {
            for (String owner : ownersWithNoEmail) {
                MessageUtil.addErrorMessage(messageKey, owner);
            }
            MessageUtil.addErrorMessage("workflow_compound_contact_administrator");
            return true;
        }
        return false;
    }

    private boolean askConfirmIfHasSameTask(String title, DialogAction requiredAction) {
        List<Pair<String, QName>> hasSameTask = WorkflowUtil.haveSameTask(compoundWorkflow);
        if (!hasSameTask.isEmpty()) {
            ArrayList<MessageData> messageDataList = new ArrayList<MessageData>();
            String msgKey = "workflow_compound_confirm_same_task";
            for (Pair<String, QName> ownerNameTypePair : hasSameTask) {
                MessageData msgData = new MessageDataImpl(MessageSeverity.WARN, msgKey, ownerNameTypePair.getFirst(), MessageUtil.getTypeName(ownerNameTypePair.getSecond()));
                messageDataList.add(msgData);
            }
            messageDataList.add(new MessageDataImpl(MessageSeverity.WARN, "workflow_compound_confirm_continue"));
            BeanHelper.getConfirmDialog().setupConfirmDialog(this, messageDataList, title, requiredAction);
            isFinished = false;
            WebUtil.navigateTo("dialog:confirmDialog", null);
            return true;
        }
        return false;
    }

    private boolean saveCompWorkflow() {
        try {
            preprocessWorkflow();
            getWorkflowService().saveCompoundWorkflow(compoundWorkflow);
            if (isUnsavedWorkFlow) {
                getDocumentLogService().addDocumentLog(compoundWorkflow.getParent(), MessageUtil.getMessage("document_log_status_workflow"));
                isUnsavedWorkFlow = false;
            }
            MessageUtil.addInfoMessage("save_success");
            return true;
        } catch (NodeLockedException e) {
            log.debug("Compound workflow action failed: document locked!", e);
            String lockedBy = getUserService().getUserFullName((String) getNodeService().getProperty(e.getNodeRef(), ContentModel.PROP_LOCK_OWNER));
            MessageUtil.addErrorMessage("workflow_compound_save_failed_docLocked", lockedBy);
        } catch (WorkflowChangedException e) {
            handleException(e, null);
        } catch (WorkflowActiveResponsibleTaskException e) {
            log.debug("Compound workflow action failed: more than one active responsible task!", e);
            MessageUtil.addErrorMessage("workflow_compound_save_failed_responsible");
        } catch (RuntimeException e) {
            throw e;
        }
        return false;
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
            WebUtil.navigateTo(getDefaultCancelOutcome(), context);
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
        NodeRef document = new NodeRef(ActionUtil.getParam(event, "documentNodeRef"));
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
        preprocessWorkflow();
        if (hasOwnerWithNoEmail("workflow_compound_start_failed_owner_without_email")) {
            return;
        }
        if (validate(FacesContext.getCurrentInstance(), true, false, true)) {
            List<String> confirmationMessages = getConfirmationMessages(true);
            if (confirmationMessages != null && !confirmationMessages.isEmpty()) {
                updatePanelGroup(confirmationMessages, START_VALIDATED_WORKFLOW);
                return;
            }
            if (!askConfirmIfHasSameTask(MessageUtil.getMessage("workflow_compound_starting"), DialogAction.STARTING)) {
                startValidatedWorkflow(null);
            }

        }
    }

    /**
     * This method assumes that workflows has been validated
     */
    public void startValidatedWorkflow(ActionEvent event) {
        try {
            preprocessWorkflow();
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

    private List<String> getConfirmationMessages(boolean isStartWorkflow) {
        NodeService nodeService = BeanHelper.getNodeService();
        NodeRef docRef = compoundWorkflow.getParent();
        Date invoiceDueDate = null;
        Date notInvoiceDueDate = null;
        if (SystematicDocumentType.INVOICE.isSameType((String) nodeService.getProperty(docRef, DocumentAdminModel.Props.ID))) {
            invoiceDueDate = (Date) nodeService.getProperty(docRef, DocumentSpecificModel.Props.INVOICE_DUE_DATE);
        } else {
            notInvoiceDueDate = (Date) nodeService.getProperty(docRef, DocumentSpecificModel.Props.DUE_DATE);
        }
        List<String> messages = new ArrayList<String>();
        boolean addedDueDateInPastMsg = false;
        Date now = new Date(System.currentTimeMillis());
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            for (Task task : workflow.getTasks()) {
                Date taskDueDate = task.getDueDate();
                if (taskDueDate != null) {
                    if (invoiceDueDate != null) {
                        Date invoiceDueDateMinus3Days = DateUtils.addDays(invoiceDueDate, -3);
                        if (!DateUtils.isSameDay(invoiceDueDateMinus3Days, taskDueDate) && taskDueDate.after(invoiceDueDateMinus3Days)) {
                            getAndAddMessage(messages, workflow, taskDueDate, "task_confirm_invoice_task_due_date", invoiceDueDate);
                        }
                    }
                    if (!addedDueDateInPastMsg && ((isStartWorkflow || task.isStatus(Status.NEW)) && taskDueDate.before(now))) {
                        messages.add(MessageUtil.getMessage("task_confirm_due_date_in_past"));
                        addedDueDateInPastMsg = true;
                    }
                    if (notInvoiceDueDate != null) {
                        if (!DateUtils.isSameDay(notInvoiceDueDate, taskDueDate) && taskDueDate.after(notInvoiceDueDate)) {
                            getAndAddMessage(messages, workflow, taskDueDate, "task_confirm_not_invoice_task_due_date", notInvoiceDueDate);
                        }
                    }
                }
            }
        }
        return messages;
    }

    private void getAndAddMessage(List<String> messages, Workflow workflow, Date taskDueDate, String msgKey, Date date) {
        FacesContext fc = FacesContext.getCurrentInstance();
        DateFormat dateFormat = Utils.getDateFormat(fc);
        String invoiceTaskDueDateConfirmationMsg = MessageUtil.getMessage(msgKey,
                MessageUtil.getMessage(workflow.getType().getLocalName()),
                dateFormat.format(taskDueDate), dateFormat.format(date));
        messages.add(invoiceTaskDueDateConfirmationMsg);
    }

    /**
     * Action listener for JSP.
     */
    public void stopWorkflow(ActionEvent event) {
        log.debug("stopWorkflow");
        try {
            preprocessWorkflow();
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
            preprocessWorkflow();
            if (hasOwnerWithNoEmail("workflow_compound_continue_failed_owner_without_email")) {
                return;
            }
            if (validate(FacesContext.getCurrentInstance(), true, false, true)) {
                List<String> confirmationMessages = getConfirmationMessages(false);
                if (confirmationMessages != null && !confirmationMessages.isEmpty()) {
                    updatePanelGroup(confirmationMessages, CONTINUE_VALIDATED_WORKFLOW);
                    return;
                }
                if (!askConfirmIfHasSameTask(MessageUtil.getMessage("workflow_compound_continuing"), DialogAction.CONTINUING)) {
                    continueValidatedWorkflow(true);
                }

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
            preprocessWorkflow();
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
        preprocessWorkflow();
        if (validate(FacesContext.getCurrentInstance(), false, false, false)) {
            try {
                preprocessWorkflow();
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
            preprocessWorkflow();
            getWorkflowService().deleteCompoundWorkflow(compoundWorkflow.getNodeRef());
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
        ModalLayerSubmitEvent commentEvent = (ModalLayerSubmitEvent) event;
        int index = (Integer) event.getComponent().getAttributes().get(TaskListGenerator.ATTR_WORKFLOW_INDEX);
        int taskIndex = commentEvent.getActionIndex();
        String comment = (String) commentEvent.getSubmittedValue(MODAL_KEY_ENTRY_COMMENT);
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

    public void calculateDueDate(ActionEvent event) {
        int wfIndex = ActionUtil.getParam(event, WF_INDEX, Integer.class);
        int taskIndex = ActionUtil.getParam(event, TASK_INDEX, Integer.class);
        Workflow block = compoundWorkflow.getWorkflows().get(wfIndex);
        Task task = block.getTasks().get(taskIndex);
        Integer dueDateDays = task.getDueDateDays();
        if (dueDateDays != null) {
            LocalDate newDueDate = calculateDueDate(task, dueDateDays);
            LocalTime newTime;
            Date existingDueDate = task.getDueDate();
            if (existingDueDate != null) {
                newTime = new LocalTime(existingDueDate.getHours(), existingDueDate.getMinutes());
            } else {
                newTime = new LocalTime(23, 59);
            }
            task.setDueDate(newDueDate.toDateTime(newTime).toDate());
        }
        updatePanelGroup();
    }

    @Override
    protected void preprocessWorkflow() {
        super.preprocessWorkflow();
        removeImproperDueDateDays();
    }

    private void removeImproperDueDateDays() {
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            for (Task task : workflow.getTasks()) {
                if (task.isStatus(Status.NEW) && task.getDueDate() != null && task.getDueDateDays() != null) {
                    if (!DateUtils.isSameDay(task.getDueDate(), calculateDueDate(task, task.getDueDateDays()).toDateMidnight().toDate())) {
                        task.setProp(WorkflowSpecificModel.Props.DUE_DATE_DAYS, null);
                        task.setProp(WorkflowSpecificModel.Props.IS_DUE_DATE_WORKING_DAYS, Boolean.FALSE); // reset to default value
                    }
                }
            }
        }
    }

    private LocalDate calculateDueDate(Task task, Integer dueDateDays) {
        LocalDate newDueDate = new LocalDate();
        if (task.getPropBoolean(WorkflowSpecificModel.Props.IS_DUE_DATE_WORKING_DAYS)) {
            newDueDate = CalendarUtil.addWorkingDaysToDate(newDueDate, dueDateDays, BeanHelper.getClassificatorService());
        } else {
            newDueDate = newDueDate.plusDays(dueDateDays);
        }
        return newDueDate;
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
            WorkflowService workflowService = getWorkflowService();

            sortedTypes = new TreeMap<String, QName>();
            Map<QName, WorkflowType> workflowTypes = workflowService.getWorkflowTypes();
            String docStatus = (String) BeanHelper.getNodeService().getProperty(docRef, DocumentCommonModel.Props.DOC_STATUS);
            boolean isDocStatusWorking = DocumentStatus.WORKING.getValueName().equals(docStatus);
            for (QName wfType : workflowTypes.keySet()) {
                if (wfType.equals(WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_WORKFLOW)) {
                    continue;
                }
                if ((wfType.equals(WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW)
                            || wfType.equals(WorkflowSpecificModel.Types.OPINION_WORKFLOW)
                            || wfType.equals(WorkflowSpecificModel.Types.REVIEW_WORKFLOW))
                            && !isDocStatusWorking) {
                    continue;
                }
                if ((wfType.equals(WorkflowSpecificModel.Types.OPINION_WORKFLOW)
                            || wfType.equals(WorkflowSpecificModel.Types.CONFIRMATION_WORKFLOW)
                            || wfType.equals(WorkflowSpecificModel.Types.REVIEW_WORKFLOW))
                            && !BaseDialogBean.hasPermission(docRef, DocumentCommonModel.Privileges.EDIT_DOCUMENT_META_DATA)) {
                    continue;
                }
                if (wfType.equals(WorkflowSpecificModel.Types.DOC_REGISTRATION_WORKFLOW)
                            && !BeanHelper.getUserService().isAdministrator()) {
                    continue;
                }
                if (wfType.equals(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW) && !workflowService.externalReviewWorkflowEnabled()) {
                    continue;

                }
                String tmpName = MessageUtil.getMessage(wfType.getLocalName());
                sortedTypes.put(tmpName, wfType);
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
            parametersService = (ParametersService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    ParametersService.BEAN_NAME);
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

    @Override
    protected void updateFullAccess() {
        fullAccess = false;

        if (getUserService().isDocumentManager()) {
            fullAccess = true;
        } else if (getDocumentDynamicService().isOwner(compoundWorkflow.getParent(), AuthenticationUtil.getRunAsUser())) {
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
            String[] lockedBy = new String[] { BeanHelper.getUserService().getUserFullName(
                    (String) BeanHelper.getNodeService().getProperty(((NodeLockedException) e).getNodeRef(), ContentModel.PROP_LOCK_OWNER)) };
            @SuppressWarnings("unchecked")
            Pair<String, Object[]>[] messages = new Pair[] { new Pair<String, Object[]>(failMsg, null),
                    new Pair<String, Object[]>("document_error_docLocked", lockedBy) };
            MessageUtil.addErrorMessage(context, messages);
        } else if (e instanceof InvalidNodeRefException) {
            MessageUtil.addErrorMessage(context, "workflow_task_save_failed_docDeleted");
            WebUtil.navigateTo(AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME, context);
        } else if (e instanceof UnableToPerformException) {
            MessageUtil.addStatusMessage(context, (UnableToPerformException) e);
        } else if (e instanceof ExternalReviewException) {
            ExternalReviewException externalReviewException = (ExternalReviewException) e;
            if (externalReviewException.getExceptionType().equals(ExternalReviewException.ExceptionType.DVK_CAPABILITY_ERROR)) {
                log.debug("Compound workflow action failed: external review task owner not dvk capable. ", e);
                MessageUtil.addErrorMessage(context, "workflow_compound_save_failed_external_review_owner_not_task_capable");
            } else {
                log.debug("Compound workflow action failed: external review workflow error of type: " + externalReviewException.getExceptionType(), e);
                MessageUtil.addErrorMessage(context, "workflow_compound_save_failed_external_review_error");
            }
        } else {
            log.error("Compound workflow action failed!", e);
            MessageUtil.addErrorMessage(context, failMsg);
        }
    }

    private boolean validate(FacesContext context, boolean checkFinished, boolean allowInactiveResponsibleTask, boolean checkInvoice) {
        boolean valid = true;
        boolean activeResponsibleAssignTaskInSomeWorkFlow = false;
        // true if some orderAssignmentWorkflow in status NEW has no active responible task (but has some co-responsible tasks)
        boolean checkOrderAssignmentResponsibleTask = false;
        boolean missingOwnerAssignment = false;
        Boolean missingInformationTasks = null;
        Set<String> missingOwnerMessageKeys = null;
        boolean hasForbiddenFlowsForFinished = false;
        DueDateRegressionHelper regressionTest = new DueDateRegressionHelper();
        boolean isCategoryEnabled = BeanHelper.getWorkflowService().getOrderAssignmentCategoryEnabled();
        for (Workflow block : compoundWorkflow.getWorkflows()) {
            boolean foundOwner = false;
            QName blockType = block.getNode().getType();
            boolean activeResponsibleAssigneeNeeded = blockType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW)
                    && !activeResponsibleAssignTaskInSomeWorkFlow && !isActiveResponsibleAssignedForDocument(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW, false);
            boolean activeResponsibleAssigneeAssigned = !activeResponsibleAssigneeNeeded;

            if (WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW.equals(blockType) ||
                    WorkflowSpecificModel.Types.REVIEW_WORKFLOW.equals(blockType) ||
                    WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW.equals(blockType) ||
                    WorkflowSpecificModel.Types.OPINION_WORKFLOW.equals(blockType)) {
                if (WorkflowUtil.isStatus(block, Status.NEW, Status.STOPPED)) {
                    hasForbiddenFlowsForFinished = true;
                }
            }
            if (block.isType(WorkflowSpecificModel.Types.REVIEW_WORKFLOW)
                    && block.getNode().getProperties().get(WorkflowCommonModel.Props.PARALLEL_TASKS) == null) {
                valid = false;
                MessageUtil.addErrorMessage(context, "workflow_save_error_missingParallelOrNot");
            }

            if (block.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW) && isCategoryEnabled && StringUtils.isBlank(((OrderAssignmentWorkflow) block).getCategory())) {
                MessageUtil.addErrorMessage(context, "task_category_empty");
                valid = false;
            }

            boolean hasOrderAssignmentActiveResponsible = !(block.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW) && block.isStatus(Status.NEW));
            for (Task task : block.getTasks()) {
                final boolean activeResponsible = WorkflowUtil.isActiveResponsible(task);
                boolean inactiveResponsible = false;
                if (allowInactiveResponsibleTask) {
                    inactiveResponsible = WorkflowUtil.isInactiveResponsible(task);
                }
                if (activeResponsibleAssigneeNeeded
                        && StringUtils.isNotBlank(task.getOwnerName())
                        && (activeResponsible || inactiveResponsible)) {
                    activeResponsibleAssignTaskInSomeWorkFlow = true;
                    activeResponsibleAssigneeAssigned = true;
                    missingOwnerAssignment = false;
                }
                hasOrderAssignmentActiveResponsible |= activeResponsible;
                foundOwner |= StringUtils.isNotBlank(task.getOwnerName());
                QName taskType = task.getNode().getType();
                String taskOwnerMsg = getTaskOwnerMessage(block, taskType, task.isResponsible());
                if (activeResponsible) {
                    // both fields must be empty or filled
                    if (hasNoOwnerOrDueDate(task)) {
                        valid = false;
                        MessageUtil.addErrorMessage(context, "task_name_and_due_required", taskOwnerMsg);
                        break;
                    }
                } else {
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
                        if (StringUtils.isBlank(task.getInstitutionName()) || task.getDueDate() == null) {
                            MessageUtil.addErrorMessage(context, "task_name_and_due_required", taskOwnerMsg);
                            break;
                        }
                    }
                    // both fields must be filled
                    else {
                        if (hasNoOwnerOrDueDate(task)) {
                            valid = false;
                            MessageUtil.addErrorMessage(context, "task_name_and_due_required", taskOwnerMsg);
                            break;
                        }
                    }
                }
                regressionTest.checkDueDate(task);
            }
            checkOrderAssignmentResponsibleTask |= !hasOrderAssignmentActiveResponsible;
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

        if (checkOrderAssignmentResponsibleTask) {
            valid = false;
            MessageUtil.addErrorMessage("workflow_save_error_missing_orderAssigmnentResponsibleTask");
        }

        if (checkFinished && hasForbiddenFlowsForFinished) {
            String docStatus = (String) BeanHelper.getDocumentDialogHelperBean().getProps().get(DocumentCommonModel.Props.DOC_STATUS);
            if (DocumentStatus.FINISHED.getValueName().equals(docStatus)) {
                valid = false;
                MessageUtil.addErrorMessage(context, "workflow_start_failed_docFinished");
            }
        }

        if (checkInvoice) {
            valid = valid && validateInvoice();
        }
        if (!valid) {
            updatePanelGroup();
        }
        return valid;
    }

    private String getTaskOwnerMessage(Workflow block, QName taskType, boolean isResponsible) {
        String suffix = "";
        if (isResponsibleAllowed(taskType) && !isResponsible) {
            suffix = "_co";
        }
        String taskOwnerMsg = MessageUtil.getMessage(block.getNode().getType().getLocalName() + "_tasks" + suffix);
        return taskOwnerMsg;
    }

    private boolean isResponsibleAllowed(QName taskType) {
        return taskType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_TASK) || taskType.equals(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK);
    }

    private boolean hasNoOwnerOrDueDate(Task task) {
        return StringUtils.isBlank(task.getOwnerName()) != (task.getDueDate() == null && task.getDueDateDays() == null);
    }

    /**
     * Helper class that validates that dueDates are not getting smaller for consecutive tasks(that run after each other)
     * Exceptions to this rule are tasks inside blocks that are started at the same time:
     * 1) tasks inside one parallel workflow
     * 2) tasks inside assignment, opinion and information workflows immediately following each other in any number and any order
     * Tasks inside each of these blocks are not compared to each other, BUT must still be compared to tasks outside these blocks
     * 
     * @author Ats Uiboupin
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

    @Override
    public void afterConfirmationAction(Object action) {
        switch ((WorkflowServiceImpl.DialogAction) action) {
        case SAVING:
            if (saveCompWorkflow()) {
                resetState();
                WebUtil.navigateTo(getDefaultFinishOutcome());
            }
            break;
        case STARTING:
            startValidatedWorkflow(null);
            break;
        case CONTINUING:
            continueValidatedWorkflow(true);
        }
    }
}
