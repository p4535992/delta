<<<<<<< HEAD
package ee.webmedia.alfresco.document.einvoice.model;

import org.alfresco.service.namespace.QName;

public interface TransactionModel {
    String URI = "http://alfresco.webmedia.ee/model/transaction/1.0";
    String PREFIX = "tra:";

    interface Types {
        QName TRANSACTION = QName.createQName(URI, "transaction");
        QName TRANSACTION_TEMPLATE = QName.createQName(URI, "transactionTemplate");
    }

    interface Associations {
        QName TRANSACTION = QName.createQName(URI, "transaction");
        QName TRANSACTION_TEMPLATE = QName.createQName(URI, "transactionTemplate");
    }

    interface Props {
        QName FUNDS_CENTER = QName.createQName(URI, "fundsCenter");
        QName COST_CENTER = QName.createQName(URI, "costCenter");
        QName FUND = QName.createQName(URI, "fund");
        QName EA_COMMITMENT_ITEM = QName.createQName(URI, "eaCommitmentItem");
        QName COMMITMENT_ITEM = QName.createQName(URI, "commitmentItem");
        QName ORDER_NUMBER = QName.createQName(URI, "orderNumber");
        QName ASSET_INVENTORY_NUMBER = QName.createQName(URI, "assetInventoryNumber");
        QName SUM_WITHOUT_VAT = QName.createQName(URI, "sumWithoutVat");
        QName POSTING_KEY = QName.createQName(URI, "postingKey");
        QName ACCOUNT = QName.createQName(URI, "account");
        QName INVOICE_TAX_CODE = QName.createQName(URI, "invoiceTaxCode");
        QName INVOICE_TAX_PERCENT = QName.createQName(URI, "invoiceTaxPercent");
        QName TRADING_PARTNER_CODE = QName.createQName(URI, "tradingPartnerCode");
        QName FUNCTIONAL_ARE_CODE = QName.createQName(URI, "functionalAreaCode");
        QName CASH_FLOW_CODE = QName.createQName(URI, "cashFlowCode");
        QName SOURCE = QName.createQName(URI, "source");
        QName PAYMENT_METHOD = QName.createQName(URI, "paymentMethod");
        QName HOUSE_BANK = QName.createQName(URI, "houseBank");
        QName ENTRY_CONTENT = QName.createQName(URI, "entryContent");

        // transaction template properties
        QName NAME = QName.createQName(URI, "name");
        QName ACTIVE = QName.createQName(URI, "active");
    }

}
=======
package ee.webmedia.alfresco.document.einvoice.model;

import org.alfresco.service.namespace.QName;

public interface TransactionModel {
    String URI = "http://alfresco.webmedia.ee/model/transaction/1.0";
    String PREFIX = "tra:";

    interface Types {
        QName TRANSACTION = QName.createQName(URI, "transaction");
        QName TRANSACTION_TEMPLATE = QName.createQName(URI, "transactionTemplate");
    }

    interface Associations {
        QName TRANSACTION = QName.createQName(URI, "transaction");
        QName TRANSACTION_TEMPLATE = QName.createQName(URI, "transactionTemplate");
    }

    interface Props {
        QName FUNDS_CENTER = QName.createQName(URI, "fundsCenter");
        QName COST_CENTER = QName.createQName(URI, "costCenter");
        QName FUND = QName.createQName(URI, "fund");
        QName EA_COMMITMENT_ITEM = QName.createQName(URI, "eaCommitmentItem");
        QName COMMITMENT_ITEM = QName.createQName(URI, "commitmentItem");
        QName ORDER_NUMBER = QName.createQName(URI, "orderNumber");
        QName ASSET_INVENTORY_NUMBER = QName.createQName(URI, "assetInventoryNumber");
        QName SUM_WITHOUT_VAT = QName.createQName(URI, "sumWithoutVat");
        QName POSTING_KEY = QName.createQName(URI, "postingKey");
        QName ACCOUNT = QName.createQName(URI, "account");
        QName INVOICE_TAX_CODE = QName.createQName(URI, "invoiceTaxCode");
        QName INVOICE_TAX_PERCENT = QName.createQName(URI, "invoiceTaxPercent");
        QName TRADING_PARTNER_CODE = QName.createQName(URI, "tradingPartnerCode");
        QName FUNCTIONAL_ARE_CODE = QName.createQName(URI, "functionalAreaCode");
        QName CASH_FLOW_CODE = QName.createQName(URI, "cashFlowCode");
        QName SOURCE = QName.createQName(URI, "source");
        QName PAYMENT_METHOD = QName.createQName(URI, "paymentMethod");
        QName HOUSE_BANK = QName.createQName(URI, "houseBank");
        QName ENTRY_CONTENT = QName.createQName(URI, "entryContent");

        // transaction template properties
        QName NAME = QName.createQName(URI, "name");
        QName ACTIVE = QName.createQName(URI, "active");
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
