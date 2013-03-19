package ee.webmedia.alfresco.document.service;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * @author Riina Tens
 *         Refactored from DocumentServiceImpl.
 */
public class DocumentFavoritesServiceImpl extends AbstractFavoritesServiceImpl implements DocumentFavoritesService {

    String BEAN_NAME = "DocumentFavoritesService";
    private DocumentService documentService;

    @Override
    public boolean isFavoriteAddable(NodeRef docRef) {
        return super.isFavoriteAddable(docRef) && !documentService.isDraft(docRef);
    }

    @Override
    protected QName getFavoriteAssocQName() {
        return DocumentCommonModel.Assocs.FAVORITE;
    }

    @Override
    public List<Document> getDocumentFavorites(NodeRef containerNodeRef) {
        List<NodeRef> favouriteRefs = getFavorites(containerNodeRef);
        List<Document> favorites = new ArrayList<Document>(favouriteRefs.size());
        for (NodeRef docRef : favouriteRefs) {
            if (!DocumentCommonModel.Types.DOCUMENT.equals(nodeService.getType(docRef))) { // XXX DLSeadist filter out old document types
                continue;
            }
            favorites.add(documentService.getDocumentByNodeRef(docRef));
        }
        return favorites;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

}
