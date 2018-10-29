package ee.webmedia.alfresco.email.service;

import ee.webmedia.alfresco.email.model.EmailAttachment;
import org.alfresco.service.cmr.repository.NodeRef;

import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Common service for sending emails.
 */
public interface EmailService {

    String BEAN_NAME = "CommonEmailService";

    /**
     * Sends out an email with given information and attached files from given document (if provided).
     *
     * @param toEmails  list of email addresses
     * @param toNames   list of email address person names (should be null or the same size as email addresses).
     * @param fromEmail from email address
     * @param subject   mail subject
     * @param content   mail content text
     * @param isHtml    is content html format
     * @param document  email is sent about given document
     * @throws EmailException If e-mail sending fails in JavaMail (or Spring's JavaMail support) layer, not connected to Alfresco
     */
    void sendEmail(List<String> toEmails, List<String> toNames, String fromEmail, String subject, String content, boolean isHtml, NodeRef document,
                   List<EmailAttachment> attachments) throws EmailException;

    /**
     * Sends out an email with given information and attached files from given document (if provided).
     *
     * @param toEmails    list of email addresses
     * @param toNames     list of email address person names (should be null or the same size as email addresses).
     * @param toBccEmails list of email addresses for BCC field
     * @param toBccNames  list of email address person names (should be null or the same size as email addresses) for BCC field.
     * @param fromEmail   from email address
     * @param subject     mail subject
     * @param content     mail content text
     * @param isHtml      is content html format
     * @param document    email is sent about given document
     * @throws EmailException If e-mail sending fails in JavaMail (or Spring's JavaMail support) layer, not connected to Alfresco
     */
    void sendEmail(List<String> toEmails, List<String> toNames, List<String> toBccEmails, List<String> toBccNames, String fromEmail, String subject, String content,
                   boolean isHtml, NodeRef document, List<EmailAttachment> attachments) throws EmailException;

    List<EmailAttachment> getAttachments(List<NodeRef> fileRefs, boolean zipIt, List<X509Certificate> encryptionCertificates, String zipAndEncryptFileTitle) throws Exception;

    long getAttachmentsTotalSize(List<NodeRef> fileRefs, boolean zipIt, boolean encrypt);

}
