package ee.webmedia.alfresco.imap;

import com.icegreen.greenmail.store.FolderException;
import ee.webmedia.alfresco.imap.service.ImapServiceExt;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.ServiceRegistry;
import org.apache.xml.security.transforms.TransformationException;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Date;

/**
 * Saves attachments to the folder.
 *
 * @author Romet Aidla
 */
public class AttachmentsFolderAppendBehaviour implements AppendBehaviour {
    public static final String BEHAVIOUR_NAME = "attachmentBehaviour";
    private ImapServiceExt imapServiceExt;

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
