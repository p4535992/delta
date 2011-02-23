package ee.webmedia.alfresco.imap;

import com.icegreen.greenmail.store.FolderException;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;
import java.util.Date;

/**
 * Behaviour denies appending message.
 *
 * @author Romet Aidla
 */
public class PermissionDeniedAppendBehaviour implements AppendBehaviour {
    public static final String BEHAVIOUR_NAME = "permissionDeniedBehaviour";

    @Override
    public long appendMessage(ImmutableFolder folder, MimeMessage mimeMessage, Flags flags, Date date) throws FolderException {
        throw new FolderException(ImmutableFolder.PERMISSION_DENIED);
    }
}
