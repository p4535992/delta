<<<<<<< HEAD
package ee.webmedia.alfresco.common.propertysheet.validator;

import javax.faces.component.UIComponent;
import javax.faces.event.ActionEvent;

/**
 * Marker class to allow different processing of mandatory if validation event
 * 
 * @author Riina Tens
 */
public class MandatoryIfValidationEvent extends ActionEvent {

    private static final long serialVersionUID = 1L;

    public MandatoryIfValidationEvent(UIComponent uiComponent) {
        super(uiComponent);
    }

}
=======
package ee.webmedia.alfresco.common.propertysheet.validator;

import javax.faces.component.UIComponent;
import javax.faces.event.ActionEvent;

/**
 * Marker class to allow different processing of mandatory if validation event
 */
public class MandatoryIfValidationEvent extends ActionEvent {

    private static final long serialVersionUID = 1L;

    public MandatoryIfValidationEvent(UIComponent uiComponent) {
        super(uiComponent);
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
