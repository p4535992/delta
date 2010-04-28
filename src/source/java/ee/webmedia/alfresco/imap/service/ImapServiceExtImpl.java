package ee.webmedia.alfresco.imap.service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.imap.AlfrescoImapConst;
import org.alfresco.repo.imap.AlfrescoImapFolder;
import org.alfresco.repo.imap.AlfrescoImapUser;
import org.alfresco.repo.imap.ImapService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.transforms.TransformationException;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.classificator.enums.StorageType;
import ee.webmedia.alfresco.classificator.enums.TransmittalMode;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.imap.AppendBehaviour;
import ee.webmedia.alfresco.imap.AttachmentsFolderAppendBehaviour;
import ee.webmedia.alfresco.imap.ImmutableFolder;
import ee.webmedia.alfresco.imap.IncomingFolderAppendBehaviour;
import ee.webmedia.alfresco.imap.PermissionDeniedAppendBehaviour;
import ee.webmedia.alfresco.imap.model.ImapModel;

/**
 * SimDhs specific IMAP logic.
 *
 * @author Romet Aidla
 */
public class ImapServiceExtImpl implements ImapServiceExt {
    private static final Log log = LogFactory.getLog(ImapServiceExtImpl.class);

    private ImapService imapService;
    private FileFolderService fileFolderService;
    private DocumentLogService documentLogService;
    private NodeService nodeService;
    private ContentService contentService;
    private GeneralService generalService;
    private FileService fileService;
    private MimetypeService mimetypeService;

    // todo: make this configurable with spring
    private Set<String> allowedFolders = null;

    @Override
    public MailFolder getFolder(AlfrescoImapUser user, String folderName) {
        return addBehaviour(imapService.getFolder(user, folderName));
    }

    public long saveEmail(NodeRef folderNodeRef, MimeMessage mimeMessage) throws FolderException { // todo: ex handling
        try {
            String name = AlfrescoImapConst.MESSAGE_PREFIX + GUID.generate();
            FileInfo docInfo = fileFolderService.create(folderNodeRef, name, DocumentSubtypeModel.Types.INCOMING_LETTER);

            Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
            String subject = mimeMessage.getSubject();
            if (StringUtils.isBlank(subject)) {
                subject = I18NUtil.getMessage("imap.letter_subject_missing");
            }
            properties.put(DocumentCommonModel.Props.DOC_NAME, subject);
            if (mimeMessage.getFrom() != null) {
                InternetAddress sender = (InternetAddress) mimeMessage.getFrom()[0];
                properties.put(DocumentSpecificModel.Props.SENDER_DETAILS_NAME, sender.getPersonal());
                properties.put(DocumentSpecificModel.Props.SENDER_DETAILS_EMAIL, sender.getAddress());            }            properties.put(DocumentSpecificModel.Props.TRANSMITTAL_MODE, TransmittalMode.EMAIL.getValueName());
            properties.put(DocumentCommonModel.Props.DOC_STATUS, DocumentStatus.WORKING.getValueName());
            properties.put(DocumentCommonModel.Props.STORAGE_TYPE, StorageType.DIGITAL.getValueName());

            final NodeRef docRef = docInfo.getNodeRef();
            nodeService.addProperties(docRef, properties);
            saveAttachments(docRef, mimeMessage, true);

            documentLogService.addDocumentLog(docRef, I18NUtil.getMessage("document_log_status_imported", "DVK") //
                    , I18NUtil.getMessage("document_log_creator_imap"));

            return (Long) nodeService.getProperty(docRef, ContentModel.PROP_NODE_DBID);
        } catch (Exception e) { //todo: improve exception handling
            log.warn("Cannot save email, folderNodeRef=" + folderNodeRef, e);
            throw new FolderException("Cannot save email: " + e.getMessage());
        }
    }

    @Override
    public Collection<MailFolder> createAndListFolders(AlfrescoImapUser user, String mailboxPattern) {
        try {
            return addBehaviour(filter(imapService.listSubscribedMailboxes(user, mailboxPattern)));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void saveAttachments(NodeRef document, MimeMessage originalMessage, boolean saveBody)
            throws IOException, MessagingException, TransformationException {
        if (saveBody) {
            Part p = getText(originalMessage);
            createBody(document, originalMessage);
        }

        Object content = originalMessage.getContent();
        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;

            for (int i = 0, n = multipart.getCount(); i < n; i++) {
                Part part = multipart.getBodyPart(i);
                if ("attachment".equalsIgnoreCase(part.getDisposition())) {
                    createAttachment(document, part);
                }
            }
        }
    }

    public NodeRef getAttachmentRoot() {
        NodeRef attachmentSpaceRef = generalService.getNodeRef(ImapModel.Repo.ATTACHMENT_SPACE);
        Assert.notNull(attachmentSpaceRef, "Attachment node reference not found");
        return attachmentSpaceRef;
    }

    private void createAttachment(NodeRef document, Part part) throws MessagingException, IOException {
        ContentType contentType = new ContentType(part.getContentType());
        String filename = part.getFileName();
        String mimeType = contentType.getBaseType();
        if (filename == null) {
            filename = I18NUtil.getMessage("imap.letter_attachment_filename") + "." + mimetypeService.getExtension(mimeType);
        }
        FileInfo createdFile = fileFolderService.create(
                document,
                generalService.getUniqueFileName(document, filename),
                ContentModel.TYPE_CONTENT);
        ContentWriter writer = fileFolderService.getWriter(createdFile.getNodeRef());
        writer.setMimetype(mimeType);
        OutputStream os = writer.getContentOutputStream();
        FileCopyUtils.copy(part.getInputStream(), os);
    }

    private void createBody(NodeRef document, MimeMessage originalMessage) throws MessagingException, IOException {
        Part p = getText(originalMessage);
        if (p == null) {
            log.debug("No body part found from message, skipping body PDF creation");
            return;
        }

        String mimeType;
        if (p.isMimeType(MimetypeMap.MIMETYPE_HTML)) {
            mimeType = MimetypeMap.MIMETYPE_HTML;
        } else if (p.isMimeType(MimetypeMap.MIMETYPE_TEXT_PLAIN)) {
            mimeType = MimetypeMap.MIMETYPE_TEXT_PLAIN;
        } else {
            log.debug("Found body part from message, but don't know how to handle it, skipping body PDF creation, contentType=" + p.getContentType());
            return;
        }
        String encoding = getEncoding(p);
        log.debug("Found body part from message, parsed mimeType=" + mimeType + " and encoding=" + encoding + " from contentType=" + p.getContentType());

        ContentWriter tempWriter = contentService.getTempWriter();
        tempWriter.setMimetype(mimeType);
        tempWriter.setEncoding(encoding);
        tempWriter.putContent(p.getInputStream());

        // THIS DOES NOT WORK:
        // String content = (String) p.getContent(); <-- JavaMail does not care for encoding when parsing content to string this way!
        // tempWriter.putContent(content);

        // p.writeTo(tempWriter.getContentOutputStream()); <-- THIS DOES NOT WORK

        ContentReader reader = tempWriter.getReader();

        fileService.transformToPdf(document, reader, I18NUtil.getMessage("imap.letter_body_filename"));
    }

    // Workaround for getting encoding from content type
    // contentType=text/plain; charset="iso-8859-1"
    private static String getEncoding(Part p) throws MessagingException {
        String encoding = "UTF-8"; // default encoding is UTF-8
        String regExp = "(.*charset=\"([^\"]+)\".*)";

        Matcher matcher = Pattern.compile(regExp).matcher(p.getContentType().replace('\n', ' ').replace('\r', ' '));
        if (matcher.matches()) {
            if (matcher.groupCount() == 2) {
                encoding = matcher.group(2);
            }
        }

        return encoding;
    }

    /**
     * Return the primary text content of the message.
     */
    // taken from http://java.sun.com/products/javamail/FAQ.html
    private Part getText(Part p) throws MessagingException, IOException {
        if (p.isMimeType("text/*")) {
            return p;
        }

        if (p.isMimeType("multipart/alternative")) {
            // prefer html text over plain text
            Multipart mp = (Multipart) p.getContent();
            Part text = null;
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) {
                    if (text == null)
                        text = getText(bp);
                    continue;
                } else if (bp.isMimeType("text/html")) {
                    Part s = getText(bp);
                    if (s != null)
                        return s;
                } else {
                    return getText(bp);
                }
            }
            return text;
        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                Part s = getText(mp.getBodyPart(i));
                if (s != null)
                    return s;
            }
        }
        return null;
    }

    private MailFolder addBehaviour(AlfrescoImapFolder folder) {

        String appendBehaviour;
        if (folder.getFolderInfo() != null) { // todo: why folder info is null?
            appendBehaviour = (String) folder.getFolderInfo().getProperties().get(ImapModel.Properties.APPEND_BEHAVIOUR);
        } else {
            appendBehaviour = PermissionDeniedAppendBehaviour.BEHAVIOUR_NAME;
        }
        return new ImmutableFolder(folder, getBehaviour(appendBehaviour));
    }

    public AppendBehaviour getBehaviour(String behaviour) {
        if (PermissionDeniedAppendBehaviour.BEHAVIOUR_NAME.equals(behaviour)) {
            return new PermissionDeniedAppendBehaviour();
        } else if (IncomingFolderAppendBehaviour.BEHAVIOUR_NAME.equals(behaviour)) {
            return new IncomingFolderAppendBehaviour(this);
        } else if (AttachmentsFolderAppendBehaviour.BEHAVIOUR_NAME.equals(behaviour)) {
            return new AttachmentsFolderAppendBehaviour(this);
        } else {
            throw new RuntimeException("Unknown behaviour: " + behaviour);
        }
    }

    private Collection<AlfrescoImapFolder> filter(Collection<AlfrescoImapFolder> folders) {
        CollectionUtils.filter(folders, new Predicate() {
            @Override
            public boolean evaluate(Object o) {
                MailFolder folder = (MailFolder) o;
                return getAllowedFolders().contains(folder.getName());
            }
        });
        return folders;
    }

    private Collection<MailFolder> addBehaviour(final Collection<AlfrescoImapFolder> folders) {
        Collection<MailFolder> immutableFolders = new ArrayList<MailFolder>();
        for (AlfrescoImapFolder folder : folders) {
            immutableFolders.add(addBehaviour(folder));
        }
        return immutableFolders;
    }

    public Set<String> getAllowedFolders() {
        if (allowedFolders == null) {
            allowedFolders = new HashSet<String>();
            allowedFolders.add(I18NUtil.getMessage("imap.folder_letters"));
            allowedFolders.add(I18NUtil.getMessage("imap.folder_attachments"));
        }
        return allowedFolders;
    }

    public void setImapService(ImapService imapService) {
        this.imapService = imapService;
    }

    public void setDocumentLogService(DocumentLogService documentLogService) {
        this.documentLogService = documentLogService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setContentService(ContentService contentService) {
        this.contentService = contentService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public void setMimetypeService(MimetypeService mimetypeService) {
        this.mimetypeService = mimetypeService;
    }

}
