package ee.webmedia.alfresco.imap.web;

import javax.faces.event.ActionEvent;

import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;
import ee.webmedia.alfresco.utils.MessageUtil;

public class SentEmailListDialog extends BaseDocumentListDialog {

    private static final long serialVersionUID = 1L;

    /** @param event */
    public void setup(ActionEvent event) {
        restored();
    }

    @Override
    public void restored() {
        documents = getDocumentService().getSentEmails();
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage("document_sent_emails");
    }
}
