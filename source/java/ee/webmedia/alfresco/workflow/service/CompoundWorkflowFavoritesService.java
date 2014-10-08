<<<<<<< HEAD
package ee.webmedia.alfresco.workflow.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.document.service.FavoritesService;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowWithObject;

/**
 * @author Riina Tens
 */
public interface CompoundWorkflowFavoritesService extends FavoritesService {

    String BEAN_NAME = "CompoundWorkflowFavoritesService";

    List<CompoundWorkflowWithObject> getCompoundWorkflowFavorites(NodeRef containerNodeRef);

}
=======
package ee.webmedia.alfresco.workflow.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.document.service.FavoritesService;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowWithObject;

public interface CompoundWorkflowFavoritesService extends FavoritesService {

    String BEAN_NAME = "CompoundWorkflowFavoritesService";

    List<CompoundWorkflowWithObject> getCompoundWorkflowFavorites(NodeRef containerNodeRef);

}
>>>>>>> develop-5.1
