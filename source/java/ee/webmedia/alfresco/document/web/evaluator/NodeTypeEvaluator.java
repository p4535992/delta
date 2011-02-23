package ee.webmedia.alfresco.document.web.evaluator;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

public class NodeTypeEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;
    protected List<QName> nodeTypes = new ArrayList<QName>();

    @Override
    public boolean evaluate(Node docNode) {
        return evaluateViewSatate(docNode) && evaluateType(docNode);
    }

    protected boolean evaluateViewSatate(Node docNode) {
        return new ViewStateActionEvaluator().evaluate(docNode);
    }

    protected boolean evaluateType(Node docNode) {
        return nodeTypes.contains(docNode.getType());
    }
}
