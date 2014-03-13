package ee.webmedia.alfresco.document.search.bootstrap;

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

import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * Add or update location properties for existing case and volume nodes.
 */
public class VolumeAndCaseLocationPropUpdater extends AbstractNodeUpdater {

    private DocumentSearchService documentSearchService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(VolumeModel.Types.VOLUME, CaseModel.Types.CASE);
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        for (StoreRef storeRef : documentSearchService.getAllStoresWithArchivalStoreVOs()) {
            result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Map<QName, Serializable> locationProps = new HashMap<QName, Serializable>();
        NodeRef volumeOrSeries = generalService.getPrimaryParent(nodeRef).getNodeRef();
        boolean isCase = nodeService.getType(nodeRef).equals(CaseModel.Types.CASE);
        if (isCase) {
            locationProps.put(DocumentCommonModel.Props.VOLUME, volumeOrSeries);
        } else {
            locationProps.put(DocumentCommonModel.Props.SERIES, volumeOrSeries);
        }
        NodeRef seriesOrFunction = generalService.getPrimaryParent(volumeOrSeries).getNodeRef();
        if (isCase) {
            locationProps.put(DocumentCommonModel.Props.SERIES, seriesOrFunction);
        } else {
            locationProps.put(DocumentCommonModel.Props.FUNCTION, seriesOrFunction);
        }
        if (isCase) {
            NodeRef functionRef = generalService.getPrimaryParent(seriesOrFunction).getNodeRef();
            locationProps.put(DocumentCommonModel.Props.FUNCTION, functionRef);
        }
        nodeService.addProperties(nodeRef, locationProps);
        return new String[0];
    }

    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

}
