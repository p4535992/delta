package ee.webmedia.alfresco.imap.service;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.alfresco.repo.imap.AlfrescoImapUser;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.xml.security.transforms.TransformationException;

import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;

import ee.webmedia.alfresco.document.file.web.Subfolder;

/**
 * Extended imap service.
 */
public interface ImapServiceExt {

    String BEAN_NAME = "ImapServiceExt";
    String FOLDER_TYPE_PREFIX_FIXED = "fixed";
    String FOLDER_TYPE_PREFIX_USER_BASED = "userBased";

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

    void saveIncomingEInvoice(NodeRef folderNodeRef, MimeMessage mimeMessage) throws FolderException;

    boolean isFixedFolder(NodeRef folderRef);

    boolean isUserBasedFolder(NodeRef folderRef);

    Set<String> getFixedSubfolderNames(NodeRef parentFolderRef);

    NodeRef createImapSubfolder(NodeRef parentFolderNodeRef, String behaviour, String subfolderName, String folderAssocName);

    long saveEmailToSubfolder(NodeRef folderNodeRef, MimeMessage mimeMessage, String behaviour, boolean incomingEmail) throws FolderException;

    void saveAttachmentsToSubfolder(NodeRef document, MimeMessage originalMessage, boolean saveBody) throws IOException, MessagingException, TransformationException,
            FolderException;

    List<Subfolder> getImapSubfolders(NodeRef parentRef);

    List<Subfolder> getImapSubfoldersWithChildCount(NodeRef parentRef, QName countableChildNodeType);

    int getAllFilesCount(NodeRef attachmentRoot, boolean countFilesInSubfolders, int limit);

    void saveFailureNoticeToSubfolder(NodeRef folderNodeRef, MimeMessage mimeMessage, String behaviour) throws FolderException;

}
