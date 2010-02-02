package ee.webmedia.alfresco.document.search.model;

import org.alfresco.service.namespace.QName;

public interface DocumentSearchModel {
    String URI = "http://alfresco.webmedia.ee/model/document/search/1.0";
    String PREFIX = "docsearch:";

    interface Types {
        QName FILTER = QName.createQName(URI, "filter");
    }

    interface Props {
        QName INPUT = QName.createQName(URI, "input");
        QName DOCUMENT_TYPE = QName.createQName(URI, "documentType");
        QName FUNCTION = QName.createQName(URI, "function");
        QName SERIES = QName.createQName(URI, "series");
        QName REG_DATE_TIME_BEGIN = QName.createQName(URI, "regDateTimeBegin");
        QName REG_DATE_TIME_END = QName.createQName(URI, "regDateTimeEnd");
        QName REG_NUMBER = QName.createQName(URI, "regNumber");
        QName DOC_STATUS = QName.createQName(URI, "docStatus");
        QName RECIPIENT_NAME = QName.createQName(URI, "recipientName");
        QName DOC_NAME = QName.createQName(URI, "docName");
        QName SENDER_REG_NUMBER = QName.createQName(URI, "senderRegNumber");
        QName SENDER_REG_DATE_BEGIN = QName.createQName(URI, "senderRegDateBegin");
        QName SENDER_REG_DATE_END = QName.createQName(URI, "senderRegDateEnd");
        QName DUE_DATE_BEGIN = QName.createQName(URI, "dueDateBegin");
        QName DUE_DATE_END = QName.createQName(URI, "dueDateEnd");
        QName COMPLIENCE_DATE_BEGIN = QName.createQName(URI, "complienceDateBegin");
        QName COMPLIENCE_DATE_END = QName.createQName(URI, "complienceDateEnd");
        QName ACCESS_RESTRICTION = QName.createQName(URI, "accessRestriction");
        QName ACCESS_RESTRICTION_REASON = QName.createQName(URI, "accessRestrictionReason");
        QName ACCESS_RESTRICTION_BEGIN_DATE_BEGIN = QName.createQName(URI, "accessRestrictionBeginDateBegin");
        QName ACCESS_RESTRICTION_BEGIN_DATE_END = QName.createQName(URI, "accessRestrictionBeginDateEnd");
        QName ACCESS_RESTRICTION_END_DATE_BEGIN = QName.createQName(URI, "accessRestrictionEndDateBegin");
        QName ACCESS_RESTRICTION_END_DATE_END = QName.createQName(URI, "accessRestrictionEndDateEnd");
        QName ACCESS_RESTRICTION_END_DESC = QName.createQName(URI, "accessRestrictionEndDesc");
        QName OWNER_NAME = QName.createQName(URI, "ownerName");
        QName OWNER_ORG_STRUCT_UNIT = QName.createQName(URI, "ownerOrgStructUnit");
        QName OWNER_JOB_TITLE = QName.createQName(URI, "ownerJobTitle");
        QName SIGNER_NAME = QName.createQName(URI, "signerName");
        QName SIGNER_JOB_TITLE = QName.createQName(URI, "signerJobTitle");
        QName KEYWORDS = QName.createQName(URI, "keywords");
        QName STORAGE_TYPE = QName.createQName(URI, "storageType");
        QName COST_MANAGER = QName.createQName(URI, "costManager");
        QName RESPONSIBLE_NAME = QName.createQName(URI, "responsibleName");
        QName CO_RESPONSIBLES = QName.createQName(URI, "coResponsibles");
        QName CONTACT_PERSON = QName.createQName(URI, "contactPerson");
        QName OUTPUT = QName.createQName(URI, "output");
    }

}
