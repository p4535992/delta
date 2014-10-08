package ee.webmedia.alfresco.signature.exception;

/**
 * Runtime exception to wrap DigiDocException.
<<<<<<< HEAD
 * 
 * @author Alar Kvell
=======
>>>>>>> develop-5.1
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
