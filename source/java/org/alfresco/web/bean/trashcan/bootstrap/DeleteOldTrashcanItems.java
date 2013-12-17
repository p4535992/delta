package org.alfresco.web.bean.trashcan.bootstrap;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.lang.time.DateUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Delete archived items that are archived earlier than 3 months.
 * This updater is present only in 3.6.30 branch to fix bugs while upgrading 3.6.21 to 3.6.30 branch
 * 
 * @author Riina Tens
 */
public class DeleteOldTrashcanItems extends AbstractNodeUpdater {

    @Override
    public boolean isContinueWithNextBatchAfterError() {
        return true;
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        Date deleteBeforeDate = DateUtils.addMonths(new Date(), -3);
        String query = SearchUtil.joinQueryPartsAnd(SearchUtil.generateAspectQuery(ContentModel.ASPECT_ARCHIVED),
                SearchUtil.generateDatePropertyRangeQuery(null, deleteBeforeDate, ContentModel.PROP_ARCHIVED_DATE));
        return Arrays.asList(searchService.query(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE, SearchService.LANGUAGE_LUCENE, query));
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        nodeService.deleteNode(nodeRef);
        return null;
    }

}
