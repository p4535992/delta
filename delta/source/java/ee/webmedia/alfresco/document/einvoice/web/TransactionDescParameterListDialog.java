package ee.webmedia.alfresco.document.einvoice.web;

import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.einvoice.model.TransactionDescParameter;

public class TransactionDescParameterListDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    private List<TransactionDescParameter> transactionDescParameters;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        transactionDescParameters = BeanHelper.getEInvoiceService().getAllTransactionDescParameters();
    }

    /**
     * Used in JSP pages.
     */
    public List<TransactionDescParameter> getTransactionDescParameters() {
        return transactionDescParameters;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        BeanHelper.getEInvoiceService().updateTransactionDescParameters(transactionDescParameters);
        return null;
    }

    @Override
    public String cancel() {
        transactionDescParameters = null;
        return super.cancel();
    }

    @Override
    public Object getActionsContext() {
        return null;
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

}
