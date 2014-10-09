package ee.webmedia.alfresco.common.evaluator;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;

public abstract class NodeBasedEvaluatorSharedResource extends EvaluatorSharedResource<Node> {
    private static final long serialVersionUID = 1L;

    private Boolean workspaceNode;
    private Boolean draftOrImapOrDvk;

    public boolean isWorkspaceNode() {
        if (workspaceNode == null) {
            workspaceNode = getObject().getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE);
        }
        return workspaceNode;
    }

    public boolean isDraftOrImapOrDvk() {
        if (draftOrImapOrDvk == null) {
            draftOrImapOrDvk = BeanHelper.getDocumentDynamicService().isDraftOrImapOrDvk(getObject().getNodeRef());
        }
        return draftOrImapOrDvk;
    }

}
