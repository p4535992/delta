package ee.webmedia.alfresco.eventplan.model;

public enum FirstEvent {

    REVIEW,
    TRANSFER,
    DESTRUCTION,
    SIMPLE_DESTRUCTION;

    public boolean is(String value) {
        return name().equals(value);
    }

}
