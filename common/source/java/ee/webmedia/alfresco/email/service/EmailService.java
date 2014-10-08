package ee.webmedia.alfresco.email.service;

import java.security.cert.X509Certificate;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.email.model.EmailAttachment;

/**
 * Common service for sending emails.
<<<<<<< HEAD
 * 
 * @author Erko Hansar
=======
>>>>>>> develop-5.1
 */
public interface EmailService {

    String BEAN_NAME = "CommonEmailService";

    /**
     * Sends out an email with given information and attached files from given document (if provided).
     * 
     * @param toEmails list of email addresses
     * @param toNames list of email address person names (should be null or the same size as email addresses).
     * @param fromEmail from email address
     * @param subject mail subject
     * @param content mail content text
     * @param isHtml is content html format
     * @param document email is sent about given document
     * @param fileNodeRefs list of file node refs as strings to match those files which should be sent out as attachments from given document
     * @param zipIt if attachments should be zipped into single file, or sent as separate files
     * @param zipFileName if zipIt then zip file name
     * @throws EmailException If e-mail sending fails in JavaMail (or Spring's JavaMail support) layer, not connected to Alfresco
     */
    void sendEmail(List<String> toEmails, List<String> toNames, String fromEmail, String subject, String content, boolean isHtml, NodeRef document,
            List<EmailAttachment> attachments) throws EmailException;

    /**
     * Sends out an email with given information and attached files from given document (if provided).
     * 
     * @param toEmails list of email addresses
     * @param toNames list of email address person names (should be null or the same size as email addresses).
     * @param toBccEmails list of email addresses for BCC field
     * @param toBccNames list of email address person names (should be null or the same size as email addresses) for BCC field.
     * @param fromEmail from email address
     * @param subject mail subject
     * @param content mail content text
     * @param isHtml is content html format
     * @param document email is sent about given document
     * @param fileNodeRefs list of file node refs as strings to match those files which should be sent out as attachments from given document
     * @param zipIt if attachments should be zipped into single file, or sent as separate files
     * @param zipFileName if zipIt then zip file name
     * @throws EmailException If e-mail sending fails in JavaMail (or Spring's JavaMail support) layer, not connected to Alfresco
     */
    void sendEmail(List<String> toEmails, List<String> toNames, List<String> toBccEmails, List<String> toBccNames, String fromEmail, String subject, String content,
            boolean isHtml, NodeRef document, List<EmailAttachment> attachments) throws EmailException;

    List<EmailAttachment> getAttachments(List<NodeRef> fileRefs, boolean zipIt, List<X509Certificate> encryptionCertificates, String zipAndEncryptFileTitle);

    long getAttachmentsTotalSize(List<NodeRef> fileRefs, boolean zipIt, boolean encrypt);

}
