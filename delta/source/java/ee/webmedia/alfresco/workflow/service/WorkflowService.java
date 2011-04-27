package ee.webmedia.alfresco.workflow.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.utils.MessageDataWrapper;
import ee.webmedia.alfresco.utils.Predicate;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListener;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListenerWithModifications;
import ee.webmedia.alfresco.workflow.service.type.WorkflowType;

/**
 * @author Alar Kvell
 */
public interface WorkflowService {
    String BEAN_NAME = "WmWorkflowService";

    /**
     * Register listener to receive workflow events. Listener is called at the end of each service call, not immediately.
     */
    void registerEventListener(WorkflowEventListener listener);

    /**
     * Register listener to receive workflow events immediately.
     */
    void registerImmediateEventListener(WorkflowEventListenerWithModifications listener);

    // Workflow types

    /**
     * Register workflow type. If {@code workflowType} implements {@link WorkflowEventListener} and/or {@link WorkflowEventListenerWithModifications}, it
     * receives events immediately for both interfaces.
     */
    void registerWorkflowType(WorkflowType workflowType);

    Map<QName, WorkflowType> getWorkflowTypes();

    // Eelseadistatud terviktöövoogude majandamine administraatori all

    List<CompoundWorkflowDefinition> getCompoundWorkflowDefinitions();

    CompoundWorkflowDefinition getCompoundWorkflowDefinition(NodeRef compoundWorkflowDefinition);

    CompoundWorkflowDefinition saveCompoundWorkflowDefinition(CompoundWorkflowDefinition compoundWorkflowDefinition);

    CompoundWorkflowDefinition getNewCompoundWorkflowDefinition();

    // Dokumendi ekraanil terviktöövoo alustamise menüü
    List<CompoundWorkflowDefinition> getCompoundWorkflowDefinitions(QName documentType, String documentStatus);

    // new in-memory object, based on existing compoundWorkflow definition
    CompoundWorkflow getNewCompoundWorkflow(NodeRef compoundWorkflowDefinition, NodeRef parent);

    // get existing object from repository
    List<CompoundWorkflow> getCompoundWorkflows(NodeRef parent);

    CompoundWorkflow getCompoundWorkflow(NodeRef compoundWorkflow);

    CompoundWorkflow saveCompoundWorkflow(CompoundWorkflow compoundWorkflow);

    /**
     * @param originalAssignmentTask - task that will be delegated(originalAssignmentTask.parent contains information about new tasks and
     *            originalAssignmentTask.parent.parent
     *            contains information about new workflows)
     */
    Pair<MessageDataWrapper, CompoundWorkflow> delegate(Task originalAssignmentTask);

    void deleteCompoundWorkflow(NodeRef compoundWorkflow);

    List<Task> getTasks4DelegationHistory(Node delegatableTask);

    CompoundWorkflow saveAndStartCompoundWorkflow(CompoundWorkflow compoundWorkflow);

    CompoundWorkflow startCompoundWorkflow(NodeRef compoundWorkflow);

    CompoundWorkflow saveAndFinishCompoundWorkflow(CompoundWorkflow compoundWorkflow);

    CompoundWorkflow saveAndStopCompoundWorkflow(CompoundWorkflow compoundWorkflow);

    CompoundWorkflow stopCompoundWorkflow(NodeRef compoundWorkflow);

    CompoundWorkflow saveAndContinueCompoundWorkflow(CompoundWorkflow compoundWorkflow);

    CompoundWorkflow continueCompoundWorkflow(NodeRef compoundWorkflow);

    void stopAllCompoundWorkflows(NodeRef parent);

    void continueAllCompoundWorkflows(NodeRef parent);

    CompoundWorkflow saveAndCopyCompoundWorkflow(CompoundWorkflow compoundWorkflow);

    int getActiveResponsibleAssignmentTasks(NodeRef document);

    /**
     * @param compoundWorkflow
     * @param workflowTypeQName
     * @param index - position where new workflow is added in the compoundWorkflow workflows list
     * @param validateWorkflowIsNew
     * @return workflow added under <code>compoundWorkflow</code> with given type, to given position
     */
    Workflow addNewWorkflow(CompoundWorkflow compoundWorkflow, QName workflowTypeQName, int index, boolean validateWorkflowIsNew);

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

    Set<Task> getTasks(NodeRef docRef, Predicate<Task> predicate);

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

    boolean isOwnerOfInProgressActiveResponsibleAssignmentTask(NodeRef docRef);

    boolean isOwnerOfInProgressExternalReviewTask(CompoundWorkflow cWorkflow);

    List<Task> getMyTasksInProgress(List<CompoundWorkflow> compoundWorkflows);

    /**
     * If document has at least one compoundWorkflow and all compoundWorkflows have {@link Status#FINISHED}.
     */
    boolean hasAllFinishedCompoundWorkflows(NodeRef parent);

    boolean hasInprogressCompoundWorkflows(NodeRef parent);

    boolean hasNoStoppedOrInprogressCompoundWorkflows(NodeRef parent);

    void finishCompoundWorkflowsOnRegisterDoc(NodeRef docRef, String comment);

    boolean isSendableExternalWorkflowDoc(NodeRef docNodeRef);

    void finishInProgressExternalReviewTask(Task taskOriginal, String comment, String outcome, Date dateCompleted, String dvkId) throws WorkflowChangedException;

    boolean isInternalTesting();

    WmNode getTaskTemplateByType(QName taskType);

    boolean isRecievedExternalReviewTask(Task task);

    boolean externalReviewWorkflowEnabled();

    void addOtherCompundWorkflows(CompoundWorkflow compoundWorkflow);

}
