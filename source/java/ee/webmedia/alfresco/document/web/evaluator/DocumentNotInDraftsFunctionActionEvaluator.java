package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

public class DocumentNotInDraftsFunctionActionEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node docNode) {
        NodeService nodeService = BeanHelper.getNodeService();

        NodeRef functionRef = (NodeRef) docNode.getProperties().get(DocumentCommonModel.Props.FUNCTION);
        if (functionRef == null || !nodeService.exists(functionRef)) { // Check if this even exists, since imported document types may have falsely set parent function
            functionRef = nodeService.getPrimaryParent(docNode.getNodeRef()).getParentRef();
        }

        return !BeanHelper.getFunctionsService().isDraftsFunction(functionRef);
    }
}
