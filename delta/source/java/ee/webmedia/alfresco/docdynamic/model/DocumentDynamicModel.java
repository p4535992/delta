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

        QName OWNER_NAME = QName.createQName(URI, "ownerName");
        QName OWNER_ID = QName.createQName(URI, "ownerId");
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

        QName SUBSTITUTE_NAME = QName.createQName(URI, "substituteName");

        /** document name used for ADR */
        QName DOC_NAME_ADR = QName.createQName(URI, "docNameAdr");
        QName SIGNER_ID = QName.createQName(URI, "signerId");
        QName SIGNER_NAME = QName.createQName(URI, "signerName");
        QName SUBSTITUTE_ID = QName.createQName(URI, "substituteId");
        QName PREVIOUS_OWNER_ID = QName.createQName(URI, "previousOwnerId");
        QName SHORT_REG_NUMBER = QName.createQName(URI, "shortRegNumber");

        QName USER_NAME = QName.createQName(URI, "userName");
        QName USER_SERVICE_RANK = QName.createQName(URI, "userServiceRank");
        QName USER_JOB_TITLE = QName.createQName(URI, "userJobTitle");
        QName USER_ORG_STRUCT_UNIT = QName.createQName(URI, "userOrgStructUnit");
        QName USER_WORK_ADDRESS = QName.createQName(URI, "userWorkAddress");
        QName USER_EMAIL = QName.createQName(URI, "userEmail");
        QName USER_PHONE = QName.createQName(URI, "userPhone");
        QName USER_ID = QName.createQName(URI, "userId");

        QName FIRST_PARTY_CONTACT_PERSON_NAME = QName.createQName(URI, "firstPartyContactPersonName");
        QName FIRST_PARTY_CONTACT_PERSON_SERVICE_RANK = QName.createQName(URI, "firstPartyContactPersonServiceRank");
        QName FIRST_PARTY_CONTACT_PERSON_JOB_TITLE = QName.createQName(URI, "firstPartyContactPersonJobTitle");
        QName FIRST_PARTY_CONTACT_PERSON_ORG_STRUCT_UNIT = QName.createQName(URI, "firstPartyContactPersonOrgStructUnit");
        QName FIRST_PARTY_CONTACT_PERSON_WORK_ADDRESS = QName.createQName(URI, "firstPartyContactPersonWorkAddress");
        QName FIRST_PARTY_CONTACT_PERSON_EMAIL = QName.createQName(URI, "firstPartyContactPersonEmail");
        QName FIRST_PARTY_CONTACT_PERSON_PHONE = QName.createQName(URI, "firstPartyContactPersonPhone");
        QName FIRST_PARTY_CONTACT_PERSON_ID = QName.createQName(URI, "firstPartyContactPersonId");

        QName CONTACT_NAME = QName.createQName(URI, "contactName");
        QName CONTACT_ADDRESS = QName.createQName(URI, "contactAddress");
        QName CONTACT_EMAIL = QName.createQName(URI, "contactEmail");
        QName CONTACT_PHONE = QName.createQName(URI, "contactPhone");
        QName CONTACT_FAX_NUMBER = QName.createQName(URI, "contactFaxNumber");
        QName CONTACT_WEB_PAGE = QName.createQName(URI, "contactWebPage");
        QName CONTACT_REG_NUMBER = QName.createQName(URI, "contactRegNumber");

        QName BEGIN_DATE = QName.createQName(URI, "beginDate");
        QName END_DATE = QName.createQName(URI, "endDate");
        QName CALCULATED_DURATION_DAYS = QName.createQName(URI, "calculatedDurationDays");

    }

}
