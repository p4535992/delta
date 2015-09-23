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

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.common.richlist.LazyListDataProvider;
import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowWithObject;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;

public class CompoundWorkflowWithObjectDataProvider extends LazyListDataProvider<NodeRef, CompoundWorkflowWithObject> {

    private static final long serialVersionUID = 1L;
    private static final Set<QName> COMPOUND_WORKFLOW_PROPS_TO_LOAD;
    private static final Set<QName> DOCUMENT_AND_CASE_FILE_TYPE = new HashSet<>(Arrays.asList(DocumentCommonModel.Types.DOCUMENT, CaseFileModel.Types.CASE_FILE));
    private static final Set<QName> DOCUMENT_AND_CASE_FILE_PROPS_TO_LOAD = new HashSet<>(Arrays.asList(DocumentCommonModel.Props.DOC_NAME, DocumentDynamicModel.Props.TITLE));
    private final Map<Long, QName> propertyTypes = new HashMap<>();

    static {
        Set<QName> props = new HashSet<>(Arrays.asList(WorkflowCommonModel.Props.TYPE, WorkflowCommonModel.Props.TITLE, WorkflowCommonModel.Props.OWNER_NAME,
                WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME, WorkflowCommonModel.Props.OWNER_JOB_TITLE, WorkflowCommonModel.Props.CREATED_DATE_TIME,
                WorkflowCommonModel.Props.STARTED_DATE_TIME, WorkflowCommonModel.Props.STOPPED_DATE_TIME, WorkflowCommonModel.Props.FINISHED_DATE_TIME));
        COMPOUND_WORKFLOW_PROPS_TO_LOAD = Collections.unmodifiableSet(props);
    }

    public CompoundWorkflowWithObjectDataProvider(List<NodeRef> taskRefs) {
        super(taskRefs, null);
    }

    @Override
    protected Map<NodeRef, CompoundWorkflowWithObject> loadData(final List<NodeRef> rowsToLoad) {
        return BeanHelper.getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Map<NodeRef, CompoundWorkflowWithObject>>() {

            @Override
            public Map<NodeRef, CompoundWorkflowWithObject> execute() throws Throwable {

                BulkLoadNodeService bulkLoadNodeService = BeanHelper.getBulkLoadNodeService();
                Map<NodeRef, Node> compoundWorkflowNodes = bulkLoadNodeService.loadNodes(rowsToLoad, COMPOUND_WORKFLOW_PROPS_TO_LOAD, propertyTypes);
                Map<NodeRef, CompoundWorkflowWithObject> compoundWorkflows = new HashMap<>();
                List<NodeRef> independentCompoundWorkflowRefs = new ArrayList<>();
                List<NodeRef> documentAndCaseFileWorkflowRefs = new ArrayList<>();
                for (Map.Entry<NodeRef, Node> entry : compoundWorkflowNodes.entrySet()) {
                    CompoundWorkflow compoundWorkflow = new CompoundWorkflow((WmNode) entry.getValue(), null);
                    CompoundWorkflowWithObject compoundWorkflowWithObject = new CompoundWorkflowWithObject(compoundWorkflow);
                    NodeRef compoundWorkflowRef = entry.getKey();
                    compoundWorkflows.put(compoundWorkflowRef, compoundWorkflowWithObject);
                    if (compoundWorkflow.isIndependentWorkflow()) {
                        independentCompoundWorkflowRefs.add(compoundWorkflowRef);
                    } else {
                        documentAndCaseFileWorkflowRefs.add(compoundWorkflowRef);
                    }
                }
                Map<NodeRef, Integer> documentCounts = bulkLoadNodeService.getSearchableTargetAssocsCount(independentCompoundWorkflowRefs,
                        DocumentCommonModel.Assocs.WORKFLOW_DOCUMENT);

                for (NodeRef independentWorkflowRef : independentCompoundWorkflowRefs) {
                    CompoundWorkflowWithObject compoundWorkflow = compoundWorkflows.get(independentWorkflowRef);
                    Integer count = documentCounts.get(independentWorkflowRef);
                    compoundWorkflow.getCompoundWorkflow().setNumberOfDocuments(count != null ? count : 0);
                }

                Map<NodeRef, Map<QName, Serializable>> allParentProps = bulkLoadNodeService.loadPrimaryParentsProperties(documentAndCaseFileWorkflowRefs,
                        DOCUMENT_AND_CASE_FILE_TYPE,
                        DOCUMENT_AND_CASE_FILE_PROPS_TO_LOAD, propertyTypes);
                for (NodeRef compoundWorkfowRef : documentAndCaseFileWorkflowRefs) {
                    Map<QName, Serializable> parentProps = allParentProps.get(compoundWorkfowRef);
                    if (parentProps == null) {
                        continue;
                    }
                    CompoundWorkflowWithObject compoundWorkflow = compoundWorkflows.get(compoundWorkfowRef);
                    compoundWorkflow.setParent((NodeRef) parentProps.get(ContentModel.PROP_NODE_REF));
                    if (compoundWorkflow.getCompoundWorkflow().isDocumentWorkflow()) {
                        compoundWorkflow.setObjectTitle("D: " + (String) parentProps.get(DocumentCommonModel.Props.DOC_NAME));
                    } else {
                        compoundWorkflow.setObjectTitle("A: " + (String) parentProps.get(DocumentDynamicModel.Props.TITLE));
                    }
                }

                Map<NodeRef, String> tasksByCompoundWorkflow = BeanHelper.getWorkflowDbService().getInProgressTaskOwners(rowsToLoad);
                for (Map.Entry<NodeRef, CompoundWorkflowWithObject> entry : compoundWorkflows.entrySet()) {
                    entry.getValue().setWorkflowStatus(tasksByCompoundWorkflow.get(entry.getKey()));
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
    protected NodeRef getKeyFromValue(CompoundWorkflowWithObject compoundWorkflow) {
        return compoundWorkflow.getCompoundWorkflow().getNodeRef();
    }

    @Override
    protected void resetObjectKeyOrder(List<CompoundWorkflowWithObject> orderedRows) {
        objectKeys.clear();
        for (CompoundWorkflowWithObject compoundWorkflow : orderedRows) {
            objectKeys.add(compoundWorkflow.getCompoundWorkflow().getNodeRef());
        }
    }

}
