package ee.webmedia.alfresco.volume.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;

/**
 * Evaluator, that evaluates to true if privileges management button is visible
 * 
 * @author Alar Kvell
 */
public class ManageVolumePrivilegesEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Object obj) {
        return obj != null && evaluate((Node) obj);
    }

    @Override
    public boolean evaluate(Node node) {
        return !(node instanceof TransientNode);
    }

}
