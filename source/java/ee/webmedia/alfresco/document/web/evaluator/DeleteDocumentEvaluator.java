package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * UI action evaluator for validating whether user can delete current document.
 * 
 * @author Romet Aidla
 * @author Ats Uiboupin - dropped most of the code in favor to permissions
 */
public class DeleteDocumentEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Node docNode) {
        if (!docNode.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)) {
            return false;
        }
        if (!new ViewStateActionEvaluator().evaluate(docNode)) {
            return false;
        }
        return new IsAdminOrDocManagerEvaluator().evaluate(docNode)
                || (StringUtils.isBlank((String) docNode.getProperties().get(DocumentCommonModel.Props.REG_NUMBER)) && new IsOwnerEvaluator().evaluate(docNode));
    }
}
