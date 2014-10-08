package ee.webmedia.alfresco.casefile.web.evaluator;

import org.alfresco.web.bean.repository.Node;

public class CaseFileRemoveNotificationEvaluator extends CaseFileAddNotificationEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    protected boolean evaluateAssocs(Node docNode) {
        return !super.evaluateAssocs(docNode);
    }
}
