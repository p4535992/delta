package ee.webmedia.alfresco.classificator.enums;


/**
 * Enum constants for KNOWN "transmittalMode" classificator values
 */
public enum TransmittalMode {
    EMAIL("e-post"),
    DVK("DVK");

    private String valueName;
    private static String classificatorName = "transmittalMode";

    TransmittalMode(String value) {
        this.valueName = value;
    }

    public String getValueName() {
        return valueName;
    }
    
    public static String getClassificatorName() {
        return classificatorName;
    }

    public boolean equals(String value) {
        return this.valueName.equalsIgnoreCase(value);
    }
}
