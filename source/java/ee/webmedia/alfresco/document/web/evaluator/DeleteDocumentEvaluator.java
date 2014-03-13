package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * UI action evaluator for validating whether user can delete current document.
 */
public class DeleteDocumentEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Node docNode) {
        boolean isInViewState = new ViewStateActionEvaluator().evaluate(docNode);
        return isInViewState && StringUtils.isBlank((String) docNode.getProperties().get(DocumentCommonModel.Props.REG_NUMBER))
                && docNode.hasPermission(DocumentCommonModel.Privileges.DELETE_DOCUMENT_META_DATA);
    }

}
