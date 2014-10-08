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
