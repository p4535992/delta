package ee.webmedia.alfresco.workflow.bootstrap;

import java.util.ArrayList;
import java.util.List;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

/**
 * Add temOutcome aspect to all review worklfows
 * 
 * @author Riina Tens
 */
public class Version22ReviewWorkflowTempOutcomeUpdater extends AbstractNodeUpdater {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(Version22ReviewWorkflowTempOutcomeUpdater.class);

    private SearchService searchService;
    private GeneralService generalService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(WorkflowSpecificModel.Types.REVIEW_WORKFLOW);
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        result.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        boolean modified = false;
        if (!nodeService.hasAspect(nodeRef, WorkflowSpecificModel.Aspects.TEMP_OUTCOME)) {
            nodeService.addAspect(nodeRef, WorkflowSpecificModel.Aspects.TEMP_OUTCOME, null);
            modified = true;
        }
        return new String[]{String.valueOf(modified)};
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

}
