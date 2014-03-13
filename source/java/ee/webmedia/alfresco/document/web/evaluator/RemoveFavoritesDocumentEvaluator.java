package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;

public class RemoveFavoritesDocumentEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node docNode) {
        if (!docNode.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)) {
            return false;
        }
        ViewStateActionEvaluator viewStateEval = new ViewStateActionEvaluator();
        if (!viewStateEval.evaluate(docNode)) {
            return false;
        }
        return BeanHelper.getDocumentFavoritesService().isFavorite(docNode.getNodeRef()) != null;
    }

}
