package ee.webmedia.alfresco.common.propertysheet.datepicker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

public class DatePickerConverter implements Converter {

    public static final String CONVERTER_ID = "ee.webmedia.alfresco.common.propertysheet.datepicker.DatePickerConverter";
    public static final String DATE_FORMAT = "dd.MM.yyyy";
    
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DatePickerConverter.class);
    public static SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT);

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
        Date date;
        try {
            if(!"".equals(value)) {
                date = simpleDateFormat.parse(value);
            } else {
                return null;
            }
        } catch (ParseException e) {
            log.error("Formatting date failed! Input was:" + value, e);
            throw new ConverterException(e);
        }
        return date;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value != null && value instanceof Date) {
            return simpleDateFormat.format((Date) value);
        }

        // If there is no default value, then field should be blank
        return "";
    }
}
