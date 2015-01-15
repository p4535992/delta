package ee.webmedia.alfresco.workflow.bootstrap;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.search.DbSearchUtil;
import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

/**
 * Update task searchable properties from compound workflow.
 */
public class TaskUpdater extends AbstractNodeUpdater {

    private static String compoundWorkflowTaskUpdateString;
    // It is important that this is ordered collection
    private static List<QName> compoundWorkflowTaskSearchableProperties = Arrays.asList(WorkflowSpecificModel.Props.COMPOUND_WORKFLOW_TITLE,
            WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_CREATED_DATE_TIME, WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_OWNER_JOB_TITLE,
            WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_OWNER_ORGANIZATION_NAME, WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_OWNER_NAME,
            WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_STARTED_DATE_TIME,
            WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_STATUS, WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_STOPPED_DATE_TIME,
            WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_FINISHED_DATE_TIME,
            WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_TYPE);

    static {
        List<String> dbColumnNames = new ArrayList<>();
        for (QName prop : compoundWorkflowTaskSearchableProperties) {
            dbColumnNames.add(DbSearchUtil.getDbFieldNameFromPropQName(prop));
        }
        compoundWorkflowTaskUpdateString = DbSearchUtil.createCommaSeparatedUpdateString(dbColumnNames);
    }

    private WorkflowDbService workflowDbService;
    private WorkflowService workflowService;
    private BulkLoadNodeService bulkLoadNodeService;
    private final Map<Long, QName> propertyTypes = new HashMap<>();

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW);
        List<ResultSet> result = new ArrayList<>(6);
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    protected List<String[]> processNodes(final List<NodeRef> batchList, File failedNodesFile) throws Exception, InterruptedException {
        final List<String[]> batchInfos = new ArrayList<>(batchList.size());
        Map<NodeRef, Node> compoundWorkflows = bulkLoadNodeService.loadNodes(batchList, null, propertyTypes);
        List<Pair<String, Map<QName, Serializable>>> compoundWorkflowtaskSearchableProps = new ArrayList<>();
        for (Map.Entry<NodeRef, Node> entry : compoundWorkflows.entrySet()) {
            Map<QName, Serializable> taskSearchableProps = WorkflowUtil.getTaskSearchableProps(RepoUtil.toQNameProperties(entry.getValue().getProperties()));
            compoundWorkflowtaskSearchableProps.add(new Pair(entry.getKey().getId(), taskSearchableProps));
        }
        int[] updateCounts = workflowDbService.updateCompoundWorkflowTaskSearchableProperties(compoundWorkflowtaskSearchableProps, compoundWorkflowTaskSearchableProperties,
                compoundWorkflowTaskUpdateString);
        for (int updateCount : updateCounts) {
            batchInfos.add(new String[] { new Integer(updateCount).toString() });
        }
        return batchInfos;
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] { "nodeRef", "updatedTaskCount" };
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        throw new RuntimeException("Method not implemented!");
    }

    public void setWorkflowDbService(WorkflowDbService workflowDbService) {
        this.workflowDbService = workflowDbService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setBulkLoadNodeService(BulkLoadNodeService bulkLoadNodeService) {
        this.bulkLoadNodeService = bulkLoadNodeService;
    }

}
