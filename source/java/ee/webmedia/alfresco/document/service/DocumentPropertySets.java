package ee.webmedia.alfresco.document.service;

import java.util.HashSet;
import java.util.Set;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.privilege.model.PrivilegeModel;

public class DocumentPropertySets {

    /* package */static Set<String> commonProperties = new HashSet<String>();
    static {
        commonProperties.add(DocumentCommonModel.Props.ACCESS_RESTRICTION.toString());
        commonProperties.add(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON.toString());
        commonProperties.add(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE.toString());
        commonProperties.add(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE.toString());
        commonProperties.add(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC.toString());
        commonProperties.add(DocumentCommonModel.Props.DOC_NAME.toString());
        commonProperties.add(DocumentCommonModel.Props.KEYWORDS.toString());
        commonProperties.add(DocumentCommonModel.Props.STORAGE_TYPE.toString());
    }

    /* package */static Set<String> ownerProperties = new HashSet<String>();
    static {
        ownerProperties.add(DocumentCommonModel.Props.OWNER_EMAIL.toString());
        ownerProperties.add(DocumentCommonModel.Props.OWNER_ID.toString());
        ownerProperties.add(DocumentCommonModel.Props.OWNER_JOB_TITLE.toString());
        ownerProperties.add(DocumentCommonModel.Props.OWNER_NAME.toString());
        ownerProperties.add(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT.toString());
        ownerProperties.add(DocumentCommonModel.Props.OWNER_PHONE.toString());
    }

    /* package */static Set<String> incomingAndOutgoingLetterProperties = new HashSet<String>();
    static {
        incomingAndOutgoingLetterProperties.addAll(commonProperties);
        incomingAndOutgoingLetterProperties.add(DocumentSpecificModel.Props.SENDER_REG_NUMBER.toString());
        incomingAndOutgoingLetterProperties.add(DocumentSpecificModel.Props.SENDER_REG_DATE.toString());
    }

    /* package */static Set<String> contractDetailsV1 = new HashSet<String>();
    static {
        contractDetailsV1.add(DocumentSpecificModel.Props.SECOND_PARTY_NAME.toString());
        contractDetailsV1.add(DocumentSpecificModel.Props.SECOND_PARTY_EMAIL.toString());
        contractDetailsV1.add(DocumentSpecificModel.Props.SECOND_PARTY_SIGNER.toString());
        contractDetailsV1.add(DocumentSpecificModel.Props.SECOND_PARTY_CONTACT_PERSON.toString());
        contractDetailsV1.add(DocumentSpecificModel.Props.THIRD_PARTY_NAME.toString());
        contractDetailsV1.add(DocumentSpecificModel.Props.THIRD_PARTY_EMAIL.toString());
        contractDetailsV1.add(DocumentSpecificModel.Props.THIRD_PARTY_SIGNER.toString());
        contractDetailsV1.add(DocumentSpecificModel.Props.THIRD_PARTY_CONTACT_PERSON.toString());
    }

    private static Set<String> privilegeUserGroupMapping = new HashSet<String>(2);
    static {
        privilegeUserGroupMapping.add(PrivilegeModel.Props.USER.toString());
        privilegeUserGroupMapping.add(PrivilegeModel.Props.GROUP.toString());
    }

    public static Set<String> ignoredPropertiesWhenMakingCopy = new HashSet<String>();
    static {
        ignoredPropertiesWhenMakingCopy.add(DocumentCommonModel.Props.REG_NUMBER.toString());
        ignoredPropertiesWhenMakingCopy.add(DocumentCommonModel.Props.SHORT_REG_NUMBER.toString());
        ignoredPropertiesWhenMakingCopy.add(DocumentCommonModel.Props.INDIVIDUAL_NUMBER.toString());
        ignoredPropertiesWhenMakingCopy.add(DocumentCommonModel.Props.REG_DATE_TIME.toString());
        ignoredPropertiesWhenMakingCopy.add(DocumentCommonModel.Props.SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS.toString());
        ignoredPropertiesWhenMakingCopy.addAll(ownerProperties);
        ignoredPropertiesWhenMakingCopy.addAll(contractDetailsV1);
        ignoredPropertiesWhenMakingCopy.addAll(privilegeUserGroupMapping);
        ignoredPropertiesWhenMakingCopy.add(DocumentCommonModel.Props.NOT_EDITABLE.toString());
        ignoredPropertiesWhenMakingCopy.add(DocumentSpecificModel.Props.ENTRY_DATE.toString());
        ignoredPropertiesWhenMakingCopy.add(DocumentSpecificModel.Props.ENTRY_SAP_NUMBER.toString());
    }

}
