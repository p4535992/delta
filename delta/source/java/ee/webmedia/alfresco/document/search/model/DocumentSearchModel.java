package ee.webmedia.alfresco.document.search.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Alar Kvell
 */
public interface DocumentSearchModel {
    String URI = "http://alfresco.webmedia.ee/model/document/search/1.0";
    String PREFIX = "docsearch:";

    public interface Repo {
        final static String FILTERS_PARENT = "/";
        final static String FILTERS_SPACE = FILTERS_PARENT + PREFIX + "documentSearchFilters";
    }

    interface Types {
        QName FILTER = QName.createQName(URI, "filter");
    }

    interface Assocs {
        QName FILTER = QName.createQName(URI, "filter");
    }

    interface Aspects {
        QName DOCUMENT_SEARCH_FILTERS_CONTAINER = QName.createQName(URI, "documentSearchFiltersContainer");
    }

    interface Props {
        QName STORE = QName.createQName(URI, "store");
        QName NAME = QName.createQName(URI, "name");
        QName INPUT = QName.createQName(URI, "input");
        QName DOCUMENT_TYPE = QName.createQName(URI, "documentType");
        QName FUNCTION = QName.createQName(URI, "function");
        QName SERIES = QName.createQName(URI, "series");
        QName VOLUME = QName.createQName(URI, "volume");
        QName CASE = QName.createQName(URI, "case");
        QName REG_DATE_TIME_BEGIN = QName.createQName(URI, "regDateTimeBegin");
        QName REG_DATE_TIME_END = QName.createQName(URI, "regDateTimeEnd");
        QName REG_NUMBER = QName.createQName(URI, "regNumber");
        QName SHORT_REG_NUMBER = QName.createQName(URI, "shortRegNumber");
        QName DOC_STATUS = QName.createQName(URI, "docStatus");
        QName SENDER_NAME = QName.createQName(URI, "senderName");
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
        QName SEND_MODE = QName.createQName(URI, "sendMode");
        QName COST_MANAGER = QName.createQName(URI, "costManager");
        QName APPLICANT_NAME = QName.createQName(URI, "applicantName");
        QName ERRAND_BEGIN_DATE_BEGIN = QName.createQName(URI, "errandBeginDateBegin");
        QName ERRAND_BEGIN_DATE_END = QName.createQName(URI, "errandBeginDateEnd");
        QName ERRAND_END_DATE_BEGIN = QName.createQName(URI, "errandEndDateBegin");
        QName ERRAND_END_DATE_END = QName.createQName(URI, "errandEndDateEnd");
        QName ERRAND_COUNTRY = QName.createQName(URI, "errandCountry");
        QName ERRAND_COUNTY = QName.createQName(URI, "errandCounty");
        QName ERRAND_CITY = QName.createQName(URI, "errandCity");
        QName RESPONSIBLE_NAME = QName.createQName(URI, "responsibleName");
        QName CO_RESPONSIBLES = QName.createQName(URI, "coResponsibles");
        QName CONTACT_PERSON = QName.createQName(URI, "contactPerson");
        QName PROCUREMENT_TYPE = QName.createQName(URI, "procurementType");
        QName OUTPUT = QName.createQName(URI, "output");
        QName INVOICE_NUMBER = QName.createQName(URI, "invoiceNumber");
        QName INVOICE_DATE_BEGIN = QName.createQName(URI, "invoiceDateBegin");
        QName INVOICE_DATE_END = QName.createQName(URI, "invoiceDateEnd");
        QName SELLER_PARTY_NAME = QName.createQName(URI, "sellerPartyName");
        QName SELLER_PARTY_REG_NUMBER = QName.createQName(URI, "sellerPartyRegNumber");
        QName TOTAL_SUM_LOWEST = QName.createQName(URI, "totalSumLowest");
        QName TOTAL_SUM_HIGHEST = QName.createQName(URI, "totalSumHighest");
    }

}
