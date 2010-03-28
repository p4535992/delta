package ee.webmedia.alfresco.signature.exception;

import ee.sk.digidoc.DigiDocException;

/**
 * Runtime exception to wrap DigiDocException.
 * 
 * @author Alar Kvell
 */
public class SignatureRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SignatureRuntimeException(String string, DigiDocException e) {
        super(string, e);
    }

    public SignatureRuntimeException(String string, SignatureException e) {
        super(string, e.getCause());
    }

    @Override
    public String getMessage() {
        return super.getMessage() + (getCause() == null ? "" : ": " + getCause().getMessage());
    }

}
