package ee.webmedia.alfresco.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Days;
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
        Set<LocalDate> nationalHolidays = getNationalHolidays(classificatorService);
        while (i < workingDaysToAdd) {
            date = addDaysIfNotWorkingDay(date, nationalHolidays);
            date = date.plusDays(1);
            i++;
        }
        // correct number of working days added, check if achieved date is working day and add additional days if needed
        date = addDaysIfNotWorkingDay(date, nationalHolidays);
        return date;
    }

    public static LocalDate addDaysIfNotWorkingDay(LocalDate date, Collection<LocalDate> nationalHolidays) {
        while (!isWorkingDay(date, nationalHolidays)) {
            date = date.plusDays(1);
        }
        return date;
    }

    public static boolean isWorkingDay(LocalDate date, ClassificatorService classificatorService) {
        return isWorkingDay(date, getNationalHolidays(classificatorService));
    }

    private static boolean isWorkingDay(LocalDate date, Collection<LocalDate> holidays) {
        if (date.getDayOfWeek() == DateTimeConstants.SATURDAY || date.getDayOfWeek() == DateTimeConstants.SUNDAY) {
            return false;
        }
        return !isHoliday(date, holidays);
    }

    private static boolean isHoliday(LocalDate date, Collection<LocalDate> holidays) {
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

    private static Set<LocalDate> getNationalHolidays(ClassificatorService classificatorService) {
        List<ClassificatorValue> classificatorValues = classificatorService.getActiveClassificatorValues(classificatorService.getClassificatorByName("nationalHolidays"));
        Set<LocalDate> holidays = new HashSet<LocalDate>();
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

    public static int getDaysBetween(LocalDate beginDate, LocalDate endDate) {
        return Math.abs(Days.daysBetween(beginDate, endDate).getDays()) + 1;
    }

    public static int getNationalHolidaysBetween(LocalDate beginDate, LocalDate endDate, ClassificatorService classificatorService) {
        int days = 0;
        Set<LocalDate> nationalHolidays = getNationalHolidays(classificatorService);
        for (LocalDate nationalHoliday : nationalHolidays) {
            if (!nationalHoliday.isBefore(beginDate) && !nationalHoliday.isAfter(endDate)) {
                days++;
            }
        }
        return days;
    }

    public static int getDaysBetween(LocalDate beginDate, LocalDate endDate, boolean subtractNationalHolidays, ClassificatorService classificatorService) {
        int days = getDaysBetween(beginDate, endDate);
        if (subtractNationalHolidays) {
            days -= getNationalHolidaysBetween(beginDate, endDate, classificatorService);
        }
        return days;
    }

    public static long duration(long startTime, long stopTime) {
        return (stopTime - startTime) / 1000000L;
    }
}
