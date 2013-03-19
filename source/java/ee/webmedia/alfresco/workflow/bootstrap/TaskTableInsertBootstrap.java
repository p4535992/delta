package ee.webmedia.alfresco.workflow.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.document.bootstrap.ConvertToDynamicDocumentsUpdater;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import ee.webmedia.alfresco.workflow.service.type.WorkflowType;

/**
 * Insert tasks' data into delta_tasks table.
 * Tasks are retireved by compound workflow in order to retrieve task index in workflow.
 * 
 * @author Riina Tens
 */
public class TaskTableInsertBootstrap extends AbstractNodeUpdater {

    private WorkflowService workflowService;
    private WorkflowDbService workflowDbService;
    private FileService fileService;
    private NamespaceService namespaceService;
    private DictionaryService dictionaryService;
    private Map<String, List<String>> orgStructNameToPath;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW, WorkflowCommonModel.Types.COMPOUND_WORKFLOW_DEFINITION);
        List<ResultSet> result = new ArrayList<ResultSet>(6);
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    public void executeUpdater() throws Exception {
        if (isEnabled()) {
            orgStructNameToPath = new HashMap<String, List<String>>();
            ConvertToDynamicDocumentsUpdater.fillOrgStructNameToPath(orgStructNameToPath, BeanHelper.getOrganizationStructureService());
        }
        super.executeUpdater();
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        List<String> taskNodeRefs = new ArrayList<String>();
        List<Workflow> workflows = getCompoundWorkflow(nodeRef, nodeService.getType(nodeRef)).getWorkflows();
        Map<String, Object> compoundWorkflowSearchableProps = RepoUtil.toStringProperties(WorkflowUtil.getTaskSearchableProps(nodeService.getProperties(nodeRef)));
        for (Workflow workflow : workflows) {
            for (Task task : workflow.getTasks()) {
                task.setDocumentType(getTaskDocumentType(nodeRef));
                setOwnerOrganizationPath(task);
                if (task.getNode().getAspects().contains(WorkflowSpecificModel.Aspects.SEARCHABLE)) {
                    task.getNode().getProperties().putAll(compoundWorkflowSearchableProps);
                }
                workflowDbService.createTaskEntry(task);
                taskNodeRefs.add(task.getNodeRef().toString());
            }
        }
        return new String[] { "Inserted tasks: ", TextUtil.joinNonBlankStringsWithComma(taskNodeRefs) };
    }

    private void setOwnerOrganizationPath(Task task) {
        Serializable ownerOrganizationName = task.getProp(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME);
        if (ownerOrganizationName != null && ownerOrganizationName instanceof String) {
            List<String> ownerOrgStructPath = orgStructNameToPath.get(ownerOrganizationName);
            if (ownerOrgStructPath != null && !ownerOrgStructPath.isEmpty()) {
                task.setOwnerOrgStructUnitProp(ownerOrgStructPath);
            } else {
                task.setOwnerOrgStructUnitProp(Collections.singletonList((String) ownerOrganizationName));
            }
        }
    }

    private String getTaskDocumentType(NodeRef nodeRef) {
        NodeRef parentRef = generalService.getAncestorNodeRefWithType(nodeRef, DocumentCommonModel.Types.DOCUMENT);
        if (parentRef != null) {
            return (String) nodeService.getProperty(parentRef, DocumentAdminModel.Props.OBJECT_TYPE_ID);
        }
        return null;
    }

    // The following methods are copied from 2.5 branch WorkflowServiceImpl,
    // because the functionality is not present in 3.13 any more

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

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

}
