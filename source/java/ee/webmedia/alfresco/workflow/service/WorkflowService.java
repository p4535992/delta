package ee.webmedia.alfresco.workflow.service;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.utils.Predicate;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException;
import ee.webmedia.alfresco.workflow.generated.DeleteLinkedReviewTaskType;
import ee.webmedia.alfresco.workflow.generated.LinkedReviewTaskType;
import ee.webmedia.alfresco.workflow.model.Comment;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.RelatedUrl;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListener;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListenerWithModifications;
import ee.webmedia.alfresco.workflow.service.event.WorkflowMultiEventListener;
import ee.webmedia.alfresco.workflow.service.type.WorkflowType;

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

    // Eelseadistatud terviktöövoogude majandamine administraatori all

    List<CompoundWorkflowDefinition> getActiveCompoundWorkflowDefinitions(boolean getUserFullName);

    CompoundWorkflowDefinition getCompoundWorkflowDefinition(NodeRef compoundWorkflowDefinition);

    CompoundWorkflowDefinition saveCompoundWorkflowDefinition(CompoundWorkflowDefinition compoundWorkflowDefinition);

    CompoundWorkflowDefinition getNewCompoundWorkflowDefinition();

    // new in-memory object, based on existing compoundWorkflow definition
    CompoundWorkflow getNewCompoundWorkflow(NodeRef compoundWorkflowDefinition, NodeRef parent);

    // get existing object from repository
    List<CompoundWorkflow> getCompoundWorkflows(NodeRef parent);

    List<CompoundWorkflow> getCompoundWorkflows(NodeRef parent, NodeRef nodeRefToSkip);

    CompoundWorkflow getCompoundWorkflow(NodeRef compoundWorkflow);

    CompoundWorkflow getCompoundWorkflow(NodeRef nodeRef, boolean loadTasks, boolean loadWorkflows);

    CompoundWorkflow saveCompoundWorkflow(CompoundWorkflow compoundWorkflow);

    CompoundWorkflow delegate(Task assignmentTaskOriginal);

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

    int getConnectedActiveResponsibleTasksCount(CompoundWorkflow compoundWorkflow, QName workflowType, boolean allowFinished, NodeRef compoundWorkflowToSkip);

    int getConnectedActiveResponsibleTasksCount(List<CompoundWorkflow> compoundWorkflows, boolean allowFinished, NodeRef compoundWorkflowToSkip);

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

    Set<Task> getTasksInProgress(NodeRef docRef);

    void setTaskOwner(NodeRef task, String ownerId, boolean retainPreviousOwnerId);

    boolean containsDocumentsWithLimitedActivities(NodeRef compoundWorkflowRef);

    void setCompoundWorkflowOwner(NodeRef task, String ownerId, boolean retainPreviousOwnerId);

    // Filtering

    /**
     * Is current user the owner of any CompoundWorkflow.
     */
    boolean isCompoundWorkflowOwner(List<NodeRef> compoundWorkflows);

    boolean isOwnerOfInProgressActiveResponsibleAssignmentTask(NodeRef docRef);

    List<Task> getMyTasksInProgress(List<NodeRef> compoundWorkflows);

    /**
     * If document has at least one compoundWorkflow and all compoundWorkflows have {@link Status#FINISHED}.
     * @param propertyTypes TODO
     */
    boolean hasAllFinishedCompoundWorkflows(NodeRef parent, Map<Long, QName> propertyTypes);

    boolean hasInprogressCompoundWorkflows(NodeRef parent);

    boolean hasNoStoppedOrInprogressCompoundWorkflows(NodeRef parent);

    void finishTasksByRegisteringReplyLetter(NodeRef docRef, String comment);

    boolean isSendableExternalWorkflowDoc(NodeRef docNodeRef);

    void finishInProgressExternalReviewTask(Task taskOriginal, String comment, String outcome, Date dateCompleted, String dvkId) throws WorkflowChangedException;

    WmNode getTaskTemplateByType(QName taskType);

    boolean isRecievedExternalReviewTask(Task task);

    void addOtherCompundWorkflows(CompoundWorkflow compoundWorkflow);

    void finishUserActiveResponsibleInProgressTask(NodeRef docRef, String comment);

    boolean hasUnfinishedReviewTasks(NodeRef docNode);

    boolean hasTaskOfType(NodeRef docRef, QName... workflowTypes);

    boolean hasInProgressOtherUserOrderAssignmentTasks(NodeRef originalDocRef);

    CompoundWorkflow getNewCompoundWorkflow(Node compoundWorkflowDefinition, NodeRef parent);

    List<String> checkAndAddMissingOwnerEmails(CompoundWorkflow compoundWorkflow);

    void createDueDateExtension(String reason, Date newDate, Date dueDate, Task initiatingTask, NodeRef containerRef, String dueDateExtenderUsername,
            String dueDateExtenderUserFullname);

    void registerMultiEventListener(WorkflowMultiEventListener listener);

    Task getTaskWithoutParentAndChildren(NodeRef nodeRef, Workflow workflow, boolean copy);
    
    Map<NodeRef, Task> getTasksWithCompoundWorkflowRef(List<NodeRef> taskRefs);

    NodeRef getCompoundWorkflowDefinitionByName(String newCompWorkflowDefinitionName, String runAsUser, boolean checkGlobalDefinitions);

    NodeRef createCompoundWorkflowDefinition(CompoundWorkflow compoundWorkflow, String userId, String newCompWorkflowDefinitionName);

    NodeRef overwriteExistingCompoundWorkflowDefinition(CompoundWorkflow compoundWorkflow, String userId, String existingCompWorkflowDefinitionName);

    void deleteCompoundWorkflowDefinition(String existingCompWorkflowDefinitionName, String runAsUser);

    List<CompoundWorkflowDefinition> getUserCompoundWorkflowDefinitions(String userId);

    List<ChildAssociationRef> getAllCompoundWorkflowDefinitionRefs();

    NodeRef getIndependentWorkflowsRoot();

    List<CompoundWorkflowDefinition> getIndependentCompoundWorkflowDefinitions(String userId);

    void updateMainDocument(NodeRef workflowRef, NodeRef mainDocRef);

    void updateIndependentWorkflowDocumentData(NodeRef workflowRef, NodeRef mainDocRef, List<NodeRef> documentsToSign);

    List<Document> getCompoundWorkflowDocuments(NodeRef compoundWorkflowRef);

    int getCompoundWorkflowDocumentCount(NodeRef compoundWorkflowRef);

    List<NodeRef> getCompoundWorkflowDocumentRefs(NodeRef compoundWorkflowRef);

    boolean hasTwoInProgressOrStoppedCWorkflowsWithMultipleWorkflows(CompoundWorkflow cWorkflow, boolean checkCurrentWorkflow);

    boolean isDocumentWorkflow(NodeRef compoundWorkflowRef);

    CompoundWorkflowType getWorkflowCompoundWorkflowType(NodeRef workflowRef);

    Map<NodeRef, List<File>> getCompoundWorkflowSigningFiles(NodeRef compoundWorkflowRef);

    String getIndependentCompoundWorkflowProcedureId(NodeRef compoundWorkflowRef);

    boolean isIndependentWorkflow(NodeRef compoundWorkflowRef);

    CompoundWorkflow reopenCompoundWorkflow(CompoundWorkflow compoundWorkflow);

    List<CompoundWorkflowDefinition> getCompoundWorkflowDefinitionsByType(String userId, CompoundWorkflowType workflowType);

    List<RelatedUrl> getRelatedUrls(NodeRef compoundWorkflowRef);

    void saveRelatedUrl(RelatedUrl relatedUrl, NodeRef compoundWorkflowRef);

    RelatedUrl getRelatedUrl(NodeRef relatedUrlNodeRef);

    void deleteRelatedUrl(NodeRef relatedUrlRef);

    void removeDeletedDocumentFromCompoundWorkflows(NodeRef docRef);

    List<NodeRef> getCompoundWorkflowSigningDocumentRefs(NodeRef compoundWorkflowRef);

    CompoundWorkflowType getCompoundWorkflowType(NodeRef compoundWorkflowRef);

    boolean hasStartedCompoundWorkflows(NodeRef docRef);

    boolean isOwner(NodeRef compoundWorkflowNodeRef);

    void updateCompWorkflowDocsSearchProps(CompoundWorkflow cWorkflow);

    void updateDocumentCompWorkflowSearchProps(NodeRef docRef);

    void changeTasksDocType(NodeRef docRef, String newTypeId);

    NodeRef importLinkedReviewTask(LinkedReviewTaskType taskToImport, String dvkId);

    NodeRef markLinkedReviewTaskDeleted(DeleteLinkedReviewTaskType deletedTask);

    void updateTaskSearchableProperties(NodeRef nodeRef);

    QName getNodeRefType(NodeRef nodeRef);

    Task createTaskInMemory(NodeRef wfRef, WorkflowType workflowType, Map<QName, Serializable> props);

    CompoundWorkflow copyCompoundWorkflowInMemory(CompoundWorkflow compoundWorkflowOriginal);

    Task replaceTask(Task replacementTask, CompoundWorkflow compoundWorkflow);

    Map<String, Object> getTaskChangedProperties(Task task);

    void injectWorkflows(CompoundWorkflow compoundWorkflow, int index, List<Workflow> workflowsToInsert);

    void injectTasks(Workflow workflow, int index, List<Task> tasksToInsert);

    void loadTaskFilesFromCompoundWorkflows(List<Task> tasks, List<NodeRef> compoundWorkflows);

    void loadTaskFiles(Task task, List<NodeRef> taskFiles);

    List<NodeRef> getChildWorkflowNodeRefs(List<NodeRef> compoundWorkflows);

    /** Load compound workflow with only workflows of given types. For these workflows tasks are also loaded. */
    CompoundWorkflow getCompoundWorkflowOfType(NodeRef nodeRef, List<QName> types);

    List<CompoundWorkflow> getOtherCompoundWorkflows(CompoundWorkflow compoundWorkflow);

    List<NodeRef> getCompoundWorkflowNodeRefs(NodeRef parent);

    List<NodeRef> getCompoundWorkflowAndTaskNodeRefs(NodeRef parentRef);

    List<Comment> getComments(NodeRef compoundWorkflowRef);

    void addCompoundWorkflowComment(Comment comment);

    void editCompoundWorkflowComment(Long commentId, String commentText);

    NodeRef getCompoundWorkflowMainDocumentRef(NodeRef compoundWorkflowRef);

    Task getTaskWithParents(NodeRef nodeRef);

    /**
     * Load CompoundWorkflowDefinition from cache. If CompoundWorkflowDefinition is not found in cache,
     * creates a new CompoundWorkflowDefinition, and adds it to cache and returns it.
     *
     * @throws InvalidNodeRefException
     */
    CompoundWorkflowDefinition getCompoundWorkflowDefinition(NodeRef nodeRef, NodeRef parentRef);

    void removeDeletedCompoundWorkflowDefinitionFromCache();

    Map<NodeRef, List<NodeRef>> getChildWorkflowNodeRefsByCompoundWorkflow(List<NodeRef> compoundWorkflows);
}
