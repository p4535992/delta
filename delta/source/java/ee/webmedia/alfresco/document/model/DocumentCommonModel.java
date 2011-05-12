package ee.webmedia.alfresco.document.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.namespace.QName;

import ee.webmedia.xtee.client.dhl.DhlXTeeService.SendStatus;

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
        QName SEND_INFO = QName.createQName(URI, "sendInfo");
        QName DOCUMENT_LOG = QName.createQName(URI, "documentLog");
        QName METADATA_CONTAINER = QName.createQName(URI, "metadataContainer");
    }

    interface Aspects {
        QName SEARCHABLE = QName.createQName(URI, "searchable");
        QName COMMON = QName.createQName(URI, "common");
        QName SEND_DESC = QName.createQName(URI, "sendDesc");
        QName OWNER = QName.createQName(URI, "owner");
        QName ACCESS_RIGHTS = QName.createQName(URI, "accessRights");
        QName SIGNER = QName.createQName(URI, "signer");
        QName SIGNER_NAME = QName.createQName(URI, "signerName");
        QName RECIPIENT = QName.createQName(URI, "recipient");
        QName ADDITIONAL_RECIPIENT = QName.createQName(URI, "additionalRecipient");
        QName FAVORITE_CONTAINER = QName.createQName(URI, "favoriteContainer");
    }

    interface Assocs {
        /** documentContainer -> document */
        QName DOCUMENT = QName.createQName(URI, "document");
        /** document(replyDocRef) -> document(initialDocRef) */
        QName DOCUMENT_REPLY = QName.createQName(URI, "reply");
        /** document(followupDocRef) -> document(initialDocRef) */
        QName DOCUMENT_FOLLOW_UP = QName.createQName(URI, "followUp");
        /** document(activeDoc) -> document(selectedAssociatedDoc) */
        QName DOCUMENT_2_DOCUMENT = QName.createQName(URI, "document2Document");
        QName SEND_INFO = QName.createQName(URI, "sendInfo");
        QName DOCUMENT_LOG = QName.createQName(URI, "documentLog");
        QName FAVORITE = QName.createQName(URI, "favorite");
    }

    interface Props {
        QName FUNCTION = QName.createQName(URI, "function");
        QName SERIES = QName.createQName(URI, "series");
        QName VOLUME = QName.createQName(URI, "volume");
        QName CASE = QName.createQName(URI, "case");
        QName FILE_NAMES = QName.createQName(URI, "fileNames");
        QName FILE_CONTENTS = QName.createQName(URI, "fileContents");
        QName SEARCHABLE_SEND_MODE = QName.createQName(URI, "searchableSendMode");
        QName SEARCHABLE_COST_MANAGER = QName.createQName(URI, "searchableCostManager");
        QName SEARCHABLE_APPLICANT_NAME = QName.createQName(URI, "searchableApplicantName");
        QName SEARCHABLE_ERRAND_BEGIN_DATE = QName.createQName(URI, "searchableErrandBeginDate");
        QName SEARCHABLE_ERRAND_END_DATE = QName.createQName(URI, "searchableErrandEndDate");
        QName SEARCHABLE_ERRAND_COUNTRY = QName.createQName(URI, "searchableErrandCountry");
        QName SEARCHABLE_ERRAND_COUNTY = QName.createQName(URI, "searchableErrandCounty");
        QName SEARCHABLE_ERRAND_CITY = QName.createQName(URI, "searchableErrandCity");
        QName SEARCHABLE_PARTY_NAME = QName.createQName(URI, "searchablePartyName");

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

        QName SIGNER_NAME = QName.createQName(URI, "signerName");
        QName SIGNER_JOB_TITLE = QName.createQName(URI, "signerJobTitle");

        QName ACCESS_RESTRICTION = QName.createQName(URI, "accessRestriction");
        QName ACCESS_RESTRICTION_REASON = QName.createQName(URI, "accessRestrictionReason");
        QName ACCESS_RESTRICTION_BEGIN_DATE = QName.createQName(URI, "accessRestrictionBeginDate");
        QName ACCESS_RESTRICTION_END_DATE = QName.createQName(URI, "accessRestrictionEndDate");
        QName ACCESS_RESTRICTION_END_DESC = QName.createQName(URI, "accessRestrictionEndDesc");

        QName REG_NUMBER = QName.createQName(URI, "regNumber");
        QName SHORT_REG_NUMBER = QName.createQName(URI, "shortRegNumber");
        QName REG_DATE_TIME = QName.createQName(URI, "regDateTime");

        QName COMMENT = QName.createQName(URI, "comment");

        QName RECIPIENT_NAME = QName.createQName(URI, "recipientName");
        QName RECIPIENT_EMAIL = QName.createQName(URI, "recipientEmail");

        QName ADDITIONAL_RECIPIENT_NAME = QName.createQName(URI, "additionalRecipientName");
        QName ADDITIONAL_RECIPIENT_EMAIL = QName.createQName(URI, "additionalRecipientEmail");

        QName SEND_DESC_VALUE = QName.createQName(URI, "sendDescValue");

        QName SEND_INFO_RECIPIENT = QName.createQName(URI, "recipient");
        QName SEND_INFO_RECIPIENT_REG_NR = QName.createQName(URI, "recipientRegNr");
        QName SEND_INFO_SEND_DATE_TIME = QName.createQName(URI, "sendDateTime");
        QName SEND_INFO_SEND_MODE = QName.createQName(URI, "sendMode");
        QName SEND_INFO_RESOLUTION = QName.createQName(URI, "resolution");
        /**
         * all values for this property are defined in Enum {@link SendStatus}
         */
        QName SEND_INFO_SEND_STATUS = QName.createQName(URI, "sendStatus");
        QName SEND_INFO_DVK_ID = QName.createQName(URI, "dvkId");

        QName CREATED_DATETIME = QName.createQName(URI, "createdDateTime");
        QName CREATOR_NAME = QName.createQName(URI, "creatorName");
        QName EVENT_DESCRIPTION = QName.createQName(URI, "eventDescription");

        /** holds non-system properties of childnodes (childNodes, that are defined in DocumentSubTypeModel) for searching */
        QName SEARCHABLE_SUB_NODE_PROPERTIES = QName.createQName(URI, "searchableSubNodeProperties");
        /** shows if the document contains at least one compoundWorkflow, that has startedDateTime set */
        QName SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS = QName.createQName(URI, "searchableHasStartedCompoundWorkflows");

        QName LEGAL_BASIS_NAME = QName.createQName(URI, "legalBasisName");
    }

    /**
     * Document related privileges (and dependencies)
     * 
     * @author Ats Uiboupin
     */
    abstract class Privileges {
        public static final String VIEW_DOCUMENT_META_DATA = "viewDocumentMetaData";
        public static final String EDIT_DOCUMENT_META_DATA = "editDocumentMetaData";
        public static final String VIEW_DOCUMENT_FILES = "viewDocumentFiles";
        /** ADD or edit document files */
        public static final String EDIT_DOCUMENT_FILES = "editDocumentFiles";
        public static final String DELETE_DOCUMENT_FILES = "deleteDocumentFiles";
        /** can delete document */
        public static final String DELETE_DOCUMENT_META_DATA = "deleteDocumentMetaData";

        public static final Set<String> READ_ONLY_PRIVILEGES = new HashSet<String>(Arrays.asList(DELETE_DOCUMENT_FILES, DELETE_DOCUMENT_META_DATA));
        public static final Map<String, Set<String>> PRIVILEGE_DEPENDENCIES;
        static {
            Map<String, Set<String>> m = new HashMap<String, Set<String>>();
            m.put(EDIT_DOCUMENT_META_DATA, Collections.singleton(VIEW_DOCUMENT_META_DATA));
            m.put(VIEW_DOCUMENT_FILES, Collections.singleton(VIEW_DOCUMENT_META_DATA));
            m.put(EDIT_DOCUMENT_FILES, Collections.singleton(VIEW_DOCUMENT_META_DATA));
            m.put(DELETE_DOCUMENT_FILES, Collections.singleton(VIEW_DOCUMENT_META_DATA));
            m.put(DELETE_DOCUMENT_META_DATA, Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(VIEW_DOCUMENT_META_DATA, VIEW_DOCUMENT_FILES))));
            PRIVILEGE_DEPENDENCIES = Collections.unmodifiableMap(m);
        }
    }

}
