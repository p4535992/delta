package ee.webmedia.alfresco.document.web.evaluator;

import static ee.webmedia.alfresco.privilege.service.PrivilegeUtil.isAdminOrDocmanagerWithViewDocPermission;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * UI action evaluator for validating whether reopen current document.
 */
public class ReopenDocumentEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Node node) {
        if (!node.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)) {
            return false;
        }
        boolean isFinished = DocumentStatus.FINISHED.getValueName().equals(node.getProperties().get(DocumentCommonModel.Props.DOC_STATUS.toString()));
        return isFinished && new ViewStateActionEvaluator().evaluate(node) && hasUserRights(node);
    }

    public static boolean hasUserRights(Node docNode) {
        return (new IsOwnerEvaluator().evaluate(docNode) || isAdminOrDocmanagerWithViewDocPermission(docNode));
    }

}
