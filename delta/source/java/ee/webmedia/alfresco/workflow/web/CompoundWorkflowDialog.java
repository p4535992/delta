package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.workflow.web.TaskListCommentComponent.TASK_INDEX;
import static ee.webmedia.alfresco.workflow.web.TaskListGenerator.WF_INDEX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.web.DocumentDialog;
import ee.webmedia.alfresco.notification.exception.EmailAttachmentSizeLimitException;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.exception.WorkflowActiveResponsibleTaskException;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Task.Action;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import ee.webmedia.alfresco.workflow.web.TaskListCommentComponent.CommentEvent;

/**
 * Dialog bean for working with one compound workflow instance which is tied to a document.
 * 
 * @author Erko Hansar
 */
public class CompoundWorkflowDialog extends CompoundWorkflowDefinitionDialog {

    private static final long serialVersionUID = 1L;

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(CompoundWorkflowDialog.class);

    private transient UserService userService;
    private transient DocumentService documentService;
    private transient DocumentLogService documentLogService;
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
        boolean checkFinished = WorkflowUtil.isStatus(workflow, Status.IN_PROGRESS);
        if (validate(context, checkFinished, false)) {
            try {
                removeEmptyTasks();
                getWorkflowService().saveCompoundWorkflow(workflow);
                if (isUnsavedWorkFlow) {
                    getDocumentLogService().addDocumentLog(workflow.getParent(), MessageUtil.getMessage("document_log_status_workflow"));
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
        workflow = getWorkflowService().getCompoundWorkflow(nodeRef);
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
            workflow = getWorkflowService().getNewCompoundWorkflow(compoundWorkflowDefinition, document);
            updateFullAccess();
            isUnsavedWorkFlow = true;
        } catch (InvalidNodeRefException e) {
            log.warn("Failed to create a new compound workflow instance because someone has probably deleted the compound workflow definition.");
        }
    }

    /**
     * Action listener for JSP.
     */
    public void startWorkflow(ActionEvent event) {
        log.debug("startWorkflow");
        if (validate(FacesContext.getCurrentInstance(), true, false)) {
            try {
                removeEmptyTasks();
                if (isUnsavedWorkFlow) {
                    getDocumentLogService().addDocumentLog(workflow.getParent(), MessageUtil.getMessage("document_log_status_workflow"));
                }
                workflow = getWorkflowService().saveAndStartCompoundWorkflow(workflow);
                if (isUnsavedWorkFlow) {
                    isUnsavedWorkFlow = false;
                }
                MessageUtil.addInfoMessage("workflow_compound_start_success");
            } catch (Exception e) {
                handleException(e, "workflow_compound_start_workflow_failed");
            }
            updatePanelGroup();
        }
    }

    /**
     * Action listener for JSP.
     */
    public void stopWorkflow(@SuppressWarnings("unused") ActionEvent event) {
        log.debug("stopWorkflow");
        try {
            removeEmptyTasks();
            if (validate(FacesContext.getCurrentInstance(), false, true)) {
                workflow = getWorkflowService().saveAndStopCompoundWorkflow(workflow);
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
    public void continueWorkflow(@SuppressWarnings("unused") ActionEvent event) {
        log.debug("continueWorkflow");
        try {
            removeEmptyTasks();
            if (validate(FacesContext.getCurrentInstance(), true, false)) {
                workflow = getWorkflowService().saveAndContinueCompoundWorkflow(workflow);
                MessageUtil.addInfoMessage("workflow_compound_continue_success");
            }
        } catch (Exception e) {
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
            workflow = getWorkflowService().saveAndFinishCompoundWorkflow(workflow);
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
        if (validate(FacesContext.getCurrentInstance(), false, false)) {
            try {
                removeEmptyTasks();
                workflow = getWorkflowService().saveAndCopyCompoundWorkflow(workflow);
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
            getWorkflowService().deleteCompoundWorkflow(workflow.getNode().getNodeRef());
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
        workflow.setOwnerId(username);
        workflow.setOwnerName(getUserService().getUserFullName(username));
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
        Workflow block = workflow.getWorkflows().get(wfIndex);
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

        Workflow block = workflow.getWorkflows().get(index);
        Task task = block.getTasks().get(taskIndex);
        task.setAction(Action.FINISH);
        task.setComment(comment);
        updatePanelGroup();
    }

    // /// PROTECTED & PRIVATE METHODS /////

    @Override
    protected String getConfigArea() {
        return null;
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
        } else if (getDocumentService().isDocumentOwner(workflow.getParent(), AuthenticationUtil.getRunAsUser())) {
            fullAccess = true;
        } else if (StringUtils.equals(workflow.getOwnerId(), AuthenticationUtil.getRunAsUser())) {
            fullAccess = true;
        } else if (StringUtils.equals(workflow.getOwnerId(), AuthenticationUtil.getFullyAuthenticatedUser())) {
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
        for (Workflow block : workflow.getWorkflows()) {
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
        if (e instanceof WorkflowChangedException) {
            log.debug("Compound workflow action failed: data changed!", e);
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "workflow_compound_save_failed");
        } else if (e instanceof WorkflowActiveResponsibleTaskException) {
            log.debug("Compound workflow action failed: more than one active responsible task!", e);
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "workflow_compound_save_failed_responsible");
        } else if (e instanceof EmailAttachmentSizeLimitException) {
            log.debug("Compound workflow action failed: email attachment exceeded size limit set in parameter!", e);
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "notification_zip_size_too_large");
        } else if (e instanceof NodeLockedException) {
            log.debug("Compound workflow action failed: document is locked!", e);
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), new String[] { failMsg, "document_error_docLocked" });
        } else if (e instanceof InvalidNodeRefException) {
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addErrorMessage(context, "workflow_task_save_failed_docDeleted");
            context.getApplication().getNavigationHandler().handleNavigation(context, null, AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME);
        } else {
            log.error("Compound workflow action failed!", e);
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), failMsg);
        }
    }

    private boolean validate(FacesContext context, boolean checkFinished, boolean allowInactiveResponsibleTask) {
        boolean valid = true;
        boolean activeResponsibleAssignedInSomeWorkFlow = false;
        boolean missingOwnerAssignment = false;
        Boolean missingInformationTasks = null;
        Set<String> missingOwnerMessageKeys = null;
        boolean hasForbiddenFlowsForFinished = false;
        for (Workflow block : workflow.getWorkflows()) {
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

            for (Task task : block.getTasks()) {
                final boolean activeResponsible = WorkflowUtil.isActiveResponsible(task);
                boolean inactiveResponsible = false;
                if(allowInactiveResponsibleTask){
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
            if (DocumentStatus.FINISHED.equals(docStatus)) {
                valid = false;
                MessageUtil.addErrorMessage(context, "workflow_start_failed_docFinished");
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
