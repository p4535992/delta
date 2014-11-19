package ee.webmedia.alfresco.workflow.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.type.WorkflowType;

/**
 * This updater is present only in 3.6.30 branch.
 * Inserts tasks that exist in repo and don't exist in delta_task table.
 * Used to fix bug during PPA 3.6.21 -> 3.6.30 upgrade, where trashcan tasks and
 * tasks in compound workflows that are missing from Lucene index are not present in delta_task table.
 * NB! This updater assumes that Lucene index has been fixed!
 */
public class TaskTableInsertMissingTasksBootstrap extends AbstractNodeUpdater {

    private WorkflowDbService workflowDbService;
    private WorkflowService workflowService;
    private DictionaryService dictionaryService;
    private NamespaceService namespaceService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW);
        List<ResultSet> result = new ArrayList<ResultSet>(6);
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        List<String> taskNodeRefs = new ArrayList<String>();
        List<Workflow> workflows = getCompoundWorkflow(nodeRef, nodeService.getType(nodeRef)).getWorkflows();
        for (Workflow workflow : workflows) {
            for (Task task : workflow.getTasks()) {
                if (!workflowDbService.taskExists(task.getNodeRef())) {
                    workflowDbService.createTaskEntry(task);
                }
                taskNodeRefs.add(task.getNodeRef().toString());
            }
        }
        return new String[] { "Inserted tasks: ", TextUtil.joinNonBlankStringsWithComma(taskNodeRefs) };
    }

    // The following methods are copied from 2.5 branch WorkflowServiceImpl,
    // because the functionality is not present in 3.6 any more
    // In 3.13 branch, the same copied methods are present in TaskTableInsertBootstrap.

    public CompoundWorkflow getCompoundWorkflow(NodeRef nodeRef, QName type) {
        WmNode node = getNode(nodeRef, type, false, false);
        NodeRef parent = nodeService.getPrimaryParent(nodeRef).getParentRef();
        CompoundWorkflow compoundWorkflow = new CompoundWorkflow(node, parent);
        getAndAddWorkflows(compoundWorkflow.getNode().getNodeRef(), compoundWorkflow, false, true);
        return compoundWorkflow;
    }

    private WmNode getNode(NodeRef nodeRef, QName typeToCheck, boolean allowSubType, boolean copy) {
        QName type = nodeService.getType(nodeRef);
        if (allowSubType) {
            if (!dictionaryService.isSubClass(type, typeToCheck)) {
                throw new RuntimeException("Node type '" + type.toPrefixString(namespaceService) + "' is not a subclass of node type '"
                        + typeToCheck.toPrefixString(namespaceService) + "'");
            }
        } else {
            if (!typeToCheck.equals(type)) {
                throw new RuntimeException("Node type '" + type.toPrefixString(namespaceService) + "' is not equal to node type '"
                        + typeToCheck.toPrefixString(namespaceService) + "'");
            }
        }
        return new WmNode(copy ? null : nodeRef, type.getPrefixedQName(namespaceService), getNodeAspects(nodeRef), getNodeProperties(nodeRef));
    }

    private void getAndAddWorkflows(NodeRef parent, CompoundWorkflow compoundWorkflow, boolean copy, boolean addTasks) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parent, workflowService.getWorkflowTypes().keySet());
        int workflowIndex = 0;
        for (ChildAssociationRef childAssoc : childAssocs) {
            workflowIndex = addWorkflow(compoundWorkflow, copy, workflowIndex, childAssoc, addTasks);
        }
    }

    private int addWorkflow(CompoundWorkflow compoundWorkflow, boolean copy, int workflowIndex, ChildAssociationRef childAssoc, boolean addTasks) {
        NodeRef nodeRef = childAssoc.getChildRef();
        Workflow workflow = getWorkflow(nodeRef, compoundWorkflow, copy);
        workflow.setIndexInCompoundWorkflow(workflowIndex);
        compoundWorkflow.addWorkflow(workflow);
        if (addTasks) {
            getAndAddTasks(nodeRef, workflow, copy);
        }
        workflowIndex++;
        return workflowIndex;
    }

    private void getAndAddTasks(NodeRef parent, Workflow workflow, boolean copy) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parent);
        int taskIndex = 0;
        for (ChildAssociationRef childAssoc : childAssocs) {
            NodeRef nodeRef = childAssoc.getChildRef();
            Task task = getTask(nodeRef, workflow, copy);
            task.setTaskIndexInWorkflow(taskIndex);
            workflow.addTask(task);
            taskIndex++;
        }
    }

    private Task getTask(NodeRef nodeRef, Workflow workflow, boolean copy) {
        WmNode taskNode = getNode(nodeRef, WorkflowCommonModel.Types.TASK, true, copy);

        WorkflowType workflowType = workflowService.getWorkflowTypesByTask().get(taskNode.getType());
        if (workflowType == null) {
            throw new RuntimeException("Task type '" + taskNode.getType() + "' not registered in service, but existing node has it: " + nodeRef);
        }

        // If workflowType exists, then getTaskClass() cannot return null
        Task task = Task.create(workflowType.getTaskClass(), taskNode, workflow, workflowType.getTaskOutcomes());
        return task;
    }

    private Workflow getWorkflow(NodeRef nodeRef, CompoundWorkflow compoundWorkflow, boolean copy) {
        WmNode workflowNode = getNode(nodeRef, WorkflowCommonModel.Types.WORKFLOW, true, copy);
        return getWorkflow(compoundWorkflow, workflowNode);
    }

    private Workflow getWorkflow(CompoundWorkflow compoundWorkflow, WmNode workflowNode) {
        WorkflowType workflowType = workflowService.getWorkflowTypes().get(workflowNode.getType());
        if (workflowType == null) {
            throw new RuntimeException("Workflow type '" + workflowNode.getType() + "' not registered in service");
        }
        QName taskType = workflowType.getTaskType();
        WmNode taskNode = workflowService.getTaskTemplateByType(taskType);

        Workflow workflow = Workflow.create(workflowType.getWorkflowClass(), workflowNode, compoundWorkflow, taskNode, workflowType.getTaskClass(),
                workflowType.getTaskOutcomes());
        return workflow;
    }

    private Set<QName> getNodeAspects(NodeRef nodeRef) {
        Set<QName> aspects = nodeService.getAspects(nodeRef);
        return RepoUtil.getAspectsIgnoringSystem(aspects);
    }

    private Map<QName, Serializable> getNodeProperties(NodeRef nodeRef) {
        return RepoUtil.getPropertiesIgnoringSystem(nodeService.getProperties(nodeRef), dictionaryService);
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setWorkflowDbService(WorkflowDbService workflowDbService) {
        this.workflowDbService = workflowDbService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

}
