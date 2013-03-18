package ee.webmedia.alfresco.document.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.TextUtil;

/**
 * Remove delete documents from favorites directories. Cl task 213149.
 * 
 * @author Riina Tens
 */
public class FavoritesDeletedDocumentsUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = generateTypeQuery(DocumentCommonModel.Types.FAVORITE_DIRECTORY);
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        List<String> deletedNodeRefs = new ArrayList<String>();
        List<AssociationRef> childAssocs = nodeService.getTargetAssocs(nodeRef, DocumentCommonModel.Assocs.FAVORITE);
        for (AssociationRef assoc : childAssocs) {
            NodeRef docRef = assoc.getTargetRef();
            if (DocumentCommonModel.Types.DOCUMENT.equals(nodeService.getType(docRef))) {
                if (nodeService.getStoreArchiveNode(docRef.getStoreRef()) == null) {
                    deletedNodeRefs.add(docRef.toString());
                    nodeService.removeAssociation(nodeRef, docRef, DocumentCommonModel.Assocs.FAVORITE);
                }
            }
        }
        return new String[] { TextUtil.joinNonBlankStringsWithComma(deletedNodeRefs) };
    }
}
