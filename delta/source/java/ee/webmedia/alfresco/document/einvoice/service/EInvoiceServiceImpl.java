package ee.webmedia.alfresco.document.einvoice.service;

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
import java.math.MathContext;
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

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
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
import org.apache.xml.security.exceptions.Base64DecodingException;
import org.apache.xml.security.utils.Base64;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.service.AddressbookService;
import ee.webmedia.alfresco.classificator.enums.TransmittalMode;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.WmNode;
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
import ee.webmedia.alfresco.document.einvoice.model.Transaction;
import ee.webmedia.alfresco.document.einvoice.model.TransactionModel;
import ee.webmedia.alfresco.document.einvoice.sellerslist.generated.HankijaInfo;
import ee.webmedia.alfresco.document.einvoice.sellerslist.generated.HankijaNimekiri;
import ee.webmedia.alfresco.document.einvoice.vatcodelist.generated.KaibemaksuKoodInfo;
import ee.webmedia.alfresco.document.einvoice.vatcodelist.generated.KaibemaksuKoodNimekiri;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;
import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.simdhs.servlet.ExternalAccessServlet;
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

    private static final String XXL_INVOICE_TEXT = "XXL";
    private static final String EXTENSION_RECORD_TYPE = "LIIK";
    private static final String ERP_NAMESPACE_URI = "erp";
    private static final String XML_FILE_EXTENSION = "xml";
    private static final String TRANSACTION_XML_CRE = "K";
    private static final String TRANSACTION_XML_DEB = "D";
    private static final String PAYMENT_INFO_NAME_TEXT = "MKSelgitus";
    private static final String ACCOUNT_VALUE_YES = "JAH";
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
    private DocumentTemplateService documentTemplateService;
    private FileService fileService;
    private DvkService dvkService;
    private static final Map<NodeRef, List<DimensionValue>> activeDimensionValueCache = new ConcurrentHashMap<NodeRef, List<DimensionValue>>();
    private static final Map<NodeRef, List<DimensionValue>> allDimensionValueCache = new ConcurrentHashMap<NodeRef, List<DimensionValue>>();
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
                if (EXTENSION_RECORD_TYPE.equalsIgnoreCase(extensionRecord.getExtensionId()) && EXTENSION_RECORD_TYPE.equalsIgnoreCase(extensionRecord.getInformationName())
                        && XXL_INVOICE_TEXT.equalsIgnoreCase(extensionRecord.getInformationContent())) {
                    props.put(DocumentSpecificModel.Props.XXL_INVOICE, Boolean.TRUE);
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
                    Document document = contracts.get(0);
                    Node user = userService.getUser(document.getOwnerId());
                    if (user != null) {
                        setOwnerPropsFromDocument(props, document.getProperties());
                        ownerSet = true;
                    }
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
        if (allDimensionValueCache.containsKey(dimensionRef)) {
            return allDimensionValueCache.get(dimensionRef);
        }
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
        allDimensionValueCache.put(dimensionRef, Collections.unmodifiableList(dimensionValues));
        return dimensionValues;
    }

    @Override
    public List<DimensionValue> getActiveDimensionValues(NodeRef dimension) {
        if (activeDimensionValueCache.containsKey(dimension)) {
            return activeDimensionValueCache.get(dimension);
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
        Collections.sort(activeDimensionValues);
        activeDimensionValueCache.put(dimension, Collections.unmodifiableList(activeDimensionValues));
        return activeDimensionValues;
    }

    @Override
    public List<DimensionValue> getVatCodeDimensionValues() {
        List<DimensionValue> tmpValues = null;
        if (vatCodeDimesnionValues == null) {
            final NodeRef nodeRef = generalService.getNodeRef(Dimensions.TAX_CODE_ITEMS.toString());
            tmpValues = new ArrayList<DimensionValue>(getAllDimensionValues(nodeRef));
            vatCodeDimesnionValues = Collections.unmodifiableList(tmpValues);
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
            @SuppressWarnings("unchecked")
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
                                    && ((StringUtils.isEmpty(sellerRegNumber) && StringUtils.isEmpty(registrikood)) || (sellerRegNumber != null && sellerRegNumber
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
                                    // FIXME: validate generated xml
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
        if (props.get(DocumentSpecificModel.Props.PURCHASE_ORDER_SAP_NUMBER) == null) {
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
            // FIXME: validate generated xml
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

        MathContext mc = new MathContext(4);
        arveInfo.setArveSumma(new BigDecimal((Double) props.get(DocumentSpecificModel.Props.TOTAL_SUM)).round(mc));
        Double vat = (Double) props.get(DocumentSpecificModel.Props.VAT);
        arveInfo.setKaibemaksKokku(vat == null ? null : new BigDecimal(vat).round(mc));
        arveInfo.setValuuta((String) props.get(DocumentSpecificModel.Props.CURRENCY));
        arveInfo.setSisemineId(documentTemplateService.getDocumentUrl(node.getNodeRef()));

        arve.setArveInfo(arveInfo);

        LisaInfo lisainfo = new LisaInfo();
        lisainfo.setInfoNimi(PAYMENT_INFO_NAME_TEXT);
        lisainfo.setInfoSisu((String) props.get(DocumentSpecificModel.Props.ADDITIONAL_INFORMATION_CONTENT));
        arveInfo.setLisaInfo(lisainfo);

        Konteering konteering = new Konteering();
        konteering.setKandeKuupaev(XmlUtil.getXmlGregorianCalendar((Date) props.get(DocumentSpecificModel.Props.ENTRY_DATE)));
        arve.setKonteering(konteering);

        List<Kanne> kanded = konteering.getKanne();
        for (Transaction transaction : transactions) {
            Kanne kanne = new Kanne();
            Double sumWithoutVat = transaction.getSumWithoutVat();
            kanne.setKandeTuup(sumWithoutVat == null || sumWithoutVat >= 0 ? TRANSACTION_XML_DEB : TRANSACTION_XML_CRE);
            kanne.setPearaamatuKonto(transaction.getAccount());
            kanne.setKaibemaksuKood(transaction.getInvoiceTaxCode());

            // it is assumed that all numeric values are present
            BigDecimal transSumWithoutVat = new BigDecimal(transaction.getSumWithoutVat()).round(mc);
            BigDecimal vatPercent = new BigDecimal(transaction.getInvoiceTaxPercent());
            kanne.setSumma(transSumWithoutVat.add(transSumWithoutVat.multiply(vatPercent).divide(new BigDecimal(100))).round(mc));
            kanne.setNetoSumma(new BigDecimal(transaction.getSumWithoutVat()).round(mc));
            kanne.setKandeKommentaar(transaction.getEntryContent());
            List<ee.webmedia.alfresco.document.einvoice.account.generated.Dimensioon> dimensioonList = kanne.getDimensioon();
            createXmlDimension(Dimensions.INVOICE_FUNDS_CENTERS, transaction.getFundsCenter(), dimensioonList);
            createXmlDimension(Dimensions.INVOICE_COST_CENTERS, transaction.getCostCenter(), dimensioonList);
            createXmlDimension(Dimensions.INVOICE_FUNDS, transaction.getFund(), dimensioonList);
            createXmlDimension(Dimensions.INVOICE_COMMITMENT_ITEM, transaction.getCommitmentItem(), dimensioonList);
            createXmlDimension(Dimensions.INVOICE_INTERNAL_ORDERS, transaction.getOrderNumber(), dimensioonList);
            createXmlDimension(Dimensions.INVOICE_ASSET_INVENTORY_NUMBERS, transaction.getAssetInventaryNumber(), dimensioonList);
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
        if (dimensionValue == null) {
            String message = "No dimension value was found for dimension='" + dimensionXmlId + "', value name='" + transDimensionValue + "'";
            LOG.error(message);
            throw new RuntimeException(message);
        }
        dimensioon.setDimensiooniVaartuseId(dimensionValue.getValueName());
        dimensioon.setDimensiooniVaartuseNimetus(dimensionValue.getValue());
        dimensioonList.add(dimensioon);
    }

    private DimensionValue getDimensionValue(NodeRef dimensionRef, String transDimensionValue) {
        List<DimensionValue> dimensionValues = getAllDimensionValues(dimensionRef);
        for (DimensionValue dimensionValue : dimensionValues) {
            if (dimensionValue.getValueName().equals(transDimensionValue)) {
                return dimensionValue;
            }
        }
        return null;
    }

    private String getAccountType(String invoiceType) {
        if ("DEB".equalsIgnoreCase(invoiceType)) {
            return TRANSACTION_XML_DEB;
        }
        if ("CRE".equalsIgnoreCase(invoiceType)) {
            return TRANSACTION_XML_CRE;
        }
        return "KS";
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

    @Override
    public Pair<String, String> getDocUrlAndErpDocNumber(String inputStr) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            org.w3c.dom.Document document = db.parse(inputStr);
            org.w3c.dom.Node root = XmlUtil.findChildByName(new javax.xml.namespace.QName(ERP_NAMESPACE_URI, "BuyInvoiceRegisteredRequest"), document);
            if (root != null && root.getChildNodes().getLength() > 0) {
                org.w3c.dom.Node regInvoiceNode = XmlUtil.findChildByName(new javax.xml.namespace.QName(ERP_NAMESPACE_URI, "RegisteredInvoice"), document);
                if (regInvoiceNode != null) {
                    org.w3c.dom.Node invoiceUrlNode = regInvoiceNode.getAttributes().getNamedItem("invoiceId");
                    if (invoiceUrlNode != null) {
                        String invoiceUrl = invoiceUrlNode.getNodeValue();
                        if (StringUtils.isNotBlank(invoiceUrl)) {
                            org.w3c.dom.Node erpDocNumberNode = XmlUtil.findChildByName(new javax.xml.namespace.QName(ERP_NAMESPACE_URI, "ErpDocumentNumber"), document);
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
    public NodeRef updateDocumentEntrySapNumber(String docUrl, String erpDocNumber) {
        try {
            Pair<String, String[]> outcomeAndArgs = ExternalAccessServlet.getDocumentUriTokens(0, docUrl);
            if (!ExternalAccessServlet.OUTCOME_DOCUMENT.equals(outcomeAndArgs.getFirst())) {
                return null;
            }
            // TODO: unify with ExternalAccessServlet
            List<String> storeNames = new ArrayList<String>(Arrays.asList("workspace://SpacesStore", "workspace://ArchivalsStore"));
            NodeRef nodeRef = ExternalAccessServlet.getNodeRefFromNodeId(outcomeAndArgs.getSecond()[0], nodeService, storeNames);
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
        return null;
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

    public void setDocumentTemplateService(DocumentTemplateService documentTemplateService) {
        this.documentTemplateService = documentTemplateService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public void setDvkService(DvkService dvkService) {
        this.dvkService = dvkService;
    }

    // END: getters / setters

}
