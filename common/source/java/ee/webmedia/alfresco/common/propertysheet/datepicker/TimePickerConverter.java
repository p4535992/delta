<<<<<<< HEAD
package ee.webmedia.alfresco.common.propertysheet.datepicker;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.faces.convert.Converter;

public class TimePickerConverter extends DatePickerConverter implements Converter {

    public static final String CONVERTER_ID = TimePickerConverter.class.getCanonicalName();
    public static final String TIME_FORMAT = "HH:mm";
    public static SimpleDateFormat simpleTimeFormat = new SimpleDateFormat(TIME_FORMAT);
    static {
        simpleDateFormat.setLenient(false);
    }

    @Override
    public DateFormat getDateFormat() {
        return simpleTimeFormat;
    }

}
=======
package ee.webmedia.alfresco.common.propertysheet.datepicker;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.faces.convert.Converter;

public class TimePickerConverter extends DatePickerConverter implements Converter {

    public static final String CONVERTER_ID = TimePickerConverter.class.getCanonicalName();
    public static final String TIME_FORMAT = "HH:mm";
    public static SimpleDateFormat simpleTimeFormat = new SimpleDateFormat(TIME_FORMAT);
    static {
        simpleDateFormat.setLenient(false);
    }

    @Override
    public DateFormat getDateFormat() {
        return simpleTimeFormat;
    }

}
>>>>>>> develop-5.1
