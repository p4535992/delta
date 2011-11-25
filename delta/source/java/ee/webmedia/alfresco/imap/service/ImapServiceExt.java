package ee.webmedia.alfresco.imap.service;

import java.io.IOException;
import java.util.Collection;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.alfresco.repo.imap.AlfrescoImapUser;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.xml.security.transforms.TransformationException;

import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;

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
     * @param incomingEmail true if incoming mail (otherwise it is assumed to be outgoing mail)
     * @return reference to node created
     * @throws FolderException
     */
    long saveEmail(NodeRef folderNodeRef, MimeMessage mimeMessage, boolean incomingEmail) throws FolderException;

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
     * 
     * @return node reference
     */
    NodeRef getAttachmentRoot();

    void saveIncomingEInvoice(NodeRef folderNodeRef, MimeMessage mimeMessage) throws FolderException;

}
