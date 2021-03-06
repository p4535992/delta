package ee.webmedia.alfresco.classificator.enums;

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
