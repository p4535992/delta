package ee.webmedia.alfresco.common.propertysheet.converter;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;
import javax.faces.convert.DoubleConverter;

public class DoubleCurrencyConverter extends DoubleConverter {
    
    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        return getAsString(value);
    }

    public String getAsString(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String) {
            return (String) value;
        }
        try {
            double number = ((Number) value).doubleValue();
            number = Math.round(number * 100) / 100.0;
						DecimalFormat format = (DecimalFormat)NumberFormat.getInstance(Locale.US);
						format.applyPattern("0.00");
            return format.format(number);
        } catch (Exception e) {
            throw new ConverterException(e);
        }
    }
    
}
