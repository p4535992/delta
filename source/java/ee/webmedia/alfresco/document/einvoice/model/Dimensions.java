package ee.webmedia.alfresco.document.einvoice.model;

import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.document.einvoice.model.DimensionModel.Repo;

public enum Dimensions {
    INVOICE_FUNDS_CENTERS("invoiceFundsCenters"),
    INVOICE_COST_CENTERS("invoiceCostCenters"),
    INVOICE_FUNDS("invoiceFunds"),
    INVOICE_COMMITMENT_ITEM("invoiceCommitmentItem"),
    INVOICE_INTERNAL_ORDERS("invoiceInternalOrders"),
    INVOICE_ASSET_INVENTORY_NUMBERS("invoiceAssetInventoryNumbers"),
    INVOICE_POSTING_KEY("invoicePostingKey"),
    INVOICE_TRADING_PARTNER_CODES("invoiceTradingPartnerCodes"),
    INVOICE_FUNCTIONAL_AREA_CODE("invoiceFunctionalAreaCode"),
    INVOICE_CASH_FLOW_CODES("invoiceCashFlowCodes"),
    INVOICE_SOURCE_CODES("invoiceSourceCodes"),
    INVOICE_PAYMENT_METHOD_CODES("invoicePaymentMethodCodes"),
    INVOICE_HOUSE_BANK_CODES("invoiceHouseBankCodes"),
    INVOICE_ACCOUNTS("invoiceAccounts"),
    TAX_CODE_ITEMS("taxCodeItems");

    private String xPath;
    private String dimensionName;

    Dimensions(String dimensionName) {
        xPath = Repo.DIMENSIONS_SPACE + "/" + DimensionModel.NAMESPACE_PREFFIX + dimensionName;
        this.dimensionName = dimensionName;
    }

    public static Dimensions get(String dimensionName) {
        final Dimensions[] values = Dimensions.values();
        for (Dimensions dimension : values) {
            if (dimension.dimensionName.equals(dimensionName)) {
                return dimension;
            }
        }
        throw new IllegalArgumentException("Unknown dimensionName: " + dimensionName + ". Known values: " + StringUtils.join(values, ", "));
    }

    @Override
    public String toString() {
        return xPath;
    }

    public String getDimensionName() {
        return dimensionName;
    }
}
