package ee.webmedia.alfresco.imap;

import com.icegreen.greenmail.store.FolderException;
import ee.webmedia.alfresco.imap.service.ImapServiceExt;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;
import java.util.Date;

/**
 * Saves mail to the folder without changing.
 *
 * @author Romet Aidla
 */
public class IncomingFolderAppendBehaviour implements AppendBehaviour {
    public static final String BEHAVIOUR_NAME = "incomingBehaviour";
    private ImapServiceExt imapService;

    public IncomingFolderAppendBehaviour(ImapServiceExt imapService) {
        this.imapService = imapService;
    }

    @Override
    public long appendMessage(ImmutableFolder folder, MimeMessage mimeMessage, Flags flags, Date date) throws FolderException {
        return imapService.saveEmail(folder.getFolderNodeRef(), mimeMessage);
    }
}
