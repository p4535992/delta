package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.evaluator.SharedResourceEvaluator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.privilege.model.Privilege;

public class DocumentSendForInformationEvaluator extends SharedResourceEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node docNode) {
        return docNode.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)
                && !BeanHelper.getDocumentDialogHelperBean().isInEditMode()
                && new DocumentNotInDraftsFunctionActionEvaluator().evaluate(docNode)
                && docNode.hasPermission(Privilege.VIEW_DOCUMENT_META_DATA, Privilege.VIEW_DOCUMENT_FILES);
    }

    @Override
    public boolean evaluate() {
        DocumentDynamicActionsGroupResources resource = (DocumentDynamicActionsGroupResources) sharedResource;
        return resource.isWorkspaceNode() && !resource.isInEditMode() && resource.isNotInDraftsFunction()
                && resource.getObject().hasPermission(Privilege.VIEW_DOCUMENT_META_DATA, Privilege.VIEW_DOCUMENT_FILES);
    }

}
