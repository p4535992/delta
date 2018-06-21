package ee.webmedia.alfresco.notification.exception;

public class EmailAttachmentSizeLimitException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EmailAttachmentSizeLimitException(String message) {
        super(message);
    }

}
