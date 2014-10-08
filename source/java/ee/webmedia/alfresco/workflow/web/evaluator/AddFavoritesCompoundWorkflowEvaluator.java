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
public class AddFavoritesCompoundWorkflowEvaluator extends BaseActionEvaluator {
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

}
