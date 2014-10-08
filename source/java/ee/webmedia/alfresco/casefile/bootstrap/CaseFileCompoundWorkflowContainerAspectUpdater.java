<<<<<<< HEAD
package ee.webmedia.alfresco.casefile.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 * Used to add wfc:compoundWorkflowContainer aspect to existing cf:caseFile type nodes
 * 
 * @author Riina Tens
 */
public class CaseFileCompoundWorkflowContainerAspectUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        List<ResultSet> resultSet = new ArrayList<ResultSet>();
        String query = SearchUtil.generateTypeQuery(CaseFileModel.Types.CASE_FILE);
        for (StoreRef storeRef : generalService.getAllWithArchivalsStoreRefs()) {
            resultSet.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSet;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        if (nodeService.hasAspect(nodeRef, WorkflowCommonModel.Aspects.COMPOUND_WORKFLOW_CONTAINER)) {
            return new String[] { "compoundWorkflowContainer aspect present" };
        }
        nodeService.addAspect(nodeRef, WorkflowCommonModel.Aspects.COMPOUND_WORKFLOW_CONTAINER, null);
        return new String[] { "added compoundWorkflowContainer aspect" };
    }
=======
package ee.webmedia.alfresco.casefile.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 * Used to add wfc:compoundWorkflowContainer aspect to existing cf:caseFile type nodes
 */
public class CaseFileCompoundWorkflowContainerAspectUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        List<ResultSet> resultSet = new ArrayList<ResultSet>();
        String query = SearchUtil.generateTypeQuery(CaseFileModel.Types.CASE_FILE);
        for (StoreRef storeRef : generalService.getAllWithArchivalsStoreRefs()) {
            resultSet.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSet;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        if (nodeService.hasAspect(nodeRef, WorkflowCommonModel.Aspects.COMPOUND_WORKFLOW_CONTAINER)) {
            return new String[] { "compoundWorkflowContainer aspect present" };
        }
        nodeService.addAspect(nodeRef, WorkflowCommonModel.Aspects.COMPOUND_WORKFLOW_CONTAINER, null);
        return new String[] { "added compoundWorkflowContainer aspect" };
    }
>>>>>>> develop-5.1
}