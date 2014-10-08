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
import ee.webmedia.alfresco.workflow.service.type.WorkflowType;

public interface WorkflowDbService {

    String BEAN_NAME = "WorkflowDbService";

    void createTaskEntry(Task task);

    List<Task> getWorkflowTasks(NodeRef workflowRef, Collection<QName> taskDataTypeDefaultAspects, List<QName> taskDataTypeDefaultProps, Map<QName, QName> taskPrefixedQNames,
            WorkflowType workflowType, Workflow workflow, boolean copy);

    void deleteTasksCascading(NodeRef nodeRef, QName nodeTypeQName);

    void createTaskEntry(Task task, NodeRef workflowfRef);

    void createTaskEntry(Task task, NodeRef workflowfRef, boolean isIndependentTask);

    void updateTaskEntry(Task task, Map<QName, Serializable> changedProps);

    void updateTaskSingleProperty(Task task, QName key, Serializable value);

    void updateTaskProperties(NodeRef taskRef, Map<QName, Serializable> props);

    void updateTaskEntry(Task task, Map<QName, Serializable> changedProps, NodeRef parentRef);

    void updateTaskEntryIgnoringParent(Task task, Map<QName, Serializable> changedProps);

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

    Collection<DueDateHistoryRecord> getDueDateHistoryRecords(NodeRef taskRef);

    boolean taskExists(NodeRef nodeRef);

    Serializable getTaskProperty(NodeRef nodeRef, QName qname);

    Task getTask(NodeRef nodeRef, Map<QName, QName> taskPrefixedQNames, Workflow workflow, boolean copy);

    /** Search tasks from main store only */
    Pair<List<Task>, Boolean> searchTasksMainStore(String queryCondition, List<Object> arguments, int limit);

    /** Search tasks from all stores */
    List<NodeRef> searchTaskNodeRefs(String queryCondition, List<Object> arguments);

    Map<NodeRef, Pair<String, String>> searchTaskSendStatusInfo(String queryCondition, List<Object> arguments);

    Pair<List<Task>, Boolean> searchTasksAllStores(String queryCondition, List<Object> arguments, int limit);

    <T extends Object> Pair<List<T>, Boolean> searchTasksAllStores(String queryCondition, List<Object> arguments, int limit, RowMapper<T> rowMapper);

    Map<QName, Integer> countTasksByType(String queryCondition, List<Object> arguments, QName... taskType);

    int countTasks(String queryCondition, List<Object> arguments);

    /**
     * This method throws no exception if no row is updated. Should not be used under normal circumtances; only for updaters
     */
    int updateTaskPropertiesAndStorRef(NodeRef taskRef, Map<QName, Serializable> props);

    List<List<String>> deleteNotExistingTasks();

    void updateWorkflowTaskProperties(NodeRef nodeRef, Map<QName, Serializable> newProps);

    Map<NodeRef, List<NodeRef>> getCompoundWorkflowsTaskFiles(List<CompoundWorkflow> compoundWorkflows);

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

}