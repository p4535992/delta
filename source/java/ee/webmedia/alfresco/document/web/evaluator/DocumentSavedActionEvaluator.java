package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.evaluator.SharedResourceEvaluator;
import ee.webmedia.alfresco.common.web.BeanHelper;

/**
 * UI action evaluator for validating whether document is saved (is not draft).
 * <p/>
 * Can be used only with {@link ee.webmedia.alfresco.document.web.DocumentDialog}.
 */
public class DocumentSavedActionEvaluator extends SharedResourceEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        return node.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE) && !BeanHelper.getDocumentDynamicService().isDraftOrImapOrDvk(node.getNodeRef());
    }

    @Override
    public boolean evaluate() {
        DocumentDynamicActionsGroupResources resource = (DocumentDynamicActionsGroupResources) sharedResource;
        return resource.isWorkspaceNode() && !resource.isDraftOrImapOrDvk();
    }
}
