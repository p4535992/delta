package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * UI action evaluator for validating whether user can delete current document.
 * 
 * @author Romet Aidla
 */
public class DeleteDocumentEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Node node) {
        ViewStateActionEvaluator viewStateEval = new ViewStateActionEvaluator();
        boolean isInViewState = viewStateEval.evaluate(node);

        boolean hasRegNumber = node.getProperties().get(DocumentCommonModel.Props.REG_NUMBER.toString()) != null;

        IsOwnerEvaluator isOwnerEval = new IsOwnerEvaluator();
        boolean isOwner = isOwnerEval.evaluate(node);

        IsAdminOrDocManagerEvaluator isAdminOrDocManEval = new IsAdminOrDocManagerEvaluator();
        boolean isAdminOrDocManager = isAdminOrDocManEval.evaluate(node);
        boolean hasUserRights = isOwner || isAdminOrDocManager;
        return isInViewState && !hasRegNumber && hasUserRights;
    }
}
