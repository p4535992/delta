<<<<<<< HEAD
package ee.webmedia.alfresco.imap;

import java.util.Date;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.store.FolderException;

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
=======
package ee.webmedia.alfresco.imap;

import java.util.Date;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.store.FolderException;

/**
 * Behaviour denies appending message.
 */
public class PermissionDeniedAppendBehaviour implements AppendBehaviour {
    public static final String BEHAVIOUR_NAME = "permissionDeniedBehaviour";

    @Override
    public long appendMessage(ImmutableFolder folder, MimeMessage mimeMessage, Flags flags, Date date) throws FolderException {
        throw new FolderException(ImmutableFolder.PERMISSION_DENIED);
    }
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
