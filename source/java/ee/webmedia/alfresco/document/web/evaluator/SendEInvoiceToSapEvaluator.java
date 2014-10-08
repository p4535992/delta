package ee.webmedia.alfresco.document.web.evaluator;

import java.util.Map;

import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.einvoice.model.InvoiceType;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

public class SendEInvoiceToSapEvaluator extends SendEInvoiceToSapManuallyEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node docNode) {
        Map<String, Object> docProps = docNode.getProperties();
        return super.evaluate(docNode)
                && !InvoiceType.ETT.getValue().equals(docProps.get(DocumentSpecificModel.Props.INVOICE_TYPE))
                && (StringUtils.isNotBlank((String) docProps.get(DocumentSpecificModel.Props.PURCHASE_ORDER_SAP_NUMBER))
                        || Boolean.TRUE.equals(docProps.get(DocumentSpecificModel.Props.XXL_INVOICE)) || BeanHelper.getWorkflowService().hasTaskOfType(docNode.getNodeRef(),
                        WorkflowSpecificModel.Types.REVIEW_WORKFLOW, WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW));
    }

}
