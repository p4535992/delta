package ee.webmedia.alfresco.classificator.enums;

public enum PublishToAdr {
    /** Läheb ADR-i */
    TO_ADR("Läheb ADR-i"),
    /** Ei lähe ADR-i */
    NOT_TO_ADR("Ei lähe ADR-i"),
    /** Avalik, väljastatakse teabenõude korras */
    REQUEST_FOR_INFORMATION("Avalik, väljastatakse teabenõude korras");

    private String valueName;

    PublishToAdr(String value) {
        valueName = value;
    }

    public String getValueName() {
        return valueName;
    }

    public boolean equals(String value) {
        return valueName.equalsIgnoreCase(value);
    }
}