package ee.webmedia.alfresco.document.web.evaluator;

import static ee.webmedia.alfresco.privilege.service.PrivilegeUtil.isAdminOrDocmanagerWithViewDocPermission;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.evaluator.SharedResourceEvaluator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * UI action evaluator for validating whether reopen current document.
 */
public class ReopenDocumentEvaluator extends SharedResourceEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Node node) {
        if (!node.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE) || BeanHelper.getDocumentDialogHelperBean().isInEditMode()) {
            return false;
        }
        boolean isFinished = DocumentStatus.FINISHED.getValueName().equals(node.getProperties().get(DocumentCommonModel.Props.DOC_STATUS.toString()));
        return isFinished && hasUserRights(node);
    }

    public static boolean hasUserRights(Node docNode) {
        return AuthenticationUtil.getRunAsUser().equals(docNode.getProperties().get(DocumentCommonModel.Props.OWNER_ID.toString()))
                || isAdminOrDocmanagerWithViewDocPermission(docNode);
    }

    @Override
    public boolean evaluate() {
        DocumentDynamicActionsGroupResources resource = (DocumentDynamicActionsGroupResources) sharedResource;
        return resource.isWorkspaceNode() && !resource.isInEditMode()
                && resource.isInStatus(DocumentStatus.FINISHED)
                && (resource.isDocOwner() || isAdminOrDocmanagerWithViewDocPermission(resource.getObject()));

    }

}
