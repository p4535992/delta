package ee.webmedia.alfresco.imap;

import java.util.Date;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.store.FolderException;

/**
 * All implementations must be <b>thread-safe</b>.
 * 
 * @author Romet Aidla
 */
public interface AppendBehaviour {
    /**
     * Appends message to the folder.
     * 
     * @param folder Message is appended to this folder
     * @param mimeMessage Message
     * @param flags Mail flags
     * @param date Message date
     * @return
     * @throws FolderException
     */
    public long appendMessage(ImmutableFolder folder, MimeMessage mimeMessage, Flags flags, Date date) throws FolderException;
}
