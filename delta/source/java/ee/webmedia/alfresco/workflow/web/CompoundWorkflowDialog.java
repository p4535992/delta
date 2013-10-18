package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDynamicService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowService;
import static ee.webmedia.alfresco.parameters.model.Parameters.MAX_ATTACHED_FILE_SIZE;
import static ee.webmedia.alfresco.privilege.service.PrivilegeUtil.isAdminOrDocmanagerWithPermission;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.TASK_INDEX;
import static ee.webmedia.alfresco.workflow.web.TaskListGenerator.WF_INDEX;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

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
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.springframework.util.Assert;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.propertysheet.datepicker.DatePickerWithDueDateGenerator;
import ee.webmedia.alfresco.common.propertysheet.modalLayer.ModalLayerComponent.ModalLayerSubmitEvent;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.Confirmable;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.document.einvoice.model.Transaction;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceUtil;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel.Privileges;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.dvk.service.ExternalReviewException;
import ee.webmedia.alfresco.notification.exception.EmailAttachmentSizeLimitException;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
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
import ee.webmedia.alfresco.workflow.service.CompoundWorkflowDefinition;
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
    private static final String STOP_VALIDATED_WORKFLOW = "stopValidatedWorkflow";
    private static final String SAVE_VALIDATED_WORKFLOW = "saveValidatedWorkflow";
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "CompoundWorkflowDialog";

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(CompoundWorkflowDialog.class);

    private transient UserService userService;
    private transient DocumentService documentService;
    private transient DocumentLogService documentLogService;
    private transient ParametersService parametersService;
    private String existingUserCompoundWorkflowDefinition;
    private String newUserCompoundWorkflowDefinition;
    private boolean finishImplConfirmed;

    private static final List<QName> knownWorkflowTypes = Arrays.asList(//
            WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW
            , WorkflowSpecificModel.Types.OPINION_WORKFLOW
            , WorkflowSpecificModel.Types.REVIEW_WORKFLOW
            , WorkflowSpecificModel.Types.INFORMATION_WORKFLOW
            , WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW
            , WorkflowSpecificModel.Types.CONFIRMATION_WORKFLOW
            );
    public static final String MODAL_KEY_ENTRY_COMMENT = "popup_comment";
    List<String> profileDurations;

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
        boolean checkConfirmations = !finishImplConfirmed;
        finishImplConfirmed = false;
        if (isInProgress && hasOwnerWithNoEmail("workflow_compound_save_failed_owner_without_email")) {
            return null;
        }
        if (validate(context, isInProgress, false)) {
            if (checkConfirmations) {
                List<String> confirmationMessages = getConfirmationMessages(false);
                if (confirmationMessages != null && !confirmationMessages.isEmpty()) {
                    updatePanelGroup(confirmationMessages, SAVE_VALIDATED_WORKFLOW);
                    return null;
                }
            } else {
                updatePanelGroup();
            }
            return saveOrConfirmValidatedWorkflow(outcome);
        }
        return null;
    }

    public void saveValidatedWorkflow(ActionEvent event) {
        finishImplConfirmed = true;
        String outcome = finish();
        if (StringUtils.isNotBlank(outcome)) {
            WebUtil.navigateTo(outcome);
        }
    }

    private String saveOrConfirmValidatedWorkflow(String originalOutcome) {
        String confirmationOutcome = askConfirmIfHasSameTask(MessageUtil.getMessage("workflow_compound_save"), DialogAction.SAVING, false);
        if (confirmationOutcome == null) {
            confirmationOutcome = originalOutcome;
            boolean saveSucceeded = saveCompWorkflow();
            if (!saveSucceeded) {
                confirmationOutcome = null;
            }
            updatePanelGroup();
        }
        return confirmationOutcome;
    }

    @Override
    protected void resetState() {
        super.resetState();
        existingUserCompoundWorkflowDefinition = null;
        newUserCompoundWorkflowDefinition = null;
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

    private String askConfirmIfHasSameTask(String title, DialogAction requiredAction, boolean navigate) {
        Set<Pair<String, QName>> hasSameTask = WorkflowUtil.haveSameTask(compoundWorkflow, getWorkflowService().getOtherCompoundWorkflows(compoundWorkflow));
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
            String navigationOutcome = "dialog:confirmDialog";
            if (navigate) {
                WebUtil.navigateTo(navigationOutcome, null);
            }
            return navigationOutcome;
        }
        return null;
    }

    private boolean saveCompWorkflow() {
        try {
            preprocessWorkflow();
            compoundWorkflow = getWorkflowService().saveCompoundWorkflow(compoundWorkflow);
            if (isUnsavedWorkFlow) {
                getDocumentLogService().addDocumentLog(compoundWorkflow.getParent(), MessageUtil.getMessage("document_log_status_workflow"));
                isUnsavedWorkFlow = false;
            }
            MessageUtil.addInfoMessage("save_success");
            return true;
        } catch (Exception e) {
            handleException(e, null);
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
        addLargeWorkflowWarning();
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
            addLargeWorkflowWarning();
            Workflow costManagerWorkflow = getCostManagerForkflow();
            if (costManagerWorkflow != null) {
                addCostManagerTasks(costManagerWorkflow);
            }
            updateFullAccess();
            setSignatureTaskOwnerProps();
            isUnsavedWorkFlow = true;
        } catch (InvalidNodeRefException e) {
            log.warn("Failed to create a new compound workflow instance because someone has probably deleted the compound workflow definition.");
        }
    }

    private void setSignatureTaskOwnerProps() {
        Map<String, Object> signatureTaskOwnerProps = loadSignatureTaskOwnerProps(compoundWorkflow.getParent());
        if (signatureTaskOwnerProps == null) {
            return;
        }
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            if (workflow.isType(WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW) && !workflow.getTasks().isEmpty()) {
                workflow.getTasks().get(0).getNode().getProperties().putAll(signatureTaskOwnerProps);
            }
        }
    }

    private Map<String, Object> loadSignatureTaskOwnerProps(NodeRef docRef) {
        Map<String, Object> signatureTaskOwnerProps = null;
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            if (workflow.isType(WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW)) {
                signatureTaskOwnerProps = TaskListGenerator.loadSignatureTaskOwnerProps(docRef);
                break;
            }
        }
        return signatureTaskOwnerProps;
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
        if (validate(FacesContext.getCurrentInstance(), true, true)) {
            List<String> confirmationMessages = getConfirmationMessages(true);
            if (confirmationMessages != null && !confirmationMessages.isEmpty()) {
                updatePanelGroup(confirmationMessages, START_VALIDATED_WORKFLOW);
                return;
            }
            if (askConfirmIfHasSameTask(MessageUtil.getMessage("workflow_compound_starting"), DialogAction.STARTING, true) == null) {
                startValidatedWorkflow(null);
            }
        }
    }

    /**
     * This method assumes that workflows has been validated
     */
    public void startValidatedWorkflow(@SuppressWarnings("unused") ActionEvent event) {
        boolean succeeded = false;
        try {
            if (isUnsavedWorkFlow) {
                getDocumentLogService().addDocumentLog(compoundWorkflow.getParent(), MessageUtil.getMessage("document_log_status_workflow"));
            }
            // clear panelGroup to avoid memory issues when working with large worflows
            panelGroup.getChildren().clear();
            preprocessWorkflow();
            compoundWorkflow = getWorkflowService().startCompoundWorkflow(compoundWorkflow);
            if (isUnsavedWorkFlow) {
                isUnsavedWorkFlow = false;
            }
            MessageUtil.addInfoMessage("workflow_compound_start_success");
            succeeded = true;
        } catch (Exception e) {
            handleException(e, "workflow_compound_start_workflow_failed");
        }
        BeanHelper.getDocumentDynamicDialog().switchMode(false); // document metadata might have changed (for example owner)
        if (succeeded) {
            WebUtil.navigateTo(getDefaultFinishOutcome());
        } else {
            // update only if we stay on same page
            updatePanelGroup();
        }
    }

    private List<String> getConfirmationMessages(boolean checkDocumentDueDate) {
        NodeService nodeService = BeanHelper.getNodeService();
        NodeRef docRef = compoundWorkflow.getParent();
        Date invoiceDueDate = null;
        Date notInvoiceDueDate = null;
        if (checkDocumentDueDate) {
            if (SystematicDocumentType.INVOICE.isSameType((String) nodeService.getProperty(docRef, DocumentAdminModel.Props.ID))) {
                invoiceDueDate = (Date) nodeService.getProperty(docRef, DocumentSpecificModel.Props.INVOICE_DUE_DATE);
            } else {
                notInvoiceDueDate = (Date) nodeService.getProperty(docRef, DocumentSpecificModel.Props.DUE_DATE);
            }
        }
        List<String> messages = new ArrayList<String>();
        boolean addedDueDateInPastMsg = false;
        Date now = new Date(System.currentTimeMillis());
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            for (Task task : workflow.getTasks()) {
                Date taskDueDate = task.getDueDate();
                if (taskDueDate != null) {
                    if (checkDocumentDueDate) {
                        if (invoiceDueDate != null) {
                            Date invoiceDueDateMinus3Days = DateUtils.addDays(invoiceDueDate, -3);
                            if (!DateUtils.isSameDay(invoiceDueDateMinus3Days, taskDueDate) && taskDueDate.after(invoiceDueDateMinus3Days)) {
                                WorkflowUtil.getAndAddMessage(messages, workflow, taskDueDate, "task_confirm_invoice_task_due_date", invoiceDueDate);
                            }
                        }
                        WorkflowUtil.getDocmentDueDateMessage(notInvoiceDueDate, messages, workflow, taskDueDate);
                    }
                    if (!addedDueDateInPastMsg && task.isStatus(Status.NEW) && taskDueDate.before(now)) {
                        messages.add(MessageUtil.getMessage("task_confirm_due_date_in_past"));
                        addedDueDateInPastMsg = true;
                    }
                }
            }
        }
        return messages;
    }

    /**
     * Action listener for JSP.
     */
    public void stopWorkflow(@SuppressWarnings("unused") ActionEvent event) {
        log.debug("stopWorkflow");
        try {
            preprocessWorkflow();
            if (validate(FacesContext.getCurrentInstance(), false, false)) {
                List<String> confirmationMessages = getConfirmationMessages(false);
                if (confirmationMessages != null && !confirmationMessages.isEmpty()) {
                    updatePanelGroup(confirmationMessages, STOP_VALIDATED_WORKFLOW);
                    return;
                }
                compoundWorkflow = getWorkflowService().stopCompoundWorkflow(compoundWorkflow);
                MessageUtil.addInfoMessage("workflow_compound_stop_success");
            }
        } catch (Exception e) {
            handleException(e, "workflow_compound_stop_workflow_failed");
        }
        updatePanelGroup();
    }

    public void stopValidatedWorkflow(@SuppressWarnings("unused") ActionEvent event) {
        log.debug("stopValidatedWorkflow");
        try {
            // clear panelGroup to avoid memory issues when working with large worflows
            panelGroup.getChildren().clear();
            preprocessWorkflow();
            compoundWorkflow = getWorkflowService().stopCompoundWorkflow(compoundWorkflow);
            MessageUtil.addInfoMessage("workflow_compound_stop_success");
        } catch (Exception e) {
            handleException(e, "workflow_compound_stop_workflow_failed");
        }
        updatePanelGroup();
    }

    /**
     * Action listener for JSP.
     */
    public void continueWorkflow(@SuppressWarnings("unused") ActionEvent event) {
        log.debug("continueWorkflow");
        try {
            preprocessWorkflow();
            if (hasOwnerWithNoEmail("workflow_compound_continue_failed_owner_without_email")) {
                return;
            }
            if (validate(FacesContext.getCurrentInstance(), true, true)) {
                List<String> confirmationMessages = getConfirmationMessages(true);
                if (confirmationMessages != null && !confirmationMessages.isEmpty()) {
                    updatePanelGroup(confirmationMessages, CONTINUE_VALIDATED_WORKFLOW);
                    return;
                }
                if (askConfirmIfHasSameTask(MessageUtil.getMessage("workflow_compound_continuing"), DialogAction.CONTINUING, true) == null) {
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
    public void continueValidatedWorkflow(@SuppressWarnings("unused") ActionEvent event) {
        continueValidatedWorkflow(false);
    }

    private void continueValidatedWorkflow(boolean throwException) {
        try {
            // clear panelGroup to avoid memory issues when working with large worflows
            panelGroup.getChildren().clear();
            preprocessWorkflow();
            compoundWorkflow = getWorkflowService().continueCompoundWorkflow(compoundWorkflow);
            MessageUtil.addInfoMessage("workflow_compound_continue_success");
        } catch (Exception e) {
            // let calling method handle error
            if (throwException) {
                throw new RuntimeException(e);
            }
            handleException(e, "workflow_compound_continue_workflow_failed");
        }
        updatePanelGroup();
        BeanHelper.getDocumentDynamicDialog().switchMode(false); // document metadata might have changed (for example owner)
    }

    /**
     * Action listener for JSP.
     */
    public void finishWorkflow(@SuppressWarnings("unused") ActionEvent event) {
        log.debug("finishWorkflow");
        try {
            preprocessWorkflow();
            // clear panelGroup to avoid memory issues when working with large worflows
            panelGroup.getChildren().clear();
            compoundWorkflow = getWorkflowService().finishCompoundWorkflow(compoundWorkflow);
            MessageUtil.addInfoMessage("workflow_compound_finish_success");
        } catch (Exception e) {
            handleException(e, "workflow_compound_finish_workflow_failed");
        }
        updatePanelGroup();
    }

    /**
     * Copy saved version of current compound workflow. Not saved changes of current compound workflow are lost.
     * Action listener for JSP.
     */
    public void copyWorkflow(@SuppressWarnings("unused") ActionEvent event) {
        log.debug("copyWorkflow");
        try {
            compoundWorkflow = getWorkflowService().copyAndResetCompoundWorkflow(compoundWorkflow.getNodeRef());
        } catch (Exception e) {
            handleException(e, "workflow_compound_copy_workflow_failed");
        }
        updatePanelGroup();
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

    /** @param event */
    public void saveasCompoundWorkflowDefinition(ActionEvent event) {
        String userId = AuthenticationUtil.getRunAsUser();
        if (validateSaveasData()) {
            if (StringUtils.isNotBlank(newUserCompoundWorkflowDefinition)) {
                getWorkflowService().createCompoundWorkflowDefinition(compoundWorkflow, userId, newUserCompoundWorkflowDefinition);
            } else {
                getWorkflowService().overwriteExistingCompoundWorkflowDefinition(compoundWorkflow, userId, existingUserCompoundWorkflowDefinition);
            }
        }
        updatePanelGroup();
    }

    private boolean validateSaveasData() {
        if (StringUtils.isBlank(newUserCompoundWorkflowDefinition) && StringUtils.isBlank(existingUserCompoundWorkflowDefinition)) {
            MessageUtil.addErrorMessage("compoundWorkflow_definition_saveas_error_fields_empty");
            return false;
        }
        if (StringUtils.isNotBlank(newUserCompoundWorkflowDefinition) && StringUtils.isNotBlank(existingUserCompoundWorkflowDefinition)) {
            MessageUtil.addErrorMessage("compoundWorkflow_definition_saveas_error_both_fields_filled");
            return false;
        }
        if (StringUtils.isNotBlank(newUserCompoundWorkflowDefinition)) {
            if (getWorkflowService().getCompoundWorkflowDefinitionByName(newUserCompoundWorkflowDefinition, AuthenticationUtil.getRunAsUser(), true) != null) {
                MessageUtil.addErrorMessage("compoundWorkflow_definition_saveas_error_definition_exists");
                return false;
            }
        }
        return true;
    }

    public void deleteCompoundWorkflowDefinition(ActionEvent event) {
        if (StringUtils.isBlank(existingUserCompoundWorkflowDefinition)) {
            MessageUtil.addErrorMessage("compoundWorkflow_definition_delete_error_definition_not_selected");
            return;
        }
        getWorkflowService().deleteCompoundWorkflowDefinition(existingUserCompoundWorkflowDefinition, AuthenticationUtil.getRunAsUser());
        updatePanelGroup();
    }

    @SuppressWarnings("unchecked")
    public List<SelectItem> getUserCompoundWorkflowDefinitions(FacesContext context, UIInput component) {
        List<SelectItem> userCompoundWorkflowDefinitions = new ArrayList<SelectItem>();
        for (CompoundWorkflowDefinition compoundWorkflowDefinition : getWorkflowService().getUserCompoundWorkflowDefinitions(AuthenticationUtil.getRunAsUser())) {
            userCompoundWorkflowDefinitions.add(new SelectItem(compoundWorkflowDefinition.getName()));
        }
        Collections.sort(userCompoundWorkflowDefinitions, new TransformingComparator(new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((SelectItem) input).getValue();
            }
        }, new NullComparator()));
        userCompoundWorkflowDefinitions.add(0, new SelectItem("", MessageUtil.getMessage("workflow_choose")));
        return userCompoundWorkflowDefinitions;
    }

    public String getExistingUserCompoundWorkflowDefinition() {
        return existingUserCompoundWorkflowDefinition;
    }

    public void setExistingUserCompoundWorkflowDefinition(String existingUserCompoundWorkflowDefinition) {
        this.existingUserCompoundWorkflowDefinition = existingUserCompoundWorkflowDefinition;
    }

    public String getNewUserCompoundWorkflowDefinition() {
        return newUserCompoundWorkflowDefinition;
    }

    public void setNewUserCompoundWorkflowDefinition(String newUserCompoundWorkflowDefinition) {
        this.newUserCompoundWorkflowDefinition = newUserCompoundWorkflowDefinition;
    }

    public void calculateDueDate(ActionEvent event) {
        int wfIndex = ActionUtil.getParam(event, WF_INDEX, Integer.class);
        int taskIndex = ActionUtil.getParam(event, TASK_INDEX, Integer.class);
        Workflow block = compoundWorkflow.getWorkflows().get(wfIndex);
        Task task = block.getTasks().get(taskIndex);
        Integer dueDateDays = task.getDueDateDays();
        if (dueDateDays != null) {
            task.setDueDate(getNewDueDate(task.getPropBoolean(WorkflowSpecificModel.Props.IS_DUE_DATE_WORKING_DAYS), dueDateDays, task.getDueDate()));
        }
    }

    public void calculateTaskGroupDueDate(ActionEvent event) {
        String selectorId = ActionUtil.getParam(event, "selector");
        int wfIndex = ActionUtil.getParam(event, WF_INDEX, Integer.class);

        UIComponent selector = ComponentUtil.findComponentById(FacesContext.getCurrentInstance(), event.getComponent().getParent(), selectorId);
        List value = (List) ((HtmlSelectOneMenu) selector).getValue();
        if (value == null) {
            return;
        }

        TaskGroup taskGroup = findTaskGroup(event);
        Date existingDueDate = taskGroup.getDueDate();
        taskGroup.setDueDate(getNewDueDate((Boolean) value.get(1), (Integer) value.get(0), existingDueDate));

        // Set the due dates according to the group
        WorkflowUtil.setGroupTasksDueDates(taskGroup, getWorkflow().getWorkflows().get(wfIndex).getTasks());
    }

    private Date getNewDueDate(Boolean isWorkingDays, Integer dueDateDays, Date existingDueDate) {
        LocalDate newDueDate = DatePickerWithDueDateGenerator.calculateDueDate(isWorkingDays, dueDateDays);
        LocalTime newTime;
        if (existingDueDate != null) {
            newTime = new LocalTime(existingDueDate.getHours(), existingDueDate.getMinutes());
        } else {
            newTime = new LocalTime(23, 59);
        }

        return newDueDate.toDateTime(newTime).toDate();
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
                    if (!DateUtils.isSameDay(task.getDueDate(),
                            DatePickerWithDueDateGenerator.calculateDueDate(task.getPropBoolean(WorkflowSpecificModel.Props.IS_DUE_DATE_WORKING_DAYS), task.getDueDateDays())
                                    .toDateMidnight().toDate())) {
                        task.setProp(WorkflowSpecificModel.Props.DUE_DATE_DAYS, null);
                        task.setProp(WorkflowSpecificModel.Props.IS_DUE_DATE_WORKING_DAYS, Boolean.FALSE); // reset to default value
                    }
                }
            }
        }
    }

    public void addDateForAllTasks(ActionEvent event) {
        Workflow block = compoundWorkflow.getWorkflows().get(ActionUtil.getParam(event, WF_INDEX, Integer.class));
        if (block.isStatus(Status.NEW)) {
            Task selectedTask = block.getTasks().get(ActionUtil.getParam(event, TASK_INDEX, Integer.class));
            Date originDate = selectedTask.getDueDate();
            for (Task task : block.getTasks()) {
                if (task != selectedTask) {
                    task.setDueDate(originDate);
                }
            }
        }
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
            Document doc = getParentDocument();
            String docStatus = doc.getDocStatus();
            boolean isDocStatusWorking = DocumentStatus.WORKING.getValueName().equals(docStatus);
            for (QName wfType : workflowTypes.keySet()) {
                if (wfType.equals(WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_WORKFLOW)) {
                    continue;
                }
                if (!isDocStatusWorking
                        && ((wfType.equals(WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW)
                                && !isAdminOrDocmanagerWithPermission(doc, Privileges.VIEW_DOCUMENT_FILES, Privileges.VIEW_DOCUMENT_META_DATA))
                                || wfType.equals(WorkflowSpecificModel.Types.OPINION_WORKFLOW)
                                || wfType.equals(WorkflowSpecificModel.Types.REVIEW_WORKFLOW))) {
                    continue;
                }
                if ((wfType.equals(WorkflowSpecificModel.Types.OPINION_WORKFLOW)
                        || wfType.equals(WorkflowSpecificModel.Types.CONFIRMATION_WORKFLOW)
                        || wfType.equals(WorkflowSpecificModel.Types.REVIEW_WORKFLOW))
                        && !BaseDialogBean.hasPermission(docRef, DocumentCommonModel.Privileges.EDIT_DOCUMENT)) {
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
            handleWorkflowChangedException((WorkflowChangedException) e, "Compound workflow action failed: data changed! ", "workflow_compound_save_failed", log);
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

    public static void handleWorkflowChangedException(WorkflowChangedException workflowChangedException, String logMessage, String displayMessageKey, Log log) {
        if (!log.isDebugEnabled()) {
            log.error(logMessage + workflowChangedException.getShortMessage());
        } else {
            log.debug(logMessage, workflowChangedException);
        }
        MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), displayMessageKey);
    }

    private boolean validate(FacesContext context, boolean checkFinished, boolean checkInvoice) {
        long profileStartTime = System.nanoTime();
        boolean valid = true;
        boolean activeResponsibleAssignTaskInSomeWorkFlow = false;
        // true if some orderAssignmentWorkflow in status NEW has no active responible task (but has some co-responsible tasks)
        boolean checkOrderAssignmentResponsibleTask = false;
        boolean missingOwnerAssignment = false;
        Set<String> missingOwnerMessageKeys = null;
        boolean hasForbiddenFlowsForFinished = false;
        boolean isCategoryEnabled = BeanHelper.getWorkflowService().getOrderAssignmentCategoryEnabled();
        Document doc = getParentDocument();
        boolean adminOrDocmanagerWithPermission = isAdminOrDocmanagerWithPermission(doc, Privileges.VIEW_DOCUMENT_FILES, Privileges.VIEW_DOCUMENT_META_DATA);
        for (Workflow block : compoundWorkflow.getWorkflows()) {
            boolean foundOwner = false;
            QName blockType = block.getNode().getType();
            boolean activeResponsibleAssigneeNeeded = blockType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW)
                    && !activeResponsibleAssignTaskInSomeWorkFlow && !isActiveResponsibleAssignedForDocument(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW, true);
            boolean activeResponsibleAssigneeAssigned = !activeResponsibleAssigneeNeeded;

            if ((WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW.equals(blockType)
                    && !adminOrDocmanagerWithPermission)
                    || WorkflowSpecificModel.Types.REVIEW_WORKFLOW.equals(blockType) ||
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
            boolean validOwnerAndDueDate = true;
            for (Task task : block.getTasks()) {
                final boolean activeResponsible = WorkflowUtil.isActiveResponsible(task) && !task.isStatus(Status.UNFINISHED);
                if (activeResponsibleAssigneeNeeded && StringUtils.isNotBlank(task.getOwnerName()) && activeResponsible) {
                    activeResponsibleAssignTaskInSomeWorkFlow = true;
                    activeResponsibleAssigneeAssigned = true;
                    missingOwnerAssignment = false;
                }
                hasOrderAssignmentActiveResponsible |= activeResponsible;
                foundOwner |= StringUtils.isNotBlank(task.getOwnerName());
                validOwnerAndDueDate = validateTaskOwnerAndDueDate(context, block, task);
                if (!validOwnerAndDueDate) {
                    break;
                }
            }
            valid &= validOwnerAndDueDate;
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
            if (!foundOwner) {
                String missingOwnerMsgKey = getMissingOwnerMessageKey(blockType);
                if (missingOwnerMsgKey != null) {
                    MessageUtil.addErrorMessage(context, missingOwnerMsgKey);
                    valid = false;
                }
            }
        }

        valid &= checkTaskDueDateRegression(compoundWorkflow.getWorkflows());

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

        if (checkFinished && hasForbiddenFlowsForFinished && DocumentStatus.FINISHED.getValueName().equals(doc.getDocStatus())) {
            valid = false;
            MessageUtil.addErrorMessage(context, adminOrDocmanagerWithPermission ? "workflow_start_failed_docFinished_admin" : "workflow_start_failed_docFinished");
        }

        if (checkInvoice) {
            valid = valid && validateInvoice();
        }
        if (!valid) {
            updatePanelGroup();
        }
        return valid;
    }

    private boolean validateTaskOwnerAndDueDate(FacesContext context, Workflow block, Task task) {
        QName taskType = task.getNode().getType();
        String taskOwnerMsg = getTaskOwnerMessage(block, taskType, task.isResponsible());
        if (taskType.equals(WorkflowSpecificModel.Types.INFORMATION_TASK)) {
            if (StringUtils.isBlank(task.getOwnerName())) {
                MessageUtil.addErrorMessage(context, "task_name_required", taskOwnerMsg);
                return false;
            }
        }
        else if (taskType.equals(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK)) {
            if (StringUtils.isBlank(task.getInstitutionName()) || task.getDueDate() == null) {
                MessageUtil.addErrorMessage(context, "task_name_and_due_required", taskOwnerMsg);
                return false;
            }
        }
        else if (StringUtils.isBlank(task.getOwnerName()) || task.getDueDate() == null) {
            MessageUtil.addErrorMessage(context, "task_name_and_due_required", taskOwnerMsg);
            return false;
        }
        return true;
    }

    private boolean checkTaskDueDateRegression(List<Workflow> workflows) {
        List<List<Workflow>> parallelWorkflowBlocks = collectParallelWorkflowBlocks(workflows);
        Date minAllowedDueDate = null;
        if (parallelWorkflowBlocks.size() == 1 && !parallelWorkflowBlocks.get(0).isEmpty() && parallelWorkflowBlocks.get(0).get(0).isParallelTasks()) {
            // one block with parallel tasks needs no additional check
            return true;
        }
        // these blocks must have strict due date order between each other:
        // minimum due date of block must be >= maximum due date in preceeding blocks
        // maximum due date of block must be <= minimum due date in succeeding blocks
        for (List<Workflow> block : parallelWorkflowBlocks) {
            if (block.isEmpty()) {
                // shouldn't normally happen, but this check is performed elsewhere
                continue;
            }
            Workflow blockFirstWorkflow = block.get(0);
            boolean checkTasksInsideBlock = !blockFirstWorkflow.isParallelTasks();
            Assert.isTrue(!checkTasksInsideBlock || block.size() == 1, "Not parallel tasks inside parallel block are not allowed.");
            Pair<Date, Date> minMaxDateInBlock = getCheckAndGetMaxDueDate(block, minAllowedDueDate, checkTasksInsideBlock);
            if (minMaxDateInBlock == null) {
                return false;
            }
            Date minDueDateInBlock = minMaxDateInBlock.getFirst();
            Date maxDueDateInBlock = minMaxDateInBlock.getSecond();
            // date mandatory check is not performed here, null values are ignored
            if (minDueDateInBlock != null && minAllowedDueDate != null && minDueDateInBlock.before(minAllowedDueDate)) {
                MessageUtil.addErrorMessage("workflow_save_error_dueDate_decreaseNotAllowed");
                return false;
            }
            if (minAllowedDueDate == null || maxDueDateInBlock != null) {
                minAllowedDueDate = maxDueDateInBlock;
            }
        }
        return true;
    }

    /** Return null indicating that regression inside the workflow block was wrong */
    private Pair<Date, Date> getCheckAndGetMaxDueDate(List<Workflow> block, Date minAllowedDueDate, boolean checkTasksInsideBlock) {
        Date minDueDate = null;
        Date maxDueDate = null;
        Date previousDueDate = null;
        for (Workflow workflow : block) {
            for (Task task : workflow.getTasks()) {
                if (!task.isStatus(Status.NEW)) {
                    continue;
                }
                Date taskDueDate = task.getDueDate();
                if (taskDueDate == null) {
                    continue;
                }
                if (checkTasksInsideBlock) {
                    if (previousDueDate != null && taskDueDate.before(previousDueDate)) {
                        MessageUtil.addErrorMessage("workflow_save_error_dueDate_decreaseNotAllowed");
                        return null;
                    }
                    previousDueDate = taskDueDate;
                }
                if (minDueDate == null || minDueDate.after(taskDueDate)) {
                    minDueDate = taskDueDate;
                }
                if (maxDueDate == null || taskDueDate.after(maxDueDate)) {
                    maxDueDate = taskDueDate;
                }
            }
        }
        return new Pair<Date, Date>(minDueDate, maxDueDate);
    }

    /**
     * Collect workflow blocks that don't need due date comparing between each others tasks inside one block,
     * but need comparing between different blocks
     * and may need comparing within single workflow depending on the workflow type
     */
    private List<List<Workflow>> collectParallelWorkflowBlocks(List<Workflow> workflows) {
        List<List<Workflow>> parallelWorkflowBlocks = new ArrayList<List<Workflow>>();
        List<Workflow> currentBlock = new ArrayList<Workflow>();
        for (Workflow currentWorkflow : workflows) {
            if (!currentBlock.isEmpty()) {
                Workflow previousWorkflow = currentBlock.get(currentBlock.size() - 1);
                if (previousWorkflow.isType(WorkflowSpecificModel.CAN_START_PARALLEL) && currentWorkflow.isType(WorkflowSpecificModel.CAN_START_PARALLEL)) {
                    currentBlock.add(currentWorkflow);
                } else {
                    parallelWorkflowBlocks.add(currentBlock);
                    currentBlock = new ArrayList<Workflow>();
                    currentBlock.add(currentWorkflow);
                }
            } else {
                currentBlock.add(currentWorkflow);
            }
        }
        if (!currentBlock.isEmpty()) {
            parallelWorkflowBlocks.add(currentBlock);
        }
        return parallelWorkflowBlocks;
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

    @Override
    protected Document getParentDocument() {
        return new Document(compoundWorkflow.getParent());
    }

    @Override
    protected boolean isAddLinkForWorkflow(Document doc, QName workflowType) {
        boolean addLinkForThisWorkflow = false;
        if (WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW.equals(workflowType)) {
            if (doc.isDocStatus(DocumentStatus.WORKING) && doc.hasPermission(Privileges.EDIT_DOCUMENT)) {
                addLinkForThisWorkflow = true;
            } else if (doc.isDocStatus(DocumentStatus.FINISHED) && isAdminOrDocmanagerWithPermission(doc, Privileges.VIEW_DOCUMENT_FILES, Privileges.VIEW_DOCUMENT_META_DATA)) {
                addLinkForThisWorkflow = true;
            }
        } else if (WorkflowSpecificModel.Types.OPINION_WORKFLOW.equals(workflowType)) {
            if (doc.isDocStatus(DocumentStatus.WORKING) && doc.hasPermission(Privileges.EDIT_DOCUMENT)) {
                addLinkForThisWorkflow = true;
            }
        } else if (WorkflowSpecificModel.Types.CONFIRMATION_WORKFLOW.equals(workflowType)) {
            if (doc.isDocStatus(DocumentStatus.WORKING) && doc.hasPermission(Privileges.EDIT_DOCUMENT) && getWorkflowService().isConfirmationWorkflowEnabled()) {
                addLinkForThisWorkflow = true;
            }
        } else if (WorkflowSpecificModel.Types.REVIEW_WORKFLOW.equals(workflowType)) {
            if (doc.isDocStatus(DocumentStatus.WORKING) && doc.hasPermission(Privileges.EDIT_DOCUMENT)) {
                addLinkForThisWorkflow = true;
            }
        } else if (WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW.equals(workflowType)) {
            if (doc.hasPermissions(Privileges.VIEW_DOCUMENT_FILES, Privileges.VIEW_DOCUMENT_META_DATA) && getWorkflowService().isOrderAssignmentWorkflowEnabled()) {
                addLinkForThisWorkflow = true;
            }
        } else if (WorkflowSpecificModel.Types.DOC_REGISTRATION_WORKFLOW.equals(workflowType)) {
            if (doc.isDocStatus(DocumentStatus.WORKING) && getUserService().isAdministrator()) {
                addLinkForThisWorkflow = true;
            }
        } else if (WorkflowSpecificModel.Types.INFORMATION_WORKFLOW.equals(workflowType)) {
            if (doc.hasPermissions(Privileges.VIEW_DOCUMENT_FILES, Privileges.VIEW_DOCUMENT_META_DATA)) {
                addLinkForThisWorkflow = true;
            }
        } else if (WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW.equals(workflowType)) {
            if (doc.hasPermissions(Privileges.VIEW_DOCUMENT_FILES, Privileges.VIEW_DOCUMENT_META_DATA)) {
                addLinkForThisWorkflow = true;
            }
        } else if (WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW.equals(workflowType)) {
            if (getWorkflowService().externalReviewWorkflowEnabled() && doc.isDocStatus(DocumentStatus.WORKING) && doc.hasPermission(Privileges.EDIT_DOCUMENT)) {
                addLinkForThisWorkflow = true;
            }
        } else if (WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_WORKFLOW.equals(workflowType)) {
            addLinkForThisWorkflow = false;
        } else {
            throw new UnableToPerformException("unknown workflow type " + workflowType.getLocalName()
                    + " - not sure if it should be displayed or not when configuring compound workflow bound to document");
        }
        return addLinkForThisWorkflow;
    }
}
