package ee.webmedia.alfresco.series.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
 * and adds SUBJECT_FILE and ANNUAL_FILE if needed.
 * 
 * @author Vladimir Drozdik
 */
public class SeriesVolumeTypesUpdater extends AbstractNodeUpdater {

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
        String logIsChanged = "volumeTypeChanged";
        String logIsAdded = "volumeTypeAdded";
        List<String> logInfo = new ArrayList<String>();
        String newVolumeTypeValue;
        Set<String> newSeriesVolTypes = new HashSet();
        newSeriesVolTypes.add(VolumeType.SUBJECT_FILE.name());
        newSeriesVolTypes.add(VolumeType.ANNUAL_FILE.name());
        List<String> oldSeriesVolTypes = null;
        oldSeriesVolTypes = (List<String>) nodeService.getProperty(nodeRef, SeriesModel.Props.VOL_TYPE);
        if (oldSeriesVolTypes == null) {
            nodeService.setProperty(nodeRef, SeriesModel.Props.VOL_TYPE, new ArrayList<String>(newSeriesVolTypes));
            logInfo.add(logIsAdded);
            logInfo.add(VolumeType.ANNUAL_FILE.name());
            logInfo.add(logIsAdded);
            logInfo.add(VolumeType.SUBJECT_FILE.name());
            return logInfo.toArray(new String[logInfo.size()]);
        }
        Iterator<String> iter = oldSeriesVolTypes.iterator();
        while (iter.hasNext()) {
            String volType = iter.next();
            if ("objektipõhine".equals(volType) || "OBJECT".equals(volType)) {
                iter.remove();
                newVolumeTypeValue = VolumeType.SUBJECT_FILE.name();
                logInfo.add(logIsChanged);
                logInfo.add(newVolumeTypeValue);
            }
            if ("aastapõhine".equals(volType) || "YEAR_BASED".equals(volType)) {
                iter.remove();
                newVolumeTypeValue = VolumeType.ANNUAL_FILE.name();
                logInfo.add(logIsChanged);
                logInfo.add(newVolumeTypeValue);
            }
            if ("Asjatoimik".equals(volType) || "CASE".equals(volType)) {
                iter.remove();
                newVolumeTypeValue = VolumeType.CASE_FILE.name();
                newSeriesVolTypes.add(newVolumeTypeValue);
                logInfo.add(logIsChanged);
                logInfo.add(newVolumeTypeValue);
            }
        }
        newSeriesVolTypes.addAll(oldSeriesVolTypes);
        nodeService.setProperty(nodeRef, SeriesModel.Props.VOL_TYPE, new ArrayList<String>(newSeriesVolTypes));
        if (logInfo.size() == 0) {
            return new String[] {};
        }
        return logInfo.toArray(new String[logInfo.size()]);
    }
}
