package ee.webmedia.alfresco.workflow.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.springframework.jdbc.core.RowMapper;

import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.workflow.model.Comment;
import ee.webmedia.alfresco.workflow.model.WorkflowBlockItem;
import ee.webmedia.alfresco.workflow.service.type.WorkflowType;

public interface WorkflowDbService {

    String BEAN_NAME = "WorkflowDbService";

    List<WorkflowBlockItem> getWorkflowBlockItemGroup(WorkflowBlockItem firstWorkflowBlockItemInGroup);

    List<WorkflowBlockItem> getWorkflowBlockItemGroup(String workflowNodeRefId, Integer offset, Integer limit);

    List<Integer> getWorkflowBlockItemRowNumbers(List<NodeRef> compoundWorkflows);

    List<WorkflowBlockItem> getWorkflowBlockItems(List<NodeRef> compoundWorkflows, Map<NodeRef, Boolean> checkWorkflowRights, String workflowGroupTasksUrl);

    List<WorkflowBlockItem> getWorkflowBlockItems(List<NodeRef> compoundWorkflows, Map<NodeRef, Boolean> checkWorkflowRights, String workflowGroupTasksUrl,
            List<Integer> rowsToLoad);

    void createTaskEntry(Task task);

    List<Task> getWorkflowTasks(NodeRef workflowRef, Collection<QName> taskDataTypeDefaultAspects, List<QName> taskDataTypeDefaultProps, Map<QName, QName> taskPrefixedQNames,
            WorkflowType workflowType, Workflow workflow, boolean copy);

    void deleteTasks(List<NodeRef> removedTaskNodeRefs);

    void deleteTasksCascading(NodeRef nodeRef, QName nodeTypeQName);

    void createTaskEntry(Task task, NodeRef workflowfRef);

    void createTaskEntry(Task task, NodeRef workflowfRef, boolean isIndependentTask);

    void createTaskEntries(List<TaskUpdateInfo> taskUpdateInfos, Set<String> usedFieldNames);

    void updateTaskEntry(Task task, Map<QName, Serializable> changedProps);

    void updateTaskEntry(Task task, Map<QName, Serializable> changedProps, NodeRef parentRef);

    void updateTaskEntries(List<TaskUpdateInfo> taskToUpdate, Set<String> updateTaskUsedFieldNames);

    void updateTaskEntryIgnoringParent(Task task, Map<QName, Serializable> changedProps);

    void updateTaskSingleProperty(Task task, QName key, Serializable value, NodeRef workflowRef);

    TaskUpdateInfo verifyTaskAndGetUpdateInfoOnCreate(Task task, NodeRef workflowfRef);

    TaskUpdateInfo verifyTaskAndGetUpdateInfoOnUpdate(Task task, NodeRef workflowfRef, Map<QName, Serializable> propsToSave);

    void updateTaskProperties(NodeRef taskRef, Map<QName, Serializable> props);

    void createTaskDueDateExtensionAssocEntry(NodeRef initiatingTaskRef, NodeRef nodeRef);

    void createTaskDueDateHistoryEntries(NodeRef taskRef, List<DueDateHistoryRecord> historyRecords);

    void createTaskFileEntries(NodeRef nodeRef, List<File> files);

    List<Task> getDueDateExtensionInitiatingTask(NodeRef nodeRef, Map<QName, QName> taskPrefixedQNames);

    void updateWorkflowTasksStore(NodeRef workflowRef, StoreRef newStoreRef);

    NodeRef getTaskParentNodeRef(NodeRef nodeRef);

    QName getTaskType(NodeRef nodeRef);

    void removeTaskFiles(NodeRef nodeRef, List<NodeRef> removedFileRefs);

    List<NodeRef> getTaskFileNodeRefs(NodeRef taskNodeRef);

    void createTaskFileEntriesFromNodeRefs(NodeRef taskRef, List<NodeRef> fileNodeRefs);

    List<DueDateHistoryRecord> getDueDateHistoryRecords(NodeRef taskRef);

    boolean taskExists(NodeRef nodeRef);

    Serializable getTaskProperty(NodeRef nodeRef, QName qname);

    Task getTask(NodeRef nodeRef, Workflow workflow, boolean copy);

    /** Search tasks from main store only */
    Pair<List<Task>, Boolean> searchTasksMainStore(String queryCondition, List<Object> arguments, int limit);

    /**
     * Search tasks from all stores
     */
    Pair<List<NodeRef>, Boolean> searchTaskNodeRefs(String queryCondition, List<Object> arguments, int limit);

    Map<NodeRef, Pair<String, String>> searchTaskSendStatusInfo(String queryCondition, List<Object> arguments);

    Pair<List<Task>, Boolean> searchTasksAllStores(String queryCondition, List<Object> arguments, int limit);

    <T extends Object> Pair<List<T>, Boolean> searchTasksAllStores(String queryCondition, List<Object> arguments, int limit, RowMapper<T> rowMapper);

    Map<QName, Integer> countTasksByType(String queryCondition, List<Object> arguments, QName... taskType);

    int countTasks(String queryCondition, List<Object> arguments);

    Map<String, Integer> countAllCurrentUserTasks();

    /**
     * This method throws no exception if no row is updated. Should not be used under normal circumtances; only for updaters
     */
    int updateTaskPropertiesAndStorRef(NodeRef taskRef, Map<QName, Serializable> props);

    boolean isOwnerOfInProgressTask(List<NodeRef> compoundWorkflowNodeRef, QName taskType, boolean requireActiveResponsible);

    List<List<String>> deleteNotExistingTasks();

    void updateWorkflowTaskProperties(NodeRef nodeRef, Map<QName, Serializable> newProps);

    Map<NodeRef, List<NodeRef>> getTaskFileNodeRefs(List<NodeRef> taskNodeRefs);

    Map<NodeRef, List<NodeRef>> getCompoundWorkflowsTaskFiles(List<NodeRef> compoundWorkflowsNodeRefs);

    int replaceTaskOutcomes(String oldOutcome, String newOutcome, String taskType);

    void deleteTask(NodeRef removedTaskNodeRef);

    void deleteWorkflowTasks(NodeRef removedWorkflowNodeRef);

    Set<NodeRef> getAllWorflowNodeRefs();

    List<NodeRef> getWorkflowTaskNodeRefs(NodeRef workflowRef);

    Set<NodeRef> getAllWorkflowsWithEmptyTasks();

    Set<NodeRef> getWorkflowsWithWrongTaskOrder();

    List<Comment> getCompoundWorkflowComments(String compoundWorkflowId);

    void addCompoundWorkfowComment(Comment comment);

    void editCompoundWorkflowComment(Long commentId, String commentText);

    boolean hasInProgressOtherUserOrderAssignmentTasks(String userName, List<NodeRef> compoundWorkflowRefs);

    boolean containsTaskOfType(List<NodeRef> compoundWorkflowRefs, QName... taskTypes);

    List<NodeRef> getCompoundWorkflowsFinishedTasks(List<NodeRef> compoundWorkflows, QName taskType);

    boolean hasInProgressTasks(List<NodeRef> compoundWorkflows, String currentUser);

    List<NodeRef> getCompoundWorkflowsFinishedTasks(List<NodeRef> compoundWorkflows, QName taskType, QName sortByProperty, boolean descending);

    List<Task> getInProgressTasks(List<NodeRef> compoundWorkflows, String ownerId);

    boolean hasNoInProgressOrOnlyActiveResponsibleAssignmentTasks(List<NodeRef> compoundWorkflows);

    Map<NodeRef, Task> loadTasksWithFiles(List<NodeRef> taskNodeRefs, Set<QName> propsToLoad);

    Map<NodeRef, Task> getTasks(List<NodeRef> taskRefs);

    Map<NodeRef, Task> getTasksWithCompoundWorkflowRef(List<NodeRef> taskRefs);

    Map<NodeRef, Task> getTasks(List<NodeRef> taskRefs, Workflow workflow, boolean copy, Set<QName> propsToLoad);

    /**
     * This method introduces remarkable performance impact, so it should be used only in case document workflows are enabled in application.
     */
    Pair<List<NodeRef>, Boolean> searchTaskNodeRefsCheckLimitedSeries(String queryCondition, String userId, List<Object> arguments, int limit);

    Map<NodeRef, String> getInProgressTaskOwners(Collection<NodeRef> compoundWorkflows);

    int[] updateCompoundWorkflowTaskSearchableProperties(List<Pair<String, Map<QName, Serializable>>> compoundWorkflowtaskSearchableProps,
            List<QName> compoundWorkflowTaskSearchableProperties, String compoundWorkflowTaskUpdateString);
}
