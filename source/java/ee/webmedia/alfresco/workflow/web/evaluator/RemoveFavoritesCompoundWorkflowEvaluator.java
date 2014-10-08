package ee.webmedia.alfresco.workflow.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;

<<<<<<< HEAD
/**
 * @author Riina Tens
 */
=======
>>>>>>> develop-5.1
public class RemoveFavoritesCompoundWorkflowEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node compoundWorkflowNode) {
        return RepoUtil.isSaved(compoundWorkflowNode) && BeanHelper.getCompoundWorkflowFavoritesService().isFavorite(compoundWorkflowNode.getNodeRef()) != null;
    }

    @Override
    public boolean evaluate(Object obj) {
        return evaluate(((CompoundWorkflow) obj).getNode());
    }

}
