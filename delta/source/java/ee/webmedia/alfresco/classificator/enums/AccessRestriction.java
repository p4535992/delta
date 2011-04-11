package ee.webmedia.alfresco.classificator.enums;

public enum AccessRestriction {
    OPEN("Avalik"),
    AK("AK"),
    INTERNAL("Majasisene");

    private String valueName;

    AccessRestriction(String value) {
        valueName = value;
    }

    public String getValueName() {
        return valueName;
    }

    public boolean equals(String value) {
        return valueName.equalsIgnoreCase(value);
    }
}
