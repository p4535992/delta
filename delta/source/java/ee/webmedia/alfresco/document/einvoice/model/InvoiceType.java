package ee.webmedia.alfresco.document.einvoice.model;

/**
 * enum for classificator invoiceTypes known values
 * 
 * @author Riina Tens
 */

public enum InvoiceType {
    ETT("ETT", null),
    DEB("DEB", "D"),
    CRE("CRE", "K"),
    ETP("ETP", "KS");

    private String value;
    private String transactionXmlValue;

    InvoiceType(String value, String transactionXmlValue) {
        this.value = value;
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
}
