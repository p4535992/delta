package ee.webmedia.alfresco.workflow.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.document.service.FavoritesService;

public interface CompoundWorkflowFavoritesService extends FavoritesService {

    String BEAN_NAME = "CompoundWorkflowFavoritesService";

    List<NodeRef> getCompoundWorkflowFavorites(NodeRef containerNodeRef);

}
