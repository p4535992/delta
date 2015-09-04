package ee.webmedia.alfresco.workflow.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.common.richlist.LazyListDataProvider;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.workflow.model.TaskAndDocument;
import ee.webmedia.alfresco.workflow.service.Task;

public class TaskAndDocumentDataProvider extends LazyListDataProvider<NodeRef, TaskAndDocument> {

    private static final long serialVersionUID = 1L;

    public TaskAndDocumentDataProvider(List<NodeRef> result) {
        super(result, null);
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
    protected void resetObjectKeyOrder(List<TaskAndDocument> orderedRows) {
        objectKeys.clear();
        for (TaskAndDocument taskAndDocument : orderedRows) {
            objectKeys.add(taskAndDocument.getTask().getNodeRef());
        }
    }

    @Override
    protected Map<NodeRef, TaskAndDocument> loadData(List<NodeRef> rowsToLoad) {
        Map<NodeRef, Task> tasks = BeanHelper.getWorkflowDbService().getTasks(rowsToLoad);
        return BeanHelper.getDocumentService().getTasksWithDocuments(tasks.values(), null);
    }

    @Override
    protected NodeRef getKeyFromValue(TaskAndDocument value) {
        return value.getTask().getNodeRef();
    }

}
