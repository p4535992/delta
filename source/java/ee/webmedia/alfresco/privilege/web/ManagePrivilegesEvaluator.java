package ee.webmedia.alfresco.privilege.web;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.web.evaluator.DocumentSavedActionEvaluator;
import ee.webmedia.alfresco.document.web.evaluator.ViewStateActionEvaluator;

/**
 * Evaluator, that evaluates to true if privileges management button is visible
 */
public class ManagePrivilegesEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        return new ViewStateActionEvaluator().evaluate(node) && new DocumentSavedActionEvaluator().evaluate(node);
    }
}
