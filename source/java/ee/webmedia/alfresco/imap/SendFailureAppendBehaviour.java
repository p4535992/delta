<<<<<<< HEAD
package ee.webmedia.alfresco.imap;

import java.util.Date;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.store.FolderException;

import ee.webmedia.alfresco.imap.service.ImapServiceExt;

/**
 * Saves mail content and subject to content object.
 * 
 * @author Riina Tens
 */
public class SendFailureAppendBehaviour implements AppendBehaviour {
    public static final String BEHAVIOUR_NAME = "sendFailureBehaviour";
    private final ImapServiceExt imapService;

    public SendFailureAppendBehaviour(ImapServiceExt imapService) {
        this.imapService = imapService;
    }

    @Override
    public long appendMessage(ImmutableFolder folder, MimeMessage mimeMessage, Flags flags, Date date) throws FolderException {
        imapService.saveFailureNoticeToSubfolder(folder.getFolderNodeRef(), mimeMessage, BEHAVIOUR_NAME);
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
 * Saves mail content and subject to content object.
 */
public class SendFailureAppendBehaviour implements AppendBehaviour {
    public static final String BEHAVIOUR_NAME = "sendFailureBehaviour";
    private final ImapServiceExt imapService;

    public SendFailureAppendBehaviour(ImapServiceExt imapService) {
        this.imapService = imapService;
    }

    @Override
    public long appendMessage(ImmutableFolder folder, MimeMessage mimeMessage, Flags flags, Date date) throws FolderException {
        imapService.saveFailureNoticeToSubfolder(folder.getFolderNodeRef(), mimeMessage, BEHAVIOUR_NAME);
        return 0;
    }
}
>>>>>>> develop-5.1
