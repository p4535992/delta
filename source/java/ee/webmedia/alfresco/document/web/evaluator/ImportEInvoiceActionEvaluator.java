<<<<<<< HEAD
package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;

public class ImportEInvoiceActionEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        return DocumentSubtypeModel.Types.INVOICE.equals(node.getType());
    }
}
=======
package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;

public class ImportEInvoiceActionEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        return DocumentSubtypeModel.Types.INVOICE.equals(node.getType());
    }
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
