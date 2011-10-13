package ee.webmedia.alfresco.docdynamic.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Alar Kvell
 */
public interface DocumentDynamicModel {
    String URI = "http://alfresco.webmedia.ee/model/document/dynamic/1.0";
    String PREFIX = "docdyn:";

    QName MODEL = QName.createQName(URI, "documentDynamicModel");

    interface Props {
        QName OWNER_SERVICE_RANK = QName.createQName(URI, "ownerServiceRank");
        QName OWNER_WORK_ADDRESS = QName.createQName(URI, "ownerWorkAddress");
        QName SIGNER_SERVICE_RANK = QName.createQName(URI, "signerServiceRank");
        QName SIGNER_WORK_ADDRESS = QName.createQName(URI, "signerWorkAddress");
        QName SIGNER_ORG_STRUCT_UNIT = QName.createQName(URI, "signerOrgStructUnit");
        QName SIGNER_EMAIL = QName.createQName(URI, "signerEmail");
        QName SIGNER_PHONE = QName.createQName(URI, "signerPhone");

        QName RECIPIENT_PERSON_NAME = QName.createQName(URI, "recipientPersonName");
        QName RECIPIENT_STREET_HOUSE = QName.createQName(URI, "recipientStreetHouse");
        QName RECIPIENT_POSTAL_CITY = QName.createQName(URI, "recipientPostalCity");
        QName ADDITIONAL_RECIPIENT_PERSON_NAME = QName.createQName(URI, "additionalRecipientPersonName");
        QName ADDITIONAL_RECIPIENT_STREET_HOUSE = QName.createQName(URI, "additionalRecipientStreetHouse");
        QName ADDITIONAL_RECIPIENT_POSTAL_CITY = QName.createQName(URI, "additionalRecipientPostalCity");

        QName SENDER_PERSON_NAME = QName.createQName(URI, "senderPersonName");
        QName SENDER_STREET_HOUSE = QName.createQName(URI, "senderStreetHouse");
        QName SENDER_POSTAL_CITY = QName.createQName(URI, "senderPostalCity");
        QName SENDER_FAX = QName.createQName(URI, "senderFax");

        QName USER_NAMES = QName.createQName(URI, "userNames");
        QName USER_JOB_TITLE = QName.createQName(URI, "userJobTitle");
        QName USER_ORG_STRUCT_UNIT = QName.createQName(URI, "userOrgStructUnit");

        QName SUBSTITUTE_NAME = QName.createQName(URI, "substituteName");

        /** document name used for ADR */
        QName DOC_NAME_ADR = QName.createQName(URI, "docNameAdr");
        QName SIGNER_ID = QName.createQName(URI, "signerId");
        QName SUBSTITUTE_ID = QName.createQName(URI, "substituteId");
        QName PREVIOUS_OWNER_ID = QName.createQName(URI, "previousOwnerId");
        QName SHORT_REG_NUMBER = QName.createQName(URI, "shortRegNumber");
    }

}
