package ee.webmedia.alfresco.volume.model;

public enum DeletionType {

    DELETE("kustutamine"),
    DISPOSITION("hävitamine");

    private String value;

    DeletionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
