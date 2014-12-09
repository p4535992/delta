package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.evaluator.SharedResourceEvaluator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicDialog;

/**
 * UI action evaluator for validating whether document can be copied.
 */
public class CopyDocumentActionEvaluator extends SharedResourceEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        NodeRef nodeRef = node.getNodeRef();
        if (!nodeRef.getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE) || StringUtils.contains(node.getPath(), DocumentDynamicDialog.FORWARDED_DEC_DOCUMENTS)) {
            return false;
        }
        return !BeanHelper.getDocumentDialogHelperBean().isInEditMode() && !BeanHelper.getDocumentDynamicService().isDraftOrImapOrDvk(nodeRef);
    }

    @Override
    public boolean evaluate() {
        DocumentDynamicActionsGroupResources resource = (DocumentDynamicActionsGroupResources) sharedResource;
        return resource.isWorkspaceNode() && !resource.isInEditMode() && !resource.isDraftOrImapOrDvk() && !resource.isInForwardedDecDocuments();
    }
}
