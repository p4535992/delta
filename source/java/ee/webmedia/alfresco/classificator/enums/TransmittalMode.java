<<<<<<< HEAD
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
        valueName = value;
    }

    public String getValueName() {
        return valueName;
    }

    public static String getClassificatorName() {
        return classificatorName;
    }

    public boolean equals(String value) {
        return valueName.equalsIgnoreCase(value);
    }
}
=======
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
        valueName = value;
    }

    public String getValueName() {
        return valueName;
    }

    public static String getClassificatorName() {
        return classificatorName;
    }

    public boolean equals(String value) {
        return valueName.equalsIgnoreCase(value);
    }
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
