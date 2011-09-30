package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.docadmin.service.DocumentType;

/**
 * @author Kaarel Jõgeva
 */
public class ChangeByNewDocumentEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        return new ViewStateActionEvaluator().evaluate(node) && new IsAdminOrDocManagerEvaluator().evaluate(node) && isChangeByNewDocumentEnabled(node);
    }

    private boolean isChangeByNewDocumentEnabled(Node node) {
        DocumentType documentType = BeanHelper.getDocumentAdminService().getDocumentType(
                (String) node.getProperties().get(Props.OBJECT_TYPE_ID));

        return documentType != null && documentType.isChangeByNewDocumentEnabled();
    }

    @Override
    public boolean evaluate(Object obj) {
        throw new RuntimeException("method evaluate(obj) is unimplemented");
    }
}
