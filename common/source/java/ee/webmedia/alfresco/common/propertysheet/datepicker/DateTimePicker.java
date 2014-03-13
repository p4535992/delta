package ee.webmedia.alfresco.common.propertysheet.datepicker;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.alfresco.util.Pair;

import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Input to divide Date property between date input and time input for editing
 */

public class DateTimePicker extends UIInput {

    public static final String DATE_TIME_PICKER_FAMILY = DateTimePicker.class.getCanonicalName();

    @SuppressWarnings("deprecation")
    @Override
    public Object getConvertedValue(FacesContext context, Object object) {
        // ignore saved submitted value and instead get converted values from child components
        object = getValueFromChildren(context);
        if (object == null || !(object instanceof Pair)) {
            return null;
        }
        Pair<Date, Date> dateTime = (Pair<Date, Date>) object;
        if (dateTime.getFirst() == null) {
            return null;
        }
        Date date = dateTime.getFirst();
        Date time = dateTime.getSecond();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        if (time != null) {
            calendar.set(Calendar.HOUR_OF_DAY, time.getHours());
            calendar.set(Calendar.MINUTE, time.getMinutes());
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
        }
        return calendar.getTime();
    }

    @Override
    public String getFamily() {
        return DATE_TIME_PICKER_FAMILY;
    }

    public Object getValueFromChildren(FacesContext context) {
        List<UIComponent> children = ComponentUtil.getChildren(this);
        Date date = getChildConvertedValue(context, children, 0);
        Date time = getChildConvertedValue(context, children, 1);
        Pair<Date, Date> dateTime = new Pair<Date, Date>(date, time);
        return dateTime;
    }

    private Date getChildConvertedValue(FacesContext context, List<UIComponent> children, int childIndex) {
        UIInput dateInput = (UIInput) children.get(childIndex);
        Date date = (Date) dateInput.getValue();
        return date;
    }

}
