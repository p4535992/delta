<<<<<<< HEAD
package ee.webmedia.alfresco.common.propertysheet.converter;

import java.util.Date;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.apache.commons.lang.time.FastDateFormat;

/**
 * @author Alar Kvell
 */
public class DateTimeConverter implements Converter {

    public static FastDateFormat dateTimeFormat = FastDateFormat.getInstance("dd.MM.yyyy HH:mm");

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {
        if (value == null || !(value instanceof Date)) {
            return "";
        }
        return dateTimeFormat.format((Date) value);
    }

}
=======
package ee.webmedia.alfresco.common.propertysheet.converter;

import java.util.Date;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.apache.commons.lang.time.FastDateFormat;

public class DateTimeConverter implements Converter {

    public static FastDateFormat dateTimeFormat = FastDateFormat.getInstance("dd.MM.yyyy HH:mm");

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {
        if (value == null || !(value instanceof Date)) {
            return "";
        }
        return dateTimeFormat.format((Date) value);
    }

}
>>>>>>> develop-5.1
