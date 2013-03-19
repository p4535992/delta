package ee.webmedia.alfresco.common.propertysheet.converter;

import java.util.Collection;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.apache.commons.lang.StringUtils;

/**
 * Convert list of non-blank strings to comma separated string.
 * 
 * @author Alar Kvell
 */
public class ListNonBlankStringsWithCommaConverter implements Converter {

    public static final String CONVERTER_ID = ListNonBlankStringsWithCommaConverter.class.getCanonicalName();

    private static final String SEPARATOR = ", ";

    private Converter singleValueConverter = null;

    public Converter getSingleValueConverter() {
        return singleValueConverter;
    }

    public void setSingleValueConverter(Converter singleValueConverter) {
        this.singleValueConverter = singleValueConverter;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {
        if (value == null) {
            return null;
        }
        if (value instanceof Collection) {
            StringBuilder s = new StringBuilder();
            Collection<?> list = (Collection<?>) value;
            for (Object listItem : list) {
                String listItemString = getSingleValueConverted(context, component, listItem);
                if (StringUtils.isNotBlank(listItemString)) {
                    if (s.length() > 0) {
                        s.append(SEPARATOR);
                    }
                    s.append(listItemString);
                }
            }
            return s.toString();
        }
        return getSingleValueConverted(context, component, value);
    }

    private String getSingleValueConverted(FacesContext context, UIComponent component, Object singleValue) {
        String listItemString = null;
        if (singleValueConverter != null) {
            listItemString = singleValueConverter.getAsString(context, component, singleValue);
        } else if (singleValue != null) {
            listItemString = singleValue.toString();
        }
        return listItemString;
    }

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
        throw new RuntimeException("Not used");
    }

}
