package ee.webmedia.alfresco.eventplan.model;

<<<<<<< HEAD
/**
 * @author Martti Tamm
 */
=======
>>>>>>> develop-5.1
public enum FirstEvent {

    REVIEW,
    TRANSFER,
    DESTRUCTION,
    SIMPLE_DESTRUCTION;

    public boolean is(String value) {
        return name().equals(value);
    }

}
