package ee.webmedia.alfresco.utils;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;

/**
 * Utility class to deal with date and time
 * 
 * @author Riina Tens
 */

public class CalendarUtil {
    private static org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(CalendarUtil.class);

    public static LocalDate addWorkingDaysToDate(LocalDate date, int workingDaysToAdd, ClassificatorService classificatorService) {
        Assert.notNull(date);
        int i = 0;
        List<LocalDate> nationalHolidays = getNationalHolidays(classificatorService);
        while (i < workingDaysToAdd) {
            date = addDaysIfNotWorkingDay(date, nationalHolidays);
            date = date.plusDays(1);
            i++;
        }
        // correct number of working days added, check if achieved date is working day and add additional days if needed
        date = addDaysIfNotWorkingDay(date, nationalHolidays);
        return date;
    }

    public static LocalDate addDaysIfNotWorkingDay(LocalDate date, List<LocalDate> nationalHolidays) {
        while (!isWorkingDay(date, nationalHolidays)) {
            date = date.plusDays(1);
        }
        return date;
    }

    public static boolean isWorkingDay(LocalDate date, ClassificatorService classificatorService) {
        return isWorkingDay(date, getNationalHolidays(classificatorService));
    }

    private static boolean isWorkingDay(LocalDate date, List<LocalDate> holidays) {
        if (date.getDayOfWeek() == DateTimeConstants.SATURDAY || date.getDayOfWeek() == DateTimeConstants.SUNDAY) {
            return false;
        }
        return !isHoliday(date, holidays);
    }

    private static boolean isHoliday(LocalDate date, List<LocalDate> holidays) {
        if (holidays == null) {
            return false;
        }
        for (LocalDate holiday : holidays) {
            if (holiday.toDateMidnight().equals(date.toDateMidnight())) {
                return true;
            }
        }
        return false;
    }

    private static List<LocalDate> getNationalHolidays(ClassificatorService classificatorService) {
        List<ClassificatorValue> classificatorValues = classificatorService.getActiveClassificatorValues(classificatorService.getClassificatorByName("nationalHolidays"));
        List<LocalDate> holidays = new ArrayList<LocalDate>();
        DateTimeFormatter parser = DateTimeFormat.forPattern("dd.MM.yyyy");
        for (ClassificatorValue classificatorValue : classificatorValues) {
            String dateStr = classificatorValue.getValueName();
            try {
                DateTime time = parser.parseDateTime(dateStr);
                holidays.add(time.toLocalDate());
            } catch (Exception e) {
                LOG.error("nationalHolidays classificator value is not in correct format 'dd.MM.yyyy', value='" + dateStr + "', skipping.", e);
            }
        }
        return holidays;
    }
}
