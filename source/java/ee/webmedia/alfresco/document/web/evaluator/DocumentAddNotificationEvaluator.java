package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.evaluator.SharedResourceEvaluator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicDialog;
import ee.webmedia.alfresco.user.model.UserModel;

public class DocumentAddNotificationEvaluator extends SharedResourceEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node docNode) {
        if (!docNode.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)
                || StringUtils.contains(docNode.getPath(), DocumentDynamicDialog.FORWARDED_DEC_DOCUMENTS)) {
            return false;
        }
        if (BeanHelper.getDocumentDialogHelperBean().isInEditMode() || !BeanHelper.getWorkflowConstantsBean().isIndependentWorkflowEnabled()) {
            return false;
        }
        return evaluateAssocs(docNode);
    }

    protected boolean evaluateAssocs(Node docNode) {
        return !BeanHelper.getNotificationService().isNotificationAssocExists(BeanHelper.getUserService().getCurrentUser(), docNode.getNodeRef(),
                UserModel.Assocs.DOCUMENT_NOTIFICATION);
    }

    @Override
    public boolean evaluate() {
        DocumentDynamicActionsGroupResources resource = (DocumentDynamicActionsGroupResources) sharedResource;
        if (!resource.isWorkspaceNode() || resource.isInEditMode() || !BeanHelper.getWorkflowConstantsBean().isIndependentWorkflowEnabled() || resource.isInForwardedDecDocuments()) {
            return false;
        }
        return !resource.isNotificationAssocExists();
    }
}
