package ee.webmedia.alfresco.document.einvoice.service;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.TransmittalMode;
import ee.webmedia.alfresco.document.einvoice.generated.Invoice;
import ee.webmedia.alfresco.document.einvoice.model.Dimension;
import ee.webmedia.alfresco.document.einvoice.model.DimensionValue;
import ee.webmedia.alfresco.document.einvoice.model.Dimensions;
import ee.webmedia.alfresco.document.einvoice.model.Transaction;

/**
 * Handle e-invoices conversion between document and xml
 * 
 * @author Riina Tens
 */

public interface EInvoiceService {

    String BEAN_NAME = "EInvoiceService";

    /**
     * This method assumes that einvoice xml is valid as to required fields exist
     * (no null check is performed if xsd states that field is required)
     */
    List<NodeRef> importInvoiceFromXml(NodeRef folderNodeRef, InputStream input, TransmittalMode transmittalMode);

    void setDocPropsFromInvoice(Invoice invoice, NodeRef docRef, TransmittalMode transmittalMode, boolean setOwnerFromInvoice);

    List<Dimension> getAllDimensions();

    List<DimensionValue> getAllDimensionValues(NodeRef dimensionRef);

    Collection<NodeRef> importDimensionsList(InputStream input);

    Collection<NodeRef> importVatCodeList(InputStream input);

    Collection<NodeRef> importAccountList(InputStream input);

    Collection<NodeRef> importSellerList(InputStream input);

    /**
     * Return nodeRef or null if dimension doesn't exist
     * 
     * @param dimension
     * @return
     */
    NodeRef getDimension(Dimensions dimension);

    Integer updateDocumentsSapAccount();

    boolean isEinvoiceEnabled();

    void updateDimensions(List<Dimension> dimensions);

    /**
     * Currently used for testing purposes only
     */
    void deleteAllDimensions();

    List<Transaction> getInvoiceTransactions(NodeRef invoiceRef);

    void updateDimensionValues(List<DimensionValue> dimensionValues, Node selectedDimension);

    /**
     * Get active dimension values from service cache
     * 
     * @param dimension
     * @return
     */
    List<DimensionValue> getActiveDimensionValues(NodeRef dimension);

    NodeRef createTransaction(NodeRef parentRef, Map<QName, Serializable> properties);

    void updateTransactions(NodeRef invoiceRef, List<Transaction> transactions, List<Transaction> removedTransactions);

    List<DimensionValue> getVatCodeDimensionValues();

    void updateDimension(Dimension dimension);

}
