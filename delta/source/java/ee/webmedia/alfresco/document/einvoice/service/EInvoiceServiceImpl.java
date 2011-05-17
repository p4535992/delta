package ee.webmedia.alfresco.document.einvoice.service;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.datatype.XMLGregorianCalendar;

import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.GUID;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.service.AddressbookService;
import ee.webmedia.alfresco.classificator.enums.TransmittalMode;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.einvoice.accountlist.generated.KontoInfo;
import ee.webmedia.alfresco.document.einvoice.accountlist.generated.KontoNimekiri;
import ee.webmedia.alfresco.document.einvoice.dimensionslist.generated.Dimensioon;
import ee.webmedia.alfresco.document.einvoice.dimensionslist.generated.DimensioonideNimekiri;
import ee.webmedia.alfresco.document.einvoice.dimensionslist.generated.Vaartus;
import ee.webmedia.alfresco.document.einvoice.generated.BillPartyRecord;
import ee.webmedia.alfresco.document.einvoice.generated.ContactDataRecord;
import ee.webmedia.alfresco.document.einvoice.generated.EInvoice;
import ee.webmedia.alfresco.document.einvoice.generated.ExtensionRecord;
import ee.webmedia.alfresco.document.einvoice.generated.Invoice;
import ee.webmedia.alfresco.document.einvoice.generated.InvoiceInformation;
import ee.webmedia.alfresco.document.einvoice.generated.InvoiceSumGroup;
import ee.webmedia.alfresco.document.einvoice.model.Dimension;
import ee.webmedia.alfresco.document.einvoice.model.DimensionModel;
import ee.webmedia.alfresco.document.einvoice.model.DimensionValue;
import ee.webmedia.alfresco.document.einvoice.model.Dimensions;
import ee.webmedia.alfresco.document.einvoice.model.Transaction;
import ee.webmedia.alfresco.document.einvoice.model.TransactionModel;
import ee.webmedia.alfresco.document.einvoice.sellerslist.generated.HankijaInfo;
import ee.webmedia.alfresco.document.einvoice.sellerslist.generated.HankijaNimekiri;
import ee.webmedia.alfresco.document.einvoice.vatcodelist.generated.KaibemaksuKoodInfo;
import ee.webmedia.alfresco.document.einvoice.vatcodelist.generated.KaibemaksuKoodNimekiri;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.utils.XmlUtil;

/**
 * @author Riina Tens
 */

public class EInvoiceServiceImpl implements EInvoiceService {

    private static final String PURCHASE_ORDER_SAP_NUMBER_PREFIX = "OT4";

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(EInvoiceServiceImpl.class);

    private AddressbookService addressbookService;
    private UserService userService;
    private DocumentSearchService documentSearchService;
    private FileFolderService fileFolderService;
    private NodeService nodeService;
    private GeneralService generalService;
    private ParametersService parametersService;
    private DocumentTypeService documentTypeService;
    private static final Map<NodeRef, List<DimensionValue>> dimensionValueCache = new ConcurrentHashMap<NodeRef, List<DimensionValue>>();
    private static List<DimensionValue> vatCodeDimesnionValues = null;

    private String dimensionsPath;

    @Override
    public List<NodeRef> importInvoiceFromXml(NodeRef folderNodeRef, InputStream input, TransmittalMode transmittalMode) {
        List<NodeRef> newInvoices = new ArrayList<NodeRef>();

        EInvoice einvoice = EInvoiceUtil.unmarshalEInvoice(input);

        if (einvoice == null) {
            return newInvoices;
        }

        for (Invoice invoice : einvoice.getInvoice()) {
            FileInfo docInfo = fileFolderService.create(folderNodeRef, GUID.generate(), DocumentSubtypeModel.Types.INVOICE);
            NodeRef docRef = docInfo.getNodeRef();
            setDocPropsFromInvoice(invoice, docRef, transmittalMode, true);
            newInvoices.add(docRef);
        }

        return newInvoices;
    }

    @Override
    public void setDocPropsFromInvoice(Invoice invoice, NodeRef docRef, TransmittalMode transmittalMode, boolean setOwnerFromInvoice) {
        Map<QName, Serializable> props = nodeService.getProperties(docRef);
        InvoiceInformation invoiceInformation = invoice.getInvoiceInformation();
        props.put(DocumentCommonModel.Props.DOC_NAME, invoiceInformation.getInvoiceNumber() + " "
                + invoice.getInvoiceItem().getInvoiceItemGroup().get(0).getItemEntry().get(0).getDescription());
        BillPartyRecord sellerParty = invoice.getInvoiceParties().getSellerParty();
        props.put(DocumentSpecificModel.Props.SELLER_PARTY_NAME, sellerParty.getName());
        String regNumber = sellerParty.getRegNumber();
        props.put(DocumentSpecificModel.Props.SELLER_PARTY_REG_NUMBER, regNumber);
        List<Node> contacts = addressbookService.getContactsByRegNumber(regNumber);
        // add sap account only if we get one corresponding contact
        if (contacts.size() == 1) {
            props.put(DocumentSpecificModel.Props.SELLER_PARTY_SAP_ACCOUNT, (Serializable) contacts.get(0).getProperties().get(AddressbookModel.Props.SAP_ACCOUNT));
        }
        ContactDataRecord sellerContactData = sellerParty.getContactData();
        if (sellerContactData != null) {
            props.put(DocumentSpecificModel.Props.SELLER_PARTY_CONTACT_NAME, sellerContactData.getContactName());
            props.put(DocumentSpecificModel.Props.SELLER_PARTY_CONTACT_EMAIL_ADDRESS, sellerContactData.getEMailAddress());
            props.put(DocumentSpecificModel.Props.SELLER_PARTY_CONTACT_PHONE_NUMBER, sellerContactData.getPhoneNumber());
        }
        props.put(DocumentSpecificModel.Props.INVOICE_DATE, XmlUtil.getDate(invoiceInformation.getInvoiceDate()));
        props.put(DocumentSpecificModel.Props.INVOICE_NUMBER, invoiceInformation.getInvoiceNumber());
        props.put(DocumentSpecificModel.Props.INVOICE_TYPE, invoiceInformation.getType().getType());
        props.put(DocumentSpecificModel.Props.INVOICE_DUE_DATE, XmlUtil.getDate(invoiceInformation.getDueDate()));
        props.put(DocumentSpecificModel.Props.PAYMENT_TERM, invoiceInformation.getPaymentTerm());
        List<InvoiceSumGroup> invoiceSumGroups = invoice.getInvoiceSumGroup();
        InvoiceSumGroup invoivceSumGroup = invoiceSumGroups.get(0);
        props.put(DocumentSpecificModel.Props.INVOICE_SUM, invoivceSumGroup.getInvoiceSum());
        props.put(DocumentSpecificModel.Props.VAT, invoivceSumGroup.getTotalVATSum());
        props.put(DocumentSpecificModel.Props.TOTAL_SUM, invoivceSumGroup.getTotalSum());
        props.put(DocumentSpecificModel.Props.CURRENCY, invoivceSumGroup.getCurrency());
        List<ExtensionRecord> extensionRecords = invoice.getAdditionalInformation();
        String purchaseOrderSapNumber = null;
        if (extensionRecords != null) {
            for (ExtensionRecord extensionRecord : extensionRecords) {
                String recordInformationContent = extensionRecord.getInformationContent();
                if (StringUtils.startsWith(recordInformationContent, PURCHASE_ORDER_SAP_NUMBER_PREFIX)) {
                    if (purchaseOrderSapNumber == null) {
                        purchaseOrderSapNumber = recordInformationContent;
                    } else {
                        purchaseOrderSapNumber = purchaseOrderSapNumber + "; " + recordInformationContent;
                    }
                }
            }
        }
        props.put(DocumentSpecificModel.Props.PURCHASE_ORDER_SAP_NUMBER, purchaseOrderSapNumber);
        props.put(DocumentSpecificModel.Props.CONTRACT_NUMBER, invoiceInformation.getContractNumber());
        props.put(DocumentSpecificModel.Props.INVOICE_XML, Boolean.TRUE);

        if (setOwnerFromInvoice) {
            findAndSetInvoiceOwner(invoice, props);
        }
        if (transmittalMode != null) {
            props.put(DocumentSpecificModel.Props.TRANSMITTAL_MODE, transmittalMode.getValueName());
        }
        nodeService.addAspect(docRef, DocumentSpecificModel.Aspects.INVOICE_XML, null);
        nodeService.setProperties(docRef, props);
    }

    private void findAndSetInvoiceOwner(Invoice invoice, Map<QName, Serializable> props) {
        ContactDataRecord buyerContactData = invoice.getInvoiceParties().getBuyerParty().getContactData();
        if (buyerContactData != null) {
            String contactName = buyerContactData.getContactName();
            String contactCode = buyerContactData.getContactPersonCode();
            Map<QName, Serializable> userProps = null;
            if (StringUtils.isNotBlank(contactCode)) {
                userProps = userService.getUserProperties(contactCode);
                userService.setOwnerPropsFromUser(props, userProps);
            }
            if (userProps == null) {
                Pair<String, String> firstNameLastName = UserUtil.splitFirstNameLastName(contactName);
                if (firstNameLastName != null && firstNameLastName.getSecond() != null) {
                    List<NodeRef> users = documentSearchService.searchUsersByFirstNameLastName(firstNameLastName.getFirst(), firstNameLastName.getSecond());
                    if (users.size() == 1) {
                        userProps = nodeService.getProperties(users.get(0));
                        userService.setOwnerPropsFromUser(props, userProps);
                    }
                }
            }
            String contractRegNumber = invoice.getInvoiceInformation().getContractNumber();
            boolean ownerSet = userProps != null;
            if (userProps == null && StringUtils.isNotBlank(contractRegNumber)) {
                List<Document> contracts = documentSearchService.searchContractsByRegNumber(contractRegNumber);
                if (contracts.size() == 1) {
                    setOwnerPropsFromDocument(props, contracts.get(0).getProperties());
                    ownerSet = true;
                }
            }
            if (!ownerSet && StringUtils.isNotBlank(contactName)) {
                props.put(DocumentCommonModel.Props.OWNER_NAME, contactName);
            }
        }
    }

    private void setOwnerPropsFromDocument(Map<QName, Serializable> props, Map<String, Object> docProps) {
        if (docProps != null) {
            props.put(DocumentCommonModel.Props.OWNER_ID, (Serializable) docProps.get(DocumentCommonModel.Props.OWNER_ID));
            props.put(DocumentCommonModel.Props.OWNER_NAME, (Serializable) docProps.get(DocumentCommonModel.Props.OWNER_NAME));
            props.put(DocumentCommonModel.Props.OWNER_JOB_TITLE, (Serializable) docProps.get(DocumentCommonModel.Props.OWNER_JOB_TITLE));
            props.put(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT, (Serializable) docProps.get(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT));
            props.put(DocumentCommonModel.Props.OWNER_EMAIL, (Serializable) docProps.get(DocumentCommonModel.Props.OWNER_EMAIL));
            props.put(DocumentCommonModel.Props.OWNER_PHONE, (Serializable) docProps.get(DocumentCommonModel.Props.OWNER_PHONE));
        }
    }

    @Override
    public List<Dimension> getAllDimensions() {
        NodeRef root = generalService.getNodeRef(dimensionsPath);
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(root);
        List<Dimension> dimensions = new ArrayList<Dimension>(childRefs.size());
        for (ChildAssociationRef childRef : childRefs) {
            Node node = new Node(childRef.getChildRef());
            node.getProperties();
            dimensions.add(new Dimension(node));
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Dimensions found: " + dimensions);
        }
        return dimensions;
    }

    @Override
    public void deleteAllDimensions() {
        NodeRef root = generalService.getNodeRef(dimensionsPath);
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(root);
        List<Dimension> dimensions = new ArrayList<Dimension>(childRefs.size());
        for (ChildAssociationRef childRef : childRefs) {
            nodeService.deleteNode(childRef.getChildRef());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Dimensions found: " + dimensions);
        }
    }

    @Override
    public NodeRef getDimension(Dimensions dimension) {
        String xPath = dimension.toString();
        final NodeRef nodeRef = generalService.getNodeRef(xPath);
        return nodeRef;
    }

    private NodeRef createDimension(String dimensionAssocName, Map<QName, Serializable> properties) {
        Assert.notNull(dimensionAssocName);
        return nodeService.createNode(generalService.getNodeRef(dimensionsPath), DimensionModel.Associations.DIMENSION, EInvoiceUtil.getDimensionAssocQName(dimensionAssocName)
                , DimensionModel.Types.DIMENSION, properties).getChildRef();
    }

    private NodeRef createDimensionValue(NodeRef parentRef, Map<QName, Serializable> properties) {
        return nodeService.createNode(parentRef, DimensionModel.Associations.DIMENSION_VALUE, DimensionModel.Associations.DIMENSION_VALUE
                , DimensionModel.Types.DIMENSION_VALUE, properties).getChildRef();
    }

    @Override
    public NodeRef createTransaction(NodeRef parentRef, Map<QName, Serializable> properties) {
        return nodeService.createNode(parentRef, TransactionModel.Associations.TRANSACTION, TransactionModel.Associations.TRANSACTION
                , TransactionModel.Types.TRANSACTION, properties).getChildRef();
    }

    @Override
    public void updateDimensions(List<Dimension> dimensions) {
        for (Dimension dimension : dimensions) {
            updateDimension(dimension);
        }
    }

    @Override
    public void updateDimension(Dimension dimension) {
        nodeService.setProperty(dimension.getNode().getNodeRef(), DimensionModel.Props.COMMENT, dimension.getComment());
    }

    @Override
    public List<DimensionValue> getAllDimensionValues(NodeRef dimensionRef) {
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(dimensionRef, DimensionModel.Associations.DIMENSION_VALUE,
                RegexQNamePattern.MATCH_ALL);
        List<DimensionValue> dimensionValues = new ArrayList<DimensionValue>(childRefs.size());
        for (ChildAssociationRef childRef : childRefs) {
            Node dimensionValue = new Node(childRef.getChildRef());
            dimensionValue.getProperties();
            dimensionValues.add(new DimensionValue(dimensionValue));
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Dimension values found: " + dimensionValues);
        }
        return dimensionValues;
    }

    @Override
    public List<DimensionValue> getActiveDimensionValues(NodeRef dimension) {
        if (dimensionValueCache.containsKey(dimension)) {
            return dimensionValueCache.get(dimension);
        }
        List<DimensionValue> allDimensionValues = getAllDimensionValues(dimension);
        List<DimensionValue> activeDimensionValues = new ArrayList<DimensionValue>(allDimensionValues.size());
        Date now = new Date();
        for (DimensionValue dimensionValue : allDimensionValues) {
            Date beginDate = dimensionValue.getBeginDateTime();
            Date endDate = dimensionValue.getEndDateTime();
            if (dimensionValue.getActive() && (beginDate == null || DateUtils.isSameDay(beginDate, now) || beginDate.before(now))
                    && (endDate == null || DateUtils.isSameDay(endDate, now) || endDate.after(now))) {
                activeDimensionValues.add(dimensionValue);
            }
        }
        dimensionValueCache.put(dimension, activeDimensionValues);
        return activeDimensionValues;
    }

    @Override
    public List<DimensionValue> getVatCodeDimensionValues() {
        List<DimensionValue> tmpValues = null;
        if (vatCodeDimesnionValues == null) {
            final NodeRef nodeRef = generalService.getNodeRef(Dimensions.TAX_CODE_ITEMS.toString());
            tmpValues = new ArrayList<DimensionValue>(getAllDimensionValues(nodeRef));
            vatCodeDimesnionValues = tmpValues;
        }
        return vatCodeDimesnionValues;
    }

    @Override
    public Collection<NodeRef> importDimensionsList(InputStream input) {
        DimensioonideNimekiri xmlDimensionsList = EInvoiceUtil.unmarshalDimensionsList(input);
        List<NodeRef> result = new ArrayList<NodeRef>();
        if (xmlDimensionsList != null) {
            // TODO: optimize - could empty only cache for updated dimensions
            emptyDimensionValueChache();
            Map<Parameters, Dimensions> dimensionParameters = EInvoiceUtil.DIMENSION_PARAMETERS;
            Map<String, Set<Parameters>> dimParameterValues = parametersService.getSwappedStringParameters(new ArrayList<Parameters>(dimensionParameters.keySet()));
            for (Dimensioon xmlDimension : xmlDimensionsList.getDimensioon()) {
                Set<Parameters> parameters = dimParameterValues.get(xmlDimension.getId());
                if (parameters != null) {
                    for (Parameters parameter : parameters) {
                        @SuppressWarnings("unchecked")
                        Collection<XmlDimensionListsValueWrapper> dimensionListsValueWrappers = CollectionUtils.collect(xmlDimension.getVaartus(), new Transformer() {

                            @Override
                            public Object transform(Object paramObject) {
                                Vaartus value = (Vaartus) paramObject;
                                return new XmlDimensionValueWrapper(value);
                            }

                        });
                        result.add(updateDimensionValuesFromXml(dimensionParameters.get(parameter), dimensionListsValueWrappers, xmlDimension.getId()));
                    }
                }
            }
            // add dimension root nodeRef to indicate successful import in case no dimensions were actually modified
            // (may theoretically occur if xml contains no dimensions that we want to import)
            if (result.size() == 0) {
                result.add(generalService.getNodeRef(dimensionsPath));
            }
        }
        return result;
    }

    @Override
    public Collection<NodeRef> importAccountList(InputStream input) {
        KontoNimekiri accountsList = EInvoiceUtil.unmarshalAccountsList(input);
        List<NodeRef> result = new ArrayList<NodeRef>();
        if (accountsList != null) {
            // TODO: optimize - could empty only cache for account list dimension
            emptyDimensionValueChache();
            @SuppressWarnings("unchecked")
            Collection<XmlDimensionListsValueWrapper> dimensionListsValueWrappers = CollectionUtils.collect(accountsList.getKontoInfo(), new Transformer() {

                @Override
                public Object transform(Object paramObject) {
                    KontoInfo value = (KontoInfo) paramObject;
                    return new XmlAccountValueReader(value);
                }

            });
            result.add(updateDimensionValuesFromXml(EInvoiceUtil.ACCOUNT_DIMENSION, dimensionListsValueWrappers, EInvoiceUtil.ACCOUNT_DIMENSION.getDimensionName()));
        }
        return result;
    }

    @Override
    public Collection<NodeRef> importVatCodeList(InputStream input) {
        KaibemaksuKoodNimekiri vatCodesList = EInvoiceUtil.unmarshalVatCodesList(input);
        List<NodeRef> result = new ArrayList<NodeRef>();
        if (vatCodesList != null) {
            // TODO: optimize - could empty only cache for vat code list dimension
            emptyDimensionValueChache();
            @SuppressWarnings("unchecked")
            Collection<XmlDimensionListsValueWrapper> dimensionListsValueWrappers = CollectionUtils.collect(vatCodesList.getKaibemaksuKoodInfo(), new Transformer() {

                @Override
                public Object transform(Object paramObject) {
                    KaibemaksuKoodInfo value = (KaibemaksuKoodInfo) paramObject;
                    return new XmlVatCodeValueReader(value);
                }

            });
            result.add(updateDimensionValuesFromXml(EInvoiceUtil.VAT_CODE_DIMENSION, dimensionListsValueWrappers, EInvoiceUtil.VAT_CODE_DIMENSION.getDimensionName()));
        }
        return result;
    }

    @Override
    public Collection<NodeRef> importSellerList(InputStream input) {
        HankijaNimekiri sellersList = EInvoiceUtil.unmarshalSellersList(input);
        List<NodeRef> result = new ArrayList<NodeRef>();
        if (sellersList != null) {
            for (HankijaInfo sellerInfo : sellersList.getHankijaInfo()) {
                String xmlRegCode = sellerInfo.getRegistriKood();
                List<Node> contacts = addressbookService.getContactsByRegNumber(xmlRegCode);
                for (Node contact : contacts) {
                    nodeService.setProperty(contact.getNodeRef(), AddressbookModel.Props.SAP_ACCOUNT, sellerInfo.getHankijaNumber());
                }
                if (contacts.size() == 0) {
                    Map<QName, Serializable> contactProps = new HashMap<QName, Serializable>();
                    contactProps.put(AddressbookModel.Props.ACTIVESTATUS, Boolean.TRUE);
                    contactProps.put(AddressbookModel.Props.ORGANIZATION_NAME, sellerInfo.getHankijaNimi());
                    contactProps.put(AddressbookModel.Props.ORGANIZATION_CODE, sellerInfo.getRegistriKood());
                    contactProps.put(AddressbookModel.Props.SAP_ACCOUNT, sellerInfo.getHankijaNumber());
                    addressbookService.createOrganization(contactProps);
                }
            }
            result.add(addressbookService.getAddressbookNodeRef());
        }
        return result;
    }

    @Override
    public Integer updateDocumentsSapAccount() {
        int documentsUpdated = 0;
        List<Document> invoices = documentSearchService.searchInvoicesWithEmptySapAccount();
        List<Node> contacts = addressbookService.getContactsWithSapAccount();
        for (Document invoice : invoices) {
            String documentRegNumber = (String) invoice.getProperties().get(DocumentSpecificModel.Props.SELLER_PARTY_REG_NUMBER);
            for (Node contact : contacts) {
                String contactRegNumber = (String) contact.getProperties().get(AddressbookModel.Props.ORGANIZATION_CODE);
                String contactSapAccount = (String) contact.getProperties().get(AddressbookModel.Props.SAP_ACCOUNT);
                if (StringUtils.isNotBlank(documentRegNumber) && StringUtils.isNotBlank(contactRegNumber) && StringUtils.isNotBlank(contactSapAccount)
                        && documentRegNumber.equalsIgnoreCase(contactRegNumber)) {
                    nodeService.setProperty(invoice.getNodeRef(), DocumentSpecificModel.Props.SELLER_PARTY_SAP_ACCOUNT,
                            contactSapAccount);
                    documentsUpdated++;
                }
            }
        }
        return documentsUpdated;
    }

    @Override
    public boolean isEinvoiceEnabled() {
        return documentTypeService.getDocumentType(DocumentSubtypeModel.Types.INVOICE).isUsed();
    }

    @Override
    public List<Transaction> getInvoiceTransactions(NodeRef invoiceRef) {
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(invoiceRef, TransactionModel.Associations.TRANSACTION,
                RegexQNamePattern.MATCH_ALL);
        List<Transaction> transactions = new ArrayList<Transaction>(childRefs.size());
        for (ChildAssociationRef childRef : childRefs) {
            NodeRef transactionRef = childRef.getChildRef();
            WmNode transaction = new WmNode(transactionRef, TransactionModel.Types.TRANSACTION, RepoUtil.toStringProperties(nodeService.getProperties(transactionRef)),
                    new HashSet<QName>(
                            nodeService.getAspects(transactionRef)));
            transactions.add(new Transaction(transaction));
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Transactions found: " + transactions);
        }
        return transactions;
    }

    @Override
    public void updateTransactions(NodeRef invoiceRef, List<Transaction> transactions, List<Transaction> removedTransactions) {
        for (Transaction removedTransaction : removedTransactions) {
            if (removedTransaction.getNode().getNodeRef() != null) {
                nodeService.deleteNode(removedTransaction.getNode().getNodeRef());
            }
        }
        for (Transaction transaction : transactions) {
            NodeRef nodeRef = transaction.getNode().getNodeRef();
            Map<String, Object> properties = transaction.getNode().getProperties();
            if (transaction.getNode().isUnsaved()) {
                // create new transaction
                createTransaction(invoiceRef, RepoUtil.toQNameProperties(properties));
            } else {
                // update existing transaction
                nodeService.addProperties(nodeRef, RepoUtil.toQNameProperties(properties));
            }
        }
    }

    @Override
    public void updateDimensionValues(List<DimensionValue> dimensionValues, Node selectedDimension) {
        emptyDimensionValueChache();
        // for optimizing update, save only dimensions that have changed
        List<DimensionValue> originalDimensionValues = new ArrayList<DimensionValue>();
        if (selectedDimension != null) {
            originalDimensionValues = getAllDimensionValues(selectedDimension.getNodeRef());
        }
        for (DimensionValue dimensionValue : dimensionValues) {
            if (hasDimensionValueChanged(dimensionValue, originalDimensionValues)) {
                nodeService.setProperties(dimensionValue.getNode().getNodeRef(), RepoUtil.toQNameProperties(dimensionValue.getNode().getProperties()));
            }
        }
    }

    private void emptyDimensionValueChache() {
        dimensionValueCache.clear();
    }

    private boolean hasDimensionValueChanged(DimensionValue dimensionValue, List<DimensionValue> originalDimensionValues) {
        NodeRef dimensionValueRef = dimensionValue.getNode().getNodeRef();
        for (DimensionValue originalDimensionValue : originalDimensionValues) {
            if (dimensionValueRef.equals(originalDimensionValue.getNode().getNodeRef())) {
                return !dimensionValue.equals(originalDimensionValue);
            }
        }
        return false;
    }

    /**
     * NB! It is assumed that xmlDimension is valid as to required fields exist;
     * no null check is performed if xsd states that element is required
     */
    private NodeRef updateDimensionValuesFromXml(Dimensions dimensions, Collection<XmlDimensionListsValueWrapper> xmlDimensionValues, String xmlDimensionId) {
        NodeRef dimensionRef = getOrCreateDimension(dimensions, xmlDimensionId);
        List<DimensionValue> dimensionValues = getAllDimensionValues(dimensionRef);
        // List xmlDimension values that already exist in application
        List<Integer> existingDimensionValues = new ArrayList<Integer>();
        // List dimensions that exist both in xmlDimensions and dimensions
        List<NodeRef> updatedDimensionValues = new ArrayList<NodeRef>();
        int xmlDimValueCounter = 0;
        for (XmlDimensionListsValueWrapper xmlDimensionValue : xmlDimensionValues) {
            for (DimensionValue dimensionValue : dimensionValues) {
                if (xmlDimensionValue.getValueName().equals(dimensionValue.getNode().getProperties().get(DimensionModel.Props.VALUE_NAME))) {
                    Map<QName, Serializable> props = getDimensionValuePropsFromXml(xmlDimensionValue, false);
                    nodeService.addProperties(dimensionValue.getNode().getNodeRef(), props);
                    existingDimensionValues.add(xmlDimValueCounter);
                    updatedDimensionValues.add(dimensionValue.getNode().getNodeRef());
                }
            }
            xmlDimValueCounter++;
        }
        // remove existing dimensions not found in xml
        for (DimensionValue dimensionValue : dimensionValues) {
            if (!updatedDimensionValues.contains(dimensionValue.getNode().getNodeRef())) {
                nodeService.deleteNode(dimensionValue.getNode().getNodeRef());
            }
        }
        // add dimensions from xml that don't exist in application
        xmlDimValueCounter = 0;
        for (XmlDimensionListsValueWrapper xmlDimensionValue : xmlDimensionValues) {
            if (!existingDimensionValues.contains(xmlDimValueCounter)) {
                Map<QName, Serializable> props = getDimensionValuePropsFromXml(xmlDimensionValue, true);
                createDimensionValue(dimensionRef, props);
            }
            xmlDimValueCounter++;
        }
        return dimensionRef;
    }

    private NodeRef getOrCreateDimension(Dimensions dimensions, String xmlDimensionId) {
        NodeRef dimensionRef = getDimension(dimensions);
        if (dimensionRef == null) {
            Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
            properties.put(DimensionModel.Props.NAME, xmlDimensionId);
            properties.put(DimensionModel.Props.COMMENT, null);
            dimensionRef = createDimension(dimensions.getDimensionName(), properties);
        }
        return dimensionRef;
    }

    private Map<QName, Serializable> getDimensionValuePropsFromXml(XmlDimensionListsValueWrapper xmlDimensionValue, boolean newDimension) {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(DimensionModel.Props.VALUE, xmlDimensionValue.getValue());
        props.put(DimensionModel.Props.BEGIN_DATE, XmlUtil.getDate(xmlDimensionValue.getBeginDate()));
        props.put(DimensionModel.Props.END_DATE, XmlUtil.getDate(xmlDimensionValue.getEndDate()));
        if (newDimension) {
            props.put(DimensionModel.Props.VALUE_NAME, xmlDimensionValue.getValueName());
            props.put(DimensionModel.Props.VALUE_COMMENT, null);
            props.put(DimensionModel.Props.ACTIVE, Boolean.TRUE);
            props.put(DimensionModel.Props.DEFAULT_VALUE, Boolean.FALSE);
        }
        return props;
    }

    // START: getters / setters

    public interface XmlDimensionListsValueWrapper {

        String getValue();

        String getValueName();

        XMLGregorianCalendar getBeginDate();

        XMLGregorianCalendar getEndDate();

    }

    public class XmlDimensionValueWrapper implements XmlDimensionListsValueWrapper {

        private final Vaartus value;

        XmlDimensionValueWrapper(Vaartus value) {
            Assert.notNull(value);
            this.value = value;
        }

        @Override
        public String getValue() {
            return value.getNimetus();
        }

        @Override
        public String getValueName() {
            return value.getId();
        }

        @Override
        public XMLGregorianCalendar getBeginDate() {
            return value.getKehtibAlates();
        }

        @Override
        public XMLGregorianCalendar getEndDate() {
            return value.getKehtibKuni();
        }

    }

    public class XmlAccountValueReader implements XmlDimensionListsValueWrapper {

        private final KontoInfo value;

        XmlAccountValueReader(KontoInfo value) {
            Assert.notNull(value);
            this.value = value;
        }

        @Override
        public String getValue() {
            return value.getKaibemaksuKood();
        }

        @Override
        public String getValueName() {
            return value.getKonto();
        }

        @Override
        public XMLGregorianCalendar getBeginDate() {
            return value.getKehtibAlates();
        }

        @Override
        public XMLGregorianCalendar getEndDate() {
            return value.getKehtibKuni();
        }

    }

    public class XmlVatCodeValueReader implements XmlDimensionListsValueWrapper {

        private final KaibemaksuKoodInfo value;

        XmlVatCodeValueReader(KaibemaksuKoodInfo value) {
            Assert.notNull(value);
            this.value = value;
        }

        @Override
        public String getValue() {
            // TODO: format
            return value.getKaibemaksuProtsent().toString();
        }

        @Override
        public String getValueName() {
            return value.getKaibemaksuKood();
        }

        @Override
        public XMLGregorianCalendar getBeginDate() {
            return null;
        }

        @Override
        public XMLGregorianCalendar getEndDate() {
            return null;
        }

    }

    public void setAddressbookService(AddressbookService addressbookService) {
        this.addressbookService = addressbookService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setDimensionsPath(String dimensionsPath) {
        this.dimensionsPath = dimensionsPath;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    public void setDocumentTypeService(DocumentTypeService documentTypeService) {
        this.documentTypeService = documentTypeService;
    }

    // END: getters / setters

}
