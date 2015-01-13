package ee.webmedia.alfresco.document.einvoice.web;

import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.einvoice.model.TransactionTemplate;
import ee.webmedia.alfresco.utils.ActionUtil;

public class TransactionTemplateListDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "TransactionTemplateListDialog";

    private List<TransactionTemplate> transactionTemplates;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        reload();
    }

    public void reload() {
        transactionTemplates = BeanHelper.getEInvoiceService().getAllTransactionTemplates();
    }

    /**
     * Used in JSP pages.
     */
    public List<TransactionTemplate> getTransactionTemplates() {
        return transactionTemplates;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return null;
    }

    @Override
    public String cancel() {
        transactionTemplates = null;
        return super.cancel();
    }

    @Override
    public Object getActionsContext() {
        return null;
    }

    /**
     * Used only in development/internal testing
     */
    public void deleteTransactionTemplate(ActionEvent event) {
        NodeRef transactionTemplateRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        BeanHelper.getEInvoiceService().deleteTransactionTemplate(transactionTemplateRef);
    }

}
