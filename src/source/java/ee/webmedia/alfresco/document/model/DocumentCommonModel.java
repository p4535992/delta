package ee.webmedia.alfresco.document.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Alar Kvell
 */
public interface DocumentCommonModel {
    String URI = "http://alfresco.webmedia.ee/model/document/common/1.0";
    String PREFIX = "doccom:";

    interface Types {
        QName DOCUMENT = QName.createQName(URI, "document");
    }

    interface Aspects {
        QName COMMON = QName.createQName(URI, "common");
        QName OWNER = QName.createQName(URI, "owner");
        QName ACCESS_RIGHTS = QName.createQName(URI, "accessRights");
        QName SIGNER = QName.createQName(URI, "signer");
        QName RECIPIENT = QName.createQName(URI, "recipient");
        QName ADDITIONAL_RECIPIENT = QName.createQName(URI, "additionalRecipient");
    }
    
    interface Assocs {
        QName DOCUMENT = QName.createQName(URI, "document");
    }

    interface Props {
        QName OWNER_NAME = QName.createQName(URI, "ownerName");
        QName OWNER_ID = QName.createQName(URI, "ownerId");
        QName OWNER_JOB_TITLE = QName.createQName(URI, "ownerJobTitle");
        QName OWNER_ORG_STRUCT_UNIT = QName.createQName(URI, "ownerOrgStructUnit");
        QName OWNER_EMAIL = QName.createQName(URI, "ownerEmail");
        QName OWNER_PHONE = QName.createQName(URI, "ownerPhone");

        QName SIGNER_NAME = QName.createQName(URI, "signerName");
        QName SIGNER_JOB_TITLE = QName.createQName(URI, "signerJobTitle");

        QName ACCESS_RESTRICTION_BEGIN_DATE = QName.createQName(URI, "accessRestrictionBeginDate");
        QName ACCESS_RESTRICTION_END_DATE = QName.createQName(URI, "accessRestrictionEndDate");

        QName REG_NUMBER = QName.createQName(URI, "regNumber");
        QName REG_DATE_TIME = QName.createQName(URI, "regDateTime");

        QName RECIPIENT_NAME = QName.createQName(URI, "recipientName");
        QName RECIPIENT_EMAIL = QName.createQName(URI, "recipientEmail");

        QName ADDITIONAL_RECIPIENT_NAME = QName.createQName(URI, "additionalRecipientName");
        QName ADDITIONAL_RECIPIENT_EMAIL = QName.createQName(URI, "additionalRecipientEmail");
    }

}
