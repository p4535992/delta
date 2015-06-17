package ee.webmedia.alfresco.document.bootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Update documents under restricted series to fix bug described in https://jira.nortal.com/browse/DELTA-824
 * (documents in restricted series are not indexed with privileges, so users with correct privileges can't see the documents).
 */
public class DocVisibleToUpdater extends AbstractNodeUpdater {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DocVisibleToUpdater.class);

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        LinkedHashSet<StoreRef> allStores = generalService.getAllStoreRefsWithTrashCan();
        List<NodeRef> restrictedSeries = BeanHelper.getDocumentSearchService().searchRestrictedSeries(allStores);
        List<String> restrictedSeriesStr = new ArrayList<>();
        for (NodeRef seriesRef : restrictedSeries) {
            restrictedSeriesStr.add(seriesRef.toString());
        }
        LOG.info("Found " + restrictedSeriesStr.size() + " restricted series");
        String query = SearchUtil.joinQueryPartsAnd(Arrays.asList(
                SearchUtil.generateTypeQuery(DocumentCommonModel.Types.DOCUMENT),
                SearchUtil.joinQueryPartsOr(SearchUtil.generatePropertyExactQuery(DocumentCommonModel.Props.SERIES, restrictedSeriesStr),
                        SearchUtil.generatePropertyNullQuery(DocumentCommonModel.Props.SERIES))));

        List<ResultSet> resultSets = new ArrayList<>();
        for (StoreRef storeRef : allStores) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        int docCount = 0;
        for (ResultSet resultSet : resultSets) {
            docCount += resultSet.length();
        }
        LOG.info("Found " + docCount + " documents to update");
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        nodeService.addProperties(nodeRef, nodeService.getProperties(nodeRef));
        return null;
    }

}
