package ee.webmedia.alfresco.document.model;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.SendStatus;

public interface DocumentCommonModel {
    String DOCCOM_URI = "http://alfresco.webmedia.ee/model/document/common/1.0";
    String DOCCOM_PREFIX = "doccom:";

    String URI = DocumentDynamicModel.URI;
    String PREFIX = DocumentDynamicModel.PREFIX;

    QName MODEL = QName.createQName(DOCCOM_URI, "documentCommonModel");

    interface Repo {
        String PARENT = "/";
        String DRAFTS_SPACE = PARENT + DOCCOM_PREFIX + "drafts";
        String TEMP_FILES_SPACE = PARENT + DOCCOM_PREFIX + "tempFiles";
        String WEB_SERVICE_SPACE = PARENT + DOCCOM_PREFIX + "webServiceReceived";
    }

    interface Types {
        QName DOCUMENT = QName.createQName(DOCCOM_URI, "document");
        QName DRAFTS = QName.createQName(DOCCOM_URI, "drafts");
        QName TEMP_FILES = QName.createQName(DOCCOM_URI, "tempFiles");
        QName SEND_INFO = QName.createQName(DOCCOM_URI, "sendInfo");
        QName METADATA_CONTAINER = QName.createQName(DOCCOM_URI, "metadataContainer");
        QName FAVORITE_DIRECTORY = QName.createQName(DOCCOM_URI, "favoriteDirectory");
    }

    interface Aspects {
        QName SEARCHABLE = QName.createQName(DOCCOM_URI, "searchable");
        QName COMMON = QName.createQName(DOCCOM_URI, "common");
        QName SEND_DESC = QName.createQName(DOCCOM_URI, "sendDesc");
        QName OWNER = QName.createQName(DOCCOM_URI, "owner");
        QName ACCESS_RIGHTS = QName.createQName(DOCCOM_URI, "accessRights");
        QName SIGNER = QName.createQName(DOCCOM_URI, "signer");
        QName SIGNER_NAME = QName.createQName(DOCCOM_URI, "signerName");
        QName RECIPIENT = QName.createQName(DOCCOM_URI, "recipient");
        QName ADDITIONAL_RECIPIENT = QName.createQName(DOCCOM_URI, "additionalRecipient");
        QName FAVORITE_CONTAINER = QName.createQName(DOCCOM_URI, "favoriteContainer");
        QName FAVORITE_DIRECTORY_ASPECT = QName.createQName(DOCCOM_URI, "favoriteDirectoryAspect");// new
        QName FORUM_PARTICIPANTS = QName.createQName(DOCCOM_URI, "forumParticipants");
        QName EMAIL_DATE_TIME = QName.createQName(DOCCOM_URI, "emailDateTime");
        QName DOCUMENT_REG_NUMBERS_CONTAINER = QName.createQName(DOCCOM_URI, "documentRegNumbersContainer");
        /** Indicates that when deleting this document, it should not be archived */
        QName DELETE_PERMANENT = QName.createQName(DOCCOM_URI, "deletePermanent");
        QName NOT_EDITABLE = QName.createQName(DOCCOM_URI, "notEditable");
        QName INVOICE_XML = QName.createQName(DOCCOM_URI, "invoiceXml");
    }

    interface Assocs {
        /** documentContainer -> document */
        QName DOCUMENT = QName.createQName(DOCCOM_URI, "document");
        /** document(replyDocRef) -> document(initialDocRef) */
        QName DOCUMENT_REPLY = QName.createQName(DOCCOM_URI, "reply");
        /** document(followupDocRef) -> document(initialDocRef) */
        QName DOCUMENT_FOLLOW_UP = QName.createQName(DOCCOM_URI, "followUp");
        /** document(activeDoc) -> document(selectedAssociatedDoc) */
        QName DOCUMENT_2_DOCUMENT = QName.createQName(DOCCOM_URI, "document2Document");
        /** document -> workflow */
        QName WORKFLOW_DOCUMENT = QName.createQName(DOCCOM_URI, "workflowDocument");
        QName SEND_INFO = QName.createQName(DOCCOM_URI, "sendInfo");
        QName FAVORITE = QName.createQName(DOCCOM_URI, "favorite");
        QName FAVORITE_DIRECTORY = QName.createQName(DOCCOM_URI, "favoriteDirectory");
        QName FAVORITE_DIRECTORY_ASPECT = QName.createQName(DOCCOM_URI, "favoriteDirectoryAspect");
    }

    interface Props {
        QName DOCUMENT_REG_NUMBERS = QName.createQName(URI, "documentRegNumbers");

        QName FUNCTION = QName.createQName(URI, "function");
        QName SERIES = QName.createQName(URI, "series");
        QName VOLUME = QName.createQName(URI, "volume");
        QName CASE = QName.createQName(URI, "case");

        QName DOCUMENT_IS_IMPORTED = QName.createQName(DOCCOM_URI, "documentIsImported");
        QName FILE_NAMES = QName.createQName(DOCCOM_URI, "fileNames");
        QName FILE_CONTENTS = QName.createQName(DOCCOM_URI, "fileContents");
        QName SEARCHABLE_SEND_MODE = QName.createQName(DOCCOM_URI, "searchableSendMode");
        QName SEARCHABLE_SEND_INFO_RECIPIENT = QName.createQName(DOCCOM_URI, "searchableSendInfoRecipient");
        QName SEARCHABLE_SEND_INFO_SEND_DATE_TIME = QName.createQName(DOCCOM_URI, "searchableSendInfoDateTime");
        QName SEARCHABLE_SEND_INFO_RESOLUTION = QName.createQName(DOCCOM_URI, "searchableSendInfoResolution");
        /** shows if the document contains at least one compoundWorkflow, that has startedDateTime set */
        QName SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS = QName.createQName(DOCCOM_URI, "searchableHasStartedCompoundWorkflows");
        /** If document has at least one compoundWorkflow and all compoundWorkflows have {@link Status#FINISHED}. */
        QName SEARCHABLE_HAS_ALL_FINISHED_COMPOUND_WORKFLOWS = QName.createQName(DOCCOM_URI, "searchableHasAllFinishedCompoundWorkflows");
        QName SEARCHABLE_FUND = QName.createQName(DOCCOM_URI, "searchableFund");
        QName SEARCHABLE_FUNDS_CENTER = QName.createQName(DOCCOM_URI, "searchableFundsCenter");
        QName SEARCHABLE_EA_COMMITMENT_ITEM = QName.createQName(DOCCOM_URI, "searchableEaCommitmentItem");
        QName FORUM_PARTICIPANTS = QName.createQName(DOCCOM_URI, "forumParticipants");

        QName DOC_NAME = QName.createQName(URI, "docName");
        QName DOC_STATUS = QName.createQName(URI, "docStatus");
        QName STORAGE_TYPE = QName.createQName(URI, "storageType");
        QName KEYWORDS = QName.createQName(URI, "keywords");

        QName OWNER_NAME = QName.createQName(URI, "ownerName");
        QName OWNER_ID = QName.createQName(URI, "ownerId");
        QName OWNER_JOB_TITLE = QName.createQName(URI, "ownerJobTitle");
        QName OWNER_ORG_STRUCT_UNIT = QName.createQName(URI, "ownerOrgStructUnit");
        QName OWNER_EMAIL = QName.createQName(URI, "ownerEmail");
        QName OWNER_PHONE = QName.createQName(URI, "ownerPhone");
        QName PREVIOUS_OWNER_ID = QName.createQName(URI, "previousOwnerId");

        QName SIGNER_NAME = QName.createQName(URI, "signerName");
        QName SIGNER_JOB_TITLE = QName.createQName(URI, "signerJobTitle");
        QName SIGNER_ORG_STRUCT_UNIT = QName.createQName(URI, "signerOrgStructUnit");

        QName ACCESS_RESTRICTION = QName.createQName(URI, "accessRestriction");
        QName ACCESS_RESTRICTION_REASON = QName.createQName(URI, "accessRestrictionReason");
        QName ACCESS_RESTRICTION_BEGIN_DATE = QName.createQName(URI, "accessRestrictionBeginDate");
        QName ACCESS_RESTRICTION_END_DATE = QName.createQName(URI, "accessRestrictionEndDate");
        QName ACCESS_RESTRICTION_END_DESC = QName.createQName(URI, "accessRestrictionEndDesc");
        QName ACCESS_RESTRICTION_CHANGE_REASON = QName.createQName(URI, "accessRestrictionChangeReason");

        QName REG_NUMBER = QName.createQName(URI, "regNumber");
        QName SHORT_REG_NUMBER = QName.createQName(URI, "shortRegNumber");
        QName INDIVIDUAL_NUMBER = QName.createQName(URI, "individualNumber");
        QName REG_DATE_TIME = QName.createQName(URI, "regDateTime");

        QName COMMENT = QName.createQName(URI, "comment");

        QName RECIPIENT_NAME = QName.createQName(URI, "recipientName");
        QName RECIPIENT_EMAIL = QName.createQName(URI, "recipientEmail");
        QName RECIPIENT_GROUP = QName.createQName(URI, "recipientGroup");

        QName ADDITIONAL_RECIPIENT_NAME = QName.createQName(URI, "additionalRecipientName");
        QName ADDITIONAL_RECIPIENT_EMAIL = QName.createQName(URI, "additionalRecipientEmail");
        QName ADDITIONAL_RECIPIENT_GROUP = QName.createQName(URI, "additionalRecipientGroup");

        QName SEND_DESC_VALUE = QName.createQName(URI, "sendDescValue");

        QName SEND_INFO_RECIPIENT = QName.createQName(DOCCOM_URI, "recipient");
        QName SEND_INFO_RECIPIENT_REG_NR = QName.createQName(DOCCOM_URI, "recipientRegNr");
        QName SEND_INFO_SEND_DATE_TIME = QName.createQName(DOCCOM_URI, "sendDateTime");
        QName SEND_INFO_SEND_MODE = QName.createQName(DOCCOM_URI, "sendMode");
        QName SEND_INFO_RESOLUTION = QName.createQName(DOCCOM_URI, "resolution");
        QName SEND_INFO_RECEIVED_DATE_TIME = QName.createQName(DOCCOM_URI, "receivedDateTime");
        QName SEND_INFO_OPENED_DATE_TIME = QName.createQName(DOCCOM_URI, "openedDateTime");
        /**
         * all values for this property are defined in Enum {@link SendStatus}
         */
        QName SEND_INFO_SEND_STATUS = QName.createQName(DOCCOM_URI, "sendStatus");
        QName SEND_INFO_DVK_ID = QName.createQName(DOCCOM_URI, "dvkId");
        
        QName SEND_INFO_IS_ZIPPED = QName.createQName(DOCCOM_URI, "isZipped");
        QName SEND_INFO_IS_ENCRYPTED = QName.createQName(DOCCOM_URI, "isEncrypted");
        QName SEND_INFO_SENT_FILES = QName.createQName(DOCCOM_URI, "sentFiles");
        QName SEND_INFO_SENDER = QName.createQName(DOCCOM_URI, "sender");

        /** Contains email Date header value for documents imported from Outlook */
        QName EMAIL_DATE_TIME = QName.createQName(DOCCOM_URI, "emailDateTime");

        QName LEGAL_BASIS_NAME = QName.createQName(URI, "legalBasisName");

        QName UPDATE_METADATA_IN_FILES = QName.createQName(DOCCOM_URI, "updateMetadataInFiles");

        QName NOT_EDITABLE = QName.createQName(DOCCOM_URI, "notEditable");
        QName INVOICE_XML = QName.createQName(DOCCOM_URI, "invoiceXml");

    }

}
