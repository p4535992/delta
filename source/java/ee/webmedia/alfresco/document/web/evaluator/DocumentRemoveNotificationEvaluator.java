<<<<<<< HEAD
package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.web.bean.repository.Node;

/**
 * @author Riina Tens
 */
public class DocumentRemoveNotificationEvaluator extends DocumentAddNotificationEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    protected boolean evaluateAssocs(Node docNode) {
        return !super.evaluateAssocs(docNode);
    }
}
=======
package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.web.bean.repository.Node;

public class DocumentRemoveNotificationEvaluator extends DocumentAddNotificationEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    protected boolean evaluateAssocs(Node docNode) {
        return !super.evaluateAssocs(docNode);
    }
}
>>>>>>> develop-5.1
