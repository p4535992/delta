package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowDbService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.richlist.LazyListDataProvider;
import ee.webmedia.alfresco.common.richlist.PageLoadCallback;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.Task;

public class TaskDataProvider extends LazyListDataProvider<NodeRef, Task> {

    private final List<NodeRef> compoundWorkflows;
    private final QName taskType;

    /**
     * Mapping for list columns to distinguish property namespaces (common vs specific model)
     */
    private static final Map<String, QName> COLUMN_MAPPING = new HashMap<>();

    static {
        COLUMN_MAPPING.put(WorkflowCommonModel.Props.OWNER_NAME.getLocalName(), WorkflowCommonModel.Props.OWNER_NAME);
        COLUMN_MAPPING.put(WorkflowCommonModel.Props.COMPLETED_DATE_TIME.getLocalName(), WorkflowCommonModel.Props.COMPLETED_DATE_TIME);
        COLUMN_MAPPING.put(WorkflowCommonModel.Props.OUTCOME.getLocalName(), WorkflowCommonModel.Props.OUTCOME);
        COLUMN_MAPPING.put(WorkflowSpecificModel.Props.FILE_VERSIONS.getLocalName(), WorkflowSpecificModel.Props.FILE_VERSIONS);
    }

    public TaskDataProvider(List<NodeRef> compoundWorkflows, QName taskType) {
        super(getWorkflowDbService().getCompoundWorkflowsFinishedTasks(compoundWorkflows, taskType), null);

        this.compoundWorkflows = compoundWorkflows;
        this.taskType = taskType;
    }

    public TaskDataProvider(List<NodeRef> taskRefs, PageLoadCallback<NodeRef, Task> callback) {
        super(taskRefs, callback);
        taskType = null;
        compoundWorkflows = null;
    }

    @Override
    protected boolean loadOrderFromDb(String column, boolean descending) {
        objectKeys = getWorkflowDbService().getCompoundWorkflowsFinishedTasks(compoundWorkflows, taskType, getColumnPropertyQName(column), descending);
        return true;
    }

    @Override
    protected void resetObjectKeyOrder(List<Task> orderedRows) {
        objectKeys.clear();
        for (Task task : orderedRows) {
            objectKeys.add(task.getNodeRef());
        }
    }

    @Override
    protected Map<NodeRef, Task> loadData(List<NodeRef> rowsToLoad) {
        return BeanHelper.getWorkflowDbService().loadTasksWithFiles(rowsToLoad, null);
    }

    private QName getColumnPropertyQName(String column) {
        final QName qName = COLUMN_MAPPING.get(column);
        if (qName == null) {
            throw new RuntimeException("Column " + column + " is not yet mapped! See TaskDataProvider#COLUMN_MAPPING");
        }

        return qName;
    }

    @Override
    protected NodeRef getKeyFromValue(Task value) {
        return value.getNodeRef();
    }
}
