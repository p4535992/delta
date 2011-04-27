package ee.webmedia.alfresco.imap.web;

import javax.faces.event.ActionEvent;

import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Dialog for incoming e-invoices list.
 * 
 * @author Riina Tens
 */
public class IncomingEInvoiceListDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 0L;

    /** @param event */
    public void setup(ActionEvent event) {
        restored();
    }

    @Override
    public void restored() {
        documents = getDocumentService().getIncomingEInvoices();
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage("document_received_invoices");
    }
}