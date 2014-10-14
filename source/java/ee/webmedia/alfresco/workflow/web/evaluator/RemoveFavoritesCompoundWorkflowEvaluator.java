package ee.webmedia.alfresco.workflow.web.evaluator;

import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.evaluator.CompoundWorkflowActionGroupSharedResource;
import ee.webmedia.alfresco.common.evaluator.SharedResourceEvaluator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;

public class RemoveFavoritesCompoundWorkflowEvaluator extends SharedResourceEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node compoundWorkflowNode) {
        return RepoUtil.isSaved(compoundWorkflowNode) && BeanHelper.getCompoundWorkflowFavoritesService().isFavorite(compoundWorkflowNode.getNodeRef()) != null;
    }

    @Override
    public boolean evaluate(Object obj) {
        return obj != null && evaluate(((CompoundWorkflow) obj).getNode());
    }

    @Override
    public boolean evaluate() {
        CompoundWorkflowActionGroupSharedResource resource = (CompoundWorkflowActionGroupSharedResource) sharedResource;
        return resource.isSaved() && resource.isFavourite();
    }

}
