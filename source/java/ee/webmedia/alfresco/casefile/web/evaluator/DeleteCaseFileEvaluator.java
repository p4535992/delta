package ee.webmedia.alfresco.casefile.web.evaluator;

import java.util.HashSet;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.web.evaluator.ViewStateActionEvaluator;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

public class DeleteCaseFileEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        if (!new ViewStateActionEvaluator().evaluate(node)) {
            return false;
        }

        if (BeanHelper.getUserService().isAdministrator()) {
            return true;
        }

        if (!AuthenticationUtil.getRunAsUser().equals(node.getProperties().get(DocumentCommonModel.Props.OWNER_ID))
                || DocListUnitStatus.CLOSED.getValueName().equals(node.getProperties().get(DocumentDynamicModel.Props.STATUS))) {
            return false;
        }

        NodeService nodeService = BeanHelper.getNodeService();
        HashSet<QName> childNodeTypeQNames = new HashSet<QName>();
        childNodeTypeQNames.add(DocumentCommonModel.Types.DOCUMENT);
        childNodeTypeQNames.add(WorkflowCommonModel.Types.COMPOUND_WORKFLOW);
        if (nodeService.getChildAssocs(node.getNodeRef(), childNodeTypeQNames).size() > 0) {
            return false;
        }

        return true;
    }

}
