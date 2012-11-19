package ee.webmedia.alfresco.document.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 * @author Riina Tens
 *         Refactored from DocumentService.
 */
public interface FavoritesService {

    List<NodeRef> getFavorites(NodeRef containerNodeRef);

    List<String> getFavoriteDirectoryNames();

    boolean isFavoriteAddable(NodeRef nodeRef);

    NodeRef isFavorite(NodeRef docRef);

    boolean addFavorite(NodeRef nodeRef, String favDirName, boolean updateMenu);

    void removeFavorite(NodeRef nodeRef);

}
