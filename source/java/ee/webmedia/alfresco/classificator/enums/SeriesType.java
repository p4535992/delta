package ee.webmedia.alfresco.classificator.enums;

/**
 * @author Taimo Peelo (taimo@webmedia.ee)
 */
public enum SeriesType {
    SERIES("sari"),
    SUBSERIES("peatatud");
    
    private String valueName;
    
    SeriesType(String value) {
        this.valueName = value;
    }

    public String getValueName() {
        return valueName;
    }

    public boolean equals(String value) {
        return this.valueName.equalsIgnoreCase(value);
    }
}
