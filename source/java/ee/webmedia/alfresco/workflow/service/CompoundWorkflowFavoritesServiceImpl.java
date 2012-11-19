package ee.webmedia.alfresco.workflow.service;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.service.AbstractFavoritesServiceImpl;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowWithObject;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 * @author Riina Tens
 */
public class CompoundWorkflowFavoritesServiceImpl extends AbstractFavoritesServiceImpl implements CompoundWorkflowFavoritesService {

    private WorkflowService workflowService;

    @Override
    public List<CompoundWorkflowWithObject> getCompoundWorkflowFavorites(NodeRef containerNodeRef) {
        List<NodeRef> favouriteRefs = getFavorites(containerNodeRef);
        List<CompoundWorkflowWithObject> favorites = new ArrayList<CompoundWorkflowWithObject>(favouriteRefs.size());
        for (NodeRef cwfRef : favouriteRefs) {
            favorites.add(workflowService.getCompoundWorkflowWithObject(cwfRef));
        }
        return favorites;
    }

    @Override
    protected QName getFavoriteAssocQName() {
        return WorkflowCommonModel.Assocs.FAVORITE;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

}
