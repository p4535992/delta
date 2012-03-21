package ee.webmedia.alfresco.document.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.SendStatus;

/**
 * @author Alar Kvell
 */
public interface DocumentCommonModel {
    String DOCCOM_URI = "http://alfresco.webmedia.ee/model/document/common/1.0";
    String DOCCOM_PREFIX = "doccom:";

    String URI = DocumentDynamicModel.URI;
    String PREFIX = DocumentDynamicModel.PREFIX;

    QName MODEL = QName.createQName(DOCCOM_URI, "documentCommonModel");

    interface Repo {
        String DRAFTS_PARENT = "/";
        String DRAFTS_SPACE = DRAFTS_PARENT + DOCCOM_PREFIX + "drafts";
    }

    interface Types {
        QName DOCUMENT = QName.createQName(DOCCOM_URI, "document");
        QName DRAFTS = QName.createQName(DOCCOM_URI, "drafts");
        QName SEND_INFO = QName.createQName(DOCCOM_URI, "sendInfo");
        QName DOCUMENT_LOG = QName.createQName(DOCCOM_URI, "documentLog");
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
        QName SEND_INFO = QName.createQName(DOCCOM_URI, "sendInfo");
        QName DOCUMENT_LOG = QName.createQName(DOCCOM_URI, "documentLog");
        QName FAVORITE = QName.createQName(DOCCOM_URI, "favorite");
        QName FAVORITE_DIRECTORY = QName.createQName(DOCCOM_URI, "favoriteDirectory");
        QName FAVORITE_DIRECTORY_ASPECT = QName.createQName(DOCCOM_URI, "favoriteDirectoryAspect");
    }

    interface Props {
        QName FUNCTION = QName.createQName(URI, "function");
        QName SERIES = QName.createQName(URI, "series");
        QName VOLUME = QName.createQName(URI, "volume");
        QName CASE = QName.createQName(URI, "case");

        QName FILE_NAMES = QName.createQName(DOCCOM_URI, "fileNames");
        QName FILE_CONTENTS = QName.createQName(DOCCOM_URI, "fileContents");
        QName SEARCHABLE_SEND_MODE = QName.createQName(DOCCOM_URI, "searchableSendMode");
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
        /**
         * all values for this property are defined in Enum {@link SendStatus}
         */
        QName SEND_INFO_SEND_STATUS = QName.createQName(DOCCOM_URI, "sendStatus");
        QName SEND_INFO_DVK_ID = QName.createQName(DOCCOM_URI, "dvkId");

        QName CREATED_DATETIME = QName.createQName(DOCCOM_URI, "createdDateTime");
        QName CREATOR_NAME = QName.createQName(DOCCOM_URI, "creatorName");
        QName EVENT_DESCRIPTION = QName.createQName(DOCCOM_URI, "eventDescription");

        QName LEGAL_BASIS_NAME = QName.createQName(URI, "legalBasisName");
    }

    /**
     * Document related privileges (and dependencies)
     * 
     * @author Ats Uiboupin
     */
    abstract class Privileges {
        /** Permission used on dynamic document types. Indicates that user can create new document of specific type. */
        public static final String CREATE_DOCUMENT = "createDocument";
        public static final String VIEW_DOCUMENT_META_DATA = "viewDocumentMetaData";
        public static final String EDIT_DOCUMENT = "editDocument";
        public static final String VIEW_DOCUMENT_FILES = "viewDocumentFiles";
        public static final String VIEW_CASE_FILE = "viewCaseFile";
        public static final String EDIT_CASE_FILE = "editCaseFile";
        /** when entry.key is added, then entry.values should be also added */
        public static final Map<String, Set<String>> PRIVILEGE_DEPENDENCIES;
        static {
            Map<String, Set<String>> m = new HashMap<String, Set<String>>();
            m.put(EDIT_DOCUMENT, Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(VIEW_DOCUMENT_META_DATA, VIEW_DOCUMENT_FILES))));
            m.put(VIEW_DOCUMENT_FILES, Collections.singleton(VIEW_DOCUMENT_META_DATA));
            m.put(EDIT_CASE_FILE, Collections.singleton(VIEW_CASE_FILE));
            PRIVILEGE_DEPENDENCIES = Collections.unmodifiableMap(m);
        }
    }

}
