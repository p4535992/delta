package ee.webmedia.alfresco.workflow.web;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.common.richlist.LazyListDataProvider;
import ee.webmedia.alfresco.common.richlist.PageLoadCallback;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.workflow.model.WorkflowBlockItem;
import ee.webmedia.alfresco.workflow.service.DueDateHistoryRecord;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;

public class WorkflowBlockItemDataProvider extends LazyListDataProvider<Integer, WorkflowBlockItem> {

    private final List<NodeRef> compoundWorkflows;
    private final Map<NodeRef, Boolean> workflowBlockItemRights;
    private final String workflowGroupTaskUrl;

    public WorkflowBlockItemDataProvider(List<NodeRef> compoundWorkflows, Map<NodeRef, Boolean> workflowBlockItemRights, String workflowGroupTaskUrl,
            PageLoadCallback<Integer, WorkflowBlockItem> pageLoadCallback) {
        super(BeanHelper.getWorkflowDbService().getWorkflowBlockItemRowNumbers(compoundWorkflows), pageLoadCallback);
        this.compoundWorkflows = compoundWorkflows;
        this.workflowBlockItemRights = workflowBlockItemRights;
        this.workflowGroupTaskUrl = workflowGroupTaskUrl;
    }

    @Override
    protected boolean loadOrderFromDb(String column, boolean descending) {
        // Sorting is not supported
        return false;
    }

    @Override
    protected void resetObjectKeyOrder(List<WorkflowBlockItem> orderedRows) {
        // Sorting is not supported
    }

    @Override
    protected Map<Integer, WorkflowBlockItem> loadData(List<Integer> rowsToLoad) {
        Map<Integer, WorkflowBlockItem> result = new HashMap<>();
        WorkflowDbService workflowDbService = BeanHelper.getWorkflowDbService();
        List<WorkflowBlockItem> workflowBlockItems = workflowDbService.getWorkflowBlockItems(compoundWorkflows, workflowBlockItemRights, workflowGroupTaskUrl, rowsToLoad);
        Set<String> blockItemIds = new HashSet<>();
        for (WorkflowBlockItem workflowBlockItem : workflowBlockItems) {
            result.put(workflowBlockItem.getRowNumber(), workflowBlockItem);
            blockItemIds.add(workflowBlockItem.getTaskNodeRef().getId());
        }
        Map<String, List<DueDateHistoryRecord>> records = workflowDbService.getDueDateHistoryRecords(blockItemIds);
        for (WorkflowBlockItem item : workflowBlockItems) {
            List<DueDateHistoryRecord> record = records.get(item.getTaskNodeRef().getId());
            item.setDueDateHistoryRecords(record);
        }

        return result;
    }

    @Override
    public void loadPage(int pageStartIndex, int pageEndIndex) {
        super.loadPage(pageStartIndex, pageEndIndex);
    }

    @Override
    protected Integer getKeyFromValue(WorkflowBlockItem workflowBlockItem) {
        return workflowBlockItem.getRowNumber();

    }
}
