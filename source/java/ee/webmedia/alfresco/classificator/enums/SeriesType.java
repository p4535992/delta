package ee.webmedia.alfresco.classificator.enums;

<<<<<<< HEAD
/**
 * @author Taimo Peelo
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public enum SeriesType {
    SERIES("sari"),
    SUBSERIES("allsari");

    private String valueName;

    SeriesType(String value) {
        valueName = value;
    }

    public String getValueName() {
        return valueName;
    }

    public boolean equals(String value) {
        return valueName.equalsIgnoreCase(value);
    }
}
