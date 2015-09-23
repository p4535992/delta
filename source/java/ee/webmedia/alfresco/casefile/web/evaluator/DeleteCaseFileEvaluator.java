package ee.webmedia.alfresco.casefile.web.evaluator;

import java.util.HashSet;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.evaluator.CaseFileActionsGroupResource;
import ee.webmedia.alfresco.common.evaluator.SharedResourceEvaluator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

public class DeleteCaseFileEvaluator extends SharedResourceEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        if (BeanHelper.getDocumentDialogHelperBean().isInEditMode()) {
            return false;
        }

        if (BeanHelper.getUserService().isAdministrator()) {
            return true;
        }

        if (!AuthenticationUtil.getRunAsUser().equals(node.getProperties().get(DocumentCommonModel.Props.OWNER_ID))
                || DocListUnitStatus.CLOSED.getValueName().equals(node.getProperties().get(DocumentDynamicModel.Props.STATUS))) {
            return false;
        }

        return !hasChildAssocs(node);
    }

    private boolean hasChildAssocs(Node node) {
        NodeService nodeService = BeanHelper.getNodeService();
        HashSet<QName> childNodeTypeQNames = new HashSet<QName>();
        childNodeTypeQNames.add(DocumentCommonModel.Types.DOCUMENT);
        childNodeTypeQNames.add(WorkflowCommonModel.Types.COMPOUND_WORKFLOW);
        if (nodeService.getChildAssocs(node.getNodeRef(), childNodeTypeQNames).size() > 0) {
            return true;
        }
        return false;
    }

    @Override
    public boolean evaluate() {
        CaseFileActionsGroupResource resource = (CaseFileActionsGroupResource) sharedResource;
        if (resource.isInEditMode()) {
            return false;
        }
        if (BeanHelper.getUserService().isAdministrator()) {
            return true;
        }
        if (!resource.isOwner() || !DocListUnitStatus.CLOSED.getValueName().equals(resource.getStatus())) {
            return false;
        }
        return hasChildAssocs(resource.getObject());
    }
}
