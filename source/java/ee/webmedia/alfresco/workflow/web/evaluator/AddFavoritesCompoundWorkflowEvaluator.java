package ee.webmedia.alfresco.workflow.web.evaluator;

import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.evaluator.CompoundWorkflowActionGroupSharedResource;
import ee.webmedia.alfresco.common.evaluator.SharedResourceEvaluator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;

public class AddFavoritesCompoundWorkflowEvaluator extends SharedResourceEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node compoundWorkflowNode) {
        return compoundWorkflowNode != null && RepoUtil.isSaved(compoundWorkflowNode)
                && BeanHelper.getCompoundWorkflowFavoritesService().isFavoriteAddable(compoundWorkflowNode.getNodeRef());
    }

    @Override
    public boolean evaluate(Object obj) {
        return obj != null && evaluate(((CompoundWorkflow) obj).getNode());
    }

    @Override
    public boolean evaluate() {
        CompoundWorkflowActionGroupSharedResource resource = (CompoundWorkflowActionGroupSharedResource) sharedResource;
        CompoundWorkflow compoundWorkflow = resource.getObject();
        return compoundWorkflow != null && resource.isSaved() && !resource.isFavourite() && !BeanHelper.getDocumentService().isDraft(compoundWorkflow.getNodeRef());
    }

}
