package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

/**
 * UI action evaluator for validating whether document can be copied.
 */
public class CopyDocumentActionEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        if (!node.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)) {
            return false;
        }
        ViewStateActionEvaluator viewStateEval = new ViewStateActionEvaluator();
        DocumentSavedActionEvaluator documentSavedEval = new DocumentSavedActionEvaluator();
        return viewStateEval.evaluate(node) && documentSavedEval.evaluate(node);
    }
}
