package ee.webmedia.alfresco.document.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * Refactored from DocumentServiceImpl.
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
        Map<NodeRef, Document> favorites = BeanHelper.getBulkLoadNodeService().loadDocuments(favouriteRefs, null);
        List<NodeRef> deletedDocuments = new ArrayList<>();
        Map<StoreRef, Boolean> archiveMappings = new HashMap<>();
        for (Document document : favorites.values()) {
            NodeRef nodeRef = document.getNodeRef();
            StoreRef storeRef = nodeRef.getStoreRef();
            Boolean hasArchive = archiveMappings.get(storeRef);
            if (hasArchive == null) {
                hasArchive = nodeService.hasStoreArchiveMapping(storeRef);
                archiveMappings.put(storeRef, hasArchive);
            }
            if (!hasArchive) {
                // trashcan documents are not displayed in favorites view
                deletedDocuments.add(nodeRef);
            }
        }
        for (NodeRef deletedDocRef : deletedDocuments) {
            favorites.remove(deletedDocRef);
        }
        return new ArrayList<>(favorites.values());
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

}
