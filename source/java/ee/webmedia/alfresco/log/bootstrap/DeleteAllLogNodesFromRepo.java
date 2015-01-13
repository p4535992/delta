package ee.webmedia.alfresco.log.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Delete all log nodes from repo.
 * NB! Assumes that all bootstraps transferring data from repo to delta_log table have been executed,
 * otherwise log data is lost.
 */
public class DeleteAllLogNodesFromRepo extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(MoveDocumentAndSeriesLogToAppLog.DOCUMENT_LOG);
        List<ResultSet> result = new ArrayList<ResultSet>(6);
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            result.add(BeanHelper.getSearchService().query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        nodeService.deleteNode(nodeRef);
        return new String[] {};
    }

    @Override
    public boolean isContinueWithNextBatchAfterError() {
        return true;
    }

}
