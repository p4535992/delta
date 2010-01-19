package ee.webmedia.alfresco.document.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Alar Kvell
 */
public interface DocumentCommonModel {
    String URI = "http://alfresco.webmedia.ee/model/document/common/1.0";
    String PREFIX = "doccom:";
    
    QName MODEL = QName.createQName(URI, "documentCommonModel");

    interface Repo {
        String DRAFTS_PARENT = "/";
        String DRAFTS_SPACE = DRAFTS_PARENT + PREFIX + "drafts";
    }

    interface Types {
        QName DOCUMENT = QName.createQName(URI, "document");
        QName DRAFTS = QName.createQName(URI, "drafts");
    }

    interface Aspects {
        QName SEARCHABLE = QName.createQName(URI, "searchable");
        QName FILE = QName.createQName(URI, "file");
        QName COMMON = QName.createQName(URI, "common");
        QName SEND_DESC = QName.createQName(URI, "sendDesc");
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
        QName DOC_NAME = QName.createQName(URI, "docName");
        QName DOC_STATUS = QName.createQName(URI, "docStatus");
        
        QName OWNER_NAME = QName.createQName(URI, "ownerName");
        QName OWNER_ID = QName.createQName(URI, "ownerId");
        QName OWNER_JOB_TITLE = QName.createQName(URI, "ownerJobTitle");
        QName OWNER_ORG_STRUCT_UNIT = QName.createQName(URI, "ownerOrgStructUnit");
        QName OWNER_EMAIL = QName.createQName(URI, "ownerEmail");
        QName OWNER_PHONE = QName.createQName(URI, "ownerPhone");

        QName SIGNER_NAME = QName.createQName(URI, "signerName");
        QName SIGNER_JOB_TITLE = QName.createQName(URI, "signerJobTitle");

        QName ACCESS_RESTRICTION = QName.createQName(URI, "accessRestriction");
        QName ACCESS_RESTRICTION_REASON = QName.createQName(URI, "accessRestrictionReason");
        QName ACCESS_RESTRICTION_BEGIN_DATE = QName.createQName(URI, "accessRestrictionBeginDate");
        QName ACCESS_RESTRICTION_END_DATE = QName.createQName(URI, "accessRestrictionEndDate");
        QName ACCESS_RESTRICTION_END_DESC = QName.createQName(URI, "accessRestrictionEndDesc");

        QName REG_NUMBER = QName.createQName(URI, "regNumber");
        QName REG_DATE_TIME = QName.createQName(URI, "regDateTime");

        QName RECIPIENT_NAME = QName.createQName(URI, "recipientName");
        QName RECIPIENT_EMAIL = QName.createQName(URI, "recipientEmail");

        QName ADDITIONAL_RECIPIENT_NAME = QName.createQName(URI, "additionalRecipientName");
        QName ADDITIONAL_RECIPIENT_EMAIL = QName.createQName(URI, "additionalRecipientEmail");
    }

}
