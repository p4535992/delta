package ee.webmedia.alfresco.volume.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.TransientNode;

import ee.webmedia.alfresco.volume.model.Volume;

/**
 * Evaluator, that evaluates to true if privileges management button is visible
 * 
 * @author Alar Kvell
 */
public class ManageVolumePrivilegesEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Object obj) {
        return !(((Volume) obj).getNode() instanceof TransientNode);
    }

}
