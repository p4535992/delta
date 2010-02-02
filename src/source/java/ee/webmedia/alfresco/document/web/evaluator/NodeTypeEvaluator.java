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
        boolean viewMode = new ViewStateActionEvaluator().evaluate(docNode);
        if (!viewMode) {
            return false;
        }
        
        boolean isOfType = false;
        for (QName nodeType : nodeTypes) {
            isOfType = nodeType.equals(docNode.getType());
            if (isOfType) {
                break;
            }
        }
        return isOfType;
    }
}
