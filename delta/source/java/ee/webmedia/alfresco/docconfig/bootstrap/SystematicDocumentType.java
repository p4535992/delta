package ee.webmedia.alfresco.docconfig.bootstrap;

/**
 * @author Riina Tens
 */
public enum SystematicDocumentType {

    /** Sissetulev kiri */
    INCOMING_LETTER("incomingLetter"),
    /** Väljaminev kiri */
    OUTGOING_LETTER("outgoingLetter"),
    /** Arve */
    INVOICE("invoice"),
    /** Koolitustaotlus */
    TRAINING_APPLICATION("trainingApplication"),
    /** Leping */
    CONTRACT("contract"),
    /** Puhkuse taotlus */
    VACATION_APPLICATION("vacationApplication"),
    /** Välislähetuse korraldus */
    ERRAND_ORDER_ABROAD("errandOrderAbroad"),
    /** Aruanne */
    REPORT("report");

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
