<<<<<<< HEAD
package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.user.model.UserModel;

/**
 * @author Riina Tens
 */
public class DocumentAddNotificationEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node docNode) {
        if (!docNode.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)) {
            return false;
        }
        ViewStateActionEvaluator viewStateEval = new ViewStateActionEvaluator();
        if (!viewStateEval.evaluate(docNode) || !BeanHelper.getWorkflowService().isIndependentWorkflowEnabled()) {
            return false;
        }
        return evaluateAssocs(docNode);
    }

    protected boolean evaluateAssocs(Node docNode) {
        return !BeanHelper.getNotificationService().isNotificationAssocExists(BeanHelper.getUserService().getCurrentUser(), docNode.getNodeRef(),
                UserModel.Assocs.DOCUMENT_NOTIFICATION);
    }
}
=======
package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.user.model.UserModel;

public class DocumentAddNotificationEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node docNode) {
        if (!docNode.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)) {
            return false;
        }
        ViewStateActionEvaluator viewStateEval = new ViewStateActionEvaluator();
        if (!viewStateEval.evaluate(docNode) || !BeanHelper.getWorkflowService().isIndependentWorkflowEnabled()) {
            return false;
        }
        return evaluateAssocs(docNode);
    }

    protected boolean evaluateAssocs(Node docNode) {
        return !BeanHelper.getNotificationService().isNotificationAssocExists(BeanHelper.getUserService().getCurrentUser(), docNode.getNodeRef(),
                UserModel.Assocs.DOCUMENT_NOTIFICATION);
    }
}
>>>>>>> develop-5.1
