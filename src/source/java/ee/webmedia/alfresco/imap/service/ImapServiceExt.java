package ee.webmedia.alfresco.imap.service;

import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;
import ee.webmedia.alfresco.imap.ImmutableFolder;
import org.alfresco.repo.imap.AlfrescoImapUser;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.xml.security.transforms.TransformationException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Extended imap service.
 *
 * @author Romet Aidla
 */
public interface ImapServiceExt {

    String BEAN_NAME = "ImapServiceExt";

    /**
     * Gets IMAP folder.
     *
     * @param user Imap user
     * @param folderName Folder name.
     * @return folder
     */
    MailFolder getFolder(AlfrescoImapUser user, String folderName);

    /**
     * Saves mail to given folder.
     *
     * @param folderNodeRef Reference to folder
     * @param mimeMessage Mail message
     * @return reference to node created
     * @throws FolderException
     */
    long saveEmail(NodeRef folderNodeRef, MimeMessage mimeMessage) throws FolderException;

    /**
     * Lists folders used for IMAP.
     *
     * @param user Imap user
     * @param mailboxPattern Mailbox pattern
     * @return Collection of IMAP folders.
     */
    Collection<MailFolder> createAndListFolders(AlfrescoImapUser user, String mailboxPattern);

    /**
     * Saves attachments from mail message to given folder.
     *
     * @param folderNodeRef Reference to folder
     * @param originalMessage Mail message
     * @throws IOException
     * @throws MessagingException
     * @throws TransformationException
     */
    void saveAttachments(NodeRef folderNodeRef, MimeMessage originalMessage, boolean saveBody) throws IOException, MessagingException, TransformationException;

    /**
     * Get node reference to attachments folder
     * @return node reference
     */
    NodeRef getAttachmentRoot();
}
