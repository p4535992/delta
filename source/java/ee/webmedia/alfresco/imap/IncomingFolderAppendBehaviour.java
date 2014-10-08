<<<<<<< HEAD
package ee.webmedia.alfresco.imap;

import java.util.Date;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.store.FolderException;

import ee.webmedia.alfresco.imap.service.ImapServiceExt;

/**
 * Saves mail to the folder without changing.
 * 
 * @author Romet Aidla
 */
public class IncomingFolderAppendBehaviour implements AppendBehaviour {
    public static final String BEHAVIOUR_NAME = "incomingBehaviour";
    private final ImapServiceExt imapService;

    public IncomingFolderAppendBehaviour(ImapServiceExt imapService) {
        this.imapService = imapService;
    }

    @Override
    public long appendMessage(ImmutableFolder folder, MimeMessage mimeMessage, Flags flags, Date date) throws FolderException {
        return imapService.saveEmailToSubfolder(folder.getFolderNodeRef(), mimeMessage, BEHAVIOUR_NAME, true);
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
 * Saves mail to the folder without changing.
 */
public class IncomingFolderAppendBehaviour implements AppendBehaviour {
    public static final String BEHAVIOUR_NAME = "incomingBehaviour";
    private final ImapServiceExt imapService;

    public IncomingFolderAppendBehaviour(ImapServiceExt imapService) {
        this.imapService = imapService;
    }

    @Override
    public long appendMessage(ImmutableFolder folder, MimeMessage mimeMessage, Flags flags, Date date) throws FolderException {
        return imapService.saveEmailToSubfolder(folder.getFolderNodeRef(), mimeMessage, BEHAVIOUR_NAME, true);
    }
}
>>>>>>> develop-5.1
