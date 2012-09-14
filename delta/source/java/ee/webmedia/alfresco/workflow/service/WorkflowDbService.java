package ee.webmedia.alfresco.workflow.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;

import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.workflow.service.type.WorkflowType;

/**
 * @author Riina Tens
 */
public interface WorkflowDbService {

    String BEAN_NAME = "WorkflowDbService";

    void createTaskEntry(Task task);

    List<Task> getWorkflowTasks(NodeRef workflowRef, Collection<QName> taskDataTypeDefaultAspects, List<QName> taskDataTypeDefaultProps,
            Map<QName, QName> taskPrefixedQNames, WorkflowType workflowType, Workflow workflow, boolean copy);

    void deleteTasksCascading(NodeRef nodeRef, QName nodeTypeQName);

    void createTaskEntry(Task task, NodeRef workflowfRef);

    void updateTaskEntry(Task task, Map<QName, Serializable> changedProps);

    void updateTaskProperties(NodeRef taskRef, Map<QName, Serializable> props);

    void createTaskDueDateExtensionAssocEntry(NodeRef initiatingTaskRef, NodeRef nodeRef);

    void createTaskDueDateHistoryEntries(NodeRef taskRef, List<Pair<String, Date>> historyRecords);

    void createTaskFileEntries(NodeRef nodeRef, List<File> files);

    List<Task> getDueDateExtensionInitiatingTask(NodeRef nodeRef, Map<QName, QName> taskPrefixedQNames);

    void updateWorkflowTasksStore(NodeRef workflowRef, StoreRef newStoreRef);

    NodeRef getTaskParentNodeRef(NodeRef nodeRef);

    QName getTaskType(NodeRef nodeRef);

    void removeTaskFiles(NodeRef nodeRef, List<NodeRef> removedFileRefs);

    List<NodeRef> getTaskFileNodeRefs(NodeRef taskNodeRef);

    void createTaskFileEntriesFromNodeRefs(NodeRef taskRef, List<NodeRef> fileNodeRefs);

    Collection<Pair<String, Date>> getDueDateHistoryRecords(NodeRef taskRef);

    boolean taskExists(NodeRef nodeRef);

    Serializable getTaskProperty(NodeRef nodeRef, QName qname);

    Task getTask(NodeRef nodeRef, Map<QName, QName> taskPrefixedQNames, Workflow workflow, boolean copy);

    /** Search tasks from main store only */
    Pair<List<Task>, Boolean> searchTasksMainStore(String queryCondition, List<Object> arguments, int limit);

    /** Search tasks from all stores */
    List<NodeRef> searchTaskNodeRefs(String queryCondition, List<Object> arguments);

    Map<NodeRef, Pair<String, String>> searchTaskSendStatusInfo(String queryCondition, List<Object> arguments);

    Pair<List<Task>, Boolean> searchTasksAllStores(String queryCondition, List<Object> arguments, int limit);

    int countTasks(String queryCondition, List<Object> arguments);

    void updateTaskPropertiesAndStorRef(NodeRef taskRef, Map<QName, Serializable> props);

    List<List<String>> deleteNotExistingTasks();

}