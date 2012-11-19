package ee.webmedia.alfresco.eventplan.model;

/**
 * @author Martti Tamm
 */
public enum FirstEvent {

    REVIEW,
    TRANSFER,
    DESTRUCTION,
    SIMPLE_DESTRUCTION;

    public boolean is(String value) {
        return name().equals(value);
    }

}
