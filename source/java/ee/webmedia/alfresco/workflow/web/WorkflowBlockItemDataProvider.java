package ee.webmedia.alfresco.workflow.web;


import ee.webmedia.alfresco.common.richlist.LazyListDataProvider;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.workflow.model.WorkflowBlockItem;
import org.alfresco.service.cmr.repository.NodeRef;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowBlockItemDataProvider extends LazyListDataProvider<Integer, WorkflowBlockItem> {

    private final List<NodeRef> compoundWorkflows;
    private final Map<NodeRef, Boolean> workflowBlockItemRights;
    private final String workflowGroupTaskUrl;

    public WorkflowBlockItemDataProvider(List<NodeRef> compoundWorkflows, Map<NodeRef, Boolean> workflowBlockItemRights, String workflowGroupTaskUrl) {
        super(BeanHelper.getWorkflowDbService().getWorkflowBlockItemRowNumbers(compoundWorkflows), null);
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
        List<WorkflowBlockItem> workflowBlockItems = BeanHelper.getWorkflowDbService()
                .getWorkflowBlockItems(compoundWorkflows, workflowBlockItemRights, workflowGroupTaskUrl, rowsToLoad);
        for (WorkflowBlockItem workflowBlockItem : workflowBlockItems) {
            result.put(workflowBlockItem.getRowNumber(), workflowBlockItem);
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
