package ee.webmedia.alfresco.docconfig.bootstrap;

/**
 * Systematic Document Types that should exist in every environment
 * 
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

    private String id;

    private SystematicDocumentType(String name) {
        id = name;
    }

    public String getId() {
        return id;
    }

    public boolean isSameType(String otherId) {
        return id.equals(otherId);
    }

    public static SystematicDocumentType of(String id) {
        for (SystematicDocumentType docType : values()) {
            if (docType.isSameType(id)) {
                return docType;
            }
        }
        return null;
    }

}
