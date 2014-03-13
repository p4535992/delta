package ee.webmedia.alfresco.document.permissions;

/**
 * Enum for permissions
 */
public enum Permission {

    DOCUMENT_WRITE("DocumentWrite");

    private String valueName;

    Permission(String value) {
        valueName = value;
    }

    public String getValueName() {
        return valueName;
    }

    public boolean equals(String value) {
        return valueName.equals(value);
    }

}
