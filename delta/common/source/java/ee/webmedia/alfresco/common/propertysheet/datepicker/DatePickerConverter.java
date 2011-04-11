package ee.webmedia.alfresco.common.propertysheet.datepicker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.alfresco.web.ui.repo.component.property.UIProperty;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

public class DatePickerConverter implements Converter {

    public static final String CONVERTER_ID = DatePickerConverter.class.getCanonicalName();
    public static final String DATE_FORMAT = "dd.MM.yyyy";

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DatePickerConverter.class);
    public static SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT);
    static {
        simpleDateFormat.setLenient(false);
    }

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
        Date date;
        try {
            if (StringUtils.isNotEmpty(value)) {
                date = simpleDateFormat.parse(value);
            } else {
                return null;
            }
        } catch (ParseException e) {
            log.warn("Formatting date failed! Input was:" + value, e);
            final String propertyLabel = ComponentUtil.getPropertyLabel(ComponentUtil.getAncestorComponent(component, UIProperty.class), "");
            final String msg = MessageUtil.getMessage(context, "validation_date_failed", propertyLabel);
            throw new ConverterException(new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, msg));
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
