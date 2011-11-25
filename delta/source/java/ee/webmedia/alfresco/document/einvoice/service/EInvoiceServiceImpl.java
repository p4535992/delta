package ee.webmedia.alfresco.document.einvoice.service;

import static ee.webmedia.alfresco.document.einvoice.service.EInvoiceUtil.DIMENSION_PROPERTIES;
import static ee.webmedia.alfresco.document.einvoice.service.EInvoiceUtil.sortByDimensionValueName;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.GUID;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.xml.security.exceptions.Base64DecodingException;
import org.apache.xml.security.utils.Base64;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.service.AddressbookService;
import ee.webmedia.alfresco.classificator.enums.StorageType;
import ee.webmedia.alfresco.classificator.enums.TransmittalMode;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.einvoice.account.generated.Arve;
import ee.webmedia.alfresco.document.einvoice.account.generated.ArveInfo;
import ee.webmedia.alfresco.document.einvoice.account.generated.Hankija;
import ee.webmedia.alfresco.document.einvoice.account.generated.Kanne;
import ee.webmedia.alfresco.document.einvoice.account.generated.Konteering;
import ee.webmedia.alfresco.document.einvoice.account.generated.LisaInfo;
import ee.webmedia.alfresco.document.einvoice.account.generated.Ostuarve;
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
import ee.webmedia.alfresco.document.einvoice.model.InvoiceType;
import ee.webmedia.alfresco.document.einvoice.model.Transaction;
import ee.webmedia.alfresco.document.einvoice.model.TransactionDescParameter;
import ee.webmedia.alfresco.document.einvoice.model.TransactionDescParameterModel;
import ee.webmedia.alfresco.document.einvoice.model.TransactionModel;
import ee.webmedia.alfresco.document.einvoice.model.TransactionTemplate;
import ee.webmedia.alfresco.document.einvoice.sellerslist.generated.HankijaInfo;
import ee.webmedia.alfresco.document.einvoice.sellerslist.generated.HankijaNimekiri;
import ee.webmedia.alfresco.document.einvoice.vatcodelist.generated.KaibemaksuKoodInfo;
import ee.webmedia.alfresco.document.einvoice.vatcodelist.generated.KaibemaksuKoodNimekiri;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.utils.XmlUtil;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.ContentToSend;
import ee.webmedia.xtee.client.dhl.types.ee.sk.digiDoc.v13.DataFileType;

/**
 * @author Riina Tens
 */

public class EInvoiceServiceImpl implements EInvoiceService {

    private static final String TRANSACTION_DISPLAY_LABEL_PREFIX = "transaction_";
    private static final String XXL_INVOICE_TEXT = "XXL";
    private static final String EXTENSION_RECORD_TYPE = "LIIK";
    private static final String ERP_NAMESPACE_URI = "erp";
    private static final String XML_FILE_EXTENSION = "xml";
    private static final String PAYMENT_INFO_NAME_TEXT = "MKSelgitus";
    private static final String ACCOUNT_VALUE_YES = "JAH";
    private static final String PURCHASE_ORDER_SAP_NUMBER_PREFIX = "OT4";
    private static final int TRANSACTION_SIZE = 50;

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(EInvoiceServiceImpl.class);

    private AddressbookService addressbookService;
    private UserService userService;
    private DocumentDynamicService documentDynamicService;
    private DocumentSearchService documentSearchService;
    private FileFolderService fileFolderService;
    private NodeService nodeService;
    private GeneralService generalService;
    private ParametersService parametersService;
    private DocumentAdminService documentAdminService;
    private DocumentTemplateService documentTemplateService;
    private FileService fileService;
    private DvkService dvkService;
    private DocumentLogService documentLogService;
    private TransactionService transactionService;
    /**
     * From CL change 165080 code review by Ats Uiboupin:
     * millalgi, kui klasterdamie toe lisamine päevakorda tuleb, siis see ilmselt ei tööta õigesti, kuna puudub võimalus klastri nodede vahel seisu sünkimiseks - aga sellega peab
     * ilmselt siis tegelema, kui klasterdamine aktuaalseks muutub(kasutama echcache vms teeki) ?
     */
    private final Map<NodeRef, List<DimensionValue>> activeDimensionValueCache = new ConcurrentHashMap<NodeRef, List<DimensionValue>>();
    private final Map<NodeRef, List<DimensionValue>> allDimensionValueCache = new ConcurrentHashMap<NodeRef, List<DimensionValue>>();
    private List<DimensionValue> vatCodeDimesnionValues = null;

    private String dimensionsPath;
    private String transactionDescParametersPath;
    private String transactionTemplatesPath;

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
        props.put(DocumentSpecificModel.Props.INVOICE_SUM,
                invoivceSumGroup.getInvoiceSum() != null ? EInvoiceUtil.roundDouble2Decimals(invoivceSumGroup.getInvoiceSum().doubleValue())
                        : null);
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
                if (EXTENSION_RECORD_TYPE.equalsIgnoreCase(extensionRecord.getExtensionId()) && EXTENSION_RECORD_TYPE.equalsIgnoreCase(extensionRecord.getInformationName())
                        && XXL_INVOICE_TEXT.equalsIgnoreCase(extensionRecord.getInformationContent())) {
                    props.put(DocumentSpecificModel.Props.XXL_INVOICE, Boolean.TRUE);
                }
            }
        }
        props.put(DocumentSpecificModel.Props.PURCHASE_ORDER_SAP_NUMBER, purchaseOrderSapNumber);
        props.put(DocumentSpecificModel.Props.CONTRACT_NUMBER, invoiceInformation.getContractNumber());
        props.put(DocumentSpecificModel.Props.INVOICE_XML, Boolean.TRUE);
        props.put(DocumentCommonModel.Props.STORAGE_TYPE, StorageType.XML.getValueName());

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
            Pair<String, String> firstNameLastName = UserUtil.splitFirstNameLastName(contactName);
            Map<QName, Serializable> userProps = null;
            if (StringUtils.isNotBlank(contactCode)) {
                userProps = userService.getUserProperties(contactCode);
                if (UserUtil.hasSameName(firstNameLastName, userProps)) {
                    documentDynamicService.setOwner(props, contactCode, false);
                }
            } else {
                if (firstNameLastName != null && firstNameLastName.getFirst() != null && firstNameLastName.getSecond() != null) {
                    List<NodeRef> users = documentSearchService.searchUsersByFirstNameLastName(firstNameLastName.getFirst(), firstNameLastName.getSecond());
                    if (users.size() == 1) {
                        userProps = nodeService.getProperties(users.get(0));
                        String userName = (String) userProps.get(ContentModel.PROP_USERNAME);
                        documentDynamicService.setOwner(props, userName, false);
                    }
                }
            }
            String contractRegNumber = invoice.getInvoiceInformation().getContractNumber();
            boolean ownerSet = userProps != null;
            if (userProps == null && StringUtils.isNotBlank(contractRegNumber)) {
                List<Document> contracts = documentSearchService.searchContractsByRegNumber(contractRegNumber);
                if (contracts.size() == 1) {
                    Document document = contracts.get(0);
                    String ownerId = document.getOwnerId();
                    Node user = userService.getUser(ownerId);
                    if (user != null) {
                        documentDynamicService.setOwner(props, ownerId, false);
                        ownerSet = true;
                    }
                }
            }
            if (!ownerSet && StringUtils.isNotBlank(contactName)) {
                props.put(DocumentCommonModel.Props.COMMENT, contactName);
            }
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
    public void setDimensionValuesActiveOrInactive(Dimension dimension, boolean active) {
        List<DimensionValue> dimensionValues = getAllDimensionValuesFromRepo(dimension.getNode().getNodeRef());
        RetryingTransactionHelper retryingTransactionHelper = transactionService.getRetryingTransactionHelper();
        Iterator<DimensionValue> i = dimensionValues.iterator();
        SetActiveOrInactiveDimensionCallback setActiveOrInactiveDimensionCallback = new SetActiveOrInactiveDimensionCallback(i, active);
        try {
            while (i.hasNext()) {
                retryingTransactionHelper.doInTransaction(setActiveOrInactiveDimensionCallback, false, true);
            }
        } finally {
            emptyDimensionValueChache();
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
    public List<DimensionValue> getAllDimensionValuesFromCache(NodeRef dimensionRef) {
        // may occur if dimensions are not imported and dimension path doesn't exist
        if (dimensionRef == null) {
            return new ArrayList<DimensionValue>();
        }
        if (!allDimensionValueCache.containsKey(dimensionRef)) {
            getAllDimensionValuesFromRepo(dimensionRef);
        }
        return allDimensionValueCache.get(dimensionRef);
    }

    @Override
    public List<DimensionValue> getAllDimensionValuesFromRepo(NodeRef dimensionRef) {
        long startTime = System.currentTimeMillis();
        Assert.notNull(dimensionRef);
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(dimensionRef, DimensionModel.Associations.DIMENSION_VALUE,
                RegexQNamePattern.MATCH_ALL);
        List<DimensionValue> dimensionValues = new ArrayList<DimensionValue>(childRefs.size());
        for (ChildAssociationRef childRef : childRefs) {
            NodeRef dimensionValueRef = childRef.getChildRef();
            WmNode dimensionValue = new WmNode(dimensionValueRef, DimensionModel.Types.DIMENSION_VALUE, RepoUtil.toStringProperties(nodeService.getProperties(dimensionValueRef)),
                    new HashSet<QName>(
                            nodeService.getAspects(dimensionValueRef)));
            dimensionValues.add(new DimensionValue(dimensionValue));
        }
        allDimensionValueCache.put(dimensionRef, Collections.unmodifiableList(dimensionValues));
        if (LOG.isDebugEnabled()) {
            LOG.debug("Loading dimensionValues from repo for dimensionRef = " + dimensionRef.getId() + ", time = " + (System.currentTimeMillis() - startTime)
                    + " ms, results size = " + dimensionValues.size());
        }
        return dimensionValues;
    }

    private List<DimensionValue> getActiveDimensionValues(NodeRef dimension) {
        if (activeDimensionValueCache.containsKey(dimension)) {
            return activeDimensionValueCache.get(dimension);
        }
        List<DimensionValue> allDimensionValues = getAllDimensionValuesFromCache(dimension);
        List<DimensionValue> activeDimensionValues = new ArrayList<DimensionValue>(allDimensionValues.size());
        for (DimensionValue dimensionValue : allDimensionValues) {
            if (dimensionValue.getActive()) {
                activeDimensionValues.add(dimensionValue);
            }
        }
        sortByDimensionValueName(activeDimensionValues);
        activeDimensionValueCache.put(dimension, Collections.unmodifiableList(activeDimensionValues));
        return activeDimensionValues;
    }

    @Override
    public List<DimensionValue> searchDimensionValues(final String searchString, NodeRef dimensionRef, Date entryDate, boolean activeOnly) {
        List<DimensionValue> dimensionValues;
        if (activeOnly) {
            dimensionValues = getActiveDimensionValues(dimensionRef);
        } else {
            dimensionValues = getAllDimensionValuesFromCache(dimensionRef);
        }
        long startTime = System.currentTimeMillis();
        List<DimensionValue> result = new ArrayList<DimensionValue>();
        for (DimensionValue dimensionValue : dimensionValues) {
            if (StringUtils.isEmpty(searchString) || StringUtils.containsIgnoreCase(dimensionValue.getValueName(), searchString)
                        || StringUtils.containsIgnoreCase(dimensionValue.getValue(), searchString)) {
                if (EInvoiceUtil.isDateInPeriod(entryDate, dimensionValue.getBeginDateTime(), dimensionValue.getEndDateTime())) {
                    result.add(dimensionValue);
                }
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Searching dimensionValues from cache for dimensionRef = " + dimensionRef.getId() + ", time = " + (System.currentTimeMillis() - startTime)
                    + " ms, search set size = " + dimensionValues.size() + ", results size = " + dimensionValues.size());
        }
        return result;
    }

    @Override
    public List<DimensionValue> getVatCodeDimensionValues() {
        List<DimensionValue> tmpValues = null;
        if (vatCodeDimesnionValues == null) {
            final NodeRef nodeRef = generalService.getNodeRef(Dimensions.TAX_CODE_ITEMS.toString());
            tmpValues = new ArrayList<DimensionValue>(getAllDimensionValuesFromCache(nodeRef));
            vatCodeDimesnionValues = Collections.unmodifiableList(tmpValues);
        }
        return vatCodeDimesnionValues;
    }

    @Override
    public List<TransactionDescParameter> getAllTransactionDescParameters() {
        List<ChildAssociationRef> childRefs = getAllTransactionDescparametersAssocRefs();
        List<TransactionDescParameter> transactionDescParameters = new ArrayList<TransactionDescParameter>(childRefs.size());
        for (ChildAssociationRef childRef : childRefs) {
            Node node = new Node(childRef.getChildRef());
            node.getProperties();
            transactionDescParameters.add(new TransactionDescParameter(node));
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("TransactionDescParameters found: " + transactionDescParameters);
        }
        return transactionDescParameters;
    }

    private List<ChildAssociationRef> getAllTransactionDescparametersAssocRefs() {
        NodeRef root = generalService.getNodeRef(transactionDescParametersPath);
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(root);
        return childRefs;
    }

    @Override
    public List<String> getCostManagerMandatoryFields() {
        List<ChildAssociationRef> transactionDescParameters = getAllTransactionDescparametersAssocRefs();
        List<String> mandatoryForCostManager = new ArrayList<String>();
        for (ChildAssociationRef transactionDescParameter : transactionDescParameters) {
            if (Boolean.TRUE.equals(nodeService.getProperty(transactionDescParameter.getChildRef(), TransactionDescParameterModel.Props.MANDATORY_FOR_COST_MANAGER))) {
                mandatoryForCostManager.add(transactionDescParameter.getQName().getLocalName());
            }
        }
        return mandatoryForCostManager;
    }

    @Override
    public List<String> getOwnerMandatoryFields() {
        List<ChildAssociationRef> transactionDescParameters = getAllTransactionDescparametersAssocRefs();
        List<String> mandatoryForOwner = new ArrayList<String>();
        for (ChildAssociationRef transactionDescParameter : transactionDescParameters) {
            if (Boolean.TRUE.equals(nodeService.getProperty(transactionDescParameter.getChildRef(), TransactionDescParameterModel.Props.MANDATORY_FOR_OWNER))) {
                mandatoryForOwner.add(transactionDescParameter.getQName().getLocalName());
            }
        }
        return mandatoryForOwner;
    }

    @Override
    public List<String> getAccountantMandatoryFields() {
        List<ChildAssociationRef> transactionDescParameters = getAllTransactionDescparametersAssocRefs();
        List<String> mandatoryForAccountant = new ArrayList<String>();
        for (ChildAssociationRef transactionDescParameter : transactionDescParameters) {
            if (Boolean.TRUE.equals(nodeService.getProperty(transactionDescParameter.getChildRef(), TransactionDescParameterModel.Props.MANDATORY_FOR_ACCOUNTANT))) {
                mandatoryForAccountant.add(transactionDescParameter.getQName().getLocalName());
            }
        }
        return mandatoryForAccountant;
    }

    @Override
    public void updateTransactionDescParameters(List<TransactionDescParameter> transactionDescParameters) {
        for (TransactionDescParameter transactionDescParameter : transactionDescParameters) {
            nodeService.setProperties(transactionDescParameter.getNode().getNodeRef(), RepoUtil.toQNameProperties(transactionDescParameter.getNode().getProperties()));
        }
    }

    @Override
    public List<TransactionTemplate> getAllTransactionTemplates() {
        NodeRef root = generalService.getNodeRef(transactionTemplatesPath);
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(root);
        List<TransactionTemplate> transactionTemplates = new ArrayList<TransactionTemplate>(childRefs.size());
        for (ChildAssociationRef childRef : childRefs) {
            NodeRef nodeRef = childRef.getChildRef();
            WmNode node = new WmNode(nodeRef, TransactionModel.Types.TRANSACTION_TEMPLATE, RepoUtil.toStringProperties(nodeService.getProperties(nodeRef)),
                    new HashSet<QName>(
                            nodeService.getAspects(nodeRef)));
            transactionTemplates.add(new TransactionTemplate(node));
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Dimensions found: " + transactionTemplates);
        }
        return transactionTemplates;
    }

    @Override
    public List<TransactionTemplate> getActiveTransactionTemplates() {
        List<TransactionTemplate> allTransactionTemplates = getAllTransactionTemplates();
        List<TransactionTemplate> activeTransactionTemplates = new ArrayList<TransactionTemplate>();
        for (TransactionTemplate transactionTemplate : allTransactionTemplates) {
            if (Boolean.TRUE.equals(transactionTemplate.getActive())) {
                activeTransactionTemplates.add(transactionTemplate);
            }
        }
        return activeTransactionTemplates;
    }

    @Override
    public List<String> getActiveTransactionTemplateNames() {
        List<TransactionTemplate> allTransactionTemplates = getAllTransactionTemplates();
        List<String> activeTransactionTemplates = new ArrayList<String>();
        for (TransactionTemplate transactionTemplate : allTransactionTemplates) {
            if (Boolean.TRUE.equals(transactionTemplate.getActive())) {
                activeTransactionTemplates.add(transactionTemplate.getName());
            }
        }
        return activeTransactionTemplates;
    }

    @Override
    public List<Transaction> getTemplateTransactions(String templateName) {
        List<Transaction> transactions = new ArrayList<Transaction>();
        TransactionTemplate template = getTransactionTemplateByName(templateName);
        if (template != null) {
            transactions.addAll(getInvoiceTransactions(template.getNode().getNodeRef()));
        }
        return transactions;
    }

    @Override
    public TransactionTemplate getTransactionTemplateByName(String templateName) {
        List<TransactionTemplate> transactionTemplates = getAllTransactionTemplates();
        for (TransactionTemplate transactionTemplate : transactionTemplates) {
            if (StringUtils.equalsIgnoreCase(transactionTemplate.getName(), templateName)) {
                return transactionTemplate;
            }
        }
        return null;
    }

    @Override
    public NodeRef updateTransactionTemplate(TransactionTemplate transactionTemplate) {
        Map<QName, Serializable> qNameProperties = RepoUtil.toQNameProperties(transactionTemplate.getNode().getProperties());
        if (transactionTemplate.getNode().isUnsaved()) {
            return createTransactionTemplate(qNameProperties);
        } else {
            nodeService.addProperties(transactionTemplate.getNode().getNodeRef(), qNameProperties);
            return transactionTemplate.getNode().getNodeRef();
        }
    }

    @Override
    public TransactionTemplate createTransactionTemplate(String templateName) {
        Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
        properties.put(TransactionModel.Props.NAME, templateName);
        properties.put(TransactionModel.Props.ACTIVE, Boolean.TRUE);
        NodeRef nodeRef = createTransactionTemplate(properties);
        WmNode node = new WmNode(nodeRef, TransactionModel.Types.TRANSACTION_TEMPLATE, RepoUtil.toStringProperties(nodeService.getProperties(nodeRef)),
                new HashSet<QName>(
                        nodeService.getAspects(nodeRef)));
        return new TransactionTemplate(node);
    }

    private NodeRef createTransactionTemplate(Map<QName, Serializable> properties) {
        return nodeService.createNode(generalService.getNodeRef(transactionTemplatesPath), TransactionModel.Associations.TRANSACTION_TEMPLATE,
                TransactionModel.Associations.TRANSACTION_TEMPLATE
                , TransactionModel.Types.TRANSACTION_TEMPLATE, properties).getChildRef();
    }

    @Override
    public void removeTransactions(NodeRef nodeRef) {
        List<Transaction> transactions = getInvoiceTransactions(nodeRef);
        for (Transaction transaction : transactions) {
            nodeService.deleteNode(transaction.getNode().getNodeRef());
        }
    }

    @Override
    public void copyTransactions(TransactionTemplate template, List<Transaction> transactions) {
        if (transactions == null) {
            return;
        }
        NodeRef parentRef = template.getNode().getNodeRef();
        copyTransactions(transactions, parentRef);
    }

    private void copyTransactions(List<Transaction> transactions, NodeRef parentRef) {
        for (Transaction transaction : transactions) {
            Map<QName, Serializable> newProps = new HashMap<QName, Serializable>();
            EInvoiceUtil.copyTransactionProperties(transaction, newProps);
            createTransaction(parentRef, newProps);
        }
    }

    @Override
    public Collection<NodeRef> importDimensionsList(InputStream input) {
        DimensioonideNimekiri xmlDimensionsList = EInvoiceUtil.unmarshalDimensionsList(input);
        List<NodeRef> result = new ArrayList<NodeRef>();
        if (xmlDimensionsList != null) {
            @SuppressWarnings("unchecked")
            Map<Parameters, Dimensions> dimensionParameters = EInvoiceUtil.DIMENSION_PARAMETERS;
            Map<String, Set<Parameters>> dimParameterValues = parametersService.getSwappedStringParameters(new ArrayList<Parameters>(dimensionParameters.keySet()));
            try {
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
            } finally {
                // TODO: optimize - could empty only cache for updated dimensions
                emptyDimensionValueChache();
            }
        }
        return result;
    }

    @Override
    public Collection<NodeRef> importAccountList(InputStream input) {
        KontoNimekiri accountsList = EInvoiceUtil.unmarshalAccountsList(input);
        List<NodeRef> result = new ArrayList<NodeRef>();
        if (accountsList != null) {
            @SuppressWarnings("unchecked")
            Collection<XmlDimensionListsValueWrapper> dimensionListsValueWrappers = CollectionUtils.collect(accountsList.getKontoInfo(), new Transformer() {

                @Override
                public Object transform(Object paramObject) {
                    KontoInfo value = (KontoInfo) paramObject;
                    return new XmlAccountValueReader(value);
                }

            });
            try {
                result.add(updateDimensionValuesFromXml(EInvoiceUtil.ACCOUNT_DIMENSION, dimensionListsValueWrappers, EInvoiceUtil.ACCOUNT_DIMENSION.getDimensionName()));
            } finally {
                // TODO: optimize - could empty only cache for account list dimension
                emptyDimensionValueChache();
            }
        }
        return result;
    }

    @Override
    public Collection<NodeRef> importVatCodeList(InputStream input) {
        KaibemaksuKoodNimekiri vatCodesList = EInvoiceUtil.unmarshalVatCodesList(input);
        List<NodeRef> result = new ArrayList<NodeRef>();
        if (vatCodesList != null) {
            @SuppressWarnings("unchecked")
            Collection<XmlDimensionListsValueWrapper> dimensionListsValueWrappers = CollectionUtils.collect(vatCodesList.getKaibemaksuKoodInfo(), new Transformer() {

                @Override
                public Object transform(Object paramObject) {
                    KaibemaksuKoodInfo value = (KaibemaksuKoodInfo) paramObject;
                    return new XmlVatCodeValueReader(value);
                }

            });
            try {
                result.add(updateDimensionValuesFromXml(EInvoiceUtil.VAT_CODE_DIMENSION, dimensionListsValueWrappers, EInvoiceUtil.VAT_CODE_DIMENSION.getDimensionName()));
                // TODO: optimize - could empty only cache for vat code list dimension
            } finally {
                emptyDimensionValueChache();
            }
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
            result.add(addressbookService.getAddressbookRoot());
        }
        return result;
    }

    @Override
    public Map<NodeRef, Integer> importTransactionsForInvoices(List<NodeRef> newInvoices, List<DataFileType> dataFileList) {
        Map<NodeRef, Integer> transactionDataFileIndexes = new HashMap<NodeRef, Integer>();
        List<Node> invoices = getInvoiceNodes(newInvoices);
        List<DataFileType> newDataFiles = new ArrayList<DataFileType>();
        int dataFileIndex = 0;
        for (Iterator<DataFileType> i = dataFileList.iterator(); i.hasNext();) {
            DataFileType dataFile = i.next();
            if (dvkService.isXmlMimetype(dataFile)) {
                InputStream input;
                try {
                    input = new ByteArrayInputStream(Base64.decode(dataFile.getStringValue()));
                } catch (Base64DecodingException e) {
                    LOG.debug("Unable to decode file " + dataFile.getFilename(), e);
                    continue;
                }
                Ostuarve ostuarve = EInvoiceUtil.unmarshalAccount(input);
                if (ostuarve != null) {
                    int matchedArveCount = 0;
                    int currentArveIndex = 0;
                    int arveCount = ostuarve.getArve().size();
                    List<Arve> originalArve = new ArrayList<Arve>(ostuarve.getArve());
                    // iterate over originalArve so we can change ostuarve content during iteration
                    // for generating different transaction dataFileContents
                    for (Arve arve : originalArve) {
                        for (Node invoice : invoices) {
                            Map<String, Object> props = invoice.getProperties();
                            String invoiceNumber = (String) props.get(DocumentSpecificModel.Props.INVOICE_NUMBER);
                            Date invoiceDate = (Date) props.get(DocumentSpecificModel.Props.INVOICE_DATE);
                            String sellerName = (String) props.get(DocumentSpecificModel.Props.SELLER_PARTY_NAME);
                            // may be empty by xsd
                            String sellerRegNumber = (String) props.get(DocumentSpecificModel.Props.SELLER_PARTY_REG_NUMBER);
                            ArveInfo arveInfo = arve.getArveInfo();
                            String registrikood = arve.getHankija().getRegistrikood();
                            Date xmlInvoiceDate = XmlUtil.getDate(arveInfo.getArveKuupaev());
                            if (invoiceNumber.equalsIgnoreCase(arveInfo.getArveNumber())
                                    && (invoiceDate != null && xmlInvoiceDate != null && DateUtils.isSameDay(invoiceDate, xmlInvoiceDate))
                                    && sellerName.equalsIgnoreCase(arve.getHankija().getHankijaNimi())
                                    && ((StringUtils.isBlank(sellerRegNumber) && StringUtils.isBlank(registrikood)) || (sellerRegNumber != null && sellerRegNumber
                                            .equalsIgnoreCase(registrikood)))) {
                                // if ostuarve contains one arve that maches invoice, use original DataFileType as transactions file
                                // otherwise generate separate DataFileType for each matched arve element
                                if (arveCount == 1) {
                                    transactionDataFileIndexes.put(invoice.getNodeRef(), dataFileIndex);
                                } else {
                                    DataFileType newDataFile = (DataFileType) dataFile.copy();
                                    int arveIndex = 0;
                                    for (Iterator<Arve> it = ostuarve.getArve().iterator(); i.hasNext();) {
                                        if (arveIndex != currentArveIndex) {
                                            it.remove();
                                        }
                                        arveIndex++;
                                    }
                                    ostuarve.setArveidKokku(BigInteger.ONE);
                                    Writer writer = new StringWriter();
                                    EInvoiceUtil.marshalAccount(ostuarve, writer);
                                    Ostuarve testOstuarve = EInvoiceUtil.unmarshalAccount(new StringReader(writer.toString()));
                                    newDataFile.setStringValue(writer.toString());
                                    newDataFiles.add(newDataFile);

                                    // restore original ostuarve
                                    ostuarve.getArve().clear();
                                    ostuarve.getArve().addAll(originalArve);
                                    ostuarve.setArveidKokku(BigInteger.valueOf(arveCount));

                                    transactionDataFileIndexes.put(invoice.getNodeRef(), dataFileList.size() + newDataFiles.size() - 1);
                                }
                                matchedArveCount++;
                            }
                        }
                        currentArveIndex++;
                    }
                    // if ostuarve contained more than one arve and all of arve found matched invoice,
                    // separate DataFileType was added for each arve, so original DataFileType must be removed
                    if (matchedArveCount == arveCount && arveCount > 1) {
                        i.remove();
                    }
                }
            }
            dataFileIndex++;
        }
        dataFileList.addAll(newDataFiles);
        return transactionDataFileIndexes;
    }

    private List<Node> getInvoiceNodes(List<NodeRef> newInvoices) {
        List<Node> invoices = new ArrayList<Node>();
        for (NodeRef invoiceRef : newInvoices) {
            invoices.add(new Node(invoiceRef));
        }
        return invoices;
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
    public Integer deleteUnusedDimensionValues() {
        Date nowMinus90Days = DateUtils.addDays(new Date(), -90);
        List<Pair<Dimensions, DimensionValue>> expiredValues = new ArrayList<Pair<Dimensions, DimensionValue>>();
        for (Dimensions dimension : Dimensions.values()) {
            NodeRef dimensionRef = getDimension(dimension);
            if (dimensionRef != null) {
                for (DimensionValue dimensionValue : getAllDimensionValuesFromRepo(dimensionRef)) {
                    Date endDate = dimensionValue.getEndDateTime();
                    if (endDate != null && endDate.before(nowMinus90Days)) {
                        expiredValues.add(new Pair<Dimensions, DimensionValue>(dimension, dimensionValue));
                    }
                }
            }
        }
        int deletedNodeCount = 0;
        for (Pair<Dimensions, DimensionValue> dimensionWithValue : expiredValues) {
            Dimensions dimension = dimensionWithValue.getFirst();
            DimensionValue dimensionValue = dimensionWithValue.getSecond();
            List<QName> dimensionProperties = EInvoiceUtil.getDimensionProperties(dimension);
            boolean isMatch = false;
            if (!dimensionProperties.isEmpty()) {
                isMatch = documentSearchService.isMatch(joinQueryPartsAnd(Arrays.asList(
                        generateTypeQuery(TransactionModel.Types.TRANSACTION),
                        generateStringExactQuery(dimensionValue.getValueName(), dimensionProperties.toArray(new QName[dimensionProperties.size()]))
                        )), true, "searchTransactionsByDimensionValue");
            }
            if (!isMatch) {
                nodeService.deleteNode(dimensionValue.getNode().getNodeRef());
                deletedNodeCount++;
            }
        }
        if (!expiredValues.isEmpty()) {
            emptyDimensionValueChache();
        }
        return deletedNodeCount;
    }

    @Override
    public boolean isEinvoiceEnabled() {
        // FIXME DLSeadist - Kui kõik süsteemsed dok.liigid on defineeritud, siis võib null kontrolli eemdaldada
        NodeRef docTypeRef = documentAdminService.getDocumentTypeRef("invoice");
        return docTypeRef != null && documentAdminService.getDocumentTypeProperty(docTypeRef, DocumentAdminModel.Props.USED, Boolean.class);
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
        boolean isDocumentParent = !TransactionModel.Types.TRANSACTION_TEMPLATE.equals(nodeService.getType(invoiceRef));
        for (Transaction removedTransaction : removedTransactions) {
            WmNode removedTransNode = removedTransaction.getNode();
            if (removedTransNode.getNodeRef() != null && !removedTransNode.isUnsaved()) {
                nodeService.deleteNode(removedTransNode.getNodeRef());
                if (isDocumentParent) {
                    documentLogService.addDocumentLog(invoiceRef, I18NUtil.getMessage("document_log_status_transaction_deleted"));
                }
            }
        }
        Map<QName, List<String>> usedDimensionValues = new HashMap<QName, List<String>>();
        for (Map.Entry<QName, Dimensions> entry : EInvoiceUtil.DIMENSION_PROPERTIES.entrySet()) {
            usedDimensionValues.put(entry.getKey(), new ArrayList<String>());
        }
        for (Transaction transaction : transactions) {
            NodeRef nodeRef = transaction.getNode().getNodeRef();
            Map<QName, Serializable> properties = RepoUtil.toQNameProperties(transaction.getNode().getProperties());
            if (transaction.getNode().isUnsaved()) {
                // create new transaction
                createTransaction(invoiceRef, properties);
                if (isDocumentParent) {
                    documentLogService.addDocumentLog(invoiceRef, I18NUtil.getMessage("document_log_status_transaction_added"));
                }
            } else {
                // update existing transaction
                Map<QName, Serializable> originalProps = nodeService.getProperties(transaction.getNode().getNodeRef());
                nodeService.addProperties(nodeRef, properties);
                if (isDocumentParent) {
                    for (Map.Entry<QName, Serializable> entry : properties.entrySet()) {
                        QName propName = entry.getKey();
                        Serializable originalValue = originalProps.get(propName);
                        if (!RepoUtil.isExistingPropertyValueEqualTo(transaction.getNode(), propName, originalValue)) {
                            documentLogService.addDocumentLog(
                                    invoiceRef,
                                    I18NUtil.getMessage("document_log_status_transaction_modified",
                                            I18NUtil.getMessage(TRANSACTION_DISPLAY_LABEL_PREFIX + propName.getLocalName()), originalValue));
                        }
                    }
                }
            }
            for (QName propName : DIMENSION_PROPERTIES.keySet()) {
                usedDimensionValues.get(propName).add((String) transaction.getNode().getProperties().get(propName));
            }
        }
        Set<NodeRef> changedDimensionRefs = new HashSet<NodeRef>();
        for (Map.Entry<QName, List<String>> entry : usedDimensionValues.entrySet()) {
            NodeRef dimensionRef = getDimension(DIMENSION_PROPERTIES.get(entry.getKey()));
            // dimensionRef == null may occur if given dimension is not imported
            if (dimensionRef != null) {
                setDimensionValuesActive(dimensionRef, entry.getValue(), changedDimensionRefs);
            }
        }
        for (NodeRef dimensionRef : changedDimensionRefs) {
            activeDimensionValueCache.remove(dimensionRef);
        }
    }

    private void setDimensionValuesActive(NodeRef dimensionRef, List<String> valueNames, Set<NodeRef> changedDimensionRefs) {
        for (String valueName : valueNames) {
            DimensionValue dimensionValue = getDimensionValue(dimensionRef, valueName);
            if (dimensionValue != null) {
                NodeRef dimensionValueRef = dimensionValue.getNode().getNodeRef();
                if (!(Boolean) nodeService.getProperty(dimensionValueRef, DimensionModel.Props.ACTIVE)) {
                    nodeService.setProperty(dimensionValueRef, DimensionModel.Props.ACTIVE, Boolean.TRUE);
                    DimensionValue cachedValue = EInvoiceUtil.findDimensionValueByValueName(valueName, allDimensionValueCache.get(dimensionRef));
                    if (cachedValue != null) {
                        cachedValue.getNode().getProperties().put(DimensionModel.Props.ACTIVE.toString(), Boolean.TRUE);
                        changedDimensionRefs.add(dimensionRef);
                    }
                }
            }
        }
    }

    @Override
    public void updateDimensionValues(List<DimensionValue> dimensionValues, Node selectedDimension) {
        // for optimizing update, save only dimensions that have changed
        List<DimensionValue> originalDimensionValues = new ArrayList<DimensionValue>();
        if (selectedDimension != null) {
            originalDimensionValues = getAllDimensionValuesFromRepo(selectedDimension.getNodeRef());
        }
        for (DimensionValue dimensionValue : dimensionValues) {
            if (dimensionValue.getNode().isUnsaved()) {
                // create new dimension
                createDimensionValue(selectedDimension.getNodeRef(), RepoUtil.toQNameProperties(dimensionValue.getNode().getProperties()));
            } else {
                if (hasDimensionValueChanged(dimensionValue, originalDimensionValues)) {
                    nodeService.setProperties(dimensionValue.getNode().getNodeRef(), RepoUtil.toQNameProperties(dimensionValue.getNode().getProperties()));
                }
            }
        }
        emptyDimensionValueChache();
    }

    @Override
    public List<ContentToSend> createContentToSend(File file) {
        List<ContentToSend> contentToSend = new ArrayList<ContentToSend>();
        ContentReader reader = fileFolderService.getReader(file.getNodeRef());
        ContentToSend content = new ContentToSend();
        content.setFileName(file.getDisplayName());
        content.setMimeType(reader.getMimetype());
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        reader.getContent(byteStream);
        byte[] byteArray = byteStream.toByteArray();
        content.setInputStream(new ByteArrayInputStream(byteArray));
        contentToSend.add(content);
        return contentToSend;
    }

    @Override
    public String getTransactionDvkFolder(Node document) {
        Map<String, Object> props = document.getProperties();
        if (StringUtils.isNotEmpty((String) props.get(DocumentSpecificModel.Props.PURCHASE_ORDER_SAP_NUMBER))) {
            if (!Boolean.TRUE.equals(props.get(DocumentSpecificModel.Props.XXL_INVOICE))) {
                return parametersService.getStringParameter(Parameters.SEND_INVOICE_TO_DVK_FOLDER);
            } else {
                return parametersService.getStringParameter(Parameters.SEND_XXL_INVOICE_TO_DVK_FOLDER);
            }
        } else {
            return parametersService.getStringParameter(Parameters.SEND_PURCHASE_ORDER_INVOICE_TO_DVK_FOLDER);
        }
    }

    @Override
    public File generateTransactionXmlFile(Node node, List<Transaction> transactions) throws IOException {
        Ostuarve ostuarve = generateTransactionXml(node, transactions);
        NodeRef fileRef = addTransactionFile(node, ostuarve);
        return fileService.getFile(fileRef);
    }

    private NodeRef addTransactionFile(Node node, Ostuarve ostuarve) throws IOException {
        String originalFileName = (String) node.getProperties().get(DocumentCommonModel.Props.DOC_NAME) + "." + XML_FILE_EXTENSION;
        List<String> documentFileDisplayNames = fileService.getDocumentFileDisplayNames(node.getNodeRef());
        String displayName = FilenameUtil.generateUniqueFileDisplayName(originalFileName, documentFileDisplayNames);
        String fileName = FilenameUtil.makeSafeUniqueFilename(displayName, documentFileDisplayNames);
        NodeRef file = fileFolderService.create(node.getNodeRef(), fileName, ContentModel.TYPE_CONTENT).getNodeRef();
        final ContentWriter writer = fileFolderService.getWriter(file);
        writer.setMimetype(MimetypeMap.MIMETYPE_XML);
        final OutputStream os = writer.getContentOutputStream();
        try {
            EInvoiceUtil.marshalAccount(ostuarve, os);
        } finally {
            os.close();
        }
        nodeService.setProperty(file, FileModel.Props.DISPLAY_NAME, displayName);
        return file;
    }

    private Ostuarve generateTransactionXml(Node node, List<Transaction> transactions) {
        Ostuarve ostuarve = new Ostuarve();
        Arve arve = new Arve();
        Hankija hankija = new Hankija();
        Map<String, Object> props = node.getProperties();
        hankija.setHankijaNimi((String) props.get(DocumentSpecificModel.Props.SELLER_PARTY_NAME));
        hankija.setHankijaNumber((String) props.get(DocumentSpecificModel.Props.SELLER_PARTY_SAP_ACCOUNT));
        hankija.setRegistrikood((String) props.get(DocumentSpecificModel.Props.SELLER_PARTY_REG_NUMBER));
        arve.setHankija(hankija);

        ArveInfo arveInfo = new ArveInfo();
        arveInfo.setAsutus(parametersService.getStringParameter(Parameters.DVK_ORGANIZATION_NAME));
        arveInfo.setArveKinnitatud(ACCOUNT_VALUE_YES);
        arveInfo.setArveTuup(getAccountType((String) props.get(DocumentSpecificModel.Props.INVOICE_TYPE)));
        arveInfo.setArveNumber((String) props.get(DocumentSpecificModel.Props.INVOICE_NUMBER));
        XMLGregorianCalendar invoiceDate = XmlUtil.getXmlGregorianCalendar((Date) props.get(DocumentSpecificModel.Props.INVOICE_DATE));
        invoiceDate.setHour(DatatypeConstants.FIELD_UNDEFINED);
        invoiceDate.setMinute(DatatypeConstants.FIELD_UNDEFINED);
        invoiceDate.setSecond(DatatypeConstants.FIELD_UNDEFINED);
        invoiceDate.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
        invoiceDate.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
        arveInfo.setArveKuupaev(invoiceDate);
        XMLGregorianCalendar invoiceDueDate = XmlUtil.getXmlGregorianCalendar((Date) props.get(DocumentSpecificModel.Props.INVOICE_DUE_DATE));
        invoiceDueDate.setHour(DatatypeConstants.FIELD_UNDEFINED);
        invoiceDueDate.setMinute(DatatypeConstants.FIELD_UNDEFINED);
        invoiceDueDate.setSecond(DatatypeConstants.FIELD_UNDEFINED);
        invoiceDueDate.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
        invoiceDueDate.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
        arveInfo.setMaksepaev(invoiceDueDate);
        arveInfo.setViitenumber((String) props.get(DocumentSpecificModel.Props.PAYMENT_REFERENCE_NUMBER));
        arveInfo.setTellimuseNumber((String) props.get(DocumentSpecificModel.Props.PURCHASE_ORDER_SAP_NUMBER));

        arveInfo.setArveSumma(EInvoiceUtil.roundDouble4Decimals(((Double) props.get(DocumentSpecificModel.Props.TOTAL_SUM))));
        Double vat = (Double) props.get(DocumentSpecificModel.Props.VAT);
        arveInfo.setKaibemaksKokku(vat == null ? BigDecimal.ZERO : EInvoiceUtil.roundDouble4Decimals(vat));
        arveInfo.setValuuta((String) props.get(DocumentSpecificModel.Props.CURRENCY));
        arveInfo.setSisemineId(documentTemplateService.getDocumentUrl(node.getNodeRef()));

        arve.setArveInfo(arveInfo);

        LisaInfo lisainfo = new LisaInfo();
        lisainfo.setInfoNimi(PAYMENT_INFO_NAME_TEXT);
        lisainfo.setInfoSisu((String) props.get(DocumentSpecificModel.Props.ADDITIONAL_INFORMATION_CONTENT));
        arveInfo.setLisaInfo(lisainfo);

        Konteering konteering = new Konteering();
        Date entryDate = (Date) props.get(DocumentSpecificModel.Props.ENTRY_DATE);
        XMLGregorianCalendar xmlEntryDate = null;
        if (entryDate != null) {
            xmlEntryDate = XmlUtil.getXmlGregorianCalendar(entryDate);
            xmlEntryDate.setHour(DatatypeConstants.FIELD_UNDEFINED);
            xmlEntryDate.setMinute(DatatypeConstants.FIELD_UNDEFINED);
            xmlEntryDate.setSecond(DatatypeConstants.FIELD_UNDEFINED);
            xmlEntryDate.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
            xmlEntryDate.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
        }
        konteering.setKandeKuupaev(xmlEntryDate);

        arve.setKonteering(konteering);

        List<Kanne> kanded = konteering.getKanne();
        for (Transaction transaction : transactions) {
            Kanne kanne = new Kanne();
            Double sumWithoutVat = transaction.getSumWithoutVat();
            kanne.setKandeTuup(sumWithoutVat == null || sumWithoutVat >= 0 ? InvoiceType.DEB.getTransactionXmlValue() : InvoiceType.CRE.getTransactionXmlValue());
            kanne.setPearaamatuKonto(transaction.getAccount());
            kanne.setKaibemaksuKood(transaction.getInvoiceTaxCode());

            // it is assumed that all numeric values are present
            BigDecimal transSumWithoutVat = EInvoiceUtil.roundDouble4Decimals(transaction.getSumWithoutVat());
            BigDecimal vatPercent = BigDecimal.valueOf(transaction.getInvoiceTaxPercent());
            BigDecimal vatSum = transSumWithoutVat.multiply(vatPercent).divide(BigDecimal.valueOf(100));
            kanne.setSumma(EInvoiceUtil.roundDouble4Decimals(transSumWithoutVat.add(vatSum).doubleValue()));
            kanne.setNetoSumma(EInvoiceUtil.roundDouble4Decimals(transaction.getSumWithoutVat()));
            kanne.setKandeKommentaar(transaction.getEntryContent());
            List<ee.webmedia.alfresco.document.einvoice.account.generated.Dimensioon> dimensioonList = kanne.getDimensioon();
            createXmlDimension(Dimensions.INVOICE_FUNDS_CENTERS, transaction.getFundsCenter(), dimensioonList);
            createXmlDimension(Dimensions.INVOICE_COST_CENTERS, transaction.getCostCenter(), dimensioonList);
            createXmlDimension(Dimensions.INVOICE_FUNDS, transaction.getFund(), dimensioonList);
            createXmlDimension(Dimensions.INVOICE_COMMITMENT_ITEM, transaction.getCommitmentItem(), dimensioonList);
            createXmlDimension(Dimensions.INVOICE_INTERNAL_ORDERS, transaction.getOrderNumber(), dimensioonList);
            createXmlDimension(Dimensions.INVOICE_ASSET_INVENTORY_NUMBERS, transaction.getAssetInventoryNumber(), dimensioonList);
            createXmlDimension(Dimensions.INVOICE_POSTING_KEY, transaction.getPostingKey(), dimensioonList);
            createXmlDimension(Dimensions.INVOICE_TRADING_PARTNER_CODES, transaction.getTradingPartnerCode(), dimensioonList);
            createXmlDimension(Dimensions.INVOICE_FUNCTIONAL_AREA_CODE, transaction.getFunctionalAreaCode(), dimensioonList);
            createXmlDimension(Dimensions.INVOICE_CASH_FLOW_CODES, transaction.getCashFlowCode(), dimensioonList);
            createXmlDimension(Dimensions.INVOICE_SOURCE_CODES, transaction.getSource(), dimensioonList);
            createXmlDimension(Dimensions.INVOICE_PAYMENT_METHOD_CODES, transaction.getPaymentMethod(), dimensioonList);
            createXmlDimension(Dimensions.INVOICE_HOUSE_BANK_CODES, transaction.getHouseBank(), dimensioonList);

            kanded.add(kanne);
        }

        ostuarve.getArve().add(arve);
        ostuarve.setArveidKokku(BigInteger.ONE);

        return ostuarve;
    }

    private void createXmlDimension(Dimensions dimension, String transDimensionValue,
            List<ee.webmedia.alfresco.document.einvoice.account.generated.Dimensioon> dimensioonList) {
        if (StringUtils.isBlank(transDimensionValue)) {
            return;
        }
        ee.webmedia.alfresco.document.einvoice.account.generated.Dimensioon dimensioon = new ee.webmedia.alfresco.document.einvoice.account.generated.Dimensioon();
        Parameters parameter = (Parameters) EInvoiceUtil.DIMENSION_PARAMETERS.getKey(dimension);
        String dimensionXmlId = parametersService.getStringParameter(parameter);
        dimensioon.setDimensiooniId(dimensionXmlId);
        DimensionValue dimensionValue = getDimensionValue(getDimension(dimension), transDimensionValue);
        dimensioon.setDimensiooniVaartuseId(transDimensionValue);
        dimensioon.setDimensiooniVaartuseNimetus(dimensionValue == null ? "" : dimensionValue.getValue());
        if (dimensionValue == null) {
            LOG.debug("No dimension value was found for dimension='" + dimensionXmlId + "', value name='" + transDimensionValue + "', sending empty value.");
        }
        dimensioonList.add(dimensioon);
    }

    @Override
    public DimensionValue getDimensionValue(NodeRef dimensionRef, String transDimensionValue) {
        List<DimensionValue> dimensionValues = getAllDimensionValuesFromCache(dimensionRef);
        for (DimensionValue dimensionValue : dimensionValues) {
            if (dimensionValue.getValueName().equals(transDimensionValue)) {
                return dimensionValue;
            }
        }
        return null;
    }

    @Override
    public DimensionValue getDimensionDefaultValue(NodeRef dimensionRef) {
        List<DimensionValue> dimensionValues = getAllDimensionValuesFromCache(dimensionRef);
        for (DimensionValue dimensionValue : dimensionValues) {
            if (dimensionValue.getDefaultValue()) {
                return dimensionValue;
            }
        }
        return null;
    }

    private String getAccountType(String invoiceTypeValue) {
        InvoiceType invoiceType = InvoiceType.getInvoiceTypeByValueName(invoiceTypeValue);
        return invoiceType == null ? null : invoiceType.getTransactionXmlValue();
    }

    private void emptyDimensionValueChache() {
        activeDimensionValueCache.clear();
        allDimensionValueCache.clear();
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
        final List<DimensionValue> dimensionValues = getAllDimensionValuesFromRepo(dimensionRef);
        // List xmlDimension values that already exist in application
        List<Integer> existingDimensionValues = new ArrayList<Integer>();
        // List dimensions that exist both in xmlDimensions and dimensions
        final List<NodeRef> updatedDimensionValues = new ArrayList<NodeRef>();
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
        // Remove existing dimensions not found in xml,
        // delete TRANSACTION_SIZE nodes during one transaction:
        // Alfresco has performance issue when large number of nodes is deleted in one transaction
        RetryingTransactionHelper retryingTransactionHelper = transactionService.getRetryingTransactionHelper();
        Iterator<DimensionValue> i = dimensionValues.iterator();
        DeleteDimensionCallback deleteDimensionCallback = new DeleteDimensionCallback(i, updatedDimensionValues);
        while (i.hasNext()) {
            retryingTransactionHelper.doInTransaction(deleteDimensionCallback, false, true);
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

    private abstract class ProcessDimensionValueCallback implements RetryingTransactionHelper.RetryingTransactionCallback<Iterator<DimensionValue>> {
        private final Iterator<DimensionValue> iterator;

        public ProcessDimensionValueCallback(Iterator<DimensionValue> iterator) {
            this.iterator = iterator;
        }

        @Override
        public Iterator<DimensionValue> execute() throws Throwable {
            int transactionCommitCounter = 0;
            for (; iterator.hasNext() && transactionCommitCounter < TRANSACTION_SIZE;) {
                transactionCommitCounter += processDimensionValue(iterator.next());
            }
            return null;
        }

        /**
         * Process node ref and return number of nodes changed
         */
        protected abstract int processDimensionValue(DimensionValue dimensionValue);

    }

    private class DeleteDimensionCallback extends ProcessDimensionValueCallback {
        List<NodeRef> updatedDimensionValues;

        public DeleteDimensionCallback(Iterator<DimensionValue> iterator, List<NodeRef> updatedDimensionValues) {
            super(iterator);
            this.updatedDimensionValues = updatedDimensionValues;
        }

        @Override
        protected int processDimensionValue(DimensionValue dimensionValue) {
            NodeRef dimensionValueRef = dimensionValue.getNode().getNodeRef();
            if (!updatedDimensionValues.contains(dimensionValueRef) && dimensionValue.getBeginDateTime() == null && dimensionValue.getEndDateTime() == null) {
                nodeService.deleteNode(dimensionValueRef);
                return 1;
            }
            return 0;
        }
    }

    private class SetActiveOrInactiveDimensionCallback extends ProcessDimensionValueCallback {
        boolean active;

        public SetActiveOrInactiveDimensionCallback(Iterator<DimensionValue> iterator, boolean active) {
            super(iterator);
            this.active = active;
        }

        @Override
        protected int processDimensionValue(DimensionValue dimensionValue) {
            nodeService.setProperty(dimensionValue.getNode().getNodeRef(), DimensionModel.Props.ACTIVE, active);
            return 1;
        }
    }

    private NodeRef getOrCreateDimension(Dimensions dimensions, String xmlDimensionId) {
        NodeRef dimensionRef = getDimension(dimensions);
        if (dimensionRef == null) {
            Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
            properties.put(DimensionModel.Props.NAME, xmlDimensionId);
            properties.put(DimensionModel.Props.COMMENT, null);
            dimensionRef = createDimension(dimensions.getDimensionName(), properties);
        } else {
            nodeService.setProperty(dimensionRef, DimensionModel.Props.NAME, xmlDimensionId);
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
            props.put(DimensionModel.Props.ACTIVE, Boolean.FALSE);
            props.put(DimensionModel.Props.DEFAULT_VALUE, Boolean.FALSE);
        }
        return props;
    }

    @Override
    public Pair<String, String> getDocUrlAndErpDocNumber(InputStream input) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            org.w3c.dom.Document document = db.parse(input);
            org.w3c.dom.Node root = XmlUtil.findChildByName(new javax.xml.namespace.QName(ERP_NAMESPACE_URI, "BuyInvoiceRegisteredRequest"), document);
            if (root != null && root.getChildNodes().getLength() > 0) {
                org.w3c.dom.Node regInvoiceNode = XmlUtil.findChildByName(new javax.xml.namespace.QName(ERP_NAMESPACE_URI, "RegisteredInvoice"), root);
                if (regInvoiceNode != null) {
                    org.w3c.dom.Node invoiceUrlNode = regInvoiceNode.getAttributes().getNamedItem("invoiceId");
                    if (invoiceUrlNode != null) {
                        String invoiceUrl = invoiceUrlNode.getNodeValue();
                        if (StringUtils.isNotBlank(invoiceUrl)) {
                            org.w3c.dom.Node erpDocNumberNode = XmlUtil.findChildByName(new javax.xml.namespace.QName(ERP_NAMESPACE_URI, "ErpDocumentNumber"), regInvoiceNode);
                            if (erpDocNumberNode != null) {
                                String erpDocNumber = StringUtils.strip(erpDocNumberNode.getTextContent());
                                if (StringUtils.isNotBlank(erpDocNumber)) {
                                    return new Pair<String, String>(invoiceUrl, erpDocNumber);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("Error parsing erp data.", e);
        }
        return null;
    }

    @Override
    public NodeRef updateDocumentEntrySapNumber(String dvkId, String erpDocNumber) {
        NodeRef nodeRef = null;
        try {
            List<Document> documents = documentSearchService.searchDocumentsByDvkId(dvkId);
            if (documents == null || documents.size() != 1) {
                return null;
            }
            nodeRef = documents.get(0).getNodeRef();
            if (nodeRef == null || !DocumentSubtypeModel.Types.INVOICE.equals(nodeService.getType(nodeRef))) {
                return null;
            }
            if (StringUtils.isNotEmpty((String) nodeService.getProperty(nodeRef, DocumentSpecificModel.Props.ENTRY_SAP_NUMBER))) {
                LOG.error("Document with nodeRef=" + nodeRef + " has already entry sap number, not overwriting.");
                return null;
            }
            nodeService.setProperty(nodeRef, DocumentSpecificModel.Props.ENTRY_SAP_NUMBER, erpDocNumber);
        } catch (IllegalArgumentException e) {
            LOG.error("Document uri could not be parsed to valid uri tokens");
        }
        return nodeRef;
    }

    @Override
    public void deleteTransactionTemplate(NodeRef transactionTemplateRef) {
        nodeService.deleteNode(transactionTemplateRef);
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
            return value.getSelgitus();
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

    @Override
    public List<String> getDimensionDefaultValueList(Dimensions dimension, Predicate filter) {
        List<String> fundsList = new ArrayList<String>();
        DimensionValue dimensionDefaultValue = getDimensionDefaultValue(getDimension(dimension));
        if (dimensionDefaultValue != null && (filter == null || filter.evaluate(dimensionDefaultValue))) {
            fundsList.add(dimensionDefaultValue.getValueName());
        } else {
            fundsList.add("");
        }
        return fundsList;
    }

    @Override
    // dimensionRef values could be cached, if this code exposes performance issue
    public Map<Dimensions, NodeRef> getDimensionToNodeRefMappings() {
        Map<Dimensions, NodeRef> dimensionRefs = new HashMap<Dimensions, NodeRef>();
        dimensionRefs.put(Dimensions.INVOICE_ACCOUNTS, getDimension(Dimensions.INVOICE_ACCOUNTS));
        dimensionRefs.put(Dimensions.INVOICE_ASSET_INVENTORY_NUMBERS, getDimension(Dimensions.INVOICE_ASSET_INVENTORY_NUMBERS));
        dimensionRefs.put(Dimensions.INVOICE_CASH_FLOW_CODES, getDimension(Dimensions.INVOICE_CASH_FLOW_CODES));
        dimensionRefs.put(Dimensions.INVOICE_COMMITMENT_ITEM, getDimension(Dimensions.INVOICE_COMMITMENT_ITEM));
        dimensionRefs.put(Dimensions.INVOICE_COST_CENTERS, getDimension(Dimensions.INVOICE_COST_CENTERS));
        dimensionRefs.put(Dimensions.INVOICE_FUNCTIONAL_AREA_CODE, getDimension(Dimensions.INVOICE_FUNCTIONAL_AREA_CODE));
        dimensionRefs.put(Dimensions.INVOICE_FUNDS, getDimension(Dimensions.INVOICE_FUNDS));
        dimensionRefs.put(Dimensions.INVOICE_FUNDS_CENTERS, getDimension(Dimensions.INVOICE_FUNDS_CENTERS));
        dimensionRefs.put(Dimensions.INVOICE_HOUSE_BANK_CODES, getDimension(Dimensions.INVOICE_HOUSE_BANK_CODES));
        dimensionRefs.put(Dimensions.INVOICE_INTERNAL_ORDERS, getDimension(Dimensions.INVOICE_INTERNAL_ORDERS));
        dimensionRefs.put(Dimensions.INVOICE_PAYMENT_METHOD_CODES, getDimension(Dimensions.INVOICE_PAYMENT_METHOD_CODES));
        dimensionRefs.put(Dimensions.INVOICE_POSTING_KEY, getDimension(Dimensions.INVOICE_POSTING_KEY));
        dimensionRefs.put(Dimensions.INVOICE_SOURCE_CODES, getDimension(Dimensions.INVOICE_SOURCE_CODES));
        dimensionRefs.put(Dimensions.INVOICE_TRADING_PARTNER_CODES, getDimension(Dimensions.INVOICE_TRADING_PARTNER_CODES));
        dimensionRefs.put(Dimensions.TAX_CODE_ITEMS, getDimension(Dimensions.TAX_CODE_ITEMS));
        return dimensionRefs;
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

    public void setDocumentDynamicService(DocumentDynamicService documentDynamicService) {
        this.documentDynamicService = documentDynamicService;
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

    public void setTransactionDescParametersPath(String transactionDescParametersPath) {
        this.transactionDescParametersPath = transactionDescParametersPath;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    public void setDocumentAdminService(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }

    public void setDocumentTemplateService(DocumentTemplateService documentTemplateService) {
        this.documentTemplateService = documentTemplateService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public void setDvkService(DvkService dvkService) {
        this.dvkService = dvkService;
    }

    public void setDocumentLogService(DocumentLogService documentLogService) {
        this.documentLogService = documentLogService;
    }

    public void setTransactionTemplatesPath(String transactionTemplatesPath) {
        this.transactionTemplatesPath = transactionTemplatesPath;
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // END: getters / setters

}
