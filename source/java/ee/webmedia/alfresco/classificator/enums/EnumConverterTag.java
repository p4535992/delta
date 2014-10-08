<<<<<<< HEAD
package ee.webmedia.alfresco.classificator.enums;

import javax.faces.convert.Converter;
import javax.faces.webapp.ConverterTag;
import javax.servlet.jsp.JspException;

import ee.webmedia.alfresco.common.propertysheet.converter.EnumTranslationConverter;

/**
 * Tag definition to use the EnumTranslationConverter on a page
 * 
 * @author Vladimir Drozdik
 */
public class EnumConverterTag extends ConverterTag {

    private static final long serialVersionUID = 1L;
    private String enumClass;

    public void setEnumClass(String enumClass) {
        this.enumClass = enumClass;
    }

    /**
     * Default Constructor
     */
    public EnumConverterTag() {
        setConverterId(EnumTranslationConverter.CONVERTER_ID);
    }

    @Override
    protected Converter createConverter() throws JspException {
        EnumTranslationConverter converter = (EnumTranslationConverter) super.createConverter();
        converter.setEnumClass(enumClass);
        return converter;
    }

}
=======
package ee.webmedia.alfresco.classificator.enums;

import javax.faces.convert.Converter;
import javax.faces.webapp.ConverterTag;
import javax.servlet.jsp.JspException;

import ee.webmedia.alfresco.common.propertysheet.converter.EnumTranslationConverter;

/**
 * Tag definition to use the EnumTranslationConverter on a page
 */
public class EnumConverterTag extends ConverterTag {

    private static final long serialVersionUID = 1L;
    private String enumClass;

    public void setEnumClass(String enumClass) {
        this.enumClass = enumClass;
    }

    /**
     * Default Constructor
     */
    public EnumConverterTag() {
        setConverterId(EnumTranslationConverter.CONVERTER_ID);
    }

    @Override
    protected Converter createConverter() throws JspException {
        EnumTranslationConverter converter = (EnumTranslationConverter) super.createConverter();
        converter.setEnumClass(enumClass);
        return converter;
    }

}
>>>>>>> develop-5.1
