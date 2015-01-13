package ee.webmedia.alfresco.common.propertysheet.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.alfresco.service.cmr.repository.StoreRef;

/**
 * JSF converter for {@link StoreRef} class
 */
public class StoreRefConverter implements Converter {

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        return new StoreRef(value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

}
