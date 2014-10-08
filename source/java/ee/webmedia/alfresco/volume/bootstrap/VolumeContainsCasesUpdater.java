package ee.webmedia.alfresco.volume.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * Updater for volumes that sets containsCases property true if it contains cases.
 */
public class VolumeContainsCasesUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = generateTypeQuery(VolumeModel.Types.VOLUME);
        return Arrays.asList(
                searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query)
                , searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query)
                );
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        if (Boolean.TRUE.equals(nodeService.getProperty(nodeRef, VolumeModel.Props.CONTAINS_CASES))) {
            return new String[] { "containsCasesAlreadyTrue" };
        }

        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef, new HashSet<QName>(Arrays.asList(CaseModel.Types.CASE)));
        if (childAssocs.isEmpty()) {
            return new String[] { "noCasesUnderVolume" };
        }

        nodeService.setProperty(nodeRef, VolumeModel.Props.CONTAINS_CASES, Boolean.TRUE);
        return new String[] { "updatedContainsCasesToTrue" };
    }

}
