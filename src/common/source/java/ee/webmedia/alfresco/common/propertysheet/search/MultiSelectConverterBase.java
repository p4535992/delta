package ee.webmedia.alfresco.common.propertysheet.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.apache.commons.lang.StringUtils;

/**
 * Base converter for {@link SearchGenerator} that delegates only conversion of single selection values to subclasses
 * @author Ats Uiboupin
 */
public abstract class MultiSelectConverterBase implements Converter {

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
        throw new RuntimeException("Not implemented converting value '" + value + "' to object");
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {
        if (value == null) {
            return null;
        }
        if (value instanceof Collection<?>) {
            Collection<?> sourceList = (Collection<?>) value;
            List<String> targetList = new ArrayList<String>(sourceList.size());
            for (Object source : sourceList) {
                if (source == null) {
                    targetList.add(null);
                } else {
                    targetList.add(convertSelectedValueToString(source));
                }
            }
            return StringUtils.join(targetList.iterator(), ", ");
        }
        // Single value
        return convertSelectedValueToString(value);
    }

    /**
     * @param selectedValue Object value to be converted to String.
     * @return String value based on object <code>selectedValue</code>
     */
    protected abstract String convertSelectedValueToString(Object selectedValue);

}
