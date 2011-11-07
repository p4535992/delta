package ee.webmedia.alfresco.imap.service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.ParseException;

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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import com.icegreen.greenmail.imap.commands.AppendCommand;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.util.GreenMailUtil;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.classificator.enums.StorageType;
import ee.webmedia.alfresco.classificator.enums.TransmittalMode;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceService;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.imap.AppendBehaviour;
import ee.webmedia.alfresco.imap.AttachmentsFolderAppendBehaviour;
import ee.webmedia.alfresco.imap.ImmutableFolder;
import ee.webmedia.alfresco.imap.IncomingEInvoiceCreateBehaviour;
import ee.webmedia.alfresco.imap.IncomingFolderAppendBehaviour;
import ee.webmedia.alfresco.imap.PermissionDeniedAppendBehaviour;
import ee.webmedia.alfresco.imap.SentFolderAppendBehaviour;
import ee.webmedia.alfresco.imap.model.ImapModel;

/**
 * SimDhs specific IMAP logic.
 * 
 * @author Romet Aidla
 */
public class ImapServiceExtImpl implements ImapServiceExt, InitializingBean {
    private static final Log log = LogFactory.getLog(ImapServiceExtImpl.class);

    private ImapService imapService;
    private FileFolderService fileFolderService;
    private DocumentLogService documentLogService;
    private NodeService nodeService;
    private ContentService contentService;
    private GeneralService generalService;
    private FileService fileService;
    private MimetypeService mimetypeService;
    private DocumentDynamicService documentDynamicService;
    private EInvoiceService einvoiceService;

    private String messageCopyFolder;

    // todo: make this configurable with spring
    private Set<String> allowedFolders = null;

    @Override
    public void afterPropertiesSet() throws Exception {
        AppendCommand.setMessageCopyFolder(messageCopyFolder);
        GreenMailUtil.setMessageCopyFolder(messageCopyFolder);
    }

    @Override
    public MailFolder getFolder(AlfrescoImapUser user, String folderName) {
        return addBehaviour(imapService.getFolder(user, folderName));
    }

    @Override
    public long saveEmail(NodeRef folderNodeRef, MimeMessage mimeMessage, boolean incomingEmail) throws FolderException { // todo: ex handling
        try {
            String docTypeId;
            if (incomingEmail) {
                docTypeId = SystematicDocumentType.INCOMING_LETTER.getId();
            } else {
                docTypeId = SystematicDocumentType.OUTGOING_LETTER.getId();
            }
            NodeRef docRef = documentDynamicService.createNewDocument(docTypeId, folderNodeRef).getNodeRef();

            Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
            String subject = mimeMessage.getSubject();
            if (StringUtils.isBlank(subject)) {
                subject = I18NUtil.getMessage("imap.letter_subject_missing");
            }
            properties.put(DocumentCommonModel.Props.DOC_NAME, subject);
            if (incomingEmail) {
                if (mimeMessage.getFrom() != null && mimeMessage.getFrom().length > 0) {
                    InternetAddress sender = (InternetAddress) mimeMessage.getFrom()[0];
                    properties.put(DocumentSpecificModel.Props.SENDER_DETAILS_NAME, sender.getPersonal());
                    properties.put(DocumentSpecificModel.Props.SENDER_DETAILS_EMAIL, sender.getAddress());
                }
                properties.put(DocumentSpecificModel.Props.TRANSMITTAL_MODE, TransmittalMode.EMAIL.getValueName());
            } else {
                if (mimeMessage.getAllRecipients() != null) {
                    Address[] allRecipients = mimeMessage.getAllRecipients();
                    List<String> names = new ArrayList<String>(allRecipients.length);
                    List<String> emails = new ArrayList<String>(allRecipients.length);
                    for (Address recient : allRecipients) {
                        names.add(((InternetAddress) recient).getPersonal());
                        emails.add(((InternetAddress) recient).getAddress());
                    }
                    properties.put(DocumentCommonModel.Props.RECIPIENT_NAME, (Serializable) names);
                    properties.put(DocumentCommonModel.Props.RECIPIENT_EMAIL, (Serializable) emails);
                }
            }
            properties.put(DocumentCommonModel.Props.STORAGE_TYPE, StorageType.DIGITAL.getValueName());
            nodeService.addProperties(docRef, properties);

            documentLogService.addDocumentLog(docRef, I18NUtil.getMessage("document_log_status_imported", I18NUtil.getMessage("document_log_creator_imap")) //
                    , I18NUtil.getMessage("document_log_creator_imap"));

            saveAttachments(docRef, mimeMessage, true);

            return (Long) nodeService.getProperty(docRef, ContentModel.PROP_NODE_DBID);
        } catch (Exception e) { // todo: improve exception handling
            log.warn("Cannot save email, folderNodeRef=" + folderNodeRef, e);
            throw new FolderException("Cannot save email: " + e.getMessage());
        }
    }

    @Override
    public void saveIncomingEInvoice(NodeRef folderNodeRef, MimeMessage mimeMessage) throws FolderException {
        try {
            Object content = mimeMessage.getContent();
            List<NodeRef> newInvoices = new ArrayList<NodeRef>();
            Map<NodeRef, Integer> invoiceRefToAttachment = new HashMap<NodeRef, Integer>();
            if (content instanceof Multipart) {
                Multipart multipart = (Multipart) content;

                for (int i = 0, n = multipart.getCount(); i < n; i++) {
                    Part part = multipart.getBodyPart(i);
                    if ("attachment".equalsIgnoreCase(part.getDisposition())) {
                        List<NodeRef> newDocRefs = createInvoice(folderNodeRef, part);
                        newInvoices.addAll(newDocRefs);
                        for (NodeRef newDocRef : newDocRefs) {
                            invoiceRefToAttachment.put(newDocRef, i);
                        }
                    }

                }
            }
            if (newInvoices.size() == 0) {
                String name = AlfrescoImapConst.MESSAGE_PREFIX + GUID.generate();
                FileInfo docInfo = fileFolderService.create(folderNodeRef, name, DocumentSubtypeModel.Types.INVOICE);

                Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
                String subject = mimeMessage.getSubject();
                if (StringUtils.isBlank(subject)) {
                    subject = I18NUtil.getMessage("imap.letter_subject_missing");
                }
                properties.put(DocumentCommonModel.Props.DOC_NAME, subject);
                if (mimeMessage.getFrom() != null) {
                    InternetAddress sender = (InternetAddress) mimeMessage.getFrom()[0];
                    properties.put(DocumentSpecificModel.Props.SELLER_PARTY_CONTACT_NAME, sender.getPersonal());
                    properties.put(DocumentSpecificModel.Props.SELLER_PARTY_CONTACT_EMAIL_ADDRESS, sender.getAddress());
                }
                properties.put(DocumentSpecificModel.Props.TRANSMITTAL_MODE, TransmittalMode.EMAIL.getValueName());

                properties.put(DocumentCommonModel.Props.DOC_STATUS, DocumentStatus.WORKING.getValueName());
                properties.put(DocumentCommonModel.Props.STORAGE_TYPE, StorageType.XML.getValueName());
                NodeRef nodeRef = docInfo.getNodeRef();
                nodeService.addProperties(nodeRef, properties);
                newInvoices.add(nodeRef);
            }

            for (NodeRef docRef : newInvoices) {
                // TODO: optimize?
                saveAttachments(docRef, mimeMessage, false, invoiceRefToAttachment);
                documentLogService.addDocumentLog(docRef, I18NUtil.getMessage("document_log_status_imported", I18NUtil.getMessage("document_log_creator_imap")) //
                        , I18NUtil.getMessage("document_log_creator_imap"));
            }

        } catch (Exception e) { // TODO: improve exception handling
            log.warn("Cannot save email, folderNodeRef=" + folderNodeRef, e);
            throw new FolderException("Cannot save email: " + e.getMessage());
        }
    }

    private List<NodeRef> createInvoice(NodeRef folderNodeRef, Part part) throws MessagingException, IOException {
        String mimetype = getMimetype(part, null);
        if (MimetypeMap.MIMETYPE_XML.equalsIgnoreCase(mimetype)) {
            return einvoiceService.importInvoiceFromXml(folderNodeRef, part.getInputStream(), TransmittalMode.EMAIL);
        }
        return new ArrayList<NodeRef>(0);
    }

    @Override
    public Collection<MailFolder> createAndListFolders(AlfrescoImapUser user, String mailboxPattern) {
        try {
            return addBehaviour(filter(imapService.listSubscribedMailboxes(user, mailboxPattern)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveAttachments(NodeRef document, MimeMessage originalMessage, boolean saveBody)
            throws IOException, MessagingException, TransformationException {
        saveAttachments(document, originalMessage, saveBody, null);
    }

    private void saveAttachments(NodeRef document, MimeMessage originalMessage, boolean saveBody, Map<NodeRef, Integer> invoiceRefToAttachment)
            throws IOException, MessagingException, TransformationException {
        Part bodyPart = null;
        if (saveBody) {
            bodyPart = createBody(document, originalMessage);
        }

        List<Part> attachments = new ArrayList<Part>();
        Object content = originalMessage.getContent();
        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;

            for (int i = 0, n = multipart.getCount(); i < n; i++) {
                Part part = multipart.getBodyPart(i);
                if (invoiceRefToAttachment == null || !invoiceRefToAttachment.containsValue(i) || invoiceRefToAttachment.get(document).equals(i)) {
                    if ("attachment".equalsIgnoreCase(part.getDisposition()) || StringUtils.isNotBlank(part.getFileName())) {
                        createAttachment(document, part, null);
                        attachments.add(part);
                    }
                }
            }
        }

        log.info("MimeMessage:" + getPartDebugInfo(originalMessage, bodyPart, attachments));
    }

    @Override
    public NodeRef getAttachmentRoot() {
        NodeRef attachmentSpaceRef = generalService.getNodeRef(ImapModel.Repo.ATTACHMENT_SPACE);
        Assert.notNull(attachmentSpaceRef, "Attachment node reference not found");
        return attachmentSpaceRef;
    }

    private void createAttachment(NodeRef document, Part part, String overrideFilename) throws MessagingException, IOException {
        String mimeType = getMimetype(part, overrideFilename);

        String filename;
        if (overrideFilename == null) {
            filename = part.getFileName();
        } else {
            filename = overrideFilename + "." + mimetypeService.getExtension(mimeType);
        }
        if (StringUtils.isBlank(filename)) {
            filename = I18NUtil.getMessage("imap.letter_attachment_filename") + "." + mimetypeService.getExtension(mimeType);
        }
        String encoding = getEncoding(part);
        FileInfo createdFile = fileFolderService.create(
                document,
                generalService.getUniqueFileName(document, filename),
                ContentModel.TYPE_CONTENT);
        ContentWriter writer = fileFolderService.getWriter(createdFile.getNodeRef());
        writer.setMimetype(mimeType);
        writer.setEncoding(encoding);
        OutputStream os = writer.getContentOutputStream();
        FileCopyUtils.copy(part.getInputStream(), os);
    }

    private String getMimetype(Part part, String overrideFilename) throws MessagingException {
        String mimeType = null;
        String contentTypeString = part.getContentType();
        try {
            ContentType contentType = new ContentType(contentTypeString);
            mimeType = StringUtils.lowerCase(contentType.getBaseType());
        } catch (ParseException e) {
            log.warn("Error parsing contentType '" + contentTypeString + "'", e);
        }
        if (overrideFilename == null && part.getFileName() != null) {
            // Always ignore user-provided mime-type
            String oldMimetype = mimeType;
            mimeType = mimetypeService.guessMimetype(part.getFileName());
            if (log.isDebugEnabled() && !StringUtils.equals(oldMimetype, mimeType)) {
                log.debug("Original mimetype '" + oldMimetype + "', but we are guessing mimetype based on filename '" + part.getFileName() + "' => '"
                            + mimeType + "'");
            }
        } else if (StringUtils.isBlank(mimeType)) {
            // If mime-type parsing from contentType failed and overrideFilename is used, then use binary mime type
            mimeType = MimetypeMap.MIMETYPE_BINARY;
        }
        return mimeType;
    }

    private Part createBody(NodeRef document, MimeMessage originalMessage) throws MessagingException, IOException {
        Part p = getText(originalMessage);
        if (p == null) {
            log.debug("No body part found from message, skipping body PDF creation");
            return p;
        }

        String mimeType;
        if (p.isMimeType(MimetypeMap.MIMETYPE_HTML)) {
            mimeType = MimetypeMap.MIMETYPE_HTML;
        } else if (p.isMimeType(MimetypeMap.MIMETYPE_TEXT_PLAIN)) {
            mimeType = MimetypeMap.MIMETYPE_TEXT_PLAIN;
        } else {
            log.info("Found body part from message, but don't know how to handle it, skipping body PDF creation, contentType=" + p.getContentType());
            return p;
        }
        // We assume that content-type header also contains charset; so far there haven't been different cases
        // If content-type header doesn't contain charset, we use UTF-8 as default
        String encoding = getEncoding(p);
        log.info("Found body part from message, parsed mimeType=" + mimeType + " and encoding=" + encoding + " from contentType=" + p.getContentType());

        ContentWriter tempWriter = contentService.getTempWriter();
        tempWriter.setMimetype(mimeType);
        tempWriter.setEncoding(encoding);
        tempWriter.putContent(p.getInputStream());

        // THIS DOES NOT WORK:
        // String content = (String) p.getContent(); <-- JavaMail does not care for encoding when parsing content to string this way!
        // tempWriter.putContent(content);

        // p.writeTo(tempWriter.getContentOutputStream()); <-- THIS DOES NOT WORK

        ContentReader reader = tempWriter.getReader();

        String filename = I18NUtil.getMessage("imap.letter_body_filename");
        FileInfo createdFile = fileService.transformToPdf(document, reader, filename, filename);
        if (createdFile == null) {
            createAttachment(document, p, filename);
        }
        return p;
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
                    if (text == null) {
                        text = getText(bp);
                    }
                    continue;
                } else if (bp.isMimeType("text/html")) {
                    Part s = getText(bp);
                    if (s != null) {
                        return s;
                    }
                } else {
                    return getText(bp);
                }
            }
            return text;
        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                Part s = getText(mp.getBodyPart(i));
                if (s != null) {
                    return s;
                }
            }
        }
        return null;
    }

    private String getPartDebugInfo(Part p, Part bodyPart, List<Part> attachments) throws MessagingException, IOException {
        String debugInfo = "\n¤Part:";
        // Compare by reference
        if (p == bodyPart) {
            debugInfo += " BODY";
        }
        for (Part attachment : attachments) {
            if (p == attachment) {
                debugInfo += " ATTACHMENT";
            }
        }
        debugInfo += " disposition=" + p.getDisposition();
        debugInfo += " contentType=" + p.getContentType();
        debugInfo += " fileName=" + p.getFileName();
        debugInfo += " size=" + p.getSize();

        if (p.isMimeType("text/plain")) {
            debugInfo += " isMimeType('text/plain')";
        } else if (p.isMimeType("text/html")) {
            debugInfo += " isMimeType('text/html')";
        }
        Object content = p.getContent();
        if (p.isMimeType("text/*")) {
            debugInfo += " isMimeType('text/*')";
        } else {
            if (content instanceof Multipart) {
                debugInfo += " isInstanceOfMultipart";
                if (p.isMimeType("multipart/alternative")) {
                    debugInfo += " isMimeType('multipart/alternative')";
                } else if (p.isMimeType("multipart/*")) {
                    debugInfo += " isMimeType('multipart/*')";
                }
                Multipart mp = (Multipart) content;
                debugInfo += " multiPartContentType=" + mp.getContentType();
                debugInfo += " multiPartCount=" + mp.getCount();
                for (int i = 0; i < mp.getCount(); i++) {
                    Part bp = mp.getBodyPart(i);
                    debugInfo += StringUtils.replace(getPartDebugInfo(bp, bodyPart, attachments), "\n¤", "\n¤  ");
                }
            }
        }
        return debugInfo;
    }

    private MailFolder addBehaviour(AlfrescoImapFolder folder) {

        String appendBehaviour;
        FileInfo folderInfo = folder.getFolderInfo();
        if (folderInfo != null) { // todo: why folder info is null?
            appendBehaviour = (String) folderInfo.getProperties().get(ImapModel.Properties.APPEND_BEHAVIOUR);
            if (appendBehaviour == null) {
                appendBehaviour = PermissionDeniedAppendBehaviour.BEHAVIOUR_NAME;
            }
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
        } else if (IncomingEInvoiceCreateBehaviour.BEHAVIOUR_NAME.equals(behaviour)) {
            return new IncomingEInvoiceCreateBehaviour(this);
        } else if (AttachmentsFolderAppendBehaviour.BEHAVIOUR_NAME.equals(behaviour)) {
            return new AttachmentsFolderAppendBehaviour(this);
        } else if (SentFolderAppendBehaviour.BEHAVIOUR_NAME.equals(behaviour)) {
            return new SentFolderAppendBehaviour(this);
        } else {
            throw new RuntimeException("Unknown behaviour: " + behaviour);
        }
    }

    private Collection<AlfrescoImapFolder> filter(Collection<AlfrescoImapFolder> folders) {
        CollectionUtils.filter(folders, new Predicate() {
            @Override
            public boolean evaluate(Object o) {
                MailFolder folder = (MailFolder) o;
                if (getAllowedFolders().contains(folder.getName())) {
                    if (folder.getName().equals(getIncomingInvoiceFolderName())) {
                        return einvoiceService.isEinvoiceEnabled();
                    }
                    return true;
                }
                return false;
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
            allowedFolders.add(getIncomingInvoiceFolderName());
            allowedFolders.add(I18NUtil.getMessage("imap.folder_attachments"));
            allowedFolders.add(I18NUtil.getMessage("imap.folder_sent_letters"));
        }
        return allowedFolders;
    }

    private String getIncomingInvoiceFolderName() {
        return I18NUtil.getMessage("imap.folder_incomingInvoice");
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

    public void setDocumentDynamicService(DocumentDynamicService documentDynamicService) {
        this.documentDynamicService = documentDynamicService;
    }

    public void setEinvoiceService(EInvoiceService einvoiceService) {
        this.einvoiceService = einvoiceService;
    }

    public void setMessageCopyFolder(String messageCopyFolder) {
        this.messageCopyFolder = messageCopyFolder;
    }

}
