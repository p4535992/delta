<<<<<<< HEAD
package ee.webmedia.alfresco.document.einvoice.service;

import static ee.webmedia.alfresco.utils.XmlUtil.getUnmarshaller;
import static ee.webmedia.alfresco.utils.XmlUtil.initJaxbContext;
import static ee.webmedia.alfresco.utils.XmlUtil.initSchema;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.validation.Schema;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.data.IDataContainer;
import org.alfresco.web.data.QuickSort;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.bidimap.DualTreeBidiMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.einvoice.account.generated.Ostuarve;
import ee.webmedia.alfresco.document.einvoice.accountlist.generated.KontoNimekiri;
import ee.webmedia.alfresco.document.einvoice.dimensionslist.generated.DimensioonideNimekiri;
import ee.webmedia.alfresco.document.einvoice.generated.EInvoice;
import ee.webmedia.alfresco.document.einvoice.model.DimensionModel;
import ee.webmedia.alfresco.document.einvoice.model.DimensionValue;
import ee.webmedia.alfresco.document.einvoice.model.Dimensions;
import ee.webmedia.alfresco.document.einvoice.model.Transaction;
import ee.webmedia.alfresco.document.einvoice.model.TransactionModel;
import ee.webmedia.alfresco.document.einvoice.sellerslist.generated.HankijaNimekiri;
import ee.webmedia.alfresco.document.einvoice.vatcodelist.generated.KaibemaksuKoodNimekiri;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;

public class EInvoiceUtil {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(EInvoiceUtil.class);

    // TODO: unify with common-build.xml script einvoice definitions
    private static final String EINVOICE_PACKAGE = "ee.webmedia.alfresco.document.einvoice.generated";
    private static JAXBContext eInvoiceJaxbContext = initJaxbContext(EINVOICE_PACKAGE);
    private static Schema eInvoiceJaxbSchema = initSchema("e-invoice_ver1.1.xsd", EInvoice.class);

    private static final String DIMENSIONS_LIST_PACKAGE = "ee.webmedia.alfresco.document.einvoice.dimensionslist.generated";
    private static final JAXBContext dimensionsListJaxbContext = initJaxbContext(DIMENSIONS_LIST_PACKAGE);
    private static final Schema dimensionsListJaxbSchema = initSchema("DimensioonideNimekiri.xsd", DimensioonideNimekiri.class);

    private static final String SELLERS_LIST_PACKAGE = "ee.webmedia.alfresco.document.einvoice.sellerslist.generated";
    private static final JAXBContext sellersListJaxbContext = initJaxbContext(SELLERS_LIST_PACKAGE);
    private static final Schema sellersListJaxbSchema = initSchema("HankijaNimekiri.xsd", HankijaNimekiri.class);

    private static final String VAT_CODES_LIST_PACKAGE = "ee.webmedia.alfresco.document.einvoice.vatcodelist.generated";
    private static final JAXBContext vatCodesListJaxbContext = initJaxbContext(VAT_CODES_LIST_PACKAGE);
    private static final Schema vatCodesListJaxbSchema = initSchema("KaibemaksuKoodNimekiri.xsd", KaibemaksuKoodNimekiri.class);

    private static final String ACCOUNTS_LIST_PACKAGE = "ee.webmedia.alfresco.document.einvoice.accountlist.generated";
    private static final JAXBContext accountsListJaxbContext = initJaxbContext(ACCOUNTS_LIST_PACKAGE);
    private static final Schema accountsListJaxbSchema = initSchema("KontoNimekiri.xsd", KontoNimekiri.class);

    private static final String ACCOUNT_PACKAGE = "ee.webmedia.alfresco.document.einvoice.account.generated";
    private static final JAXBContext accountJaxbContext = initJaxbContext(ACCOUNT_PACKAGE);
    private static final Schema accountJaxbSchema = initSchema("OstuarveKonteering.xsd", Ostuarve.class);

    public static final Dimensions ACCOUNT_DIMENSION = Dimensions.INVOICE_ACCOUNTS;
    public static final Dimensions VAT_CODE_DIMENSION = Dimensions.TAX_CODE_ITEMS;
    /** TODO: actually it would be better to use generic bidirectional map (from guava? refactored Commons-Collections?) here */
    public static final BidiMap /* <Parameters, Dimensions> */DIMENSION_PARAMETERS;

    public static final Map<QName, Dimensions> DIMENSION_PROPERTIES;

    public static final String XXL_INVOICE_TYPE = "XXL";

    static {
        DIMENSION_PARAMETERS = new DualTreeBidiMap /* <Parameters, Dimensions> */();
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_FUNDS_CENTERS, Dimensions.INVOICE_FUNDS_CENTERS);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_COST_CENTERS, Dimensions.INVOICE_COST_CENTERS);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_FUNDS, Dimensions.INVOICE_FUNDS);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_COMMITMENT_ITEM, Dimensions.INVOICE_COMMITMENT_ITEM);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_INTERNAL_ORDERS, Dimensions.INVOICE_INTERNAL_ORDERS);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_ASSET_INVENTORY_NUMBERS, Dimensions.INVOICE_ASSET_INVENTORY_NUMBERS);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_TRADING_PARTNER_CODES, Dimensions.INVOICE_TRADING_PARTNER_CODES);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_FUNCTIONAL_AREA_CODE, Dimensions.INVOICE_FUNCTIONAL_AREA_CODE);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_CASH_FLOW_CODES, Dimensions.INVOICE_CASH_FLOW_CODES);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_SOURCE_CODES, Dimensions.INVOICE_SOURCE_CODES);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_POSTING_KEY, Dimensions.INVOICE_POSTING_KEY);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_PAYMENT_METHOD_CODES, Dimensions.INVOICE_PAYMENT_METHOD_CODES);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_HOUSE_BANK_CODES, Dimensions.INVOICE_HOUSE_BANK_CODES);

        DIMENSION_PROPERTIES = new HashMap<QName, Dimensions>();
        DIMENSION_PROPERTIES.put(TransactionModel.Props.ACCOUNT, Dimensions.INVOICE_ACCOUNTS);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.ASSET_INVENTORY_NUMBER, Dimensions.INVOICE_ASSET_INVENTORY_NUMBERS);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.CASH_FLOW_CODE, Dimensions.INVOICE_CASH_FLOW_CODES);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.COMMITMENT_ITEM, Dimensions.INVOICE_COMMITMENT_ITEM);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.COST_CENTER, Dimensions.INVOICE_COST_CENTERS);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.EA_COMMITMENT_ITEM, Dimensions.INVOICE_COMMITMENT_ITEM);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.FUNCTIONAL_ARE_CODE, Dimensions.INVOICE_FUNCTIONAL_AREA_CODE);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.FUND, Dimensions.INVOICE_FUNDS);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.FUNDS_CENTER, Dimensions.INVOICE_FUNDS_CENTERS);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.HOUSE_BANK, Dimensions.INVOICE_HOUSE_BANK_CODES);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.ORDER_NUMBER, Dimensions.INVOICE_INTERNAL_ORDERS);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.PAYMENT_METHOD, Dimensions.INVOICE_PAYMENT_METHOD_CODES);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.POSTING_KEY, Dimensions.INVOICE_POSTING_KEY);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.SOURCE, Dimensions.INVOICE_SOURCE_CODES);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.TRADING_PARTNER_CODE, Dimensions.INVOICE_TRADING_PARTNER_CODES);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.INVOICE_TAX_CODE, Dimensions.TAX_CODE_ITEMS);
        // turn on jaxb debug
        // TODO: turn off when jaxb import has been tested in client environment (or make switchable by general log settings)
        System.setProperty("jaxb.debug", "true");
    }

    public static NumberFormat getInvoiceNumberFormat() {
        NumberFormat invoiceDecimalFormat = DecimalFormat.getInstance(new Locale("et", "EE", ""));
        invoiceDecimalFormat.setMinimumIntegerDigits(1);
        invoiceDecimalFormat.setMinimumFractionDigits(2);
        invoiceDecimalFormat.setMaximumFractionDigits(2);
        invoiceDecimalFormat.setGroupingUsed(true);
        return invoiceDecimalFormat;
    }

    public static Unmarshaller getEInvoiceUnmarshaller() throws JAXBException {
        Unmarshaller unmarshaller = getUnmarshaller(eInvoiceJaxbContext, eInvoiceJaxbSchema);
        unmarshaller.setEventHandler(new EInvoiceValidationEventHandler());
        return unmarshaller;
    }

    public static Unmarshaller getDimensionsListUnmarshaller() throws JAXBException {
        return getUnmarshaller(dimensionsListJaxbContext, dimensionsListJaxbSchema);
    }

    public static Unmarshaller getSellersListUnmarshaller() throws JAXBException {
        return getUnmarshaller(sellersListJaxbContext, sellersListJaxbSchema);
    }

    public static Unmarshaller getAccountsListUnmarshaller() throws JAXBException {
        return getUnmarshaller(accountsListJaxbContext, accountsListJaxbSchema);
    }

    public static Unmarshaller getVatCodesListUnmarshaller() throws JAXBException {
        return getUnmarshaller(vatCodesListJaxbContext, vatCodesListJaxbSchema);
    }

    public static Unmarshaller getAccountUnmarshaller() throws JAXBException {
        return getUnmarshaller(accountJaxbContext, accountJaxbSchema);
    }

    public static EInvoice unmarshalEInvoice(org.w3c.dom.Node input) {
        EInvoice einvoice = null;
        try {
            einvoice = (EInvoice) getEInvoiceUnmarshaller().unmarshal(input);
        } catch (JAXBException e) {
            LOG.debug("Failed to unmarshal einvoice.", e);
        }
        return einvoice;
    }

    public static EInvoice unmarshalEInvoice(InputStream input) {
        EInvoice einvoice = null;
        try {
            einvoice = (EInvoice) getEInvoiceUnmarshaller().unmarshal(input);
        } catch (JAXBException e) {
            LOG.debug("Failed to unmarshal einvoice.", e);
        }
        return einvoice;
    }

    public static EInvoice unmarshalEInvoice(java.io.File input) {
        EInvoice einvoice = null;
        try {
            einvoice = (EInvoice) getEInvoiceUnmarshaller().unmarshal(input);
        } catch (JAXBException e) {
            LOG.debug("Failed to unmarshal einvoice.", e);
        }
        return einvoice;
    }

    public static DimensioonideNimekiri unmarshalDimensionsList(InputStream input) {
        DimensioonideNimekiri dimensionsList = null;
        try {
            dimensionsList = (DimensioonideNimekiri) getDimensionsListUnmarshaller().unmarshal(input);
        } catch (JAXBException e) {
            LOG.debug("Failed to unmarshal dimensions list.", e);
        }
        return dimensionsList;
    }

    public static HankijaNimekiri unmarshalSellersList(InputStream input) {
        HankijaNimekiri sellersList = null;
        try {
            sellersList = (HankijaNimekiri) getSellersListUnmarshaller().unmarshal(input);
        } catch (JAXBException e) {
            LOG.debug("Failed to unmarshal sellers list.", e);
        }
        return sellersList;
    }

    public static KaibemaksuKoodNimekiri unmarshalVatCodesList(InputStream input) {
        KaibemaksuKoodNimekiri vatCodesList = null;
        try {
            vatCodesList = (KaibemaksuKoodNimekiri) getVatCodesListUnmarshaller().unmarshal(input);
        } catch (JAXBException e) {
            LOG.debug("Failed to unmarshal vat codes list.", e);
        }
        return vatCodesList;
    }

    public static KontoNimekiri unmarshalAccountsList(InputStream input) {
        KontoNimekiri accountsList = null;
        try {
            accountsList = (KontoNimekiri) getAccountsListUnmarshaller().unmarshal(input);
        } catch (JAXBException e) {
            LOG.debug("Failed to unmarshal accounts list.", e);
        }
        return accountsList;
    }

    public static Ostuarve unmarshalAccount(InputStream input) {
        Ostuarve account = null;
        try {
            account = (Ostuarve) getAccountUnmarshaller().unmarshal(input);
        } catch (JAXBException e) {
            LOG.debug("Failed to unmarshal account.", e);
        }
        return account;
    }

    public static Ostuarve unmarshalAccount(Reader reader) {
        Ostuarve account = null;
        try {
            account = (Ostuarve) getAccountUnmarshaller().unmarshal(reader);
        } catch (JAXBException e) {
            LOG.debug("Failed to unmarshal account.", e);
        }
        return account;
    }

    public static void marshalAccount(Ostuarve ostuarve, OutputStream outputStream) {
        try {
            getAccountMarshaller().marshal(ostuarve, outputStream);
        } catch (JAXBException e) {
            handleAccountMarshalException(e);
        }
    }

    public static void marshalAccount(Ostuarve ostuarve, Writer writer) {
        try {
            getAccountMarshaller().marshal(ostuarve, writer);
        } catch (JAXBException e) {
            handleAccountMarshalException(e);
        }
    }

    private static void handleAccountMarshalException(JAXBException e) {
        String message = "Failed to marshal account.";
        LOG.debug(message, e);
        throw new RuntimeException(message, e);
    }

    private static Marshaller getAccountMarshaller() throws JAXBException {
        Marshaller marshaller = accountJaxbContext.createMarshaller();
        // event handler to print error messages to log
        marshaller.setSchema(accountJaxbSchema);
        marshaller.setEventHandler(new DefaultValidationEventHandler());
        return marshaller;
    }

    public static QName getDimensionAssocQName(String dimensionAssocName) {
        return QName.createQName(DimensionModel.URI, dimensionAssocName);
    }

    /**
     * @param transaction - if true try to parse account file, otherwise try to parse invoice file
     * @return pair of first account xml file and number:
     *         0 if no account file was found; 1 if exactly one account file was found;
     *         2 if more than one account file was found
     */
    public static Pair<File, Integer> getTransOrInvoiceFileAndCount(List<File> files, boolean transaction) {
        int transactionFileCount = 0;
        File transactionFile = null;
        for (File file : files) {
            Object parsedFile = null;
            if (transaction) {
                parsedFile = unmarshalAccount(BeanHelper.getFileFolderService().getReader(file.getNodeRef()).getContentInputStream());
            } else {
                parsedFile = unmarshalEInvoice(BeanHelper.getFileFolderService().getReader(file.getNodeRef()).getContentInputStream());
            }
            if (parsedFile != null) {
                transactionFileCount++;
                if (transactionFile == null) {
                    transactionFile = file;
                } else {
                    break;
                }
            }
        }
        return new Pair<File, Integer>(transactionFile, transactionFileCount);
    }

    public static boolean checkTransactionMandatoryFields(List<String> mandatoryProps, List<Pair<String, String>> errorMessages, List<String> addedErrorKeys,
            Transaction transaction) {
        boolean result = true;
        Map<QName, Serializable> transProps = RepoUtil.toQNameProperties(transaction.getNode().getProperties());
        for (Entry<QName, Serializable> entry : transProps.entrySet()) {
            QName entryKey = entry.getKey();
            String propName = entryKey.getLocalName();
            if (mandatoryProps.contains(propName) && isBlank(entry.getValue())) {
                if (errorMessages != null && addedErrorKeys != null) {
                    // collect all error messages
                    if (!addedErrorKeys.contains(propName)) {
                        errorMessages.add(new Pair<String, String>("task_finish_error_transaction_mandatory_not_filled", MessageUtil.getMessage("transaction_" + propName)));
                        addedErrorKeys.add(propName);
                    }
                    result = false;
                } else {
                    // return on first failure
                    return false;
                }
            }
        }
        return result;
    }

    public static boolean isBlank(Object value) {
        if (value instanceof String) {
            return StringUtils.isBlank((String) value);
        }
        return value == null;
    }

    public static boolean checkTotalSum(List<String> errorMessageKeys, String msgKeyPrefix, Double totalSum, List<Transaction> transactions,
            Map<NodeRef, Map<QName, Serializable>> originalProperties, boolean useVat) {
        if (totalSum == null) {
            return false;
        }
        if (transactions.size() == 0) {
            return true;
        }
        for (Transaction transaction : transactions) {
            if (transaction.getSumWithoutVat() == null) {
                errorMessageKeys.add(msgKeyPrefix + "transMissingSum");
                return false;
            }
            if (useVat && transaction.getInvoiceTaxPercent() == null) {
                errorMessageKeys.add(msgKeyPrefix + "transMissingTaxPercent");
                return false;
            }
        }
        double transTotalSum = getSumWithoutVat(transactions);
        if (useVat) {
            transTotalSum += getVatSum(transactions, originalProperties, BeanHelper.getEInvoiceService().getVatCodeDimensionValues());
        }
        boolean result = Math.abs(totalSum - transTotalSum) <= 0.001;
        if (!result) {
            errorMessageKeys.add(msgKeyPrefix + "transSumsNotCorrect");
        }
        return result;
    }

    /**
     * @return sum without vat rounded to two decimal places
     */
    public static double getSumWithoutVat(List<Transaction> transactions) {
        BigDecimal sum = new BigDecimal("0.0");
        for (Transaction transaction : transactions) {
            Double rowSumWithoutVat = transaction.getSumWithoutVat();
            if (rowSumWithoutVat != null) {
                sum = sum.add(BigDecimal.valueOf(rowSumWithoutVat));
            }
        }
        return roundDouble2Decimals(sum.doubleValue());
    }

    /**
     * @return vat sum rounded to two decimal places
     */
    public static double getVatSum(List<Transaction> transactions, Map<NodeRef, Map<QName, Serializable>> originalProperties, List<DimensionValue> vatCodeDimensionValues) {
        BigDecimal sum = new BigDecimal("0.0");
        for (Transaction transaction : transactions) {
            Double rowSumWithoutVat = transaction.getSumWithoutVat();
            Integer rowVatPercentage = getVatPercentage(transaction, originalProperties, vatCodeDimensionValues);
            if (rowSumWithoutVat != null) {
                sum = sum.add((BigDecimal.valueOf(rowSumWithoutVat)).multiply(BigDecimal.valueOf(rowVatPercentage)).divide(BigDecimal.valueOf(100)));
            }
        }
        return roundDouble2Decimals(sum.doubleValue());
    }

    // in case of saved and not changed invoice tax code, read percentage from transaction, otherwise from dimension
    private static Integer getVatPercentage(Transaction transaction, Map<NodeRef, Map<QName, Serializable>> originalProperties, List<DimensionValue> vatCodeDimesnionValues) {
        if (originalProperties == null) {
            return transaction.getInvoiceTaxPercent();
        }
        Map<QName, Serializable> originalProps = originalProperties.get(transaction.getNode().getNodeRef());
        String taxCode = transaction.getInvoiceTaxCode();
        String originalTaxCode = (String) (originalProps == null ? null : originalProps.get(TransactionModel.Props.INVOICE_TAX_CODE));
        if (originalProps == null || taxCode == null || (taxCode != null && !taxCode.equals(originalTaxCode))) {
            return getVatPercentageFromDimension(taxCode, vatCodeDimesnionValues);
        }
        return transaction.getInvoiceTaxPercent();
    }

    public static double roundDouble2Decimals(Double exactDouble) {
        return roundDouble(exactDouble, 2).doubleValue();

    }

    public static BigDecimal roundDouble4Decimals(Double exactDouble) {
        return roundDouble(exactDouble, 4);
    }

    private static BigDecimal roundDouble(Double exactDouble, int scale) {
        // don't use new BigDecimal(exactDouble) as it may lead to rounding errors
        return (BigDecimal.valueOf(exactDouble)).setScale(scale, BigDecimal.ROUND_HALF_UP);
    }

    // For calculations return 0 for null value
    public static Integer getVatPercentageFromDimension(String invoiceTaxCode, List<DimensionValue> vatCodeDimensionValues) {
        if (invoiceTaxCode == null) {
            return 0;
        }
        for (DimensionValue dimensionValue : vatCodeDimensionValues) {
            if (dimensionValue.getValueName().equalsIgnoreCase(invoiceTaxCode)) {
                try {
                    return Integer.parseInt(dimensionValue.getValue());
                } catch (NumberFormatException e) {
                    LOG.error("Illegal vat percentage: dimensionValue valueName=" + dimensionValue.getValueName() + ", value=" + dimensionValue.getValue()
                            + " is not valid integer.");
                    return 0;
                }
            }
        }
        return 0;
    }

    public static ArrayList<String> buildSearchableStringProp(QName propName, List<Transaction> transactions) {
        ArrayList<String> searchableProps = new ArrayList<String>(transactions.size());
        if (transactions != null) {
            for (Transaction transaction : transactions) {
                String propValue = (String) transaction.getNode().getProperties().get(propName);
                if (StringUtils.isNotEmpty(propValue)) {
                    searchableProps.add(propValue);
                }
            }
        }
        return searchableProps;
    }

    public static void copyTransactionProperties(Transaction transaction, Map<QName, Serializable> newProps) {
        Map<QName, Serializable> props = RepoUtil.toQNameProperties(transaction.getNode().getProperties());
        for (Map.Entry<QName, Serializable> entry : props.entrySet()) {
            QName propName = entry.getKey();
            if (propName.getNamespaceURI().equals(TransactionModel.URI)) {
                newProps.put(propName, entry.getValue());
            }
        }
    }

    public static Map<String, Object> getTransSearchableProperties(List<Transaction> transactions) {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(DocumentCommonModel.Props.SEARCHABLE_FUND.toString(), buildSearchableStringProp(TransactionModel.Props.FUND, transactions));
        properties.put(DocumentCommonModel.Props.SEARCHABLE_FUNDS_CENTER.toString(), buildSearchableStringProp(TransactionModel.Props.FUNDS_CENTER, transactions));
        properties.put(DocumentCommonModel.Props.SEARCHABLE_EA_COMMITMENT_ITEM.toString(), buildSearchableStringProp(TransactionModel.Props.EA_COMMITMENT_ITEM, transactions));
        return properties;
    }

    public static boolean isDateInPeriod(Date entryDate, Date beginDate, Date endDate) {
        return entryDate == null || ((beginDate == null || beginDate.before(entryDate) || DateUtils.isSameDay(entryDate, beginDate))
                && (endDate == null || endDate.after(entryDate) || DateUtils.isSameDay(entryDate, endDate)));
    }

    public static List<QName> getDimensionProperties(Dimensions dimension) {
        List<QName> properties = new ArrayList<QName>();
        for (Map.Entry<QName, Dimensions> entry : DIMENSION_PROPERTIES.entrySet()) {
            if (dimension.equals(entry.getValue())) {
                properties.add(entry.getKey());
            }
        }
        return properties;
    }

    public static DimensionValue findDimensionValueByValueName(final String currentValueName, List<DimensionValue> dimensionValues) {
        if (dimensionValues == null) {
            return null;
        }
        DimensionValue existingValue = (DimensionValue) CollectionUtils.find(dimensionValues, new Predicate() {

            @Override
            public boolean evaluate(Object arg0) {
                DimensionValue dimensionValue = (DimensionValue) arg0;
                if (StringUtils.equals(currentValueName, dimensionValue.getValueName())) {
                    return true;
                }
                return false;
            }
        });
        return existingValue;
    }

    public static void sortByDimensionValueName(List<DimensionValue> activeDimensionValues) {
        QuickSort quickSort = new QuickSort(activeDimensionValues, "ValueName", true, IDataContainer.SORT_CASEINSENSITIVE);
        quickSort.sort();
    }

}
=======
package ee.webmedia.alfresco.document.einvoice.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.data.IDataContainer;
import org.alfresco.web.data.QuickSort;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.bidimap.DualTreeBidiMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.einvoice.account.generated.Ostuarve;
import ee.webmedia.alfresco.document.einvoice.accountlist.generated.KontoNimekiri;
import ee.webmedia.alfresco.document.einvoice.dimensionslist.generated.DimensioonideNimekiri;
import ee.webmedia.alfresco.document.einvoice.generated.EInvoice;
import ee.webmedia.alfresco.document.einvoice.model.DimensionModel;
import ee.webmedia.alfresco.document.einvoice.model.DimensionValue;
import ee.webmedia.alfresco.document.einvoice.model.Dimensions;
import ee.webmedia.alfresco.document.einvoice.model.Transaction;
import ee.webmedia.alfresco.document.einvoice.model.TransactionModel;
import ee.webmedia.alfresco.document.einvoice.sellerslist.generated.HankijaNimekiri;
import ee.webmedia.alfresco.document.einvoice.vatcodelist.generated.KaibemaksuKoodNimekiri;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;

public class EInvoiceUtil {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(EInvoiceUtil.class);

    // TODO: unify with common-build.xml script einvoice definitions
    private static final String EINVOICE_PACKAGE = "ee.webmedia.alfresco.document.einvoice.generated";
    private static JAXBContext eInvoiceJaxbContext = initJaxbContext(EINVOICE_PACKAGE);
    private static Schema eInvoiceJaxbSchema = initSchema("e-invoice_ver1.1.xsd", EInvoice.class);
    private static final String W3C_XML_SCHEMA_NS_URI = "http://www.w3.org/2001/XMLSchema";

    private static final String DIMENSIONS_LIST_PACKAGE = "ee.webmedia.alfresco.document.einvoice.dimensionslist.generated";
    private static final JAXBContext dimensionsListJaxbContext = initJaxbContext(DIMENSIONS_LIST_PACKAGE);
    private static final Schema dimensionsListJaxbSchema = initSchema("DimensioonideNimekiri.xsd", DimensioonideNimekiri.class);

    private static final String SELLERS_LIST_PACKAGE = "ee.webmedia.alfresco.document.einvoice.sellerslist.generated";
    private static final JAXBContext sellersListJaxbContext = initJaxbContext(SELLERS_LIST_PACKAGE);
    private static final Schema sellersListJaxbSchema = initSchema("HankijaNimekiri.xsd", HankijaNimekiri.class);

    private static final String VAT_CODES_LIST_PACKAGE = "ee.webmedia.alfresco.document.einvoice.vatcodelist.generated";
    private static final JAXBContext vatCodesListJaxbContext = initJaxbContext(VAT_CODES_LIST_PACKAGE);
    private static final Schema vatCodesListJaxbSchema = initSchema("KaibemaksuKoodNimekiri.xsd", KaibemaksuKoodNimekiri.class);

    private static final String ACCOUNTS_LIST_PACKAGE = "ee.webmedia.alfresco.document.einvoice.accountlist.generated";
    private static final JAXBContext accountsListJaxbContext = initJaxbContext(ACCOUNTS_LIST_PACKAGE);
    private static final Schema accountsListJaxbSchema = initSchema("KontoNimekiri.xsd", KontoNimekiri.class);

    private static final String ACCOUNT_PACKAGE = "ee.webmedia.alfresco.document.einvoice.account.generated";
    private static final JAXBContext accountJaxbContext = initJaxbContext(ACCOUNT_PACKAGE);
    private static final Schema accountJaxbSchema = initSchema("OstuarveKonteering.xsd", Ostuarve.class);

    public static final Dimensions ACCOUNT_DIMENSION = Dimensions.INVOICE_ACCOUNTS;
    public static final Dimensions VAT_CODE_DIMENSION = Dimensions.TAX_CODE_ITEMS;
    /** TODO: actually it would be better to use generic bidirectional map (from guava? refactored Commons-Collections?) here */
    public static final BidiMap /* <Parameters, Dimensions> */DIMENSION_PARAMETERS;

    public static final Map<QName, Dimensions> DIMENSION_PROPERTIES;

    public static final String XXL_INVOICE_TYPE = "XXL";

    static {
        DIMENSION_PARAMETERS = new DualTreeBidiMap /* <Parameters, Dimensions> */();
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_FUNDS_CENTERS, Dimensions.INVOICE_FUNDS_CENTERS);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_COST_CENTERS, Dimensions.INVOICE_COST_CENTERS);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_FUNDS, Dimensions.INVOICE_FUNDS);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_COMMITMENT_ITEM, Dimensions.INVOICE_COMMITMENT_ITEM);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_INTERNAL_ORDERS, Dimensions.INVOICE_INTERNAL_ORDERS);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_ASSET_INVENTORY_NUMBERS, Dimensions.INVOICE_ASSET_INVENTORY_NUMBERS);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_TRADING_PARTNER_CODES, Dimensions.INVOICE_TRADING_PARTNER_CODES);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_FUNCTIONAL_AREA_CODE, Dimensions.INVOICE_FUNCTIONAL_AREA_CODE);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_CASH_FLOW_CODES, Dimensions.INVOICE_CASH_FLOW_CODES);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_SOURCE_CODES, Dimensions.INVOICE_SOURCE_CODES);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_POSTING_KEY, Dimensions.INVOICE_POSTING_KEY);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_PAYMENT_METHOD_CODES, Dimensions.INVOICE_PAYMENT_METHOD_CODES);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_HOUSE_BANK_CODES, Dimensions.INVOICE_HOUSE_BANK_CODES);

        DIMENSION_PROPERTIES = new HashMap<QName, Dimensions>();
        DIMENSION_PROPERTIES.put(TransactionModel.Props.ACCOUNT, Dimensions.INVOICE_ACCOUNTS);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.ASSET_INVENTORY_NUMBER, Dimensions.INVOICE_ASSET_INVENTORY_NUMBERS);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.CASH_FLOW_CODE, Dimensions.INVOICE_CASH_FLOW_CODES);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.COMMITMENT_ITEM, Dimensions.INVOICE_COMMITMENT_ITEM);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.COST_CENTER, Dimensions.INVOICE_COST_CENTERS);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.EA_COMMITMENT_ITEM, Dimensions.INVOICE_COMMITMENT_ITEM);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.FUNCTIONAL_ARE_CODE, Dimensions.INVOICE_FUNCTIONAL_AREA_CODE);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.FUND, Dimensions.INVOICE_FUNDS);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.FUNDS_CENTER, Dimensions.INVOICE_FUNDS_CENTERS);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.HOUSE_BANK, Dimensions.INVOICE_HOUSE_BANK_CODES);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.ORDER_NUMBER, Dimensions.INVOICE_INTERNAL_ORDERS);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.PAYMENT_METHOD, Dimensions.INVOICE_PAYMENT_METHOD_CODES);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.POSTING_KEY, Dimensions.INVOICE_POSTING_KEY);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.SOURCE, Dimensions.INVOICE_SOURCE_CODES);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.TRADING_PARTNER_CODE, Dimensions.INVOICE_TRADING_PARTNER_CODES);
        DIMENSION_PROPERTIES.put(TransactionModel.Props.INVOICE_TAX_CODE, Dimensions.TAX_CODE_ITEMS);
        // turn on jaxb debug
        // TODO: turn off when jaxb import has been tested in client environment (or make switchable by general log settings)
        System.setProperty("jaxb.debug", "true");
    }

    public static NumberFormat getInvoiceNumberFormat() {
        NumberFormat invoiceDecimalFormat = DecimalFormat.getInstance(new Locale("et", "EE", ""));
        invoiceDecimalFormat.setMinimumIntegerDigits(1);
        invoiceDecimalFormat.setMinimumFractionDigits(2);
        invoiceDecimalFormat.setMaximumFractionDigits(2);
        invoiceDecimalFormat.setGroupingUsed(true);
        return invoiceDecimalFormat;
    }

    public static JAXBContext initJaxbContext(String destPackage) {
        try {
            return JAXBContext.newInstance(destPackage);
        } catch (Exception e) {
            LOG.error("Error getting jaxb context.", e);
            throw new RuntimeException(e);
        }
    }

    public static Schema initSchema(String xsd, Class<?> clazz) {
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(new StreamSource(clazz.getResourceAsStream(xsd)));
            return schema;
        } catch (Exception e) {
            LOG.error("Error getting jaxb schema.", e);
            throw new RuntimeException(e);
        }
    }

    public static Unmarshaller getEInvoiceUnmarshaller() throws JAXBException {
        Unmarshaller unmarshaller = getUnmarshaller(eInvoiceJaxbContext, eInvoiceJaxbSchema);
        unmarshaller.setEventHandler(new EInvoiceValidationEventHandler());
        return unmarshaller;
    }

    public static Unmarshaller getDimensionsListUnmarshaller() throws JAXBException {
        return getUnmarshaller(dimensionsListJaxbContext, dimensionsListJaxbSchema);
    }

    public static Unmarshaller getSellersListUnmarshaller() throws JAXBException {
        return getUnmarshaller(sellersListJaxbContext, sellersListJaxbSchema);
    }

    public static Unmarshaller getAccountsListUnmarshaller() throws JAXBException {
        return getUnmarshaller(accountsListJaxbContext, accountsListJaxbSchema);
    }

    public static Unmarshaller getVatCodesListUnmarshaller() throws JAXBException {
        return getUnmarshaller(vatCodesListJaxbContext, vatCodesListJaxbSchema);
    }

    public static Unmarshaller getAccountUnmarshaller() throws JAXBException {
        return getUnmarshaller(accountJaxbContext, accountJaxbSchema);
    }

    public static Unmarshaller getUnmarshaller(JAXBContext jaxbContext, Schema jaxbSchema) throws JAXBException {
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        unmarshaller.setSchema(jaxbSchema);
        // event handler to print error messages to log
        unmarshaller.setEventHandler(new DefaultValidationEventHandler());
        return unmarshaller;
    }

    public static EInvoice unmarshalEInvoice(org.w3c.dom.Node input) {
        EInvoice einvoice = null;
        try {
            einvoice = (EInvoice) getEInvoiceUnmarshaller().unmarshal(input);
        } catch (JAXBException e) {
            LOG.debug("Failed to unmarshal einvoice.", e);
        }
        return einvoice;
    }

    public static EInvoice unmarshalEInvoice(InputStream input) {
        EInvoice einvoice = null;
        try {
            einvoice = (EInvoice) getEInvoiceUnmarshaller().unmarshal(input);
        } catch (JAXBException e) {
            LOG.debug("Failed to unmarshal einvoice.", e);
        }
        return einvoice;
    }

    public static EInvoice unmarshalEInvoice(java.io.File input) {
        EInvoice einvoice = null;
        try {
            einvoice = (EInvoice) getEInvoiceUnmarshaller().unmarshal(input);
        } catch (JAXBException e) {
            LOG.debug("Failed to unmarshal einvoice.", e);
        }
        return einvoice;
    }

    public static DimensioonideNimekiri unmarshalDimensionsList(InputStream input) {
        DimensioonideNimekiri dimensionsList = null;
        try {
            dimensionsList = (DimensioonideNimekiri) getDimensionsListUnmarshaller().unmarshal(input);
        } catch (JAXBException e) {
            LOG.debug("Failed to unmarshal dimensions list.", e);
        }
        return dimensionsList;
    }

    public static HankijaNimekiri unmarshalSellersList(InputStream input) {
        HankijaNimekiri sellersList = null;
        try {
            sellersList = (HankijaNimekiri) getSellersListUnmarshaller().unmarshal(input);
        } catch (JAXBException e) {
            LOG.debug("Failed to unmarshal sellers list.", e);
        }
        return sellersList;
    }

    public static KaibemaksuKoodNimekiri unmarshalVatCodesList(InputStream input) {
        KaibemaksuKoodNimekiri vatCodesList = null;
        try {
            vatCodesList = (KaibemaksuKoodNimekiri) getVatCodesListUnmarshaller().unmarshal(input);
        } catch (JAXBException e) {
            LOG.debug("Failed to unmarshal vat codes list.", e);
        }
        return vatCodesList;
    }

    public static KontoNimekiri unmarshalAccountsList(InputStream input) {
        KontoNimekiri accountsList = null;
        try {
            accountsList = (KontoNimekiri) getAccountsListUnmarshaller().unmarshal(input);
        } catch (JAXBException e) {
            LOG.debug("Failed to unmarshal accounts list.", e);
        }
        return accountsList;
    }

    public static Ostuarve unmarshalAccount(InputStream input) {
        Ostuarve account = null;
        try {
            account = (Ostuarve) getAccountUnmarshaller().unmarshal(input);
        } catch (JAXBException e) {
            LOG.debug("Failed to unmarshal account.", e);
        }
        return account;
    }

    public static Ostuarve unmarshalAccount(Reader reader) {
        Ostuarve account = null;
        try {
            account = (Ostuarve) getAccountUnmarshaller().unmarshal(reader);
        } catch (JAXBException e) {
            LOG.debug("Failed to unmarshal account.", e);
        }
        return account;
    }

    public static void marshalAccount(Ostuarve ostuarve, OutputStream outputStream) {
        try {
            getAccountMarshaller().marshal(ostuarve, outputStream);
        } catch (JAXBException e) {
            handleAccountMarshalException(e);
        }
    }

    public static void marshalAccount(Ostuarve ostuarve, Writer writer) {
        try {
            getAccountMarshaller().marshal(ostuarve, writer);
        } catch (JAXBException e) {
            handleAccountMarshalException(e);
        }
    }

    private static void handleAccountMarshalException(JAXBException e) {
        String message = "Failed to marshal account.";
        LOG.debug(message, e);
        throw new RuntimeException(message, e);
    }

    private static Marshaller getAccountMarshaller() throws JAXBException {
        Marshaller marshaller = accountJaxbContext.createMarshaller();
        // event handler to print error messages to log
        marshaller.setSchema(accountJaxbSchema);
        marshaller.setEventHandler(new DefaultValidationEventHandler());
        return marshaller;
    }

    public static QName getDimensionAssocQName(String dimensionAssocName) {
        return QName.createQName(DimensionModel.URI, dimensionAssocName);
    }

    /**
     * @param transaction - if true try to parse account file, otherwise try to parse invoice file
     * @return pair of first account xml file and number:
     *         0 if no account file was found; 1 if exactly one account file was found;
     *         2 if more than one account file was found
     */
    public static Pair<File, Integer> getTransOrInvoiceFileAndCount(List<File> files, boolean transaction) {
        int transactionFileCount = 0;
        File transactionFile = null;
        for (File file : files) {
            Object parsedFile = null;
            if (transaction) {
                parsedFile = unmarshalAccount(BeanHelper.getFileFolderService().getReader(file.getNodeRef()).getContentInputStream());
            } else {
                parsedFile = unmarshalEInvoice(BeanHelper.getFileFolderService().getReader(file.getNodeRef()).getContentInputStream());
            }
            if (parsedFile != null) {
                transactionFileCount++;
                if (transactionFile == null) {
                    transactionFile = file;
                } else {
                    break;
                }
            }
        }
        return new Pair<File, Integer>(transactionFile, transactionFileCount);
    }

    public static boolean checkTransactionMandatoryFields(List<String> mandatoryProps, List<Pair<String, String>> errorMessages, List<String> addedErrorKeys,
            Transaction transaction) {
        boolean result = true;
        Map<QName, Serializable> transProps = RepoUtil.toQNameProperties(transaction.getNode().getProperties());
        for (Entry<QName, Serializable> entry : transProps.entrySet()) {
            QName entryKey = entry.getKey();
            String propName = entryKey.getLocalName();
            if (mandatoryProps.contains(propName) && isBlank(entry.getValue())) {
                if (errorMessages != null && addedErrorKeys != null) {
                    // collect all error messages
                    if (!addedErrorKeys.contains(propName)) {
                        errorMessages.add(new Pair<String, String>("task_finish_error_transaction_mandatory_not_filled", MessageUtil.getMessage("transaction_" + propName)));
                        addedErrorKeys.add(propName);
                    }
                    result = false;
                } else {
                    // return on first failure
                    return false;
                }
            }
        }
        return result;
    }

    public static boolean isBlank(Object value) {
        if (value instanceof String) {
            return StringUtils.isBlank((String) value);
        }
        return value == null;
    }

    public static boolean checkTotalSum(List<String> errorMessageKeys, String msgKeyPrefix, Double totalSum, List<Transaction> transactions,
            Map<NodeRef, Map<QName, Serializable>> originalProperties, boolean useVat) {
        if (totalSum == null) {
            return false;
        }
        if (transactions.size() == 0) {
            return true;
        }
        for (Transaction transaction : transactions) {
            if (transaction.getSumWithoutVat() == null) {
                errorMessageKeys.add(msgKeyPrefix + "transMissingSum");
                return false;
            }
            if (useVat && transaction.getInvoiceTaxPercent() == null) {
                errorMessageKeys.add(msgKeyPrefix + "transMissingTaxPercent");
                return false;
            }
        }
        double transTotalSum = getSumWithoutVat(transactions);
        if (useVat) {
            transTotalSum += getVatSum(transactions, originalProperties, BeanHelper.getEInvoiceService().getVatCodeDimensionValues());
        }
        boolean result = Math.abs(totalSum - transTotalSum) <= 0.001;
        if (!result) {
            errorMessageKeys.add(msgKeyPrefix + "transSumsNotCorrect");
        }
        return result;
    }

    /**
     * @return sum without vat rounded to two decimal places
     */
    public static double getSumWithoutVat(List<Transaction> transactions) {
        BigDecimal sum = new BigDecimal("0.0");
        for (Transaction transaction : transactions) {
            Double rowSumWithoutVat = transaction.getSumWithoutVat();
            if (rowSumWithoutVat != null) {
                sum = sum.add(BigDecimal.valueOf(rowSumWithoutVat));
            }
        }
        return roundDouble2Decimals(sum.doubleValue());
    }

    /**
     * @return vat sum rounded to two decimal places
     */
    public static double getVatSum(List<Transaction> transactions, Map<NodeRef, Map<QName, Serializable>> originalProperties, List<DimensionValue> vatCodeDimensionValues) {
        BigDecimal sum = new BigDecimal("0.0");
        for (Transaction transaction : transactions) {
            Double rowSumWithoutVat = transaction.getSumWithoutVat();
            Integer rowVatPercentage = getVatPercentage(transaction, originalProperties, vatCodeDimensionValues);
            if (rowSumWithoutVat != null) {
                sum = sum.add((BigDecimal.valueOf(rowSumWithoutVat)).multiply(BigDecimal.valueOf(rowVatPercentage)).divide(BigDecimal.valueOf(100)));
            }
        }
        return roundDouble2Decimals(sum.doubleValue());
    }

    // in case of saved and not changed invoice tax code, read percentage from transaction, otherwise from dimension
    private static Integer getVatPercentage(Transaction transaction, Map<NodeRef, Map<QName, Serializable>> originalProperties, List<DimensionValue> vatCodeDimesnionValues) {
        if (originalProperties == null) {
            return transaction.getInvoiceTaxPercent();
        }
        Map<QName, Serializable> originalProps = originalProperties.get(transaction.getNode().getNodeRef());
        String taxCode = transaction.getInvoiceTaxCode();
        String originalTaxCode = (String) (originalProps == null ? null : originalProps.get(TransactionModel.Props.INVOICE_TAX_CODE));
        if (originalProps == null || taxCode == null || (taxCode != null && !taxCode.equals(originalTaxCode))) {
            return getVatPercentageFromDimension(taxCode, vatCodeDimesnionValues);
        }
        return transaction.getInvoiceTaxPercent();
    }

    public static double roundDouble2Decimals(Double exactDouble) {
        return roundDouble(exactDouble, 2).doubleValue();

    }

    public static BigDecimal roundDouble4Decimals(Double exactDouble) {
        return roundDouble(exactDouble, 4);
    }

    private static BigDecimal roundDouble(Double exactDouble, int scale) {
        // don't use new BigDecimal(exactDouble) as it may lead to rounding errors
        return (BigDecimal.valueOf(exactDouble)).setScale(scale, BigDecimal.ROUND_HALF_UP);
    }

    // For calculations return 0 for null value
    public static Integer getVatPercentageFromDimension(String invoiceTaxCode, List<DimensionValue> vatCodeDimensionValues) {
        if (invoiceTaxCode == null) {
            return 0;
        }
        for (DimensionValue dimensionValue : vatCodeDimensionValues) {
            if (dimensionValue.getValueName().equalsIgnoreCase(invoiceTaxCode)) {
                try {
                    return Integer.parseInt(dimensionValue.getValue());
                } catch (NumberFormatException e) {
                    LOG.error("Illegal vat percentage: dimensionValue valueName=" + dimensionValue.getValueName() + ", value=" + dimensionValue.getValue()
                            + " is not valid integer.");
                    return 0;
                }
            }
        }
        return 0;
    }

    public static ArrayList<String> buildSearchableStringProp(QName propName, List<Transaction> transactions) {
        ArrayList<String> searchableProps = new ArrayList<String>(transactions.size());
        if (transactions != null) {
            for (Transaction transaction : transactions) {
                String propValue = (String) transaction.getNode().getProperties().get(propName);
                if (StringUtils.isNotEmpty(propValue)) {
                    searchableProps.add(propValue);
                }
            }
        }
        return searchableProps;
    }

    public static void copyTransactionProperties(Transaction transaction, Map<QName, Serializable> newProps) {
        Map<QName, Serializable> props = RepoUtil.toQNameProperties(transaction.getNode().getProperties());
        for (Map.Entry<QName, Serializable> entry : props.entrySet()) {
            QName propName = entry.getKey();
            if (propName.getNamespaceURI().equals(TransactionModel.URI)) {
                newProps.put(propName, entry.getValue());
            }
        }
    }

    public static Map<String, Object> getTransSearchableProperties(List<Transaction> transactions) {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(DocumentCommonModel.Props.SEARCHABLE_FUND.toString(), buildSearchableStringProp(TransactionModel.Props.FUND, transactions));
        properties.put(DocumentCommonModel.Props.SEARCHABLE_FUNDS_CENTER.toString(), buildSearchableStringProp(TransactionModel.Props.FUNDS_CENTER, transactions));
        properties.put(DocumentCommonModel.Props.SEARCHABLE_EA_COMMITMENT_ITEM.toString(), buildSearchableStringProp(TransactionModel.Props.EA_COMMITMENT_ITEM, transactions));
        return properties;
    }

    public static boolean isDateInPeriod(Date entryDate, Date beginDate, Date endDate) {
        return entryDate == null || ((beginDate == null || beginDate.before(entryDate) || DateUtils.isSameDay(entryDate, beginDate))
                && (endDate == null || endDate.after(entryDate) || DateUtils.isSameDay(entryDate, endDate)));
    }

    public static List<QName> getDimensionProperties(Dimensions dimension) {
        List<QName> properties = new ArrayList<QName>();
        for (Map.Entry<QName, Dimensions> entry : DIMENSION_PROPERTIES.entrySet()) {
            if (dimension.equals(entry.getValue())) {
                properties.add(entry.getKey());
            }
        }
        return properties;
    }

    public static DimensionValue findDimensionValueByValueName(final String currentValueName, List<DimensionValue> dimensionValues) {
        if (dimensionValues == null) {
            return null;
        }
        DimensionValue existingValue = (DimensionValue) CollectionUtils.find(dimensionValues, new Predicate() {

            @Override
            public boolean evaluate(Object arg0) {
                DimensionValue dimensionValue = (DimensionValue) arg0;
                if (StringUtils.equals(currentValueName, dimensionValue.getValueName())) {
                    return true;
                }
                return false;
            }
        });
        return existingValue;
    }

    public static void sortByDimensionValueName(List<DimensionValue> activeDimensionValues) {
        QuickSort quickSort = new QuickSort(activeDimensionValues, "ValueName", true, IDataContainer.SORT_CASEINSENSITIVE);
        quickSort.sort();
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
