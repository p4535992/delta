package ee.webmedia.alfresco.eventplan.model;

public enum RetaintionStart {

    FROM_CREATION,
    FROM_CLOSING,
    FIXED_DATE,
    FROM_CLOSING_YEAR_END;

    public boolean is(String value) {
        return name().equals(value);
    }

}
