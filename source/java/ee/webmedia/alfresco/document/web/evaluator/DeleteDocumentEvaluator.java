package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.evaluator.SharedResourceEvaluator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * UI action evaluator for validating whether user can delete current document.
 */
public class DeleteDocumentEvaluator extends SharedResourceEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Node docNode) {
        if (!docNode.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)) {
            return false;
        }
        if (BeanHelper.getDocumentDialogHelperBean().isInEditMode()) {
            return false;
        }
        if (BeanHelper.getUserService().isGuest()) {
        	return false;
        }
        return BeanHelper.getUserService().isDocumentManager()
                || (StringUtils.isBlank((String) docNode.getProperties().get(DocumentCommonModel.Props.REG_NUMBER))
                && AuthenticationUtil.getRunAsUser().equals(docNode.getProperties().get(DocumentCommonModel.Props.OWNER_ID.toString())));
    }

    @Override
    public boolean evaluate() {
        DocumentDynamicActionsGroupResources resource = (DocumentDynamicActionsGroupResources) sharedResource;
        if (!resource.isWorkspaceNode() || resource.isInEditMode()) {
            return false;
        }
        if (BeanHelper.getUserService().isGuest()) {
        	return false;
        }
        return BeanHelper.getUserService().isDocumentManager() || (StringUtils.isBlank(resource.getRegNr()) && resource.isDocOwner());
    }

}