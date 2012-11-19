package ee.webmedia.alfresco.workflow.bootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 * Add default type = DOCUMENT_WORKFLOW to all compound workflows (and compound workflow definitions), where type = null.
 * 
 * @author Riina Tens
 */

public class AddTypeToCompoundWorkflowUpdater extends AbstractNodeUpdater {

    private DocumentSearchService documentSearchService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.joinQueryPartsAnd(Arrays.asList(
                SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW),
                SearchUtil.generatePropertyNullQuery(WorkflowCommonModel.Props.TYPE)
                ));
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        for (StoreRef storeRef : documentSearchService.getAllStoresWithArchivalStoreVOs()) {
            result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef compoundWorkflowRef) throws Exception {
        String action = "not updated";
        if (StringUtils.isBlank((String) nodeService.getProperty(compoundWorkflowRef, WorkflowCommonModel.Props.TYPE))) {
            nodeService.setProperty(compoundWorkflowRef, WorkflowCommonModel.Props.TYPE, CompoundWorkflowType.DOCUMENT_WORKFLOW.toString());
            action = "updated";
        }
        return new String[] { action };
    }

    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

}
