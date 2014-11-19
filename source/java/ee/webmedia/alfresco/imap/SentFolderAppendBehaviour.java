<<<<<<< HEAD
package ee.webmedia.alfresco.imap;

import java.util.Date;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.store.FolderException;

import ee.webmedia.alfresco.imap.service.ImapServiceExt;

public class SentFolderAppendBehaviour implements AppendBehaviour {
    public static final String BEHAVIOUR_NAME = "sentBehaviour";
    private final ImapServiceExt imapService;

    public SentFolderAppendBehaviour(ImapServiceExt imapService) {
        this.imapService = imapService;
    }

    @Override
    public long appendMessage(ImmutableFolder folder, MimeMessage mimeMessage, Flags flags, Date date) throws FolderException {
        return imapService.saveEmailToSubfolder(folder.getFolderNodeRef(), mimeMessage, BEHAVIOUR_NAME, false);
    }
}
=======
package ee.webmedia.alfresco.imap;

import java.util.Date;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.store.FolderException;

import ee.webmedia.alfresco.imap.service.ImapServiceExt;

public class SentFolderAppendBehaviour implements AppendBehaviour {
    public static final String BEHAVIOUR_NAME = "sentBehaviour";
    private final ImapServiceExt imapService;

    public SentFolderAppendBehaviour(ImapServiceExt imapService) {
        this.imapService = imapService;
    }

    @Override
    public long appendMessage(ImmutableFolder folder, MimeMessage mimeMessage, Flags flags, Date date) throws FolderException {
        return imapService.saveEmailToSubfolder(folder.getFolderNodeRef(), mimeMessage, BEHAVIOUR_NAME, false);
    }
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
