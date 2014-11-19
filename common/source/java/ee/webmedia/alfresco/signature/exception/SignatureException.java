package ee.webmedia.alfresco.signature.exception;

/**
 * Checked exception to wrap DigiDocException.
<<<<<<< HEAD
 * 
 * @author Alar Kvell
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class SignatureException extends Exception {
    private static final long serialVersionUID = 1L;

    public SignatureException(String string, Exception e) {
        super(string, e);
    }

    public SignatureException(String string) {
        super(string);
    }

    @Override
    public String getMessage() {
        return super.getMessage() + (getCause() == null ? "" : ": " + getCause().getMessage());
    }

}
