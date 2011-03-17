package ee.webmedia.alfresco.workflow.bootstrap;

import java.util.ArrayList;
import java.util.List;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 * Updates parallelTasks property if workflow's paralleTask == null 
 * (the situation is actually not allowed in current model, but it somehow appears in live environment)
 * and also rewrites all properties for all tasks in such workflow
 * to force lucene index update (CL task 155454)
 * 
 * @author Riina Tens
 */

public class ParallelTasksPropertiesUpdater extends AbstractNodeUpdater {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ParallelTasksPropertiesUpdater.class);

    private SearchService searchService;
    private NodeService nodeService;
    private GeneralService generalService;
    private boolean enabled = false;
    
    @Override
    protected void executeInternal() throws Throwable{
        if (!enabled) {
            log.debug("Skipping parallelTasks property update, execution parameter (parallelTasksPropertiesUpdater.enabled) set to false.");
            return;
        }
        super.executeInternal();
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        List<String> queryParts = new ArrayList<String>(2);
        queryParts.add(SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.WORKFLOW));
        queryParts.add(SearchUtil.generatePropertyNullQuery(WorkflowCommonModel.Props.PARALLEL_TASKS));
        String query = SearchUtil.joinQueryPartsAnd(queryParts);
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        result.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        
        if (!nodeService.exists(nodeRef)) {
            return new String[] { nodeRef.toString(), "nodeDoesNotExist" };
        }
        
        nodeService.setProperty(nodeRef, WorkflowCommonModel.Props.PARALLEL_TASKS, Boolean.TRUE);
        
        List<String> updatedRefs = new ArrayList<String>();
        updatedRefs.add(nodeRef.toString());
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef);
        for (ChildAssociationRef childAssoc : childAssocs) {
            NodeRef taskRef = childAssoc.getChildRef();
            updatedRefs.add(taskRef.toString());            
            nodeService.setProperties(taskRef, nodeService.getProperties(taskRef));
        }

        return (String[]) updatedRefs.toArray(new String[updatedRefs.size()]);
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
