<<<<<<< HEAD
package ee.webmedia.alfresco.common.propertysheet.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;

import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Converter that translates enum constant or enum keyword string.
 * 
 * @author Vladimir Drozdik
 */
public class EnumTranslationConverter extends EnumConverter {

    public static final String CONVERTER_ID = EnumTranslationConverter.class.getCanonicalName();

    @Override
    public String getAsString(FacesContext context, UIComponent enumSelect, Object value) throws ConverterException {
        if (value instanceof Enum) {
            return MessageUtil.getMessage((Enum<?>) value);
        }
        try {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            Class<Enum> enumActualClass = (Class<Enum>) Class.forName(getEnumClass());
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Enum enumConstant = Enum.valueOf(enumActualClass, (String) value);
            return MessageUtil.getMessage(enumConstant);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
        throw new RuntimeException("method evaluate(Node node) is unimplemented");
    }
}
=======
package ee.webmedia.alfresco.common.propertysheet.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;

import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Converter that translates enum constant or enum keyword string.
 */
public class EnumTranslationConverter extends EnumConverter {

    public static final String CONVERTER_ID = EnumTranslationConverter.class.getCanonicalName();

    @Override
    public String getAsString(FacesContext context, UIComponent enumSelect, Object value) throws ConverterException {
        if (value instanceof Enum) {
            return MessageUtil.getMessage((Enum<?>) value);
        }
        try {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            Class<Enum> enumActualClass = (Class<Enum>) Class.forName(getEnumClass());
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Enum enumConstant = Enum.valueOf(enumActualClass, (String) value);
            return MessageUtil.getMessage(enumConstant);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
        throw new RuntimeException("method evaluate(Node node) is unimplemented");
    }
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
