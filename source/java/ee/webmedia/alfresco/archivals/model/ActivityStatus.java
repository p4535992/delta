package ee.webmedia.alfresco.archivals.model;

public enum ActivityStatus {

    IN_PROGRESS("teostamisel"),
    FINISHED("lõpetatud");

    private String value;

    private ActivityStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
