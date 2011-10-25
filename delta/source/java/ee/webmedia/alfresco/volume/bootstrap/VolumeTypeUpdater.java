package ee.webmedia.alfresco.volume.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * Changes volume type value in repository.
 * Teemapõhine toimik -> SUBJECT_FILE
 * Aastapõhine toimik -> ANNUAL_FILE
 * Asjatoimik -> CASE_FILE
 * (CL task 177957)
 * 
 * @author Vladimir Drozdik
 */
public class VolumeTypeUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() {
        Collection<String> acceptablePropertyValues = Arrays.asList("objektipõhine", "OBJECT", "aastapõhine", "YEAR_BASED", "Asjatoimik", "CASE");
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        String query = joinQueryPartsAnd(Arrays.asList(generateTypeQuery(VolumeModel.Types.VOLUME),
                SearchUtil.generatePropertyExactQuery(VolumeModel.Props.VOLUME_TYPE, acceptablePropertyValues, false)
                ));
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        result.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) {
        String newVolumeTypeValue = null;

        String volType = (String) nodeService.getProperty(nodeRef, VolumeModel.Props.VOLUME_TYPE);
        if (volType.equals("objektipõhine") || volType.equals("OBJECT")) {
            newVolumeTypeValue = VolumeType.SUBJECT_FILE.name();
            nodeService.setProperty(nodeRef, VolumeModel.Props.VOLUME_TYPE, newVolumeTypeValue);
        } else if (volType.equals("aastapõhine") || volType.equals("YEAR_BASED")) {
            newVolumeTypeValue = VolumeType.ANNUAL_FILE.name();
            nodeService.setProperty(nodeRef, VolumeModel.Props.VOLUME_TYPE, newVolumeTypeValue);
        } else if (volType.equals("Asjatoimik") || volType.equals("CASE")) {
            newVolumeTypeValue = VolumeType.CASE_FILE.name();
            nodeService.setProperty(nodeRef, VolumeModel.Props.VOLUME_TYPE, newVolumeTypeValue);
        }
        if (newVolumeTypeValue == null) {
            newVolumeTypeValue = "no changes";
        }
        return new String[] { "updatedVolumeTypeValue", newVolumeTypeValue };
    }
}
