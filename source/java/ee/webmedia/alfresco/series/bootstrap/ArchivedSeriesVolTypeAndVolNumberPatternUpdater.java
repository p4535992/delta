package ee.webmedia.alfresco.series.bootstrap;

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
import org.apache.commons.collections4.CollectionUtils;

import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.SearchUtil;

public class ArchivedSeriesVolTypeAndVolNumberPatternUpdater extends AbstractNodeUpdater {

    private static final List<String> types = new ArrayList<String>(2);

    static {
        for (VolumeType type : VolumeType.values()) {
            if (VolumeType.CASE_FILE.equals(type)) {
                continue;
            }
            types.add(type.name());
        }
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(SeriesModel.Types.SERIES);

        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getArchivalsStoreRefs()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        @SuppressWarnings("unchecked")
        List<String> volTypes = (List<String>) nodeService.getProperty(nodeRef, SeriesModel.Props.VOL_TYPE);
        Map<QName, Serializable> propsToUpdate = new HashMap<QName, Serializable>();
        boolean addVolTypes = CollectionUtils.isEmpty(volTypes);
        if (addVolTypes) {
            propsToUpdate.put(SeriesModel.Props.VOL_TYPE, (Serializable) types);
        }
        if (addVolTypes || CollectionUtils.isNotEmpty(volTypes) && !volTypes.contains(VolumeType.CASE_FILE.name())) {
            propsToUpdate.put(SeriesModel.Props.VOL_NUMBER_PATTERN, "");
        }
        if (!propsToUpdate.isEmpty()) {
            nodeService.addProperties(nodeRef, propsToUpdate);
        }
        return propsToUpdate.keySet().toArray(new String[0]);
    }
}
