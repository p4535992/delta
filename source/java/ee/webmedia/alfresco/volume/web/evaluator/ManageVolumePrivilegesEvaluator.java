package ee.webmedia.alfresco.volume.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * Evaluator, that evaluates to true if privileges management button is visible
<<<<<<< HEAD
 * 
 * @author Alar Kvell
=======
>>>>>>> develop-5.1
 */
public class ManageVolumePrivilegesEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Object obj) {
<<<<<<< HEAD
        return evaluate((Node) obj);
=======
        return obj != null && evaluate((Node) obj);
>>>>>>> develop-5.1
    }

    @Override
    public boolean evaluate(Node node) {
        return RepoUtil.isSaved(node);
    }

}
