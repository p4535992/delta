package ee.webmedia.alfresco.workflow.web;


import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.exception.WorkflowActiveResponsibleTaskException;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.Task.Action;
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

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (validate(context)) {
            try {
                removeEmptyResponsibleTasks();
                getWorkflowService().saveCompoundWorkflow(workflow);
                getDocumentService().getDocumentLogService().addDocumentLog(workflow.getParent(), MessageUtil.getMessage("document_log_status_workflow"));
                
                resetState();
                return outcome;
            }
            catch (Exception e) {
                if (e instanceof WorkflowChangedException) {
                    log.debug("Compound workflow action failed: data changed!", e);
                    MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "workflow_compound_save_failed");
                }
                else if (e instanceof WorkflowActiveResponsibleTaskException) {
                    log.debug("Compound workflow action failed: more than one active responsible task!", e);
                    MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "workflow_compound_save_failed_responsible");
                }
                else {
                    throw e;
                }
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
        NodeRef document = new NodeRef(ActionUtil.getParam(event, "documentNodeRef"));
        workflow = getWorkflowService().getNewCompoundWorkflow(compoundWorkflowDefinition, document);
        updateFullAccess();
    }
    
    /**
     * Action listener for JSP.
     */
    public void startWorkflow(ActionEvent event) {
        log.debug("startWorkflow");
        if (validate(FacesContext.getCurrentInstance())) {
            try {
                removeEmptyResponsibleTasks();
                workflow = getWorkflowService().saveAndStartCompoundWorkflow(workflow);
            }
            catch (Exception e) {
                handleException(e, "workflow_compound_start_workflow_failed");
            }
            updatePanelGroup();
        }
    }

    /**
     * Action listener for JSP.
     */
    public void stopWorkflow(ActionEvent event) {
        log.debug("stopWorkflow");
        try {
            removeEmptyResponsibleTasks();
            workflow = getWorkflowService().saveAndStopCompoundWorkflow(workflow);
        }
        catch (Exception e) {
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
            removeEmptyResponsibleTasks();
            workflow = getWorkflowService().saveAndContinueCompoundWorkflow(workflow);
        }
        catch (Exception e) {
            handleException(e, "workflow_compound_continue_workflow_failed");
        }
        updatePanelGroup();
    }

    /**
     * Action listener for JSP.
     */
    public void finishWorkflow(ActionEvent event) {
        log.debug("finishWorkflow");
        try {
            removeEmptyResponsibleTasks();
            workflow = getWorkflowService().saveAndFinishCompoundWorkflow(workflow);
        }
        catch (Exception e) {
            handleException(e, "workflow_compound_finish_workflow_failed");
        }
        updatePanelGroup();
    }

    /**
     * Action listener for JSP.
     */
    public void copyWorkflow(ActionEvent event) {
        log.debug("copyWorkflow");
        if (validate(FacesContext.getCurrentInstance())) {
            try {
                removeEmptyResponsibleTasks();
                workflow = getWorkflowService().saveAndCopyCompoundWorkflow(workflow);
            }
            catch (Exception e) {
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
            removeEmptyResponsibleTasks();
            getWorkflowService().deleteCompoundWorkflow(workflow.getNode().getNodeRef());
            resetState();
            return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
        }
        catch (Exception e) {
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
        int index = Integer.parseInt(ActionUtil.getParam(event, "index"));
        int taskIndex = Integer.parseInt(ActionUtil.getParam(event, "taskIndex"));
        log.debug("cancelWorkflowTask: " + index + ", " + taskIndex);
        Workflow block = workflow.getWorkflows().get(index);
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
    
    ///// PROTECTED & PRIVATE METHODS /////
    
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

    @Override
    protected void updateFullAccess() {
        fullAccess = false;

        if (getUserService().isDocumentManager()) {
            fullAccess = true;
        } else if (getDocumentService().isDocumentOwner(workflow.getParent(), AuthenticationUtil.getRunAsUser())) {
            fullAccess = true;
        } else if (StringUtils.equals(workflow.getOwnerId(), AuthenticationUtil.getRunAsUser())) {
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
                    if (StringUtils.equals(task.getOwnerId(), user)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void handleException(Exception e, String failMsg) {
        if (e instanceof WorkflowChangedException) {
            log.debug("Compound workflow action failed: data changed!", e);
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "workflow_compound_save_failed");
        }
        else if (e instanceof WorkflowActiveResponsibleTaskException) {
            log.debug("Compound workflow action failed: more than one active responsible task!", e);
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "workflow_compound_save_failed_responsible");
        }
        else {
            log.error("Compound workflow action failed!", e);
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), failMsg);
        }
    }

    private boolean validate(FacesContext context) {
        boolean valid = true;
        for (Workflow block : workflow.getWorkflows()) {
            for (Task task : block.getTasks()) {
                if (task.getNode().hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE)) {
                    if (Boolean.TRUE.equals(task.getNode().getProperties().get(WorkflowSpecificModel.Props.ACTIVE))) {
                        // both fields must be empty or filled 
                        if (StringUtils.isBlank(task.getOwnerName()) != (task.getDueDate() == null)) {
                            valid = false;
                            String taskOwnerMsg = MessageUtil.getMessage(block.getNode().getType().getLocalName() + "_tasks");
                            MessageUtil.addErrorMessage(context, "task_name_and_due_required", taskOwnerMsg);
                            break;
                        }
                    }
                }
                else {
                    QName taskType = task.getNode().getType();
                    // only name is required for information tasks
                    if (taskType.equals(WorkflowSpecificModel.Types.INFORMATION_TASK)) {
                        if (StringUtils.isBlank(task.getOwnerName())) {
                            valid = false;
                            String taskOwnerMsg = MessageUtil.getMessage(block.getNode().getType().getLocalName() + "_tasks");
                            MessageUtil.addErrorMessage(context, "task_name_required", taskOwnerMsg);
                            break;
                        }
                    }
                    // both fields must be filled
                    else if (StringUtils.isBlank(task.getOwnerName()) || task.getDueDate() == null) {
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
        }
        return valid;
    }

}
