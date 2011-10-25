package ee.webmedia.alfresco.series.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.series.model.SeriesModel;

/**
 * Changes
 * Teemapõhine toimik -> SUBJECT_FILE
 * Aastapõhine toimik -> ANNUAL_FILE
 * Asjatoimik -> CASE_FILE
 * (CL task 177957)
 * 
 * @author Vladimir Drozdik
 */
public class SeriesVolumeTypesUpdater extends AbstractNodeUpdater {
    String IS_CHANGED = "volumeType is changed to ";
    String NO_CHANGES = "no changes";

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() {
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        String query = generateTypeQuery(SeriesModel.Types.SERIES);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        result.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) {
        List<String> logInfo = new ArrayList<String>();
        logInfo.add("updatedSeriesVolumeTypesValues");
        String newVolumeTypeValue;
        List<String> oldSeriesVolTypes = null;
        oldSeriesVolTypes = (List<String>) nodeService.getProperty(nodeRef, SeriesModel.Props.VOL_TYPE);
        if (oldSeriesVolTypes == null) {
            logInfo.add(NO_CHANGES);
            return logInfo.toArray(new String[logInfo.size()]);
        }
        List<String> newSeriesVolTypes = new ArrayList<String>();
        Iterator<String> iter = oldSeriesVolTypes.iterator();
        while (iter.hasNext()) {
            String volType = iter.next();
            if ("objektipõhine".equals(volType) || "OBJECT".equals(volType)) {
                iter.remove();
                newVolumeTypeValue = VolumeType.SUBJECT_FILE.name();
                newSeriesVolTypes.add(newVolumeTypeValue);
                logInfo.add(IS_CHANGED + newVolumeTypeValue);
            }
            if ("aastapõhine".equals(volType) || "YEAR_BASED".equals(volType)) {
                iter.remove();
                newVolumeTypeValue = VolumeType.ANNUAL_FILE.name();
                newSeriesVolTypes.add(newVolumeTypeValue);
                logInfo.add(IS_CHANGED + newVolumeTypeValue);
            }
            if ("Asjatoimik".equals(volType) || "CASE".equals(volType)) {
                iter.remove();
                newVolumeTypeValue = VolumeType.CASE_FILE.name();
                newSeriesVolTypes.add(newVolumeTypeValue);
                logInfo.add(IS_CHANGED + newVolumeTypeValue);
            }
        }
        newSeriesVolTypes.addAll(oldSeriesVolTypes);
        nodeService.setProperty(nodeRef, SeriesModel.Props.VOL_TYPE, (Serializable) newSeriesVolTypes);
        if (logInfo.size() == 1) {
            logInfo.add(NO_CHANGES);
        }
        return logInfo.toArray(new String[logInfo.size()]);
    }
}
