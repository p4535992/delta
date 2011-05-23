package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.einvoice.model.InvoiceType;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;

public class SendEInvoiceToSapEvaluator extends SendEInvoiceToSapManuallyEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node docNode) {
        return super.evaluate(docNode) && !InvoiceType.ETT.getValue().equals(docNode.getProperties().get(DocumentSpecificModel.Props.INVOICE_TYPE));
    }

}
