package ee.webmedia.alfresco.document.bootstrap;

import java.util.Arrays;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Set document permission inheritance to false.
 * 
 * @author Alar Kvell
 */
public class DocumentInheritPermissionsUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.joinQueryPartsAnd(Arrays.asList(
                SearchUtil.generateTypeQuery(DocumentCommonModel.Types.DOCUMENT),
                SearchUtil.generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE)));
        return Arrays.asList(
                searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query),
                searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
    }

    @Override
    protected String[] updateNode(final NodeRef docRef) throws Exception {
        // This does not change node modified time
        serviceRegistry.getPermissionService().setInheritParentPermissions(docRef, false);
        return null;
    }

}
