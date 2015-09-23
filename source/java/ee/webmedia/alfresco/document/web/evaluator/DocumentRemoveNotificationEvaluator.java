package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.web.bean.repository.Node;

public class DocumentRemoveNotificationEvaluator extends DocumentAddNotificationEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    protected boolean evaluateAssocs(Node docNode) {
        return !super.evaluateAssocs(docNode);
    }

    @Override
    public boolean evaluate() {
        return ((DocumentDynamicActionsGroupResources) sharedResource).isNotificationAssocExists();
    }
}
