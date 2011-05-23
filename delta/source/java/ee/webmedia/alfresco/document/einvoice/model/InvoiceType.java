package ee.webmedia.alfresco.document.einvoice.model;

/**
 * enum for classificator invoiceTypes values
 * 
 * @author Riina Tens
 */

public enum InvoiceType {
    ETT("ETT"),
    DEB("DEB"),
    CRE("CRE");

    private String value;

    InvoiceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
