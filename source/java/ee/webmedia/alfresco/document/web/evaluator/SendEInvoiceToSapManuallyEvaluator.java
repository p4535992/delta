package ee.webmedia.alfresco.document.web.evaluator;

import java.util.List;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.xtee.client.dhl.DhlXTeeService.SendStatus;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.einvoice.model.Transaction;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceUtil;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.sendout.model.SendInfo;
import ee.webmedia.alfresco.parameters.model.Parameters;

public class SendEInvoiceToSapManuallyEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node docNode) {
        if (!docNode.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)) {
            return false;
        }
        return BeanHelper.getApplicationConstantsBean().isEinvoiceEnabled()
                && !BeanHelper.getDocumentDialogHelperBean().isInEditMode()
                && DocumentSubtypeModel.Types.INVOICE.equals(docNode.getType())
                && BeanHelper.getUserService().isInAccountantGroup()
                && (StringUtils.isNotBlank((String) docNode.getProperties().get(DocumentSpecificModel.Props.PURCHASE_ORDER_SAP_NUMBER)) || Boolean.TRUE.equals(docNode
                        .getProperties().get(DocumentSpecificModel.Props.XXL_INVOICE)) || mandatoryTransactionFieldsFilled(docNode))
                        && StringUtils.isNotBlank((String) docNode.getProperties().get(DocumentSpecificModel.Props.SELLER_PARTY_SAP_ACCOUNT))
                        && (!hasSendInfo(docNode) && StringUtils.isEmpty((String) docNode.getProperties().get(DocumentSpecificModel.Props.ENTRY_SAP_NUMBER)))
                        && !BeanHelper.getWorkflowService().hasUnfinishedReviewTasks(docNode.getNodeRef());

    }

    private boolean hasSendInfo(Node docNode) {
        List<SendInfo> sendInfos = BeanHelper.getSendOutService().getDocumentSendInfos(docNode.getNodeRef());
        String dvkCode = BeanHelper.getParametersService().getStringParameter(Parameters.SAP_DVK_CODE);
        if (StringUtils.isEmpty(dvkCode)) {
            return false;
        }
        for (SendInfo sendInfo : sendInfos) {
            if (!SendStatus.CANCELLED.toString().equalsIgnoreCase(sendInfo.getSendStatus())
                    && dvkCode.equalsIgnoreCase((String) sendInfo.getProperties().get(DocumentCommonModel.Props.SEND_INFO_RECIPIENT_REG_NR))) {
                return true;
            }
        }
        return false;
    }

    private boolean mandatoryTransactionFieldsFilled(Node docNode) {
        List<String> accountantMandatoryProps = BeanHelper.getEInvoiceService().getAccountantMandatoryFields();
        List<Transaction> transactions = BeanHelper.getEInvoiceService().getInvoiceTransactions(docNode.getNodeRef());
        for (Transaction transaction : transactions) {
            if (!EInvoiceUtil.checkTransactionMandatoryFields(accountantMandatoryProps, null, null, transaction)) {
                return false;
            }
        }
        return true;
    }
}
