package ee.webmedia.alfresco.document.service;

import java.util.HashSet;
import java.util.Set;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;

public class DocumentPropertySets {
    
    /*package*/ static Set<String> commonProperties = new HashSet<String>();
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

    /*package*/ static Set<String> incomingAndOutgoingLetterProperties = new HashSet<String>();
    static {
        incomingAndOutgoingLetterProperties.addAll(commonProperties);
        incomingAndOutgoingLetterProperties.add(DocumentSpecificModel.Props.SENDER_REG_NUMBER.toString());
        incomingAndOutgoingLetterProperties.add(DocumentSpecificModel.Props.SENDER_REG_DATE.toString());
    }

    /*package*/ static Set<String> ignoredPropertiesWhenMakingCopy = new HashSet<String>();
    static {
        ignoredPropertiesWhenMakingCopy.add(DocumentCommonModel.Props.REG_NUMBER.toString());
        ignoredPropertiesWhenMakingCopy.add(DocumentCommonModel.Props.REG_DATE_TIME.toString());
        ignoredPropertiesWhenMakingCopy.add(DocumentCommonModel.Props.OWNER_EMAIL.toString());
        ignoredPropertiesWhenMakingCopy.add(DocumentCommonModel.Props.OWNER_ID.toString());
        ignoredPropertiesWhenMakingCopy.add(DocumentCommonModel.Props.OWNER_JOB_TITLE.toString());
        ignoredPropertiesWhenMakingCopy.add(DocumentCommonModel.Props.OWNER_NAME.toString());
        ignoredPropertiesWhenMakingCopy.add(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT.toString());
        ignoredPropertiesWhenMakingCopy.add(DocumentCommonModel.Props.OWNER_PHONE.toString());
    }
}
