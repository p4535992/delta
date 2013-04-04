package ee.webmedia.alfresco.classificator.enums;

public enum DocumentStatus {
    /** töös */
    WORKING("töös"),
    /** peatatud */
    STOPPED("peatatud"),
    /** lõpetatud */
    FINISHED("lõpetatud");

    private String valueName;

    DocumentStatus(String value) {
        valueName = value;
    }

    public String getValueName() {
        return valueName;
    }
}
