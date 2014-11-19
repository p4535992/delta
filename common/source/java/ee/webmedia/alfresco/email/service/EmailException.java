package ee.webmedia.alfresco.email.service;

/**
 * Checked exception that can be used to catch e-mail sending failures and resume activity. Thrown when e-mail sending fails in JavaMail (or Spring's JavaMail
 * support) layer, not connected to Alfresco.
<<<<<<< HEAD
 * 
 * @author Alar Kvell
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class EmailException extends Exception {
    private static final long serialVersionUID = 1L;

    public EmailException(String message) {
        super(message);
    }

    public EmailException(Throwable cause) {
        super(cause);
    }

}
