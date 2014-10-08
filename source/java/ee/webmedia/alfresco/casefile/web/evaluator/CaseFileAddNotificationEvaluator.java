package ee.webmedia.alfresco.casefile.web.evaluator;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.evaluator.CaseFileActionsGroupResource;
import ee.webmedia.alfresco.common.evaluator.SharedResourceEvaluator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.user.model.UserModel;

public class CaseFileAddNotificationEvaluator extends SharedResourceEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node docNode) {
        if (!docNode.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)) {
            return false;
        }
        if (BeanHelper.getDocumentDialogHelperBean().isInEditMode()) {
            return false;
        }
        return evaluateAssocs(docNode);
    }

    protected boolean evaluateAssocs(Node docNode) {
        return !BeanHelper.getNotificationService().isNotificationAssocExists(BeanHelper.getUserService().getCurrentUser(), docNode.getNodeRef(),
                UserModel.Assocs.CASE_FILE_NOTIFICATION);
    }

    @Override
    public boolean evaluate() {
        CaseFileActionsGroupResource resource = (CaseFileActionsGroupResource) sharedResource;
        return resource.isWorkspaceNode() && !resource.isInEditMode() && !resource.isNotificationAssocExists();
    }

}
