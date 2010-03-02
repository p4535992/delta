package ee.webmedia.alfresco.imap.web;

import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;
import ee.webmedia.alfresco.utils.MessageUtil;

import javax.faces.event.ActionEvent;

/**
 * Dialog for incoming emails list.
 *
 * @author Romet Aidla
 */
public class IncomingEmailListDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 0L;

    public void setup(ActionEvent event) {
        documents = getDocumentService().getIncomingEmails();
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage("document_incoming_emails");
    }
}
