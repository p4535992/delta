package ee.webmedia.alfresco.document.einvoice.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.TransmittalMode;
import ee.webmedia.alfresco.document.einvoice.generated.Invoice;
import ee.webmedia.alfresco.document.einvoice.model.Dimension;
import ee.webmedia.alfresco.document.einvoice.model.DimensionValue;
import ee.webmedia.alfresco.document.einvoice.model.Dimensions;
import ee.webmedia.alfresco.document.einvoice.model.Transaction;
import ee.webmedia.alfresco.document.einvoice.model.TransactionDescParameter;
import ee.webmedia.alfresco.document.einvoice.model.TransactionTemplate;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.ContentToSend;
import ee.webmedia.xtee.client.dhl.types.ee.sk.digiDoc.v13.DataFileType;

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

    /**
     * Try to read all dimension's values from cache, if cache is empty, read dimension values from repo
     * using getAllDimensionValuesFromCache and update service cache
     */
    List<DimensionValue> getAllDimensionValuesFromCache(NodeRef dimensionRef);

    /**
     * Read all dimension values from repo; service cache is not updated
     */
    List<DimensionValue> getAllDimensionValuesFromRepo(NodeRef dimensionRef);

    /**
     * Get active dimension values from service cache using getAllDimensionValuesFromCache
     */
    List<DimensionValue> getActiveDimensionValues(NodeRef dimension);

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

    List<Transaction> getInvoiceTransactions(NodeRef invoiceRef);

    void updateDimensionValues(List<DimensionValue> dimensionValues, Node selectedDimension);

    NodeRef createTransaction(NodeRef parentRef, Map<QName, Serializable> properties);

    void updateTransactions(NodeRef invoiceRef, List<Transaction> transactions, List<Transaction> removedTransactions);

    List<DimensionValue> getVatCodeDimensionValues();

    void updateDimension(Dimension dimension);

    /**
     * Modify dataFileList - add new files for transactions containing multiple Arve elements
     * and remove data files that were completely transfered to new transaction files
     * 
     * @return map of invoice corresponding transaction file
     */
    Map<NodeRef, Integer> importTransactionsForInvoices(List<NodeRef> newInvoices, List<DataFileType> dataFileList);

    List<ContentToSend> createContentToSend(File file);

    String getTransactionDvkFolder(Node document);

    File generateTransactionXmlFile(Node node, List<Transaction> transactions) throws IOException;

    /**
     * Retrieve dvk id and SAP document number from xml input (see Liidestused - e-arved for input xml format)
     */
    Pair<String, String> getDocUrlAndErpDocNumber(InputStream input);

    NodeRef updateDocumentEntrySapNumber(String first, String second);

    List<TransactionDescParameter> getAllTransactionDescParameters();

    void updateTransactionDescParameters(List<TransactionDescParameter> transactionDescParameters);

    List<String> getCostManagerMandatoryFields();

    List<String> getOwnerMandatoryFields();

    List<String> getAccountantMandatoryFields();

    boolean isEditableDimension(NodeRef nodeRef);

    void deleteTransactionTemplate(NodeRef transactionTemplateRef);

    List<TransactionTemplate> getAllTransactionTemplates();

    NodeRef updateTransactionTemplate(TransactionTemplate transactionTemplate);

    List<TransactionTemplate> getActiveTransactionTemplates();

    List<String> getActiveTransactionTemplateNames();

    List<Transaction> getTemplateTransactions(String templateName);

    TransactionTemplate getTransactionTemplateByName(String templateName);

    TransactionTemplate createTransactionTemplate(String templateName);

    void removeTransactions(NodeRef nodeRef);

    void copyTransactions(TransactionTemplate template, List<Transaction> transactions);

    void setDimensionValuesActiveOrInactive(Dimension dimension, boolean active);

    DimensionValue getDimensionValue(NodeRef dimensionRef, String transDimensionValue);

}
