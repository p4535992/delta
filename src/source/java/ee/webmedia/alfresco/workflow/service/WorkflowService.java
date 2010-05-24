package ee.webmedia.alfresco.workflow.service;

import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListener;
import ee.webmedia.alfresco.workflow.service.type.WorkflowType;

/**
 * @author Alar Kvell
 */
public interface WorkflowService {
    String BEAN_NAME = "WmWorkflowService";

    void registerEventListener(WorkflowEventListener listener);

    // Workflow types

    void registerWorkflowType(WorkflowType workflowType);

    Map<QName, WorkflowType> getWorkflowTypes();

    // Eelseadistatud terviktöövoogude majandamine administraatori all

    List<CompoundWorkflowDefinition> getCompoundWorkflowDefinitions();

    CompoundWorkflowDefinition getCompoundWorkflowDefinition(NodeRef compoundWorkflowDefinition);

    CompoundWorkflowDefinition saveCompoundWorkflowDefinition(CompoundWorkflowDefinition compoundWorkflowDefinition);

    CompoundWorkflowDefinition getNewCompoundWorkflowDefinition();

    // Dokumendi ekraanil terviktöövoo alustamise menüü
    List<CompoundWorkflowDefinition> getCompoundWorkflowDefinitions(QName documentType);

    // new in-memory object, based on existing compoundWorkflow definition
    CompoundWorkflow getNewCompoundWorkflow(NodeRef compoundWorkflowDefinition, NodeRef parent);

    // get existing object from repository
    List<CompoundWorkflow> getCompoundWorkflows(NodeRef parent);

    CompoundWorkflow getCompoundWorkflow(NodeRef compoundWorkflow);

    CompoundWorkflow saveCompoundWorkflow(CompoundWorkflow compoundWorkflow);

    void deleteCompoundWorkflow(NodeRef compoundWorkflow);

    CompoundWorkflow saveAndStartCompoundWorkflow(CompoundWorkflow compoundWorkflow);

    CompoundWorkflow startCompoundWorkflow(NodeRef compoundWorkflow);

    CompoundWorkflow saveAndFinishCompoundWorkflow(CompoundWorkflow compoundWorkflow);

    CompoundWorkflow finishCompoundWorkflow(NodeRef compoundWorkflow);

    CompoundWorkflow saveAndStopCompoundWorkflow(CompoundWorkflow compoundWorkflow);

    CompoundWorkflow stopCompoundWorkflow(NodeRef compoundWorkflow);

    CompoundWorkflow saveAndContinueCompoundWorkflow(CompoundWorkflow compoundWorkflow);

    CompoundWorkflow continueCompoundWorkflow(NodeRef compoundWorkflow);

    void stopAllCompoundWorkflows(NodeRef parent);

    void continueAllCompoundWorkflows(NodeRef parent);

    CompoundWorkflow saveAndCopyCompoundWorkflow(CompoundWorkflow compoundWorkflow);
    
    int getActiveResponsibleAssignmentTasks(NodeRef document);

    void addNewWorkflow(CompoundWorkflow compoundWorkflow, QName workflowTypeQName, int index);

    /**
     * Save task properties.
     * 
     * @throws WorkflowChangedException If task's status is not IN_PROGRESS or ownerId does not equal to current run-as user.
     */
    void saveInProgressTask(Task task) throws WorkflowChangedException;

    /**
     * Save task properties and finish task with specified outcome.
     * 
     * @throws WorkflowChangedException If task's status is not IN_PROGRESS or ownerId does not equal to current run-as user.
     */
    void finishInProgressTask(Task task, int outcomeIndex) throws WorkflowChangedException;

    /**
     * Get task from repository.
     * 
     * @param fetchWorkflow if {@code false}, then parent workflow is not fetched, so {@code task.getParent()} returns {@code null}. Workflow's parent
     *            compoundWorkflow is not fetched, so {@code workflow.getParent()} returns {@code null}.
     */
    Task getTask(NodeRef task, boolean fetchWorkflow);

    void setTaskOwner(NodeRef task, String ownerId);

    // Filtering

    /**
     * Is current user the owner of any CompoundWorkflow.
     */
    boolean isOwner(List<CompoundWorkflow> compoundWorkflows);

    /**
     * Is current user the owner CompoundWorkflow.
     */
    boolean isOwner(CompoundWorkflow compoundWorkflow);

    boolean isOwner(Task task);

    boolean isOwnerOfInProgressAssignmentTask(CompoundWorkflow compoundWorkflow);

    List<Task> getMyTasksInProgress(List<CompoundWorkflow> compoundWorkflows);

    /**
     * If document has at least one compoundWorkflow and all compoundWorkflows have {@link Status#FINISHED}.
     */
    boolean hasAllFinishedCompoundWorkflows(NodeRef parent);

    boolean hasInprogressCompoundWorkflows(NodeRef parent);

    boolean hasNoStoppedOrInprogressCompoundWorkflows(NodeRef parent);

}
