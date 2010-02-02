package ee.webmedia.alfresco.classificator.enums;

/**
 * Enum constants for KNOWN "storageType" classificator values
 * 
 * @author Ats Uiboupin
 */
public enum StorageType {
    DIGITAL("Digitaalne"),
    PAPER("Paberil");

    private String valueName;

    StorageType(String value) {
        this.valueName = value;
    }

    public String getValueName() {
        return valueName;
    }

    public boolean equals(String value) {
        return this.valueName.equalsIgnoreCase(value);
    }
}
