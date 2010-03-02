package ee.webmedia.alfresco.email.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.service.GeneralService;

/**
 * @author Erko Hansar
 */
public class EmailServiceImpl implements EmailService {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(EmailServiceImpl.class);

    private JavaMailSender mailService;
    private FileFolderService fileFolderService;
    private GeneralService generalService;

    // /// PUBLIC METHODS

    @Override
    public void sendEmail(List<String> toEmails, List<String> toNames, String fromEmail, String subject, String content, boolean isHtml, NodeRef document,
            List<String> fileNodeRefs, boolean zipIt, String zipFileName) throws EmailException {
        Assert.notEmpty(toEmails, "Parameter toEmails is mandatory.");
        Assert.notNull(fromEmail, "Parameter fromEmail is mandatory.");
        Assert.notNull(subject, "Parameter subject is mandatory.");
        Assert.notNull(content, "Parameter content is mandatory.");

        MimeMessage message;
        try {
            message = mailService.createMimeMessage();
        } catch (Exception e) {
            throw new EmailException(e);
        }
        MimeMessageHelper helper;
        String encoding;
        boolean hasFiles = fileNodeRefs != null && fileNodeRefs.size() > 0;
        try {
            helper = new MimeMessageHelper(message, hasFiles, "UTF-8");
            helper.setValidateAddresses(true);
            encoding = helper.getEncoding();
            helper.setFrom(fromEmail);
        } catch (Exception e) {
            throw new EmailException(e);
        }

        for (int i = 0; i < toEmails.size(); i++) {
            String toEmail = toEmails.get(i);
            InternetAddress toAddr;
            try {
                toAddr = new InternetAddress(toEmail);
            } catch (Exception e) {
                throw new EmailException(e);
            }
            if (toNames != null && toNames.size() == toEmails.size()) {
                String name = toNames.get(i);
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
                helper.addTo(toAddr);
            } catch (Exception e) {
                throw new EmailException(e);
            }
        }
        try {
            helper.setSubject(subject);
            helper.setText(content, isHtml);
        } catch (Exception e) {
            throw new EmailException(e);
        }

        if (hasFiles) {
            if (zipIt) {
                ByteArrayOutputStream byteStream = generalService.getZipFileFromFiles(document, fileNodeRefs);
                BytesContentSource contentSource = new BytesContentSource(byteStream.toByteArray());
                try {
                    helper.addAttachment(zipFileName, contentSource, "application/zip");
                } catch (Exception e) {
                    throw new EmailException(e);
                }
                byteStream.reset();
            } else {
                for (FileInfo fileInfo : fileFolderService.listFiles(document)) {
                    if (fileNodeRefs.contains(fileInfo.getNodeRef().toString())) {
                        String name = fileInfo.getName();
                        AlfrescoContentSource contentSource = new AlfrescoContentSource(fileInfo.getNodeRef());
                        String reader = fileFolderService.getReader(fileInfo.getNodeRef()).getMimetype();
                        try {
                            helper.addAttachment(name, contentSource, reader);
                        } catch (AlfrescoRuntimeException e) {
                            throw e;
                        } catch (Exception e) {
                            throw new EmailException(e);
                        }
                    }
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
            mailService.send(message);
        } catch (AlfrescoRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new EmailException(e);
        }
    }

    // /// PRIVATE METHODS

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

    // /// CLASSES

    public class AlfrescoContentSource implements InputStreamSource {

        protected NodeRef nodeRef;

        public AlfrescoContentSource(NodeRef file) {
            this.nodeRef = file;
        }

        @SuppressWarnings("synthetic-access")
        public InputStream getInputStream() throws IOException {
            return fileFolderService.getReader(nodeRef).getContentInputStream();
        }

    }

    public class BytesContentSource implements InputStreamSource {

        protected byte[] bytes;

        public BytesContentSource(byte[] bytes) {
            this.bytes = bytes;
        }

        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(bytes);
        }

    }

}
