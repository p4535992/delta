package ee.webmedia.alfresco.versions.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateAspectQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyBooleanQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.versions.model.VersionsModel;
import ee.webmedia.alfresco.versions.service.VersionsService;

/**
 * Updater that unlocks versions that might be locked accidentally.
 * 
 * @author Kaarel Jõgeva
 */
public class VersionUnlockUpdater extends AbstractNodeUpdater {

    private VersionsService versionsService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = joinQueryPartsAnd(generateAspectQuery(VersionsModel.Aspects.VERSION_LOCKABLE),
                generatePropertyBooleanQuery(VersionsModel.Props.VersionLockable.LOCKED, Boolean.TRUE));

        List<ResultSet> result = new ArrayList<ResultSet>(6);
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        versionsService.setVersionLockableAspect(nodeRef, Boolean.FALSE);
        return new String[] { "unlockedVersion" };
    }

    public void setVersionsService(VersionsService versionsService) {
        this.versionsService = versionsService;
    }

}
