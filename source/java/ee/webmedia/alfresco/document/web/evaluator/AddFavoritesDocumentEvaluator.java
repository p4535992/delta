package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.evaluator.SharedResourceEvaluator;
import ee.webmedia.alfresco.common.web.BeanHelper;

public class AddFavoritesDocumentEvaluator extends SharedResourceEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node docNode) {
        if (!docNode.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)) {
            return false;
        }
        if (BeanHelper.getDocumentDialogHelperBean().isInEditMode()) {
            return false;
        }
        return BeanHelper.getDocumentFavoritesService().isFavoriteAddable(docNode.getNodeRef());
    }

    @Override
    public boolean evaluate() {
        DocumentDynamicActionsGroupResources resource = (DocumentDynamicActionsGroupResources) sharedResource;
        return resource.isWorkspaceNode() && !resource.isInEditMode()
                && !resource.isFavourite() && !BeanHelper.getDocumentService().isDraft(resource.getObject().getNodeRef());
    }

}
