package ee.webmedia.alfresco.signature.exception;

import ee.sk.digidoc.DigiDocException;

/**
 * Checked exception to wrap DigiDocException.
 * 
 * @author Alar Kvell
 */
public class SignatureException extends Exception {
    private static final long serialVersionUID = 1L;

    public SignatureException(String string, DigiDocException e) {
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
