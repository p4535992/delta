package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

public class ContractSimOrSmitNodeTypeEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        return new ContractSimNodeTypeEvaluator().evaluate(node) || new ContractSmitNodeTypeEvaluator().evaluate(node);
    }
}
