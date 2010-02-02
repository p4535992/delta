package ee.webmedia.alfresco.email.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

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
            List<String> fileNodeRefs, boolean zipIt, String zipFileName) {
        Assert.notEmpty(toEmails, "Parameter toEmails is mandatory.");
        Assert.notNull(fromEmail, "Parameter fromEmail is mandatory.");
        Assert.notNull(subject, "Parameter subject is mandatory.");
        Assert.notNull(content, "Parameter content is mandatory.");

        MimeMessage message = mailService.createMimeMessage();
        MimeMessageHelper helper = null;
        boolean hasFiles = fileNodeRefs != null && fileNodeRefs.size() > 0;
        try {
            helper = new MimeMessageHelper(message, hasFiles, "UTF-8");
            helper.setValidateAddresses(true);
            String encoding = helper.getEncoding();

            helper.setFrom(fromEmail);
            for (int i = 0; i < toEmails.size(); i++) {
                String toEmail = toEmails.get(i);
                InternetAddress toAddr = new InternetAddress(toEmail);
                if (toNames != null && toNames.size() == toEmails.size()) {
                    if (StringUtils.isNotBlank(encoding)) {
                        toAddr.setPersonal(toNames.get(i), encoding);
                    } else {
                        toAddr.setPersonal(toNames.get(i));
                    }
                }
                helper.addTo(toAddr);
            }
            helper.setSubject(subject);
            helper.setText(content, isHtml);

            if (hasFiles) {
                if (zipIt) {
                    ByteArrayOutputStream byteStream = generalService.getZipFileFromFiles(document, fileNodeRefs);
                    helper.addAttachment(zipFileName, new BytesContentSource(byteStream.toByteArray()), "application/zip");
                    byteStream.reset();
                } else {
                    for (FileInfo fileInfo : fileFolderService.listFiles(document)) {
                        if (fileNodeRefs.contains(fileInfo.getNodeRef().toString())) {
                            helper.addAttachment(fileInfo.getName(), new AlfrescoContentSource(fileInfo.getNodeRef()), fileFolderService.getReader(
                                    fileInfo.getNodeRef()).getMimetype());
                        }
                    }
                }
            }

            log.debug("Sending out email: " + Arrays.toString(message.getAllRecipients()));
            mailService.send(message);

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
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

        @SuppressWarnings("synthetic-access")
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(bytes);
        }

    }

}
