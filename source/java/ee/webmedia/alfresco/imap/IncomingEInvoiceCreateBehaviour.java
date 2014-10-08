<<<<<<< HEAD
package ee.webmedia.alfresco.imap;

import java.util.Date;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.store.FolderException;

import ee.webmedia.alfresco.imap.service.ImapServiceExt;

/**
 * Create e-invoice from incoming e-mail
 * 
 * @author Riina Tens
 */
public class IncomingEInvoiceCreateBehaviour implements AppendBehaviour {
    public static final String BEHAVIOUR_NAME = "incomingInvoiceBehaviour";
    private final ImapServiceExt imapService;

    public IncomingEInvoiceCreateBehaviour(ImapServiceExt imapService) {
        this.imapService = imapService;
    }

    @Override
    public long appendMessage(ImmutableFolder folder, MimeMessage mimeMessage, Flags flags, Date date) throws FolderException {
        imapService.saveIncomingEInvoice(folder.getFolderNodeRef(), mimeMessage);
        return 0;
    }
}
=======
package ee.webmedia.alfresco.imap;

import java.util.Date;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.store.FolderException;

import ee.webmedia.alfresco.imap.service.ImapServiceExt;

/**
 * Create e-invoice from incoming e-mail
 */
public class IncomingEInvoiceCreateBehaviour implements AppendBehaviour {
    public static final String BEHAVIOUR_NAME = "incomingInvoiceBehaviour";
    private final ImapServiceExt imapService;

    public IncomingEInvoiceCreateBehaviour(ImapServiceExt imapService) {
        this.imapService = imapService;
    }

    @Override
    public long appendMessage(ImmutableFolder folder, MimeMessage mimeMessage, Flags flags, Date date) throws FolderException {
        imapService.saveIncomingEInvoice(folder.getFolderNodeRef(), mimeMessage);
        return 0;
    }
}
>>>>>>> develop-5.1
