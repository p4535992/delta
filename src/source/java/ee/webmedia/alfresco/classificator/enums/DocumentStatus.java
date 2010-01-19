package ee.webmedia.alfresco.classificator.enums;

public enum DocumentStatus {
    WORKING("töös"),
    STOPPED("peatatud"),
    FINISHED("lõpetatud");
    
    private String valueName;
    
    DocumentStatus(String value) {
        this.valueName = value;
    }

    public String getValueName() {
        return valueName;
    }

    public boolean equals(String value) {
        return this.valueName.equals(value);
    }
}
