package ee.webmedia.alfresco.signature.exception;

/**
 * Runtime exception to wrap DigiDocException.
<<<<<<< HEAD
 * 
 * @author Alar Kvell
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class SignatureRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SignatureRuntimeException(String string, Exception e) {
        super(string, e);
    }

    @Override
    public String getMessage() {
        return super.getMessage() + (getCause() == null ? "" : ": " + getCause().getMessage());
    }

}
