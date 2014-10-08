package ee.webmedia.alfresco.docdynamic.web;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.evaluator.NodeBasedEvaluatorSharedResource;
import ee.webmedia.alfresco.common.evaluator.SharedResourceEvaluator;
import ee.webmedia.alfresco.common.web.BeanHelper;

/**
 * Evaluator, that evaluates to true if privileges management button is visible
 */
public class ManageDocumentPrivilegesEvaluator extends SharedResourceEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        NodeRef nodeRef = node.getNodeRef();
        return nodeRef.getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)
                && !BeanHelper.getDocumentDialogHelperBean().isInEditMode()
                && !BeanHelper.getDocumentDynamicService().isDraftOrImapOrDvk(nodeRef);
    }

    @Override
    public boolean evaluate() {
        NodeBasedEvaluatorSharedResource resource = (NodeBasedEvaluatorSharedResource) sharedResource;
        return resource.isWorkspaceNode() && !resource.isInEditMode() && !resource.isDraftOrImapOrDvk();
    }
}