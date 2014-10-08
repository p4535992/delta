package ee.webmedia.alfresco.user.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

/**
 * Evaluator, that evaluates to true if user is admin or archivist
 */
public class ArchivistEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        return evaluate((Object) node);
    }

    @Override
    public boolean evaluate(Object obj) {
        return getUserService().isArchivist();
    }

}
