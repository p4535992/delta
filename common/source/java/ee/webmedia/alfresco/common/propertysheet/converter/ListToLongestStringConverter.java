package ee.webmedia.alfresco.common.propertysheet.converter;

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;

import org.alfresco.web.ui.common.converter.MultiValueConverter;

import ee.webmedia.alfresco.utils.UserUtil;

/**
 * Convert list of strings to longest value in the list
 */
public class ListToLongestStringConverter extends MultiValueConverter {

    public static final String CONVERTER_ID = ListToLongestStringConverter.class.getCanonicalName();

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {
        String result = "";
        if (value instanceof List && !((List) value).isEmpty() && ((List) value).get(0) instanceof String) {
            result = UserUtil.getDisplayUnit((List<String>) value);
        } else if (value != null) {
            result = value.toString();
        }

        return result;
    }
}
