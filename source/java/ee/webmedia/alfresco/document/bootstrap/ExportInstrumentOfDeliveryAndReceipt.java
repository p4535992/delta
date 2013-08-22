package ee.webmedia.alfresco.document.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Run manually from nodebrowser.
 * Export delivererName field for instrumentOfDeliveryAndReceipt document type.
 * 
 * @author Riina Tens
 */
public class ExportInstrumentOfDeliveryAndReceipt extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(DocumentSubtypeModel.Types.INSTRUMENT_OF_DELIVERY_AND_RECEIPT);
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        String delivereName = (String) nodeService.getProperty(nodeRef, DocumentSpecificModel.Props.DELIVERER_NAME);
        return new String[] { StringUtils.defaultString(delivereName, "") };
    }

}
