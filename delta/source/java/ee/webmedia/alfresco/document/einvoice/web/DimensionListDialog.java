package ee.webmedia.alfresco.document.einvoice.web;

import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.einvoice.model.Dimension;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceService;

public class DimensionListDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    private transient EInvoiceService einvoiceService;
    private List<Dimension> dimensions;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        dimensions = BeanHelper.getEInvoiceService().getAllDimensions();
    }

    /**
     * Used in JSP pages.
     */
    public List<Dimension> getDimensions() {
        return dimensions;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return null;
    }

    @Override
    public String cancel() {
        dimensions = null;
        return super.cancel();
    }

    @Override
    public Object getActionsContext() {
        return null;
    }

}
