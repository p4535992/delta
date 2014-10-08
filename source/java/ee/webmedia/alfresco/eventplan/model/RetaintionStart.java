package ee.webmedia.alfresco.eventplan.model;

<<<<<<< HEAD
/**
 * @author Martti Tamm
 */
=======
>>>>>>> develop-5.1
public enum RetaintionStart {

    FROM_CREATION,
    FROM_CLOSING,
    FIXED_DATE,
    FROM_CLOSING_YEAR_END;

    public boolean is(String value) {
        return name().equals(value);
    }

}
