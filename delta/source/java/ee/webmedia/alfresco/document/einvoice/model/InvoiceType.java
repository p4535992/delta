package ee.webmedia.alfresco.document.einvoice.model;

/**
 * enum for classificator invoiceTypes known values
 * 
 * @author Riina Tens
 */

public enum InvoiceType {
    ETT("Ettemaksuarve", "ETT", null),
    DEB("Ostuarve", "DEB", "D"),
    CRE("Kreeditarve", "CRE", "K"),
    ETP("Erineva TP arve", "ETP", "KS");

    private String value;
    private String comment;
    private String transactionXmlValue;

    InvoiceType(String value, String comment, String transactionXmlValue) {
        this.value = value;
        this.comment = comment;
        this.transactionXmlValue = transactionXmlValue;
    }

    public String getValue() {
        return value;
    }

    public String getTransactionXmlValue() {
        return transactionXmlValue;
    }

    public static InvoiceType getInvoiceTypeByValueName(String valueName) {
        for (InvoiceType invoiceType : InvoiceType.values()) {
            if (invoiceType.getValue().equalsIgnoreCase(valueName)) {
                return invoiceType;
            }
        }
        return null;
    }

    public static InvoiceType getInvoiceTypeByComment(String comment) {
        for (InvoiceType invoiceType : InvoiceType.values()) {
            if (invoiceType.getComment().equalsIgnoreCase(comment)) {
                return invoiceType;
            }
        }
        return null;
    }

    public String getComment() {
        return comment;
    }
}
