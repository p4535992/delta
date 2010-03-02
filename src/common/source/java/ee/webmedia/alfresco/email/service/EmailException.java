package ee.webmedia.alfresco.email.service;

/**
 * Checked exception that can be used to catch e-mail sending failures and resume activity. Thrown when e-mail sending fails in JavaMail (or Spring's JavaMail
 * support) layer, not connected to Alfresco.
 * 
 * @author Alar Kvell
 */
public class EmailException extends Exception {
    private static final long serialVersionUID = 1L;

    public EmailException(Throwable cause) {
        super(cause);
    }

}
