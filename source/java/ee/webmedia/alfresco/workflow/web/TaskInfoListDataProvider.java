package ee.webmedia.alfresco.workflow.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.richlist.LazyListDataProvider;
import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.document.model.CreatedOrRegistratedDateComparator;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.search.model.TaskInfo;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;

public class TaskInfoListDataProvider extends LazyListDataProvider<NodeRef, TaskInfo> {

    private static final long serialVersionUID = 1L;
    private static final Set<QName> DOC_PROPS_TO_LOAD;
    private final Map<Long, QName> propertyTypes = new HashMap<>();

    static {
        Set<QName> props = new HashSet<>(Arrays.asList(DocumentAdminModel.Props.OBJECT_TYPE_ID, ContentModel.PROP_CREATED, DocumentCommonModel.Props.DOC_NAME,
                DocumentCommonModel.Props.REG_DATE_TIME, DocumentCommonModel.Props.REG_NUMBER));
        DOC_PROPS_TO_LOAD = Collections.unmodifiableSet(props);
    }

    public TaskInfoListDataProvider(List<NodeRef> taskRefs) {
        super(taskRefs, null);
        List<TaskInfo> tasksToSort = new ArrayList<>(loadData(objectKeys).values());
        Collections.sort(tasksToSort, CreatedOrRegistratedDateComparator.getComparator());
        List<NodeRef> sortedNodeRefs = new ArrayList<>();
        for (TaskInfo task : tasksToSort) {
            sortedNodeRefs.add(getKeyFromValue(task));
        }
        objectKeys = sortedNodeRefs;
    }

    @Override
    protected Map<NodeRef, TaskInfo> loadData(final List<NodeRef> rowsToLoad) {
        final Set<QName> propsToLoad = null;
        // don't waste transactions!
        return BeanHelper.getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Map<NodeRef, TaskInfo>>() {

            @Override
            public Map<NodeRef, TaskInfo> execute() throws Throwable {

                Map<NodeRef, Task> tasks = BeanHelper.getWorkflowDbService().getTasks(rowsToLoad, null, false, propsToLoad);

                List<NodeRef> workflowRefs = new ArrayList<>();
                for (Task task : tasks.values()) {
                    if (!task.isType(WorkflowSpecificModel.Types.LINKED_REVIEW_TASK)) {
                        NodeRef workflowNodeRef = task.getWorkflowNodeRef();
                        Assert.notNull(workflowNodeRef);
                        workflowRefs.add(workflowNodeRef);
                    }
                }

                BulkLoadNodeService bulkLoadNodeService = BeanHelper.getBulkLoadNodeService();
                Map<NodeRef, Node> workflows = bulkLoadNodeService.loadNodes(workflowRefs, Collections.singleton(WorkflowSpecificModel.Props.RESOLUTION), propertyTypes);
                Map<NodeRef, Map<QName, Serializable>> compoundWorkflows = bulkLoadNodeService.loadPrimaryParentsProperties(workflowRefs,
                        Collections.singleton(WorkflowCommonModel.Types.COMPOUND_WORKFLOW), null, propertyTypes);
                List<NodeRef> compoundWorkflowRefs = RepoUtil.getNodeRefsFromProps(compoundWorkflows);

                Map<NodeRef, Map<QName, Serializable>> documents = bulkLoadNodeService.loadPrimaryParentsProperties(compoundWorkflowRefs,
                        Collections.singleton(DocumentCommonModel.Types.DOCUMENT), DOC_PROPS_TO_LOAD, propertyTypes);
                List<NodeRef> indpendentCompoundWorkflows = new ArrayList<>();
                for (Map<QName, Serializable> compoundWorkflowProps : compoundWorkflows.values()) {
                    String type = (String) compoundWorkflowProps.get(WorkflowCommonModel.Props.TYPE);
                    if (CompoundWorkflowType.INDEPENDENT_WORKFLOW.equals(CompoundWorkflowType.valueOf(type))) {
                        indpendentCompoundWorkflows.add((NodeRef) compoundWorkflowProps.get(ContentModel.PROP_NODE_REF));
                    }
                }
                Map<NodeRef, Integer> docCounts = new HashMap<>();
                if (!indpendentCompoundWorkflows.isEmpty()) {
                    docCounts.putAll(bulkLoadNodeService.getSearchableChildDocCounts(indpendentCompoundWorkflows));
                }
                Map<NodeRef, TaskInfo> taskInfos = new HashMap<>();
                for (Map.Entry<NodeRef, Task> task : tasks.entrySet()) {
                    NodeRef taskRef = task.getKey();
                    NodeRef workflowNodeRef = tasks.get(taskRef).getWorkflowNodeRef();
                    Node workflow = workflows != null ? workflows.get(workflowNodeRef) : null;
                    Map<QName, Serializable> compoundWorkflowProps = workflow != null ? compoundWorkflows.get(workflowNodeRef) : null;
                    Map<QName, Serializable> documentProps = compoundWorkflowProps != null && documents != null
                            ? documents.get(compoundWorkflowProps.get(ContentModel.PROP_NODE_REF)) : null;
                            NodeRef compoundWorkflowRef = compoundWorkflowProps != null ? (NodeRef) compoundWorkflowProps.get(ContentModel.PROP_NODE_REF) : null;
                            CompoundWorkflow compoundWorkflow = compoundWorkflowProps != null ? new CompoundWorkflow(
                                    new WmNode(compoundWorkflowRef, WorkflowCommonModel.Types.COMPOUND_WORKFLOW, null, compoundWorkflowProps)) : null;
                                    WmNode document = documentProps != null ? new WmNode((NodeRef) documentProps.get(ContentModel.PROP_NODE_REF), DocumentCommonModel.Types.DOCUMENT, null,
                                            documentProps) : null;
                                    String workflowResolution = workflow != null ? (String) workflow.getProperties().get(WorkflowSpecificModel.Props.RESOLUTION) : "";
                                    Integer compoundWorkflowDocumentsCount = 0;
                                    if (docCounts.containsKey(compoundWorkflowRef)) {
                                        compoundWorkflowDocumentsCount = docCounts.get(compoundWorkflowRef);
                                    } else if (document != null) {
                                        compoundWorkflowDocumentsCount = 1;
                                    }
                                    TaskInfo taskInfo = new TaskInfo(task.getValue().getNode(), workflowResolution, compoundWorkflow,
                                            document, compoundWorkflowDocumentsCount);
                                    taskInfos.put(taskRef, taskInfo);
                }
                return taskInfos;
            }
        }, true);
    }

    @Override
    protected boolean loadOrderFromDb(String column, boolean descending) {
        if (WebUtil.exceedsLimit(objectKeys, null)) {
            return checkAndSetOrderedList(null);
        }
        List<NodeRef> orderedList = new ArrayList<>();
        sortAndFillOrderedList(orderedList, column, loadData(objectKeys), descending);
        return checkAndSetOrderedList(orderedList);

    }

    @Override
    protected NodeRef getKeyFromValue(TaskInfo task) {
        return task.getTask().getNodeRef();
    }

    @Override
    protected void resetObjectKeyOrder(List<TaskInfo> orderedRows) {
        objectKeys.clear();
        for (TaskInfo taskInfo : orderedRows) {
            objectKeys.add(taskInfo.getTask().getNodeRef());
        }
    }

}
