package ee.webmedia.alfresco.workflow.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.utils.MessageDataWrapper;
import ee.webmedia.alfresco.utils.Predicate;
import ee.webmedia.alfresco.utils.UnableToPerformMultiReasonException;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowWithObject;
import ee.webmedia.alfresco.workflow.model.RelatedUrl;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListener;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListenerWithModifications;
import ee.webmedia.alfresco.workflow.service.event.WorkflowMultiEventListener;
import ee.webmedia.alfresco.workflow.generated.LinkedReviewTaskType;
import ee.webmedia.alfresco.workflow.generated.DeleteLinkedReviewTaskType;
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

    List<CompoundWorkflowDefinition> getActiveCompoundWorkflowDefinitions(boolean getUserFullName);

    CompoundWorkflowDefinition getCompoundWorkflowDefinition(NodeRef compoundWorkflowDefinition);

    CompoundWorkflowDefinition saveCompoundWorkflowDefinition(CompoundWorkflowDefinition compoundWorkflowDefinition);

    CompoundWorkflowDefinition getNewCompoundWorkflowDefinition();

    // new in-memory object, based on existing compoundWorkflow definition
    CompoundWorkflow getNewCompoundWorkflow(NodeRef compoundWorkflowDefinition, NodeRef parent);

    // get existing object from repository
    List<CompoundWorkflow> getCompoundWorkflows(NodeRef parent);

    CompoundWorkflow getCompoundWorkflow(NodeRef compoundWorkflow);

    CompoundWorkflow saveCompoundWorkflow(CompoundWorkflow compoundWorkflow);

    /**
     * @param originalAssignmentTask - task that will be delegated(originalAssignmentTask.parent contains information about new tasks and
     *            originalAssignmentTask.parent.parent contains information about new workflows)
     * @return MessageDataWrapper with 0 or more non-error messages to be shown to the user
     * @throws UnableToPerformMultiReasonException - when at least one error-message should be shown to the user
     */
    MessageDataWrapper delegate(Task originalAssignmentTask) throws UnableToPerformMultiReasonException;

    void deleteCompoundWorkflow(NodeRef compoundWorkflow, boolean validateStatuses);

    List<Task> getTasks4DelegationHistory(Node delegatableTask);

    CompoundWorkflow startCompoundWorkflow(CompoundWorkflow compoundWorkflow);

    CompoundWorkflow finishCompoundWorkflow(CompoundWorkflow compoundWorkflow);

    CompoundWorkflow stopCompoundWorkflow(CompoundWorkflow compoundWorkflow);

    CompoundWorkflow continueCompoundWorkflow(CompoundWorkflow compoundWorkflow);

    CompoundWorkflow copyAndResetCompoundWorkflow(NodeRef compoundWorkflowRef);

    /**
     * For compoundWorkflow.type=DOCUMENT_WORKFLOW, count active responsible tasks in parent document's compound workflows,
     * for compoundWorkflow.type=INDEPENDENT_WORKFLOW, CASE_FILE_WORKFLOW, count active responsible tasks in given compound workflow only
     */
    int getConnectedActiveResponsibleTasksCount(CompoundWorkflow compoundWorkflow, QName workflowType);

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
     * @param removedFiles
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

    Set<Task> getTasksInProgress(NodeRef docRef);

    void setTaskOwner(NodeRef task, String ownerId, boolean retainPreviousOwnerId);

    void setCompoundWorkflowOwner(NodeRef task, String ownerId, boolean retainPreviousOwnerId);

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

    void finishTasksByRegisteringReplyLetter(NodeRef docRef, String comment);

    boolean isSendableExternalWorkflowDoc(NodeRef docNodeRef);

    void finishInProgressExternalReviewTask(Task taskOriginal, String comment, String outcome, Date dateCompleted, String dvkId) throws WorkflowChangedException;

    boolean isInternalTesting();

    WmNode getTaskTemplateByType(QName taskType);

    boolean isRecievedExternalReviewTask(Task task);

    boolean externalReviewWorkflowEnabled();

    void addOtherCompundWorkflows(CompoundWorkflow compoundWorkflow);

    void finishUserActiveResponsibleInProgressTask(NodeRef docRef, String comment);

    boolean hasUnfinishedReviewTasks(NodeRef docNode);

    boolean hasTaskOfType(NodeRef docRef, QName... workflowTypes);

    boolean getOrderAssignmentCategoryEnabled();

    boolean isOrderAssignmentWorkflowEnabled();

    boolean isConfirmationWorkflowEnabled();

    boolean hasInProgressOtherUserOrderAssignmentTasks(NodeRef originalDocRef);

    CompoundWorkflow getNewCompoundWorkflow(Node compoundWorkflowDefinition, NodeRef parent);

    void createDueDateExtension(CompoundWorkflow compoundWorkflow, NodeRef nodeRef);

    void registerMultiEventListener(WorkflowMultiEventListener listener);

    Task getTaskWithoutParentAndChildren(NodeRef nodeRef, Workflow workflow, boolean copy);

    Map<QName, WorkflowType> getWorkflowTypesByTask();

    NodeRef getCompoundWorkflowDefinitionByName(String newCompWorkflowDefinitionName, String runAsUser, boolean checkGlobalDefinitions);

    NodeRef createCompoundWorkflowDefinition(CompoundWorkflow compoundWorkflow, String userId, String newCompWorkflowDefinitionName);

    NodeRef overwriteExistingCompoundWorkflowDefinition(CompoundWorkflow compoundWorkflow, String userId, String existingCompWorkflowDefinitionName);

    void deleteCompoundWorkflowDefinition(String existingCompWorkflowDefinitionName, String runAsUser);

    List<CompoundWorkflowDefinition> getUserCompoundWorkflowDefinitions(String userId);

    List<ChildAssociationRef> getAllCompoundWorkflowDefinitionRefs();

    boolean isIndependentWorkflowEnabled();

    NodeRef getIndependentWorkflowsRoot();

    List<CompoundWorkflowDefinition> getIndependentCompoundWorkflowDefinitions(String userId);

    boolean isWorkflowTitleEnabled();

    void updateMainDocument(NodeRef workflowRef, NodeRef mainDocRef);

    void updateIndependentWorkflowDocumentData(NodeRef workflowRef, NodeRef mainDocRef, List<NodeRef> documentsToSign);

    CompoundWorkflowWithObject getCompoundWorkflowWithObject(NodeRef compoundWorkflowRef);

    List<Document> getCompoundWorkflowDocuments(NodeRef compoundWorkflowRef);

    int getCompoundWorkflowDocumentCount(NodeRef compoundWorkflowRef);

    List<NodeRef> getCompoundWorkflowDocumentRefs(NodeRef compoundWorkflowRef);

    boolean hasTwoInProgressOrStoppedCWorkflowsWithMultipleWorkflows(CompoundWorkflow cWorkflow, boolean checkCurrentWorkflow);

    boolean isDocumentWorkflow(NodeRef compoundWorkflowRef);

    CompoundWorkflowType getWorkflowCompoundWorkflowType(NodeRef workflowRef);

    Map<NodeRef, List<File>> getCompoundWorkflowSigningFiles(CompoundWorkflow compoundWorkflow);

    String getIndependentCompoundWorkflowProcedureId(NodeRef compoundWorkflowRef);

    boolean isIndependentWorkflow(NodeRef compoundWorkflowRef);

    CompoundWorkflow reopenCompoundWorkflow(CompoundWorkflow compoundWorkflow);

    List<Task> getWorkflowTasks(NodeRef workflow);

    Map<QName, Collection<QName>> getTaskDataTypeDefaultAspects();

    List<CompoundWorkflowDefinition> getCompoundWorkflowDefinitionsByType(String userId, CompoundWorkflowType workflowType);

    List<RelatedUrl> getRelatedUrls(NodeRef compoundWorkflowRef);

    void saveRelatedUrl(RelatedUrl relatedUrl, NodeRef compoundWorkflowRef);

    RelatedUrl getRelatedUrl(NodeRef relatedUrlNodeRef);

    void deleteRelatedUrl(NodeRef relatedUrlRef);

    boolean isDocumentWorkflowEnabled();

    boolean isWorkflowEnabled();

    void removeDeletedDocumentFromCompoundWorkflows(NodeRef docRef);

    List<NodeRef> getCompoundWorkflowSigningDocumentRefs(NodeRef compoundWorkflowRef);

    CompoundWorkflowType getCompoundWorkflowType(NodeRef compoundWorkflowRef);

    boolean hasStartedCompoundWorkflows(NodeRef docRef);

    void updateCompWorkflowDocsSearchProps(CompoundWorkflow cWorkflow);

    void updateDocumentCompWorkflowSearchProps(NodeRef docRef);

    void changeTasksDocType(NodeRef docRef, String newTypeId);

    boolean isReviewToOtherOrgEnabled();

    NodeRef importLinkedReviewTask(LinkedReviewTaskType taskToImport, String dvkId);

    void markLinkedReviewTaskDeleted(DeleteLinkedReviewTaskType deletedTask);

    void updateTaskSearchableProperties(NodeRef nodeRef);

    boolean isGroupAssignmentWorkflowEnabled();

    Map<QName, QName> getTaskPrefixedQNames();

    Map<QName, List<QName>> getTaskDataTypeDefaultProps();

    QName getNodeRefType(NodeRef nodeRef);

    Task createTaskInMemory(NodeRef wfRef, WorkflowType workflowType, Map<QName, Serializable> props);

    List<QName> getTaskDataTypeSearchableProps();

    CompoundWorkflow copyCompoundWorkflowInMemory(CompoundWorkflow compoundWorkflowOriginal);

    Task replaceTask(Task replacementTask, CompoundWorkflow compoundWorkflow);

    Map<String, Object> getTaskChangedProperties(Task task);

    void injectWorkflows(CompoundWorkflow compoundWorkflow, int index, List<Workflow> workflowsToInsert);

    void injectTasks(Workflow workflow, int index, List<Task> tasksToInsert);

    void retrieveTaskFiles(Task task, List<NodeRef> taskFiles);

}