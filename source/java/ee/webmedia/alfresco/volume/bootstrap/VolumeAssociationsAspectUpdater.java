package ee.webmedia.alfresco.volume.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * Volumes and casefiles that were created before ver 3.8 are missing a mandatory aspect "volumeAssociations". This updater adds this aspect.
 */
public class VolumeAssociationsAspectUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        List<String> queryParts = new ArrayList<>();
        queryParts.add("(" + SearchUtil.generateTypeQuery(VolumeModel.Types.VOLUME, CaseFileModel.Types.CASE_FILE) + ")");
        queryParts.add(SearchUtil.generateAspectMissingQuery(VolumeModel.Aspects.VOLUME_ASSOCIATIONS));
        String query = SearchUtil.joinQueryPartsAnd(queryParts, false);

        List<ResultSet> result = new ArrayList<>();
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        nodeService.addAspect(nodeRef, VolumeModel.Aspects.VOLUME_ASSOCIATIONS, null);
        return null;
    }

}
