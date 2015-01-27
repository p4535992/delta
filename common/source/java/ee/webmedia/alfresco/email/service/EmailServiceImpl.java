package ee.webmedia.alfresco.email.service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.zip.DeflaterOutputStream;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.CollectionUtils;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.email.model.EmailAttachment;
import ee.webmedia.alfresco.monitoring.MonitoredService;
import ee.webmedia.alfresco.monitoring.MonitoringUtil;
import ee.webmedia.alfresco.signature.service.SignatureService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.MimeUtil;

public class EmailServiceImpl implements EmailService {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(EmailServiceImpl.class);

    private static final FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy-MM-dd-HH-mm-ss-SSSZ");

    private JavaMailSender mailService;
    private FileFolderService fileFolderService;
    private GeneralService generalService;
    private SignatureService _signatureService;
    private MimetypeService mimetypeService;
    private static String messageCopyFolder = null;

    // /// PUBLIC METHODS

    @Override
    public void sendEmail(List<String> toEmails, List<String> toNames, String fromEmail, String subject, String content, boolean isHtml, NodeRef document,
            List<EmailAttachment> attachments) throws EmailException {
        sendEmail(toEmails, toNames, null, null, fromEmail, subject, content, isHtml, document, attachments);
    }

    @Override
    public void sendEmail(List<String> toEmails, List<String> toNames, List<String> toBccEmails, List<String> toBccNames, String fromEmail, String subject,
            String content, boolean isHtml, NodeRef document, List<EmailAttachment> attachments) throws EmailException {

        long step0 = System.currentTimeMillis();

        if (CollectionUtils.isEmpty(toEmails) && CollectionUtils.isEmpty(toBccEmails)) {
            throw new EmailException("At least one of toEmails and toBccEmails is mandatory.");
        }
        if (fromEmail == null) {
            throw new EmailException("Parameter fromEmail is mandatory.");
        }
        if (subject == null) {
            throw new EmailException("Parameter subject is mandatory.");
        }
        if (content == null) {
            throw new EmailException("Parameter content is mandatory.");
        }

        // Avoid NPE
        if (toEmails == null) {
            toEmails = Collections.emptyList();
        } else if (toBccEmails == null) {
            toBccEmails = Collections.emptyList();
        }

        MimeMessage message;
        try {
            message = mailService.createMimeMessage();
        } catch (Exception e) {
            throw new EmailException(e);
        }
        MimeMessageHelper helper;
        try {
            helper = new MimeMessageHelper(message, attachments != null && !attachments.isEmpty(), AppConstants.CHARSET);
            helper.setValidateAddresses(true);
            helper.setFrom(fromEmail);
        } catch (Exception e) {
            throw new EmailException(e);
        }

        // To field
        addEmailRecipients(toEmails, toNames, helper, false);
        // Bcc field
        addEmailRecipients(toBccEmails, toBccNames, helper, true);

        subject = clean(subject);
        try {
            helper.setSubject(subject);
            helper.setText(content, isHtml);
        } catch (Exception e) {
            throw new EmailException(e);
        }

        if (attachments != null) {
            for (EmailAttachment attachment : attachments) {
                try {
                    String contentType = MimeUtil.getContentType(attachment.getMimeType(), attachment.getEncoding());
                    helper.addAttachment(attachment.getFileName(), attachment.getInputStreamSource(), contentType);
                } catch (AlfrescoRuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new EmailException(e);
                }
            }
        }

        if (log.isDebugEnabled()) {
            Address[] recipients;
            try {
                recipients = message.getAllRecipients();
            } catch (Exception e) {
                throw new EmailException(e);
            }
            log.debug("Sending out email: " + Arrays.toString(recipients));
        }

        try {
            long step1 = System.currentTimeMillis();
            mailService.send(message);
            long step2 = System.currentTimeMillis();

            // Write copy of the message after it has been send, then it has same header as were set during sending
            String info = "";
            if (StringUtils.isNotBlank(messageCopyFolder)) {
                try {
                    String filename = "MimeMessage-" + dateFormat.format(new Date());
                    File messageFile = new File(messageCopyFolder, filename);
                    FileOutputStream messageOutputStream = new FileOutputStream(messageFile);
                    try {
                        message.writeTo(messageOutputStream, new String[] { "Bcc", "Content-Length" });
                    } finally {
                        IOUtils.closeQuietly(messageOutputStream);
                    }
                    info = "\n    wrote message to file " + messageFile;
                } catch (Exception e) {
                    log.error("Error copying message contents to file", e);
                }
            }

            MonitoringUtil.logSuccess(MonitoredService.OUT_SMTP);
            if (log.isInfoEnabled()) {
                log.info("sendEmail service call took " + (step2 - step0) + " ms\n    prepare message - " + (step1 - step0) + " ms\n    send message - "
                        + (step2 - step1) + " ms" + info);
            }
        } catch (AlfrescoRuntimeException e) {
            MonitoringUtil.logError(MonitoredService.OUT_SMTP, e);
            throw e;
        } catch (Exception e) {
            MonitoringUtil.logError(MonitoredService.OUT_SMTP, e);
            throw new EmailException(e);
        }
    }

    private static String clean(String input) {
        // Forbids the use of characters in range 1-31 (i.e., 0x01-0x1F)
        return input == null ? null : input.replaceAll("\\p{Cntrl}", " ");
    }

    @Override
    public List<EmailAttachment> getAttachments(List<NodeRef> fileRefs, boolean zipIt, List<X509Certificate> encryptionCertificates, String zipAndEncryptFileTitle) {
        List<EmailAttachment> attachments = new ArrayList<EmailAttachment>();
        if (fileRefs != null && !fileRefs.isEmpty()) {
            String containerExtension = null;
            // If both ZIP and CDOC is selected, then only produce CDOC and ignore ZIP, because CDOC also compresses files
            if (encryptionCertificates != null && !encryptionCertificates.isEmpty()) {
                containerExtension = "cdoc";
            } else if (zipIt) {
                containerExtension = "zip";
            }
            if (containerExtension != null) {
                String fileName = FilenameUtil.buildFileName(zipAndEncryptFileTitle, containerExtension);
                final File tmpFile = TempFileProvider.createTempFile("sendout-", "." + containerExtension);
                AlfrescoTransactionSupport.bindListener(new TransactionListenerAdapter() {
                    @Override
                    public void beforeCompletion() {
                        tmpFile.delete();
                    }
                });
                try {
                    OutputStream tmpOutput = new BufferedOutputStream(new FileOutputStream(tmpFile));
                    if (encryptionCertificates != null && !encryptionCertificates.isEmpty()) {
                        getSignatureService().writeEncryptedContainer(tmpOutput, fileRefs, encryptionCertificates, fileName);
                    } else {
                        generalService.writeZipFileFromFiles(tmpOutput, fileRefs);
                    }
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                InputStreamSource contentSource = new FileSystemResource(tmpFile);
                String mimeType = mimetypeService.guessMimetype(fileName);
                attachments.add(new EmailAttachment(fileName, mimeType, AppConstants.CHARSET, contentSource, fileRefs.get(0)));
            } else {
                for (NodeRef fileRef : fileRefs) {
                    FileInfo fileInfo = fileFolderService.getFileInfo(fileRef);
                    ContentData contentData = fileInfo.getContentData();
                    AlfrescoContentSource contentSource = new AlfrescoContentSource(fileRef);
                    attachments.add(new EmailAttachment(fileInfo.getName(), contentData.getMimetype(), contentData.getEncoding(), contentSource, fileRef));
                }
            }
        }
        return attachments;
    }

    @Override
    public long getAttachmentsTotalSize(List<NodeRef> fileRefs, boolean zipIt, boolean encrypt) {
        long size = 0;
        // CDOC uses the same compression algorithm as ZIP (Deflater#DEFAULT_COMPRESSION)
        if (zipIt || encrypt) {
            CountingOutputStream tmpOutput = new CountingOutputStream(new NullOutputStream());
            if (encrypt) {
                DeflaterOutputStream zipOutput = new DeflaterOutputStream(tmpOutput);
                try {
                    getSignatureService().writeContainer(zipOutput, fileRefs);
                } finally {
                    IOUtils.closeQuietly(zipOutput);
                }
                size = tmpOutput.getCount() / 3 * 4; // CDOC encodes data in Base64
                size += size / 64; // CDOC adds one newline character every 64 characters
                size += 3072; // CDOC XML data (if one recipient) adds approx. 3 KB
            } else {
                generalService.writeZipFileFromFiles(tmpOutput, fileRefs);
                size = tmpOutput.getCount();
            }
        } else {
            for (NodeRef fileRef : fileRefs) {
                FileInfo fileInfo = fileFolderService.getFileInfo(fileRef);
                ContentData contentData = fileInfo.getContentData();
                size += contentData.getSize();
            }
        }
        return size;
    }

    // /// PRIVATE METHODS

    private MimeMessageHelper addEmailRecipients(List<String> toEmails, List<String> toNames, MimeMessageHelper helper, boolean asBcc) throws EmailException {
        String encoding = helper.getEncoding();

        for (int i = 0; i < toEmails.size(); i++) {
            String toEmail = toEmails.get(i);
            InternetAddress toAddr;
            try {
                toAddr = new InternetAddress(toEmail);
            } catch (Exception e) {
                throw new EmailException(e);
            }
            if (toNames != null && toNames.size() == toEmails.size()) {
                String name = clean(toNames.get(i));
                if (StringUtils.isNotBlank(encoding)) {
                    try {
                        toAddr.setPersonal(name, encoding);
                    } catch (Exception e) {
                        throw new EmailException(e);
                    }
                } else {
                    try {
                        toAddr.setPersonal(name);
                    } catch (Exception e) {
                        throw new EmailException(e);
                    }
                }
            }
            try {
                if (asBcc) {
                    helper.addBcc(toAddr);
                } else {
                    helper.addTo(toAddr);
                }
            } catch (Exception e) {
                throw new EmailException(e);
            }
        }

        return helper;
    }

    // /// GETTERS AND SETTERS

    public void setMailService(JavaMailSender mailService) {
        this.mailService = mailService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setMimetypeService(MimetypeService mimetypeService) {
        this.mimetypeService = mimetypeService;
    }

    public void setMessageCopyFolder(String messageCopyFolder) {
        this.messageCopyFolder = messageCopyFolder;
    }

    // /// CLASSES

    private SignatureService getSignatureService() {
        if (_signatureService == null) {
            _signatureService = BeanHelper.getSignatureService();
        }
        return _signatureService;
    }

    public class AlfrescoContentSource implements InputStreamSource {

        protected NodeRef nodeRef;

        public AlfrescoContentSource(NodeRef file) {
            nodeRef = file;
        }

        @Override
        @SuppressWarnings("synthetic-access")
        public InputStream getInputStream() throws IOException {
            return fileFolderService.getReader(nodeRef).getContentInputStream();
        }

    }

}
