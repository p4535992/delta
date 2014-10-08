package ee.webmedia.alfresco.document.einvoice.model;

import org.alfresco.service.namespace.QName;

public interface TransactionDescParameterModel {
    String URI = "http://alfresco.webmedia.ee/model/transactionDescParameter/1.0";
    String NAMESPACE_PREFFIX = "tdp:";

    public interface Repo {
        final static String TRANSACTION_DESC_PARAMETER_PARENT = "/";
        final static String TRANSACTION_DESC_PARAMETERS_SPACE = TRANSACTION_DESC_PARAMETER_PARENT + NAMESPACE_PREFFIX + "transactionDescParameters";
    }

    interface Types {
        QName TRANSACTION_DESC_PARAMETER_ROOT = QName.createQName(URI, "transactionDescParameters");
        QName TRANSACTION_DESC_PARAMETER = QName.createQName(URI, "transactionDescParameter");
    }

    interface Associations {
        QName TRANSACTION_DESC_PARAMETER = QName.createQName(URI, "transactionDescParameter");
    }

    interface Props {
        QName NAME = QName.createQName(URI, "name");
        QName MANDATORY_FOR_OWNER = QName.createQName(URI, "mandatoryForOwner");
        QName MANDATORY_FOR_COST_MANAGER = QName.createQName(URI, "mandatoryForCostManager");
        QName MANDATORY_FOR_ACCOUNTANT = QName.createQName(URI, "mandatoryForAccountant");
    }

}