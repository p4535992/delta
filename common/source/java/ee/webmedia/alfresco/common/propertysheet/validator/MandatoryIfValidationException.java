<<<<<<< HEAD
package ee.webmedia.alfresco.common.propertysheet.validator;

import javax.faces.event.AbortProcessingException;

/**
 * Marker class to allow different processing of mandatory if validation exception
 * 
 * @author Riina Tens
 */
public class MandatoryIfValidationException extends AbortProcessingException {

    public MandatoryIfValidationException(String string) {
        super(string);
    }

}
=======
package ee.webmedia.alfresco.common.propertysheet.validator;

import javax.faces.event.AbortProcessingException;

/**
 * Marker class to allow different processing of mandatory if validation exception
 */
public class MandatoryIfValidationException extends AbortProcessingException {

    public MandatoryIfValidationException(String string) {
        super(string);
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
