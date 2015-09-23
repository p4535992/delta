package ee.webmedia.alfresco.workflow.search.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.richlist.LazyListDataProvider;
import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;

public class CompoundWorkflowDataProvider extends LazyListDataProvider<NodeRef, CompoundWorkflow> {

    private static final long serialVersionUID = 1L;
    private static final Set<QName> COMPOUND_WORKFLOW_PROPS_TO_LOAD;
    private final Map<Long, QName> propertyTypes = new HashMap<>();

    static {
        Set<QName> props = new HashSet<>(Arrays.asList(WorkflowCommonModel.Props.TYPE, WorkflowCommonModel.Props.TITLE, WorkflowCommonModel.Props.OWNER_NAME,
                WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME, WorkflowCommonModel.Props.OWNER_JOB_TITLE, WorkflowCommonModel.Props.CREATED_DATE_TIME,
                WorkflowCommonModel.Props.STARTED_DATE_TIME, WorkflowCommonModel.Props.STOPPED_DATE_TIME, WorkflowCommonModel.Props.FINISHED_DATE_TIME));
        COMPOUND_WORKFLOW_PROPS_TO_LOAD = Collections.unmodifiableSet(props);
    }

    public CompoundWorkflowDataProvider(List<NodeRef> taskRefs) {
        super(taskRefs, null);
    }

    @Override
    protected Map<NodeRef, CompoundWorkflow> loadData(final List<NodeRef> rowsToLoad) {
        return BeanHelper.getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Map<NodeRef, CompoundWorkflow>>() {

            @Override
            public Map<NodeRef, CompoundWorkflow> execute() throws Throwable {

                BulkLoadNodeService bulkLoadNodeService = BeanHelper.getBulkLoadNodeService();
                Map<NodeRef, Node> compoundWorkflowNodes = bulkLoadNodeService.loadNodes(rowsToLoad, COMPOUND_WORKFLOW_PROPS_TO_LOAD, propertyTypes);
                Map<NodeRef, CompoundWorkflow> compoundWorkflows = new HashMap<>();
                List<NodeRef> independentCompoundWorkflowRefs = new ArrayList<>();
                for (Map.Entry<NodeRef, Node> entry : compoundWorkflowNodes.entrySet()) {
                    CompoundWorkflow compoundWorkflow = new CompoundWorkflow((WmNode) entry.getValue(), null);
                    NodeRef compoundWorkflowRef = entry.getKey();
                    compoundWorkflows.put(compoundWorkflowRef, compoundWorkflow);
                    if (compoundWorkflow.isIndependentWorkflow()) {
                        independentCompoundWorkflowRefs.add(compoundWorkflowRef);
                    } else if (compoundWorkflow.isDocumentWorkflow()) {
                        compoundWorkflow.setNumberOfDocuments(1);
                    } else {
                        compoundWorkflow.setNumberOfDocuments(0);
                    }
                }
                Map<NodeRef, Integer> documentCounts = bulkLoadNodeService.getSearchableTargetAssocsCount(independentCompoundWorkflowRefs,
                        DocumentCommonModel.Assocs.WORKFLOW_DOCUMENT);

                for (NodeRef independentWorkflowRef : independentCompoundWorkflowRefs) {
                    CompoundWorkflow compoundWorkflow = compoundWorkflows.get(independentWorkflowRef);
                    Integer count = documentCounts.get(independentWorkflowRef);
                    compoundWorkflow.setNumberOfDocuments(count != null ? count : 0);
                }
                return compoundWorkflows;
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
    protected NodeRef getKeyFromValue(CompoundWorkflow compoundWorkflow) {
        return compoundWorkflow.getNodeRef();
    }

    @Override
    protected void resetObjectKeyOrder(List<CompoundWorkflow> orderedRows) {
        objectKeys.clear();
        for (CompoundWorkflow compoundWorkflow : orderedRows) {
            objectKeys.add(compoundWorkflow.getNodeRef());
        }
    }

}
