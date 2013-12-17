package ee.webmedia.alfresco.classificator.enums;

public enum DocumentStatus {
    /** töös */
    WORKING("töös"),
    /** @deprecated peatatud - seda staatust enam ei kasutata */
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

    public boolean equals(String value) {
        return valueName.equalsIgnoreCase(value);
    }
}
