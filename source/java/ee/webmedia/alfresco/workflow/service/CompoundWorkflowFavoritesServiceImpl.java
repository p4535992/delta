package ee.webmedia.alfresco.workflow.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.service.AbstractFavoritesServiceImpl;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

public class CompoundWorkflowFavoritesServiceImpl extends AbstractFavoritesServiceImpl implements CompoundWorkflowFavoritesService {

    private WorkflowService workflowService;

    @Override
    public List<NodeRef> getCompoundWorkflowFavorites(NodeRef containerNodeRef) {
        return getFavorites(containerNodeRef);
    }

    @Override
    protected QName getFavoriteAssocQName() {
        return WorkflowCommonModel.Assocs.FAVORITE;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

}
