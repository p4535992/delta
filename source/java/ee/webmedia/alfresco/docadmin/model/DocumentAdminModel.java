package ee.webmedia.alfresco.docadmin.model;

import org.alfresco.service.namespace.QName;

public interface DocumentAdminModel {
    String URI = "http://alfresco.webmedia.ee/model/document/admin/1.0";
    String PREFIX = "docadmin:";

    QName MODEL = QName.createQName(URI, "documentAdminModel");

    public interface Repo {
        String DOCUMENT_TYPES_PARENT = "/";
        String DOCUMENT_TYPES_SPACE = DOCUMENT_TYPES_PARENT + PREFIX + "documentTypes";
        String CASE_FILE_TYPES_SPACE = DOCUMENT_TYPES_PARENT + PREFIX + "caseFileTypes";
        String FIELD_DEFINITIONS_SPACE = DOCUMENT_TYPES_PARENT + PREFIX + "fieldDefinitions";
        String FIELD_DEFINITIONS_TMP_SPACE = FIELD_DEFINITIONS_SPACE + "Tmp";
        String FIELD_GROUP_DEFINITIONS_SPACE = DOCUMENT_TYPES_PARENT + PREFIX + "fieldGroupDefinitions";
        String FIELD_GROUP_DEFINITIONS_TMP_SPACE = FIELD_GROUP_DEFINITIONS_SPACE + "Tmp";
    }

    interface Assocs {
        QName FIELD_DEFINITIONS_TMP = QName.createQName(URI, "fieldDefinitionsTmp");
        QName FIELD_GROUP_DEFINITIONS_TMP = QName.createQName(URI, "fieldGroupDefinitionsTmp");
    }

    interface Types {
        QName FIELD_DEFINITIONS = QName.createQName(URI, "fieldDefinitions");
        QName FIELD_GROUP_DEFINITIONS = QName.createQName(URI, "fieldGroupDefinitions");
        QName DOCUMENT_TYPE = QName.createQName(URI, "documentType");
        QName CASE_FILE_TYPE = QName.createQName(URI, "caseFileType");
        QName DOCUMENT_TYPE_VERSION = QName.createQName(URI, "documentTypeVersion");
        QName FIELD = QName.createQName(URI, "field");
        QName FIELD_DEFINITION = QName.createQName(URI, "fieldDefinition");
        QName FIELD_GROUP = QName.createQName(URI, "fieldGroup");
        QName SEPARATION_LINE = QName.createQName(URI, "separationLine");
        // QName ASSOCIATION_TO_DOC_TYPE = QName.createQName(URI, "associationModel");
        QName FOLLOWUP_ASSOCIATION = QName.createQName(URI, "followupAssociation");
        QName REPLY_ASSOCIATION = QName.createQName(URI, "replyAssociation");
        QName FIELD_MAPPING = QName.createQName(URI, "fieldMapping");
    }

    interface Aspects {
        QName OBJECT = QName.createQName(URI, "object");
        QName INAPPLICABLE_FOR_TYPE = QName.createQName(URI, "inapplicableForType");
    }

    interface Props {
        QName OBJECT_TYPE_ID = QName.createQName(URI, "objectTypeId");
        QName OBJECT_TYPE_VERSION_NR = QName.createQName(URI, "objectTypeVersionNr");

        /** DynamicType.id */
        QName ID = QName.createQName(URI, "id");

        // START: properties of type fieldBase
        QName FIELD_ID = QName.createQName(URI, "fieldId");
        QName MANDATORY = QName.createQName(URI, "mandatory");
        QName MANDATORY_CHANGEABLE = QName.createQName(URI, "mandatoryChangeable");
        QName FIELD_TYPE = QName.createQName(URI, "fieldType");
        QName CHANGEABLE_IF = QName.createQName(URI, "changeableIf");
        QName CHANGEABLE_IF_CHANGEABLE = QName.createQName(URI, "changeableIfChangeable");
        QName COMBOBOX_NOT_RELATED_TO_CLASSIFICATOR = QName.createQName(URI, "comboboxNotRelatedToClassificator");
        QName CLASSIFICATOR = QName.createQName(URI, "classificator");
        QName DEFAULT_VALUE = QName.createQName(URI, "defaultValue");
        QName CLASSIFICATOR_DEFAULT_VALUE = QName.createQName(URI, "classificatorDefaultValue");
        QName DEFAULT_DATE_SYSDATE = QName.createQName(URI, "defaultDateSysdate");
        QName DEFAULT_USER_LOGGED_IN = QName.createQName(URI, "defaultUserLoggedIn");
        QName DEFAULT_SELECTED = QName.createQName(URI, "defaultSelected");
        QName ONLY_IN_GROUP = QName.createQName(URI, "onlyInGroup");
        QName REMOVABLE_FROM_SYSTEMATIC_FIELD_GROUP = QName.createQName(URI, "removableFromSystematicFieldGroup");
        QName MAPPING_RESTRICTION = QName.createQName(URI, "mappingRestriction");
        QName ORIGINAL_FIELD_ID = QName.createQName(URI, "originalFieldId");
        // END: properties of type fieldBase

        // Start: properties of type fieldDefinition
        QName DOC_TYPES = QName.createQName(URI, "docTypes");
        QName IS_PARAMETER_IN_DOC_SEARCH = QName.createQName(URI, "isParameterInDocSearch");
        QName IS_PARAMETER_IN_VOL_SEARCH = QName.createQName(URI, "isParameterInVolSearch");
        QName PARAMETER_ORDER_IN_DOC_SEARCH = QName.createQName(URI, "parameterOrderInDocSearch");
        QName PARAMETER_ORDER_IN_VOL_SEARCH = QName.createQName(URI, "parameterOrderInVolSearch");
        QName VOL_TYPES = QName.createQName(URI, "volTypes");
        QName IS_FIXED_PARAMETER_IN_DOC_SEARCH = QName.createQName(URI, "isFixedParameterInDocSearch");
        QName IS_FIXED_PARAMETER_IN_VOL_SEARCH = QName.createQName(URI, "isFixedParameterInVolSearch");
        // END: properties of type fieldDefinition

        // START: properties of type documentTypeVersion
        QName VERSION_NR = QName.createQName(URI, "versionNr");
        QName CREATOR_ID = QName.createQName(URI, "creatorId");
        QName CREATOR_NAME = QName.createQName(URI, "creatorName");
        QName CREATED_DATE_TIME = QName.createQName(URI, "createdDateTime");
        // END: properties of type documentTypeVersion

        // START: properties of type documentType
        QName NAME = QName.createQName(URI, "name");
        QName USED = QName.createQName(URI, "used");
        QName PUBLIC_ADR = QName.createQName(URI, "publicAdr");
        QName SHOW_UNVALUED = QName.createQName(URI, "showUnvalued");
        QName CHANGE_BY_NEW_DOCUMENT_ENABLED = QName.createQName(URI, "changeByNewDocumentEnabled");
        QName MENU_GROUP_NAME = QName.createQName(URI, "menuGroupName");
        QName REGISTRATION_ENABLED = QName.createQName(URI, "registrationEnabled");
        QName FINISH_DOC_BY_REGISTRATION = QName.createQName(URI, "finishDocByRegistration");
        QName SEND_UNREGISTRATED_DOC_ENABLED = QName.createQName(URI, "sendUnregistratedDocEnabled");
        QName ADD_FOLLOW_UP_TO_UNREGISTRATED_DOC_ENABLED = QName.createQName(URI, "addFollowUpToUnregistratedDocEnabled");
        QName ADD_REPLY_TO_UNREGISTRATED_DOC_ENABLED = QName.createQName(URI, "addReplyToUnregistratedDocEnabled");
        QName EDIT_FILES_OF_FINISHED_DOC_ENABLED = QName.createQName(URI, "editFilesOfFinishedDocEnabled");
        QName LATEST_VERSION = QName.createQName(URI, "latestVersion");
        QName REGISTRATION_ON_DOC_FORM_ENABLED = QName.createQName(URI, "registrationOnDocFormEnabled");
        // END: properties of type documentType
        // START: properties of type fieldGroup
        QName FIELD_DEFINITIONS_IDS = QName.createQName(URI, "fieldDefinitionIds");
        QName READONLY_FIELDS_NAME = QName.createQName(URI, "readonlyFieldsName");
        QName READONLY_FIELDS_NAME_CHANGEABLE = QName.createQName(URI, "readonlyFieldsNameChangeable");
        QName READONLY_FIELDS_RULE = QName.createQName(URI, "readonlyFieldsRule");
        QName READONLY_FIELDS_RULE_CHANGEABLE = QName.createQName(URI, "readonlyFieldsRuleChangeable");
        QName SHOW_IN_TWO_COLUMNS = QName.createQName(URI, "showInTwoColumns");
        QName SHOW_IN_TWO_COLUMNS_CHANGEABLE = QName.createQName(URI, "showInTwoColumnsChangeable");
        QName THESAURUS = QName.createQName(URI, "thesaurus");
        // END: properties of type fieldGroup

        // START: properties of type associationModel
        QName DOC_TYPE = QName.createQName(URI, "docType");
        QName ASSOCIATE_WITH_SOURCE_DOCUMENT_WORKFLOW = QName.createQName(URI, "associateWithSourceDocumentWorkflow");
        // START: properties of type associationModel
        // START: properties of type associatedFields
        QName FROM_FIELD = QName.createQName(URI, "fromField");
        QName TO_FIELD = QName.createQName(URI, "toField");
        // START: properties of type associatedFields

        // START: properties of aspect systematic
        QName SYSTEMATIC = QName.createQName(URI, "systematic");
        QName SYSTEMATIC_COMMENT = QName.createQName(URI, "systematicComment");
        // END: properties of aspect systematic
        // START: properties of aspect order
        QName ORDER = QName.createQName(URI, "order");
        // END: properties of aspect order
        // START: properties of aspect comment
        QName COMMENT = QName.createQName(URI, "comment");
        // END: properties of aspect comment
        // START: properties of aspect fieldAndGroupCommon
        QName MANDATORY_FOR_DOC = QName.createQName(URI, "mandatoryForDoc");
        QName REMOVABLE_FROM_SYSTEMATIC_DOC_TYPE = QName.createQName(URI, "removableFromSystematicDocType");
        QName MANDATORY_FOR_VOL = QName.createQName(URI, "mandatoryForVol");
        // END: properties of aspect mandatoryForVol

        // START: properties of aspect inapplicableForType
        QName INAPPLICABLE_FOR_DOC = QName.createQName(URI, "inapplicableForDoc");
        QName INAPPLICABLE_FOR_VOL = QName.createQName(URI, "inapplicableForVol");
        // END: properties of aspect inapplicableForType

    }

}
