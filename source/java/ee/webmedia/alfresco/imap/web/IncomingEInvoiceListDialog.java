package ee.webmedia.alfresco.imap.web;

import javax.faces.event.ActionEvent;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.search.web.DocumentListDataProvider;
import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Dialog for incoming e-invoices list.
 */
public class IncomingEInvoiceListDialog extends BaseDocumentListDialog {

    public static final String BEAN_NAME = "IncomingEInvoiceListDialog";

    private static final long serialVersionUID = 0L;

    /** @param event */
    public void setup(ActionEvent event) {
        restored();
    }

    @Override
    public void restored() {
        UserService userService = BeanHelper.getUserService();
        if (userService.isAdministrator() || userService.isDocumentManager() || userService.isInAccountantGroup()) {
            documentProvider = new DocumentListDataProvider(getDocumentService().getIncomingEInvoices(), true);
        } else {
            throw new RuntimeException("E-invoice functionality is not supported!");
        }
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage("document_received_invoices");
    }
}