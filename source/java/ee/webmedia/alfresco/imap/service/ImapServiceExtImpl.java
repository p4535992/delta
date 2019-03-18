package ee.webmedia.alfresco.imap.service;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
import javax.swing.text.BadLocationException;
import javax.swing.text.rtf.RTFEditorKit;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.imap.AlfrescoImapConst;
import org.alfresco.repo.imap.AlfrescoImapFolder;
import org.alfresco.repo.imap.AlfrescoImapUser;
import org.alfresco.repo.imap.ImapService;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.alfresco.util.Pair;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.DocumentNode;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem;
import org.apache.xml.security.transforms.TransformationException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.FileCopyUtils;

import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.util.GreenMailUtil;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.classificator.enums.StorageType;
import ee.webmedia.alfresco.classificator.enums.TransmittalMode;
import ee.webmedia.alfresco.common.service.ApplicationConstantsBean;
import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceService;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.file.web.Subfolder;
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
import ee.webmedia.alfresco.imap.SendFailureAppendBehaviour;
import ee.webmedia.alfresco.imap.SentFolderAppendBehaviour;
import ee.webmedia.alfresco.imap.model.ImapModel;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

import net.freeutils.tnef.Attachment;
import net.freeutils.tnef.CompressedRTFInputStream;
import net.freeutils.tnef.MAPIProp;
import net.freeutils.tnef.MAPIProps;
import net.freeutils.tnef.Message;
import net.freeutils.tnef.RawInputStream;
import net.freeutils.tnef.TNEFInputStream;
import net.freeutils.tnef.TNEFUtils;

/**
 * SimDhs specific IMAP logic.
 */
public class ImapServiceExtImpl implements ImapServiceExt, InitializingBean {
    private static final Log log = LogFactory.getLog(ImapServiceExtImpl.class);
    private static final FastDateFormat dateTimeFormat = FastDateFormat.getInstance("dd.MM.yyyy HH:mm");

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
    public UserService userService;

    private String messageCopyFolder;
    private boolean saveOriginalToRepo;
    private String incomingLetterSubfolderType;
    private String attachmentsSubfolderType;
    private String outgoingLettersSubfolderType;
    private String sendFailureNoticesSubfolderType;
    private Map<NodeRef, String> imapFolderTypes = null;
    private Map<NodeRef, Set<String>> imapFolderFixedSubfolders = null;
    private ApplicationConstantsBean applicationConstantsBean;
    private BulkLoadNodeService bulkLoadNodeService;

    // todo: make this configurable with spring
    private Set<String> allowedFolders = null;

    @Override
    public void afterPropertiesSet() throws Exception {
        GreenMailUtil.setMessageCopyFolder(messageCopyFolder);
        GreenMailUtil.setSaveOriginalToRepo(saveOriginalToRepo);
    }

    @Override
    public MailFolder getFolder(AlfrescoImapUser user, String folderName) {
        return addBehaviour(imapService.getFolder(user, folderName));
    }

    @Override
    public long saveEmailToSubfolder(NodeRef folderNodeRef, MimeMessage mimeMessage, String behaviour, boolean incomingEmail) throws FolderException {
        NodeRef parentNodeRef = findOrCreateFolder(folderNodeRef, behaviour);
        return saveEmail(parentNodeRef, mimeMessage, incomingEmail);
    }

    @Override
    public void saveFailureNoticeToSubfolder(NodeRef folderNodeRef, MimeMessage mimeMessage, String behaviour) throws FolderException {
        NodeRef parentNodeRef = findOrCreateFolder(folderNodeRef, behaviour);
        saveFailureNotice(parentNodeRef, mimeMessage);
    }

    private void saveFailureNotice(NodeRef parentNodeRef, MimeMessage mimeMessage) throws FolderException {
        try {
            createBody(parentNodeRef, mimeMessage, mimeMessage.getSubject(), null);
        } catch (Exception e) {
            log.warn("Cannot save email, folderNodeRef=" + parentNodeRef, e);
            throw new FolderException("Cannot save email: " + e.getMessage());
        }
    }

    private NodeRef findOrCreateFolder(NodeRef folderNodeRef, String behaviour) throws FolderException {
        NodeRef parentNodeRef = folderNodeRef;
        if (isUserBasedFolder(parentNodeRef)) {
            String username = AuthenticationUtil.getRunAsUser();
            if (userService.getPerson(username) == null) {
                throw new FolderException("User " + username + " doesn't exist, cannot create imap folder!");
            }
            parentNodeRef = fileService.findSubfolderWithName(parentNodeRef, username, ImapModel.Types.IMAP_FOLDER);
            if (parentNodeRef == null) {
                parentNodeRef = createImapSubfolder(folderNodeRef, behaviour, userService.getUserFullName(username), username);
            }
        }
        return parentNodeRef;
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
            NodeRef docRef = documentDynamicService.createNewDocument(docTypeId, folderNodeRef).getFirst().getNodeRef();

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

            Map<QName, Serializable> emailAspectProps = new HashMap<QName, Serializable>();
            emailAspectProps.put(DocumentCommonModel.Props.EMAIL_DATE_TIME, mimeMessage.getSentDate());
            nodeService.addAspect(docRef, DocumentCommonModel.Aspects.EMAIL_DATE_TIME, emailAspectProps);

            documentLogService.addDocumentLog(docRef, I18NUtil.getMessage("document_log_status_imported", I18NUtil.getMessage("document_log_creator_imap")) //
                    , I18NUtil.getMessage("document_log_creator_imap"));

            saveOriginalEmlFile(mimeMessage, docRef);
            saveAttachments(docRef, mimeMessage, true);
            fileService.reorderFiles(docRef);

            return (Long) nodeService.getProperty(docRef, ContentModel.PROP_NODE_DBID);
        } catch (Exception e) { // todo: improve exception handling
            log.warn("Cannot save email, folderNodeRef=" + folderNodeRef, e);
            throw new FolderException("Cannot save email: " + e.getMessage());
        }
    }

    private void saveOriginalEmlFile(MimeMessage mimeMessage, NodeRef docRef) throws MessagingException {
        String[] contentDataHeader = mimeMessage.getHeader(GreenMailUtil.SAVE_ORIGINAL_TO_REPO_CONTENT_DATA_HEADER_NAME);
        if (contentDataHeader == null || contentDataHeader.length <= 0 || StringUtils.isBlank(contentDataHeader[0])) {
            return;
        }
        ContentData contentData = ContentData.createContentProperty(contentDataHeader[0]);
        String filename = I18NUtil.getMessage("imap.letter_body_filename") + "." + mimetypeService.getExtension(contentData.getMimetype());
        NodeRef fileRef = fileFolderService.create(
                docRef,
                generalService.getUniqueFileName(docRef, filename),
                ContentModel.TYPE_CONTENT).getNodeRef();
        HashMap<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(ContentModel.PROP_CONTENT, contentData);
        props.put(FileModel.Props.DISPLAY_NAME, fileService.getUniqueFileDisplayName(docRef, filename));
        props.put(FileModel.Props.ACTIVE, Boolean.FALSE);
        nodeService.addProperties(fileRef, props);
    }

    @Override
    public void saveIncomingEInvoice(NodeRef folderNodeRef, MimeMessage mimeMessage) throws FolderException {
        try {
            Object content = mimeMessage.getContent();
            List<NodeRef> newInvoices = new ArrayList<NodeRef>();
            Map<NodeRef, Integer> invoiceRefToAttachment = new HashMap<NodeRef, Integer>();
            if (content instanceof Multipart) {
                Multipart multipart = (Multipart) content;

                // Handle only top-level attachment files, not files included inside message body (usually pictures)
                // TODO detect invoices also from Rich Text (winmail.dat) messages
                for (int i = 0, n = multipart.getCount(); i < n; i++) {
                    Part part = multipart.getBodyPart(i);
                    if (isAttachment(part)) {
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
            if (!newInvoices.isEmpty()) {
                fileService.reorderFiles(newInvoices);
            }

        } catch (Exception e) { // TODO: improve exception handling
            log.warn("Cannot save email, folderNodeRef=" + folderNodeRef, e);
            throw new FolderException("Cannot save email: " + e.getMessage());
        }
    }

    private List<NodeRef> createInvoice(NodeRef folderNodeRef, Part part) throws MessagingException, IOException {
        String mimetype = getMimetype(part, null);
        if (MimetypeMap.MIMETYPE_XML.equalsIgnoreCase(mimetype)) {
            InputStream inputStream = part.getInputStream();
            try {
                return einvoiceService.importInvoiceFromXml(folderNodeRef, inputStream, TransmittalMode.EMAIL);
            } finally {
                inputStream.close();
            }
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
    public NodeRef createImapSubfolder(NodeRef parentFolderNodeRef, String behaviour, String subfolderName, String folderAssocName) {
        QName assocName = QName.createQName(ImapModel.URI, folderAssocName);
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(ContentModel.PROP_NAME, subfolderName);
        if (StringUtils.isNotBlank(behaviour)) {
            props.put(ImapModel.Properties.APPEND_BEHAVIOUR, behaviour);
        }
        NodeRef folderRef = nodeService.createNode(parentFolderNodeRef, ContentModel.ASSOC_CONTAINS, assocName, ImapModel.Types.IMAP_FOLDER, props).getChildRef();
        BeanHelper.getPrivilegeService().setPermissions(folderRef, UserService.AUTH_DOCUMENT_MANAGERS_GROUP, Privilege.EDIT_DOCUMENT);
        return folderRef;
    }

    @Override
    public void saveAttachmentsToSubfolder(NodeRef document, MimeMessage originalMessage, boolean saveBody) throws IOException, MessagingException, TransformationException,
            FolderException, BadLocationException  {
        NodeRef parentNodeRef = findOrCreateFolder(document, AttachmentsFolderAppendBehaviour.BEHAVIOUR_NAME);
        saveAttachments(parentNodeRef, originalMessage, saveBody);
    }

    @Override
    public void saveAttachments(NodeRef document, MimeMessage originalMessage, boolean saveBody)
            throws IOException, MessagingException, BadLocationException  {
        saveAttachments(document, originalMessage, saveBody, null);
    }

    private void saveAttachments(NodeRef document, MimeMessage originalMessage, boolean saveBody, Map<NodeRef, Integer> invoiceRefToAttachment) throws IOException,
            MessagingException, BadLocationException  {

        Object content = originalMessage.getContent();
        //if (content instanceof String) {
        //	content = fixPlainTextContentEncoding((String)content);
        //}
        Part tnefPart = getTnefPart(content);
        Message tnefMessage = null;
        InputStream tnefInputStream = null;
        try {
            List<Part> attachments = new ArrayList<>();
            saveAttachments(document, content, attachments, invoiceRefToAttachment, tnefPart);
            String metadata = generateEmailMetadata(originalMessage, attachments);
            if (tnefPart != null) {
                tnefInputStream = tnefPart.getInputStream();
                tnefMessage = new Message(new TNEFInputStream(tnefInputStream));
                saveBody = saveTnefBodyAndAttachments(document, tnefMessage, metadata, saveBody);
            }

            Part bodyPart = null;
            if (saveBody) {
                bodyPart = createBody(document, originalMessage, metadata);
            }

            log.info("MimeMessage:" + getPartDebugInfo(originalMessage, bodyPart, tnefPart, attachments, tnefMessage));

        } finally {
            if (tnefMessage != null) {
                tnefMessage.close();
            }
            if (tnefInputStream != null) {
                tnefInputStream.close();
            }
        }
    }

    public List<String> getAttachmentNames(List<Part> attachments) throws MessagingException {
        List<String> attachmentNames = new ArrayList<>();
        for (Part attachment : attachments) {
            if (StringUtils.isNotBlank(attachment.getFileName())) {
                attachmentNames.add(attachment.getFileName());
            }
        }
        return attachmentNames;
    }

    private String generateEmailMetadata(MimeMessage message, List<Part> attachments) throws MessagingException {
        StringBuilder metadata = new StringBuilder();
        boolean isText = isPlainText(message);
        String lineBreak = isText ? "\n" : "<br />";
        if (message.getFrom() != null && message.getFrom().length > 0) {
            InternetAddress sender = (InternetAddress) message.getFrom()[0];
            metadata.append(getFieldName("email_from_field")).append(getNameEmailString(sender, isText)).append(lineBreak);
        }
        if (message.getSentDate() != null) {
            metadata.append(getFieldName("email_date_field")).append(dateTimeFormat.format(message.getSentDate())).append(lineBreak);
        }
        List<String> recipientsTo = getNameEmailList((InternetAddress[]) message.getRecipients(javax.mail.Message.RecipientType.TO), isText);
        if (CollectionUtils.isNotEmpty(recipientsTo)) {
            metadata.append(getFieldName("email_to_field")).append(StringUtils.join(recipientsTo, "; ")).append(lineBreak);
        }
        List<String> recipientsCc = getNameEmailList((InternetAddress[]) message.getRecipients(javax.mail.Message.RecipientType.CC), isText);
        if (CollectionUtils.isNotEmpty(recipientsCc)) {
            metadata.append(getFieldName("email_cc_field")).append(StringUtils.join(recipientsCc, "; ")).append(lineBreak);
        }
        if (StringUtils.isNotBlank(message.getSubject())) {
            metadata.append(getFieldName("email_subject_field")).append(message.getSubject()).append(lineBreak);
        }
        String priority = getPriorotyValue(message);
        if (StringUtils.isNotBlank(priority)) {
            metadata.append(getFieldName("email_priority_field")).append(priority).append(lineBreak);
        }
        List<String> attachmentNames = getAttachmentNames(attachments);
        if (CollectionUtils.isNotEmpty(attachmentNames)) {
            metadata.append(getFieldName("email_attachment_field")).append(StringUtils.join(attachmentNames, "; ")).append(lineBreak);
        }
        if (metadata.length() > 0) {
            metadata.append(lineBreak);
            metadata.append(lineBreak);
        }
        return metadata.toString();
    }

    private boolean isPlainText(MimeMessage message) {
        try {
            Part p = getText(message);
            if (p.isMimeType(MimetypeMap.MIMETYPE_HTML)) {
                return false;
            }
        } catch (Exception e) {
            return true;
        }
        return true;
    }

    private String getFieldName(String code){
        return MessageUtil.getMessage(code) + ": ";
    }

    private String getPriorotyValue(MimeMessage message) throws MessagingException {
        String priority = message.getHeader("X-Priority", "(");
        String code = null;
        if (StringUtils.isNotBlank(priority)) {
            Integer priorityValue = Integer.valueOf(priority.substring(0, 1));
            if (priorityValue == 1) {
                code = "email_priority_highest";
            } else if (priorityValue == 2) {
                code = "email_priority_high";
            } else if (priorityValue == 3) {
                code = "email_priority_normal";
            } else if (priorityValue == 4) {
                code = "email_priority_low";
            } else if (priorityValue == 5) {
                code = "email_priority_lowest";
            }
        }
        return StringUtils.isNotBlank(code) ? MessageUtil.getMessage(code) : null;
    }

    private List<String> getNameEmailList(InternetAddress[] addresses, boolean isText) {
        List<String> result = new ArrayList<>();
        if (addresses != null && addresses.length > 0) {
            for (InternetAddress address : addresses) {
                String nameEmailString = getNameEmailString(address, isText);
                if (StringUtils.isNotBlank(nameEmailString)) {
                    result.add(nameEmailString);
                }
            }
        }
        return result;
    }

    private String getNameEmailString(InternetAddress address, boolean isText) {
        if (address != null) {
            String result = StringUtils.EMPTY;
            if (StringUtils.isNotBlank(address.getPersonal())) {
                result += address.getPersonal() + " ";
            }
            if (StringUtils.isNotBlank(address.getAddress())) {
                result += isText ? "<" : "&lt;";
                result += address.getAddress();
                result += isText ? ">" : "&gt;";
            }
            return result;
        }
        return null;
    }

    private String fixPlainTextContentEncoding(String content) {
        byte[] tempBytes =  Charset.forName("ISO-8859-1").encode(content).array();
        String tempContent = new String(tempBytes);
        content = new String(Charset.forName("UTF-8").encode(tempContent).array());
        return content;
    }

    private boolean saveTnefBodyAndAttachments(NodeRef document, Message tnefMessage, String metadata, boolean saveBody) throws IOException, UnsupportedEncodingException, BadLocationException {
        if (saveBody) {
            String bodyFilename = I18NUtil.getMessage("imap.letter_body_filename");
            MAPIProps props = tnefMessage.getMAPIProps();
            if (props != null) {
                // compressed RTF body
                RawInputStream ris = (RawInputStream) props.getPropValue(MAPIProp.PR_RTF_COMPRESSED);
                if (ris != null) {
                    InputStream bodyStream = new CompressedRTFInputStream(ris);
                    try {
                        createBody(document, "application/rtf", StandardCharsets.UTF_8.name(), metadata, bodyStream, bodyFilename);
                    } finally {
                        bodyStream.close();
                    }
                    saveBody = false;
                }
                if (saveBody) {
                    // HTML body (either PR_HTML or PR_BODY_HTML - both have the same ID, but one is a string and one is a byte array)
                    Object html = props.getPropValue(MAPIProp.PR_HTML);
                    if (html != null) {
                        if (html instanceof RawInputStream) {
                            ris = (RawInputStream) html;
                            File file = TempFileProvider.createTempFile("tnefHtmlBody", null);
                            try {
                                OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                                FileCopyUtils.copy(ris, out);

                                InputStream in = new BufferedInputStream(new FileInputStream(file));
                                Pair<String, String> mimetypeAndEncoding;
                                try {
                                    mimetypeAndEncoding = generalService.getMimetypeAndEncoding(in, "test.html", MimetypeMap.MIMETYPE_HTML);
                                } finally {
                                    in.close();
                                }
                                in = new BufferedInputStream(new FileInputStream(file));
                                try {
                                    createBody(document, mimetypeAndEncoding.getFirst(), mimetypeAndEncoding.getSecond(), metadata, ris, bodyFilename);
                                } finally {
                                    in.close();
                                }
                                saveBody = false;
                            } finally {
                                file.delete();
                            }
                        } else {
                            String text = (String) html;
                            if (StringUtils.isNotEmpty(text)) {
                                InputStream bodyStream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
                                try {
                                    createBody(document, MimetypeMap.MIMETYPE_HTML, StandardCharsets.UTF_8.name(), metadata, bodyStream, bodyFilename);
                                } finally {
                                    bodyStream.close();
                                }
                                saveBody = false;
                            }
                        }
                    }
                    // If RTF and HTML body is not available from inside TNEF, then skip getting TEXT body from inside
                    // HTML (message.getAttribute(Attr.attBody)), because it should be available as plain MIME part
                }
            }
        }

        @SuppressWarnings("unchecked")
        List<Attachment> attachments = tnefMessage.getAttachments();
        for (Attachment attachment : attachments) {
            if (attachment.getNestedMessage() != null) { // nested message
                saveBody = saveTnefBodyAndAttachments(document, attachment.getNestedMessage(), metadata, saveBody);
            } else { // regular attachment

                // XXX attachment.getRawData() returns always the same InputStream, so we cannot use it multiple times (because we usually close it, not reset it to beginning);
                // therefore we have to write attachment contents to a temp file, to be able to use it multiple times (for encoding detection; for reading contents into Alfresco
                // repo)
                File file = TempFileProvider.createTempFile("tnefAttachment", null);
                try {
                    OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                    try {
                        attachment.writeTo(out);
                    } finally {
                        out.close();
                    }

                    boolean foundAttachmentsFromOle = saveAttachmentsFromOle(document, file, attachment);
                    if (!foundAttachmentsFromOle) {
                        saveAttachment(document, file, attachment);
                    }
                } finally {
                    file.delete();
                }
            }
        }
        return saveBody;
    }

    private void saveAttachment(NodeRef document, File tnefFile, Attachment tnefAttachment) throws FileNotFoundException, IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(tnefFile));
        Pair<String, String> mimetypeAndEncoding;
        try {
            mimetypeAndEncoding = generalService.getMimetypeAndEncoding(in, tnefAttachment.getFilename(), null);
        } finally {
            in.close();
        }
        in = new BufferedInputStream(new FileInputStream(tnefFile));
        try {
            createAttachment(document, tnefAttachment.getFilename(), mimetypeAndEncoding.getFirst(), mimetypeAndEncoding.getSecond(), in, null);
        } finally {
            in.close();
        }
    }

    private boolean saveAttachmentsFromOle(NodeRef document, File tnefFile, Attachment tnefAttachment) throws IOException {
        boolean foundAttachments = false;
        // In Rich Text e-mails inline pictures (added in Outlook as "Insert Picture", not as "Insert Attachment") are in "OLE 2 Compound Document" format

        if (tnefAttachment.getMAPIProps() == null) {
            return foundAttachments;
        }
        Object attachMethod = tnefAttachment.getMAPIProps().getPropValue(MAPIProp.PR_ATTACH_METHOD);
        // According to http://stackoverflow.com/questions/4657684/nicely-reading-outlook-mailitem-properties
        // PR_ATTACH_METHOD value ATTACH_OLE should be 6
        if (!new Integer(6).equals(attachMethod)) {
            return foundAttachments;
        }

        Object displayName = tnefAttachment.getMAPIProps().getPropValue(MAPIProp.PR_DISPLAY_NAME);
        String fileName;
        if ("Picture (Device Independent Bitmap)".equals(displayName)) {
            fileName = tnefAttachment.getFilename() + ".bmp";
        } else if (displayName instanceof String && StringUtils.isNotBlank((String) displayName)) {
            fileName = tnefAttachment.getFilename() + " " + ((String) displayName) + ".bin";
        } else {
            fileName = tnefAttachment.getFilename() + ".bin";
        }
        String mimeType = mimetypeService.guessMimetype(fileName);

        NPOIFSFileSystem fs = new NPOIFSFileSystem(tnefFile);
        try {
            DirectoryNode root = fs.getRoot();
            for (Entry entry : root) {
                if ("CONTENTS".equals(entry.getName()) && entry instanceof DocumentNode) {
                    DocumentInputStream inputStream = root.createDocumentInputStream(entry);
                    try {
                        createAttachment(document, fileName, mimeType, "UTF-8", inputStream, null);
                    } finally {
                        inputStream.close();
                    }
                    foundAttachments = true;
                }
            }
        } finally {
            fs.close();
        }
        return foundAttachments;
    }

    private Part getTnefPart(Object content) throws MessagingException, IOException {
        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;

            for (int i = 0, n = multipart.getCount(); i < n; i++) {
                Part part = multipart.getBodyPart(i);
                if (TNEFUtils.isTNEFMimeType(part.getContentType())) {
                    return part;
                }
                Part tnefPart = getTnefPart(part.getContent());
                if (tnefPart != null) {
                    return tnefPart;
                }
            }
        }
        return null;
    }

    private void saveAttachments(NodeRef document, Object content, List<Part> attachments, Map<NodeRef, Integer> invoiceRefToAttachment, Part tnefPart) throws MessagingException,
            IOException {
        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;

            for (int i = 0, n = multipart.getCount(); i < n; i++) {
                Part part = multipart.getBodyPart(i);
                if (part != tnefPart &&
                        (invoiceRefToAttachment == null || !invoiceRefToAttachment.containsValue(i) || invoiceRefToAttachment.get(document).equals(i))) {
                    if (isAttachment(part)) {
                        createAttachment(document, part, null);
                        attachments.add(part);
                    } else {
                        saveAttachments(document, part.getContent(), attachments, null, tnefPart); // invoice map values represent only top level attachments
                    }
                }
            }
        }
    }

    private boolean isAttachment(Part part) throws MessagingException {
        return Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || StringUtils.isNotBlank(part.getFileName());
    }

    @Override
    public boolean isFixedFolder(NodeRef folderRef) {
        return StringUtils.equals(FOLDER_TYPE_PREFIX_FIXED, getImapFolderTypes().get(folderRef));
    }

    @Override
    public boolean isUserBasedFolder(NodeRef folderRef) {
        return StringUtils.equals(FOLDER_TYPE_PREFIX_USER_BASED, getImapFolderTypes().get(folderRef));
    }

    @Override
    public Set<String> getFixedSubfolderNames(NodeRef parentFolderRef) {
        Set<String> subfolderNames = getImapFolderFixedSubfolders().get(parentFolderRef);
        if (subfolderNames == null) {
            return new HashSet<String>();
        }
        return subfolderNames;
    }

    private void createAttachment(NodeRef document, Part part, String overrideFilename) throws MessagingException, IOException {
        String partFileName = part.getFileName();
        String mimeType = getMimetype(part, overrideFilename);
        String encoding = getEncoding(part);
        if (StringUtils.isBlank(encoding)) {
            InputStream inputStream = part.getInputStream();
            try {
                Pair<String, String> mimetypeAndEncoding = generalService.getMimetypeAndEncoding(inputStream, partFileName, mimeType);
                encoding = mimetypeAndEncoding.getSecond();
                // mimeType was already guessed the same way in getMimetype()
            } finally {
                inputStream.close();
            }
        }
        InputStream inputStream = part.getInputStream();
        try {
            createAttachment(document, partFileName, mimeType, encoding, inputStream, overrideFilename);
        } finally {
            inputStream.close();
        }
    }

    private void createAttachment(NodeRef document, String partFileName, String mimeType, String encoding, InputStream inputStream, String overrideFilename) throws IOException {
        String filename;
        if (overrideFilename == null) {
            filename = partFileName;
        } else {
            filename = overrideFilename + "." + mimetypeService.getExtension(mimeType);
        }
        if (StringUtils.isBlank(filename)) {
            filename = I18NUtil.getMessage("imap.letter_attachment_filename") + "." + mimetypeService.getExtension(mimeType);
        }
        FileInfo createdFile = fileFolderService.create(
                document,
                generalService.getUniqueFileName(document, FilenameUtil.makeSafeFilename(filename)),
                ContentModel.TYPE_CONTENT);
        nodeService.setProperty(createdFile.getNodeRef(), FileModel.Props.DISPLAY_NAME, filename);
        ContentWriter writer = fileFolderService.getWriter(createdFile.getNodeRef());
        writer.setMimetype(mimeType);
        writer.setEncoding(encoding);
        OutputStream os = writer.getContentOutputStream();
        FileCopyUtils.copy(inputStream, os);
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

    private Part createBody(NodeRef document, MimeMessage originalMessage, String metadata) throws MessagingException, IOException, BadLocationException  {
        String filename = I18NUtil.getMessage("imap.letter_body_filename");
        return createBody(document, originalMessage, filename, metadata);
    }

    private Part createBody(NodeRef document, MimeMessage originalMessage, String filename, String metadata) throws MessagingException, IOException, BadLocationException  {
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

        createBody(document, mimeType, encoding, metadata, p.getInputStream(), filename);

        return p;
    }

    private void createBody(NodeRef document, String mimeType, String encoding, String metadata, InputStream contentStream, String filename) throws IOException, BadLocationException  {
        ContentWriter tempWriter = contentService.getTempWriter();
        tempWriter.setMimetype(mimeType);
        tempWriter.setEncoding(encoding);
        tempWriter.putContent(createContentString(metadata, contentStream, mimeType, encoding));
        String safeFileName = FilenameUtil.makeSafeFilename(filename);
        FileInfo createdFile = fileService.transformToPdf(document, null, tempWriter.getReader(), safeFileName, filename, null);
        if (createdFile == null) {
            // Don't re-use previous reader (channel already opened)
            InputStream contentInputStream = tempWriter.getReader().getContentInputStream();
            try {
                createAttachment(document, null, mimeType, encoding, contentInputStream, filename);
            } finally {
                contentInputStream.close();
            }
        }
    }

    private InputStream createContentString(String metadata, InputStream contentStream, String mimeType, String encoding) throws IOException, BadLocationException {
        String resultString = StringUtils.EMPTY;
        if (StringUtils.isNotBlank(metadata)) {
            if (MimetypeMap.MIMETYPE_HTML.equals(mimeType)) {
                Document html = Jsoup.parse(IOUtils.toString(contentStream, encoding));
                html.body().before(metadata);
                resultString = html.toString();
            } else if ("application/rtf".equals(mimeType)) {
                RTFEditorKit rtfParser = new RTFEditorKit();
                javax.swing.text.Document document = rtfParser.createDefaultDocument();
                rtfParser.read(contentStream, document, 0);
                String rtfContent = document.getText(0, document.getLength());
                resultString += metadata;
                resultString += rtfContent;
            } else {
                resultString += metadata;
                resultString += IOUtils.toString(contentStream, encoding);
            }
            return new ByteArrayInputStream(resultString.getBytes(encoding));
        }
        return contentStream;
    }

    // Workaround for getting encoding from content type
    // contentType=text/plain; charset="iso-8859-1"
    private static String getEncoding(Part p) throws MessagingException {
        String encoding = StandardCharsets.UTF_8.name(); // default encoding is UTF-8
        String regExp = "(.*charset=\"([^\"]+)\".*)";

        Matcher matcher = Pattern.compile(regExp).matcher(p.getContentType().replace('\n', ' ').replace('\r', ' '));
        if (matcher.matches()) {
            if (matcher.groupCount() == 2) {
                encoding = matcher.group(2);
            }
        }

        // What is the point?
        // Why we should change encoding?
        if ("windows-1257".equals(encoding)) {
            // AARE: REMOVING active encondig change.
            //return StandardCharsets.UTF_8.name();
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

        Object content = p.getContent();
        Multipart mp;
        if (content instanceof Multipart) {
            mp = (Multipart) content;
        } else {
            return null;
        }

        if (p.isMimeType("multipart/alternative")) {
            // prefer html text over plain text
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
            for (int i = 0; i < mp.getCount(); i++) {
                Part s = getText(mp.getBodyPart(i));
                if (s != null) {
                    return s;
                }
            }
        }
        return null;
    }

    private String getPartDebugInfo(Part p, Part bodyPart, Part tnefPart, List<Part> attachments, Message tnefMessage) throws MessagingException, IOException {
        String debugInfo = "\n¤Part:";
        // Compare by reference
        if (p == bodyPart) {
            debugInfo += " BODY";
        }
        if (p == tnefPart) {
            debugInfo += " TNEF";
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
            }
        }
        if (p == tnefPart && tnefMessage != null) {
            debugInfo += StringUtils.replace(getTnefMessageDebugInfo(tnefMessage), "\n¤", "\n¤  ");
        }
        if (content instanceof Multipart) {
            Multipart mp = (Multipart) content;
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                debugInfo += StringUtils.replace(getPartDebugInfo(bp, bodyPart, tnefPart, attachments, tnefMessage), "\n¤", "\n¤  ");
            }
        }
        return debugInfo;
    }

    private String getTnefMessageDebugInfo(Message tnefMessage) throws IOException {
        String debugInfo = "\n¤TNEF Message:";
        MAPIProps props = tnefMessage.getMAPIProps();
        if (props == null) {
            debugInfo += " mapiPropsIsNull";
        } else {
            debugInfo += " PR_RTF_COMPRESSED=" + getObjectDebugInfo(props.getPropValue(MAPIProp.PR_RTF_COMPRESSED));
            debugInfo += " PR_HTML=" + getObjectDebugInfo(props.getPropValue(MAPIProp.PR_HTML));
        }

        @SuppressWarnings("unchecked")
        List<Attachment> tnefAttachments = tnefMessage.getAttachments();
        for (Attachment attachment : tnefAttachments) {
            debugInfo += "\n¤  TNEF Attachment:";
            debugInfo += " nestedMessage=" + getObjectDebugInfo(attachment.getNestedMessage());
            debugInfo += " filename=" + attachment.getFilename();
            debugInfo += " rawData=" + getObjectDebugInfo(attachment.getRawData());
            props = attachment.getMAPIProps();
            if (props == null) {
                debugInfo += " mapiPropsIsNull";
            } else {
                debugInfo += " PR_ATTACH_METHOD=" + getObjectDebugInfo(props.getPropValue(MAPIProp.PR_ATTACH_METHOD));
                debugInfo += " PR_DISPLAY_NAME=" + getObjectDebugInfo(props.getPropValue(MAPIProp.PR_DISPLAY_NAME));
            }
            if (attachment.getNestedMessage() != null) { // nested message
                debugInfo += StringUtils.replace(getTnefMessageDebugInfo(attachment.getNestedMessage()), "\n¤", "\n¤    ");
            }
        }
        return debugInfo;
    }

    private static String getObjectDebugInfo(Object object) throws IOException {
        if (object == null) {
            return "null";
        }
        String info = object.getClass().getSimpleName();
        if (object instanceof InputStream) {
            info += "[available=" + ((InputStream) object).available() + "]";
        } else if (object instanceof Number) {
            info = object.toString();
        } else if (object instanceof String) {
            int length = ((String) object).length();
            if (length <= 64) {
                info = object.toString();
            } else {
                info += "[length=" + length + "]";
            }
        }
        return info;
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
        } else if (SendFailureAppendBehaviour.BEHAVIOUR_NAME.equals(behaviour)) {
            return new SendFailureAppendBehaviour(this);
        } else {
            throw new RuntimeException("Unknown behaviour: " + behaviour);
        }
    }

    private Collection<AlfrescoImapFolder> filter(Collection<AlfrescoImapFolder> folders) {
        CollectionUtils.filter(folders, new Predicate() {
            @Override
            public boolean evaluate(Object o) {
                AlfrescoImapFolder folder = (AlfrescoImapFolder) o;
                NodeRef parentRef = null;
                ChildAssociationRef parentAssoc = nodeService.getPrimaryParent(folder.getFolderInfo().getNodeRef());
                if (parentAssoc != null) {
                    parentRef = parentAssoc.getParentRef();
                }
                if (parentRef != null) {
                    if (isFixedFolder(parentRef)) {
                        if (getFixedSubfolderNames(parentRef).contains(folder.getName())) {
                            return true;
                        }
                        return false;
                    }
                    if (isUserBasedFolder(parentRef)) {
                        return false;
                    }
                    // subfolder type is not specified, user general logic for filtering
                }
                if (getAllowedFolders().contains(folder.getName())) {
                    if (folder.getName().equals(getIncomingInvoiceFolderName())) {
                        return applicationConstantsBean.isEinvoiceEnabled();
                    }
                    return true;
                }

                return false;
            }
        });
        return folders;
    }

    @Override
    public int getAllFilesCount(NodeRef attachmentRoot, boolean countFilesInSubfolders, int limit) {
        int count = bulkLoadNodeService.countChildNodes(attachmentRoot, ContentModel.TYPE_CONTENT);

        if (countFilesInSubfolders && count < limit) {
            Set<NodeRef> childRefs = bulkLoadNodeService.loadChildRefs(attachmentRoot, null, null, ContentModel.TYPE_CONTENT);
            Map<NodeRef, Integer> childCounts = bulkLoadNodeService.countChildNodes(new ArrayList<NodeRef>(childRefs), ContentModel.TYPE_CONTENT);
            for (Integer childCount : childCounts.values()) {
                count += childCount;
                if (count > limit) {
                    break;
                }
            }
        }
        return count;
    }

    @Override
    public List<Subfolder> getImapSubfoldersWithChildCount(NodeRef parentRef, QName countableChildNodeType) {
        return fileService.getSubfolders(parentRef, ImapModel.Types.IMAP_FOLDER, countableChildNodeType, true);
    }

    @Override
    public List<Subfolder> getImapSubfolders(NodeRef parentRef) {
        return fileService.getSubfolders(parentRef, ImapModel.Types.IMAP_FOLDER, null, false);
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
            allowedFolders.add(I18NUtil.getMessage("imap-folders.sendFailureNotices"));
        }
        return allowedFolders;
    }

    private String getIncomingInvoiceFolderName() {
        return I18NUtil.getMessage("imap.folder_incomingInvoice");
    }

    private synchronized void setImapFolderTypesAndSubFolders() {
        imapFolderTypes = new HashMap<NodeRef, String>();
        imapFolderFixedSubfolders = new HashMap<NodeRef, Set<String>>();
        setSubfolderType(incomingLetterSubfolderType, generalService.getNodeRef(ImapModel.Repo.INCOMING_SPACE));
        setSubfolderType(attachmentsSubfolderType, generalService.getNodeRef(ImapModel.Repo.ATTACHMENT_SPACE));
        setSubfolderType(outgoingLettersSubfolderType, generalService.getNodeRef(ImapModel.Repo.SENT_SPACE));
        setSubfolderType(sendFailureNoticesSubfolderType, generalService.getNodeRef(ImapModel.Repo.SEND_FAILURE_NOTICE_SPACE));
    }

    private void setSubfolderType(String subfolderType, NodeRef parentNodeRef) {
        if (StringUtils.isNotBlank(subfolderType)) {
            StringTokenizer tokenizer = new StringTokenizer(subfolderType, ";");
            String folderType = tokenizer.nextToken();
            if (StringUtils.equals(FOLDER_TYPE_PREFIX_FIXED, folderType)) {
                imapFolderTypes.put(parentNodeRef, FOLDER_TYPE_PREFIX_FIXED);
                Set<String> fixedFolderNames = new HashSet<String>();
                while (tokenizer.hasMoreTokens()) {
                    String folderName = tokenizer.nextToken();
                    QName.createValidLocalName(folderName);
                    if (StringUtils.isNotBlank(folderName)) {
                        fixedFolderNames.add(QName.createValidLocalName(folderName));
                    }
                }
                imapFolderFixedSubfolders.put(parentNodeRef, fixedFolderNames);
            } else if (StringUtils.equals(FOLDER_TYPE_PREFIX_USER_BASED, folderType)) {
                imapFolderTypes.put(parentNodeRef, FOLDER_TYPE_PREFIX_USER_BASED);
                imapFolderFixedSubfolders.remove(parentNodeRef);
            }
        }
    }

    public Map<NodeRef, String> getImapFolderTypes() {
        if (imapFolderTypes == null) {
            setImapFolderTypesAndSubFolders();
        }
        return imapFolderTypes;
    }

    public Map<NodeRef, Set<String>> getImapFolderFixedSubfolders() {
        if (imapFolderFixedSubfolders == null) {
            setImapFolderTypesAndSubFolders();
        }
        return imapFolderFixedSubfolders;
    }

    public void setIncomingLettersSubfolderType(String incomingLetterSubfolderType) {
        this.incomingLetterSubfolderType = incomingLetterSubfolderType;
    }

    public void setAttachmentsSubfolderType(String attachmentsSubfolderType) {
        this.attachmentsSubfolderType = attachmentsSubfolderType;
    }

    public void setOutgoingLettersSubfolderType(String outgoingLettersSubfolderType) {
        this.outgoingLettersSubfolderType = outgoingLettersSubfolderType;
    }

    public void setSendFailureNoticesSubfolderType(String sendFailureNoticesSubfolderType) {
        this.sendFailureNoticesSubfolderType = sendFailureNoticesSubfolderType;
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

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setMessageCopyFolder(String messageCopyFolder) {
        this.messageCopyFolder = messageCopyFolder;
    }

    public void setSaveOriginalToRepo(boolean saveOriginalToRepo) {
        this.saveOriginalToRepo = saveOriginalToRepo;
    }

    public void setApplicationConstantsBean(ApplicationConstantsBean applicationConstantsBean) {
        this.applicationConstantsBean = applicationConstantsBean;
    }

    public void setBulkLoadNodeService(BulkLoadNodeService bulkLoadNodeService) {
        this.bulkLoadNodeService = bulkLoadNodeService;
    }

}
