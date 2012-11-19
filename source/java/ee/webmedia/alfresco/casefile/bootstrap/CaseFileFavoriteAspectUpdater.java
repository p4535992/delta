package ee.webmedia.alfresco.casefile.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateAndNotQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateAspectQuery;

import java.util.Arrays;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * Used to add cf:favorite aspect to existing doccom:favoriteContainer aspect nodes to avoid circular dependency between cf and doccom model
 * 
 * @author Kaarel Jõgeva
 */
public class CaseFileFavoriteAspectUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        return Arrays.asList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE,
                generateAndNotQuery(generateAspectQuery(DocumentCommonModel.Aspects.FAVORITE_CONTAINER), generateAspectQuery(CaseFileModel.Aspects.FAVORITE))));
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        if (nodeService.hasAspect(nodeRef, CaseFileModel.Aspects.FAVORITE)) {
            return new String[] { "caseFileFavoriteAspectPresent" };
        }

        nodeService.addAspect(nodeRef, CaseFileModel.Aspects.FAVORITE, null);
        return new String[] { "addedCaseFileFavoriteAspect" };
    }
}