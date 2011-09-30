package ee.webmedia.alfresco.docconfig.bootstrap;

/**
 * @author Riina Tens
 */
public enum SystematicDocumentType {

    /** Arve */
    INVOICE("invoice");

    private String name;

    private SystematicDocumentType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isSameType(String otherName) {
        return name.equals(otherName);
    }

    public static SystematicDocumentType of(String name) {
        for (SystematicDocumentType status : values()) {
            if (status.equals(name)) {
                return status;
            }
        }
        return null;
    }
}
