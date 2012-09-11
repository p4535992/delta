package ee.webmedia.alfresco.document.einvoice.model;

import java.io.Serializable;

import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.WmNode;

public class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;
    // Use WmNode as we want to use it both for saved and unsaved transactions
    private final WmNode node;

    public Transaction(WmNode node) {
        Assert.notNull(node);
        this.node = node;
    }

    public WmNode getNode() {
        return node;
    }

    public String getFundsCenter() {
        return (String) node.getProperties().get(TransactionModel.Props.FUNDS_CENTER);
    }

    public void setFundsCenter(String fundsCenter) {
        node.getProperties().put(TransactionModel.Props.FUNDS_CENTER.toString(), fundsCenter);
    }

    public String getCostCenter() {
        return (String) node.getProperties().get(TransactionModel.Props.COST_CENTER);
    }

    public void setCostCenter(String costCenter) {
        node.getProperties().put(TransactionModel.Props.COST_CENTER.toString(), costCenter);
    }

    public String getFund() {
        return (String) node.getProperties().get(TransactionModel.Props.FUND);
    }

    public void setFund(String fund) {
        node.getProperties().put(TransactionModel.Props.FUND.toString(), fund);
    }

    public String getEaCommitmentItem() {
        return (String) node.getProperties().get(TransactionModel.Props.EA_COMMITMENT_ITEM);
    }

    public void setEaCommitmentItem(String eaCommitmentItem) {
        node.getProperties().put(TransactionModel.Props.EA_COMMITMENT_ITEM.toString(), eaCommitmentItem);
    }

    public String getCommitmentItem() {
        return (String) node.getProperties().get(TransactionModel.Props.COMMITMENT_ITEM);
    }

    public void setCommitmentItem(String commitmentItem) {
        node.getProperties().put(TransactionModel.Props.COMMITMENT_ITEM.toString(), commitmentItem);
    }

    public String getOrderNumber() {
        return (String) node.getProperties().get(TransactionModel.Props.ORDER_NUMBER);
    }

    public void setOrderNumber(String orderNumber) {
        node.getProperties().put(TransactionModel.Props.ORDER_NUMBER.toString(), orderNumber);
    }

    public String getAssetInventoryNumber() {
        return (String) node.getProperties().get(TransactionModel.Props.ASSET_INVENTORY_NUMBER);
    }

    public void setAssetInventoryNumber(String assetInventoryNumber) {
        node.getProperties().put(TransactionModel.Props.ASSET_INVENTORY_NUMBER.toString(), assetInventoryNumber);
    }

    public Double getSumWithoutVat() {
        return (Double) node.getProperties().get(TransactionModel.Props.SUM_WITHOUT_VAT);
    }

    public void setSumWithoutVat(Double sumWithoutVat) {
        node.getProperties().put(TransactionModel.Props.SUM_WITHOUT_VAT.toString(), sumWithoutVat);
    }

    public String getPostingKey() {
        return (String) node.getProperties().get(TransactionModel.Props.POSTING_KEY);
    }

    public void setPostingKey(String postingKey) {
        node.getProperties().put(TransactionModel.Props.POSTING_KEY.toString(), postingKey);
    }

    public String getAccount() {
        return (String) node.getProperties().get(TransactionModel.Props.ACCOUNT);
    }

    public void setAccount(String account) {
        node.getProperties().put(TransactionModel.Props.ACCOUNT.toString(), account);
    }

    public String getInvoiceTaxCode() {
        return (String) node.getProperties().get(TransactionModel.Props.INVOICE_TAX_CODE);
    }

    public void setInvoiceTaxCode(String invoiceTaxCode) {
        node.getProperties().put(TransactionModel.Props.INVOICE_TAX_CODE.toString(), invoiceTaxCode);
    }

    public Integer getInvoiceTaxPercent() {
        return (Integer) node.getProperties().get(TransactionModel.Props.INVOICE_TAX_PERCENT);
    }

    public void setInvoiceTaxPercent(Integer invoiceTaxPercent) {
        node.getProperties().put(TransactionModel.Props.INVOICE_TAX_PERCENT.toString(), invoiceTaxPercent);
    }

    public String getTradingPartnerCode() {
        return (String) node.getProperties().get(TransactionModel.Props.TRADING_PARTNER_CODE);
    }

    public void setTradingPartnerCode(String tradingPartnerCode) {
        node.getProperties().put(TransactionModel.Props.TRADING_PARTNER_CODE.toString(), tradingPartnerCode);
    }

    public String getFunctionalAreaCode() {
        return (String) node.getProperties().get(TransactionModel.Props.FUNCTIONAL_ARE_CODE);
    }

    public void setFunctionalAreaCode(String functionalAreaCode) {
        node.getProperties().put(TransactionModel.Props.FUNCTIONAL_ARE_CODE.toString(), functionalAreaCode);
    }

    public String getCashFlowCode() {
        return (String) node.getProperties().get(TransactionModel.Props.CASH_FLOW_CODE);
    }

    public void setCashFlowCode(String cashFlowCode) {
        node.getProperties().put(TransactionModel.Props.CASH_FLOW_CODE.toString(), cashFlowCode);
    }

    public String getSource() {
        return (String) node.getProperties().get(TransactionModel.Props.SOURCE);
    }

    public void setSource(String source) {
        node.getProperties().put(TransactionModel.Props.SOURCE.toString(), source);
    }

    public String getPaymentMethod() {
        return (String) node.getProperties().get(TransactionModel.Props.PAYMENT_METHOD);
    }

    public void setPaymentMethod(String paymentMethod) {
        node.getProperties().put(TransactionModel.Props.PAYMENT_METHOD.toString(), paymentMethod);
    }

    public String getHouseBank() {
        return (String) node.getProperties().get(TransactionModel.Props.HOUSE_BANK);
    }

    public void setHouseBank(String houseBank) {
        node.getProperties().put(TransactionModel.Props.HOUSE_BANK.toString(), houseBank);
    }

    public String getEntryContent() {
        return (String) node.getProperties().get(TransactionModel.Props.ENTRY_CONTENT);
    }

    public void setEntryContent(String entryContent) {
        node.getProperties().put(TransactionModel.Props.ENTRY_CONTENT.toString(), entryContent);
    }

}
