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
>>>>>>> develop-5.1
