package ee.webmedia.alfresco.workflow.bootstrap;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

public class CompoundWorkflowDefinitionTypeUpdater extends AbstractNodeUpdater {

    private BulkLoadNodeService bulkLoadNodeService;
    private final Map<Long, QName> propertyTypes = new HashMap<>();

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW_DEFINITION);
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
        Map<NodeRef, Map<QName, Serializable>> compoundWorkflowsNewProps = new HashMap<>();
        for (Map.Entry<NodeRef, Node> entry : compoundWorkflows.entrySet()) {
            Map<String, Object> compoundWorkflowProps = entry.getValue().getProperties();
            Map<QName, Serializable> propsToAdd = new HashMap<>();
            if (StringUtils.isBlank((String) compoundWorkflowProps.get(WorkflowCommonModel.Props.TYPE.toString()))) {
                propsToAdd.put(WorkflowCommonModel.Props.TYPE, CompoundWorkflowType.DOCUMENT_WORKFLOW.toString());
            }
            compoundWorkflowsNewProps.put(entry.getKey(), propsToAdd);
        }
        for (NodeRef nodeRef : batchList) {
            if (!compoundWorkflowsNewProps.containsKey(nodeRef)) {
                batchInfos.add(new String[] { "not found " });
                continue;
            }
            Map<QName, Serializable> propsToAdd = compoundWorkflowsNewProps.get(nodeRef);
            if (propsToAdd.isEmpty()) {
                batchInfos.add(new String[] { "no update needed" });
                continue;
            }
            nodeService.addProperties(nodeRef, propsToAdd);
            batchInfos.add(new String[] { "updated" });

        }

        return batchInfos;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        throw new RuntimeException("Method not implemented!");
    }

    public void setBulkLoadNodeService(BulkLoadNodeService bulkLoadNodeService) {
        this.bulkLoadNodeService = bulkLoadNodeService;
    }

}