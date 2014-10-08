<<<<<<< HEAD
package ee.webmedia.alfresco.common.propertysheet.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;

public class QNameConverter implements Converter {

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
        if (value == null || value.length() == 0) {
            return null;
        }
        return DefaultTypeConverter.INSTANCE.convert(QName.class, value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {
        if (value == null) {
            return null;
        }
        return DefaultTypeConverter.INSTANCE.convert(String.class, value);
    }

}
=======
package ee.webmedia.alfresco.common.propertysheet.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;

public class QNameConverter implements Converter {

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
        if (value == null || value.length() == 0) {
            return null;
        }
        return DefaultTypeConverter.INSTANCE.convert(QName.class, value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {
        if (value == null) {
            return null;
        }
        return DefaultTypeConverter.INSTANCE.convert(String.class, value);
    }

}
>>>>>>> develop-5.1
