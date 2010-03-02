package ee.webmedia.alfresco.email.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Common service for sending emails.
 * 
 * @author Erko Hansar
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
    void sendEmail(List<String> toEmails, List<String> toNames, String fromEmail, String subject, String content, boolean isHtml, NodeRef document, List<String> fileNodeRefs, boolean zipIt, String zipFileName) throws EmailException;

}
