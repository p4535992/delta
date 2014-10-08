<<<<<<< HEAD
package ee.webmedia.alfresco.common.propertysheet.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.MessageUtil;

public class BooleanToLabelConverter implements Converter {

    public static final String CONVERTER_LABEL_PREFIX = "converterLabelPrefix";

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
        if (StringUtils.isBlank(value)) {
            return null;
        } else if (value.equals(getLabel(component, true))) {
            return Boolean.TRUE;
        } else if (value.equals(getLabel(component, false))) {
            return Boolean.FALSE;
        }
        throw new RuntimeException("Invalid value: " + value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {
        if (value == null || !(value instanceof Boolean)) {
            return "";
        }
        return getLabel(component, (Boolean) value);
    }

    private String getLabel(UIComponent component, boolean value) {
        String prefix = (String) component.getAttributes().get(CONVERTER_LABEL_PREFIX);
        String result = MessageUtil.getMessage(prefix + "_" + Boolean.toString(value));
        if (StringUtils.isBlank(result)) {
            throw new RuntimeException("Failed to convert '" + value + "' to string because there are no messages with the key '" + prefix + "_" + Boolean.toString(value) + "'.");
        }
        return result;
    }

}
=======
package ee.webmedia.alfresco.common.propertysheet.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.MessageUtil;

public class BooleanToLabelConverter implements Converter {

    public static final String CONVERTER_LABEL_PREFIX = "converterLabelPrefix";

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
        if (StringUtils.isBlank(value)) {
            return null;
        } else if (value.equals(getLabel(component, true))) {
            return Boolean.TRUE;
        } else if (value.equals(getLabel(component, false))) {
            return Boolean.FALSE;
        }
        throw new RuntimeException("Invalid value: " + value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {
        if (value == null || !(value instanceof Boolean)) {
            return "";
        }
        return getLabel(component, (Boolean) value);
    }

    private String getLabel(UIComponent component, boolean value) {
        String prefix = (String) component.getAttributes().get(CONVERTER_LABEL_PREFIX);
        String result = MessageUtil.getMessage(prefix + "_" + Boolean.toString(value));
        if (StringUtils.isBlank(result)) {
            throw new RuntimeException("Failed to convert '" + value + "' to string because there are no messages with the key '" + prefix + "_" + Boolean.toString(value) + "'.");
        }
        return result;
    }

}
>>>>>>> develop-5.1
