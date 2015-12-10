package ee.webmedia.alfresco.series.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * This updater sets volumeAssociation aspect to volumes and caseFiles if it is missing.
 */
public class VolumeCaseFileVolumeAssociationUpdater extends AbstractNodeUpdater {
    

    private boolean executeInBackground;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() {

        String query = generateTypeQuery(VolumeModel.Types.VOLUME, CaseFileModel.Types.CASE_FILE);
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllWithArchivalsStoreRefs()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        List<String> logInfo = new ArrayList<String>();

        addVolumeAssociationAspect(nodeRef, logInfo);

        return logInfo.toArray(new String[logInfo.size()]);
    }

    private void addVolumeAssociationAspect(NodeRef nodeRef, List<String> logInfo) {
        QName aspect = VolumeModel.Aspects.VOLUME_ASSOCIATIONS;
        if (nodeService.hasAspect(nodeRef, aspect)) {
            logInfo.add("volumeAssociationAspectExists");
        } else {
            nodeService.addAspect(nodeRef, aspect, null);
            logInfo.add("volumeAssociationAspectAdded");
        }
    }


    @Override
    protected void executeInternal() throws Throwable {
        if (!isEnabled()) {
            log.info("Skipping node updater, because it is disabled" + (isExecuteOnceOnly() ? ". It will not be executed again, because executeOnceOnly=true" : ""));
            return;
        }
        if (executeInBackground) {
            super.executeUpdaterInBackground();
        } else {
            super.executeUpdater();
        }
    }

    public void setExecuteInBackground(boolean executeInBackground) {
        this.executeInBackground = executeInBackground;
    }

}
