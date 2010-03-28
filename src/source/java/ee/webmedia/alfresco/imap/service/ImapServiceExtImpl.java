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

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.repo.imap.AlfrescoImapConst;
import org.alfresco.repo.imap.AlfrescoImapFolder;
import org.alfresco.repo.imap.AlfrescoImapUser;
import org.alfresco.repo.imap.ImapService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
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

    // todo: make this configurable with spring
    private static Set<String> allowedFolders = new HashSet<String>();

    static {
        allowedFolders.add("Sissetulevad kirjad");
        allowedFolders.add("E-kirja manused");
    }


    private static final String BODY_FILE_NAME = "E-kiri.pdf";

    @Override
    public MailFolder getFolder(AlfrescoImapUser user, String folderName) {
        return addBehaviour(imapService.getFolder(user, folderName));
    }

    public long saveEmail(NodeRef folderNodeRef, MimeMessage mimeMessage) throws FolderException { // todo: ex handling
        try {
            String name = AlfrescoImapConst.MESSAGE_PREFIX + GUID.generate();
            FileInfo docInfo = fileFolderService.create(folderNodeRef, name, DocumentSubtypeModel.Types.INCOMING_LETTER);

            Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
            properties.put(DocumentCommonModel.Props.DOC_NAME, mimeMessage.getSubject());
            InternetAddress sender = (InternetAddress) mimeMessage.getFrom()[0]; // todo: unsafe cast?
            properties.put(DocumentSpecificModel.Props.SENDER_DETAILS_NAME, sender.getPersonal());
            properties.put(DocumentSpecificModel.Props.SENDER_DETAILS_EMAIL, sender.getAddress());
            properties.put(DocumentSpecificModel.Props.TRANSMITTAL_MODE, TransmittalMode.EMAIL.getValueName());
            properties.put(DocumentCommonModel.Props.DOC_STATUS, DocumentStatus.WORKING.getValueName());
            properties.put(DocumentCommonModel.Props.STORAGE_TYPE, StorageType.DIGITAL.getValueName());

            final NodeRef docRef = docInfo.getNodeRef();
            nodeService.addProperties(docRef, properties);
            saveAttachments(docRef, mimeMessage, true);

            documentLogService.addDocumentLog(docRef, I18NUtil.getMessage("document_log_status_imported", "DVK") //
                    , I18NUtil.getMessage("document_log_creator_imap"));

            return (Long) nodeService.getProperty(docRef, ContentModel.PROP_NODE_DBID);
        } catch (Exception e) { //todo: improve exception handling
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

    public void saveAttachments(NodeRef folderNodeRef, MimeMessage originalMessage, boolean saveBody)
            throws IOException, MessagingException, TransformationException {
        if (saveBody) {
            Part p = getText(originalMessage);
            createBody(folderNodeRef, originalMessage);
        }

        Object content = originalMessage.getContent();
        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;

            for (int i = 0, n = multipart.getCount(); i < n; i++) {
                Part part = multipart.getBodyPart(i);
                if ("attachment".equalsIgnoreCase(part.getDisposition())) {
                    createAttachment(folderNodeRef, part);
                }
            }
        }
    }

    public NodeRef getAttachmentRoot() {
        NodeRef attachmentSpaceRef = generalService.getNodeRef(ImapModel.Repo.ATTACHMENT_SPACE);
        Assert.notNull(attachmentSpaceRef, "Attachment node reference not found");
        return attachmentSpaceRef;
    }

    private void createAttachment(NodeRef folderNodeRef, Part part) throws MessagingException, IOException {
        ContentType contentType = new ContentType(part.getContentType());
        FileInfo createdFile = fileFolderService.create(
                folderNodeRef,
                generalService.getUniqueFileName(folderNodeRef, part.getFileName()),
                ContentModel.TYPE_CONTENT);
        ContentWriter writer = fileFolderService.getWriter(createdFile.getNodeRef());
        writer.setMimetype(contentType.getBaseType());
        OutputStream os = writer.getContentOutputStream();
        FileCopyUtils.copy(part.getInputStream(), os);
    }

    //todo: should be refactored to File Converter Service

    private void createBody(NodeRef folderNodeRef, MimeMessage originalMessage) throws MessagingException, IOException, TransformationException {
        Part p = getText(originalMessage);
        if (p == null) {
            log.debug("No text part found from message, skipping body PDF creation");
            return;
        }
        log.debug("Found text part from message, contentType=" + p.getContentType());
        ContentWriter tempWriter = contentService.getTempWriter();
        tempWriter.setMimetype(p.isMimeType("text/html") ? MimetypeMap.MIMETYPE_HTML : MimetypeMap.MIMETYPE_TEXT_PLAIN);

        String content = (String) p.getContent();
/*
        // Workaround for getting encoding from contenttype
        // contentType=text/plain; charset="iso-8859-1"
        if (!p.isMimeType("text/html")) {
            Matcher matcher = Pattern.compile(".*charset=\"([^\"]+)\".*").matcher(p.getContentType().replace('\n', ' ').replace('\r', ' '));
            if (matcher.matches()) {
                if (matcher.groupCount() == 1) {
                    log.debug("Found encoding '" + matcher.group(1) + "'");
                    try {
                        content = new String(content.getBytes(), matcher.group(1));
                    } catch (UnsupportedEncodingException e) {
                        log.debug("Encoding '" + matcher.group(1) + "' not supported");
                    }
                }
            }
        }
*/
        // For HTML e-mails JavaMail apparently converts windows-1257 content to UTF-8 string correctly
        // But for plaintext e-mails windows-1257 and iso-8859-1 content is not converted to string correctly

        // OpenOffice HTML -> PDF converter does not read given encoding, but apparently expects ISO-8859-1
        tempWriter.setEncoding(p.isMimeType("text/html") ? "ISO-8859-1" : "UTF-8");
//        p.writeTo(tempWriter.getContentOutputStream()); <-- THIS DOES NOT WORK
        tempWriter.putContent(content);
        ContentReader reader = tempWriter.getReader();

        FileInfo createdFile = fileFolderService.create(
                folderNodeRef,
                BODY_FILE_NAME,
                ContentModel.TYPE_CONTENT);
        ContentWriter writer = fileFolderService.getWriter(createdFile.getNodeRef());
        writer.setMimetype(MimetypeMap.MIMETYPE_PDF);
        ContentTransformer transformer = contentService.getTransformer(reader.getMimetype(), writer.getMimetype());
        if (transformer == null) {
            throw new TransformationException("cannot_create_pdf_from_file");
        }
        try {
            transformer.transform(reader, writer);
        } catch (ContentIOException e) {
            log.debug("Transformation failed", e);
            throw e;
        }
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

    private static Collection<AlfrescoImapFolder> filter(Collection<AlfrescoImapFolder> folders) {
        CollectionUtils.filter(folders, new Predicate() {
            @Override
            public boolean evaluate(Object o) {
                MailFolder folder = (MailFolder) o;
                return allowedFolders.contains(folder.getName());
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
}
