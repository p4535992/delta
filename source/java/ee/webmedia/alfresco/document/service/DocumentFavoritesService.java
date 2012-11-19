package ee.webmedia.alfresco.document.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.document.model.Document;

/**
 * @author Riina Tens
 *         Refactored from DocumentService.
 */
public interface DocumentFavoritesService extends FavoritesService {

    String BEAN_NAME = "DocumentFavoritesService";

    List<Document> getDocumentFavorites(NodeRef containerNodeRef);

}
