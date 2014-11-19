package ee.webmedia.alfresco.volume.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
<<<<<<< HEAD

import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * Evaluator, that evaluates to true if privileges management button is visible
 * 
 * @author Alar Kvell
=======
import org.alfresco.web.bean.repository.TransientNode;

/**
 * Evaluator, that evaluates to true if privileges management button is visible
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class ManageVolumePrivilegesEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Object obj) {
<<<<<<< HEAD
        return evaluate((Node) obj);
=======
        return obj != null && evaluate((Node) obj);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    @Override
    public boolean evaluate(Node node) {
<<<<<<< HEAD
        return RepoUtil.isSaved(node);
=======
        return !(node instanceof TransientNode);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

}
