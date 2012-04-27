package ee.webmedia.alfresco.workflow.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

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

}