package ee.webmedia.alfresco.classificator.enums;

/**
 * Enum constants for KNOWN "storageType" classificator values
 */
public enum StorageType {
    DIGITAL("Digitaalne"),
    XML("Xml"),
    PAPER("Paber");

    private String valueName;

    StorageType(String value) {
        valueName = value;
    }

    public String getValueName() {
        return valueName;
    }

    public boolean equals(String value) {
        return valueName.equalsIgnoreCase(value);
    }
}
