<<<<<<< HEAD
package ee.webmedia.alfresco.workflow.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

/**
 * Add temOutcome aspect to all review worklfows
 * 
 * @author Riina Tens
 */
public class Version22ReviewWorkflowTempOutcomeUpdater extends AbstractNodeUpdater {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(Version22ReviewWorkflowTempOutcomeUpdater.class);

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
        return new String[] { String.valueOf(modified) };
    }

}
=======
package ee.webmedia.alfresco.workflow.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

/**
 * Add temOutcome aspect to all review worklfows
 */
public class Version22ReviewWorkflowTempOutcomeUpdater extends AbstractNodeUpdater {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(Version22ReviewWorkflowTempOutcomeUpdater.class);

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
        return new String[] { String.valueOf(modified) };
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
