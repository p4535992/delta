package ee.webmedia.alfresco.eventplan.model;

/**
 * @author Martti Tamm
 */
public enum FirstEventStart {

    FROM_CREATION,
    FROM_CLOSING,
    FIXED_DATE,
    FROM_CLOSING_YEAR_END;

    public boolean is(String value) {
        return name().equals(value);
    }

}
