package ee.webmedia.alfresco.imap;

import java.util.Date;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.store.FolderException;

import ee.webmedia.alfresco.imap.service.ImapServiceExt;

/**
 * Saves attachments to the folder.
 * 
 * @author Romet Aidla
 */
public class AttachmentsFolderAppendBehaviour implements AppendBehaviour {
    public static final String BEHAVIOUR_NAME = "attachmentBehaviour";
    private final ImapServiceExt imapServiceExt;

    public AttachmentsFolderAppendBehaviour(ImapServiceExt imapServiceExt) {
        this.imapServiceExt = imapServiceExt;
    }

    @Override
    public long appendMessage(ImmutableFolder folder, MimeMessage mimeMessage, Flags flags, Date date) throws FolderException {
        try {
            imapServiceExt.saveAttachments(folder.getFolderNodeRef(), mimeMessage, false);
            return 0;
        } catch (Exception e) {
            throw new FolderException(e.getMessage());
        }
    }
}
