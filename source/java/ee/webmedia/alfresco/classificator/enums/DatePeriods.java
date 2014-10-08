package ee.webmedia.alfresco.classificator.enums;

<<<<<<< HEAD
/**
 * @author Keit Tehvan
 */
=======
>>>>>>> develop-5.1
public enum DatePeriods {
    TODAY("TODAY"),
    YESTERDAY("YESTERDAY"),
    CURRENT_WEEK("CURRENT_WEEK"),
    CURRENT_MONTH("CURRENT_MONTH"),
    PREV_WEEK("PREV_WEEK"),
    FROM_PREV_WEEK("FROM_PREV_WEEK"),
    PREV_MONTH("PREV_MONTH"),
    FROM_PREV_MONTH("FROM_PREV_MONTH"),
    CURRENT_YEAR("CURRENT_YEAR");

    private String valueName;

    DatePeriods(String value) {
        valueName = value;
    }

    public String getValueName() {
        return valueName;
    }

    public boolean equals(String value) {
        return valueName.equalsIgnoreCase(value);
    }

}
