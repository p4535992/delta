package ee.webmedia.alfresco.classificator.enums;

<<<<<<< HEAD
/**
 * @author Keit Tehvan
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
