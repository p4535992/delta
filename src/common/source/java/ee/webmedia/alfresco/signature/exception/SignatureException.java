package ee.webmedia.alfresco.signature.exception;

import ee.sk.digidoc.DigiDocException;

/**
 * Checked exception to wrap DigiDocException.
 * 
 * @author Oleg Šelajev.
 */
public class SignatureException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SignatureException(String string, DigiDocException e) {
        super(string, e);
    }

    public SignatureException(String string) {
        super(string);
    }

}
