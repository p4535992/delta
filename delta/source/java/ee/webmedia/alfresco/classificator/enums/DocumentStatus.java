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
        this.valueName = value;
    }

    public String getValueName() {
        return valueName;
    }

    public boolean equals(String value) {
        return this.valueName.equalsIgnoreCase(value);
    }
}
