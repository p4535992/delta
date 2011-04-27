package ee.webmedia.alfresco.document.einvoice.service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.einvoice.account.generated.Ostuarve;
import ee.webmedia.alfresco.document.einvoice.accountlist.generated.KontoNimekiri;
import ee.webmedia.alfresco.document.einvoice.dimensionslist.generated.DimensioonideNimekiri;
import ee.webmedia.alfresco.document.einvoice.generated.EInvoice;
import ee.webmedia.alfresco.document.einvoice.model.DimensionModel;
import ee.webmedia.alfresco.document.einvoice.model.Dimensions;
import ee.webmedia.alfresco.document.einvoice.sellerslist.generated.HankijaNimekiri;
import ee.webmedia.alfresco.document.einvoice.vatcodelist.generated.KaibemaksuKoodNimekiri;
import ee.webmedia.alfresco.parameters.model.Parameters;

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
    public static final Map<Parameters, Dimensions> DIMENSION_PARAMETERS;

    static {
        DIMENSION_PARAMETERS = new HashMap<Parameters, Dimensions>();
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_FUNDS_CENTERS, Dimensions.INVOICE_FUNDS_CENTERS);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_COST_CENTERS, Dimensions.INVOICE_COST_CENTERS);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_FUNDS, Dimensions.INVOICE_FUNDS);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_COMMITMENT_ITEM, Dimensions.INVOICE_COMMITMENT_ITEM);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_INTERNAL_ORDERS, Dimensions.INVOICE_INTERNAL_ORDERS);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_ASSET_INVENTORY_NUMBERS, Dimensions.INVOICE_ASSET_INVENTORY_NUMBERS);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_POSTING_KEY, Dimensions.INVOICE_POSTING_KEY);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_TRADING_PARTNER_CODES, Dimensions.INVOICE_TRADING_PARTNER_CODES);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_FUNCTIONAL_AREA_CODE, Dimensions.INVOICE_FUNCTIONAL_AREA_CODE);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_CASH_FLOW_CODES, Dimensions.INVOICE_CASH_FLOW_CODES);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_SOURCE_CODES, Dimensions.INVOICE_SOURCE_CODES);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_PAYMENT_METHOD_CODES, Dimensions.INVOICE_PAYMENT_METHOD_CODES);
        DIMENSION_PARAMETERS.put(Parameters.DIMENSION_CODE_INVOICE_HOUSE_BANK_CODES, Dimensions.INVOICE_HOUSE_BANK_CODES);
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
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
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

    public static Unmarshaller getUnmarshaller(JAXBContext jaxbContext, Schema jaxbSchema) throws JAXBException {
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        unmarshaller.setSchema(jaxbSchema);
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

    public static QName getDimensionAssocQName(String dimensionAssocName) {
        return QName.createQName(DimensionModel.URI, dimensionAssocName);
    }

}
