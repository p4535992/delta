package ee.webmedia.alfresco.report.service;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.classificator.enums.TemplateReportType;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.search.model.DocumentReportModel;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.report.model.ReportModel;
import ee.webmedia.alfresco.workflow.search.model.TaskReportModel;
import ee.webmedia.alfresco.workflow.search.model.TaskSearchModel;

public class ReportHelper {

    /** Document report headings that are displayed conditionally (depending whether or not corresponding field is used) */
    private static final String DOCUMENT_REPORT_HIERARCHICAL_KEYWORDS = "document_report_hierarchicalKeywords";
    private static final String DOCUMENT_REPORT_KEYWORDS = "document_report_keywords";
    private static final String DOCUMENT_REPORT_TOTAL_SUM = "document_report_totalSum";
    private static final String DOCUMENT_REPORT_SELLER_PARTY_REG_NUMBER = "document_report_sellerPartyRegNumber";
    private static final String DOCUMENT_REPORT_SELLER_PARTY_NAME = "document_report_sellerPartyName";
    private static final String DOCUMENT_REPORT_INVOICE_DATE = "document_report_invoiceDate";
    private static final String DOCUMENT_REPORT_INVOICE_NUMBER = "document_report_invoiceNumber";
    private static final String DOCUMENT_REPORT_PROCUREMENT_TYPE = "document_report_procurementType";
    private static final String DOCUMENT_REPORT_DELIVERER_NAME = "document_report_delivererName";
    private static final String DOCUMENT_REPORT_CITY = "document_report_city";
    private static final String DOCUMENT_REPORT_COUNTY = "document_report_county";
    private static final String DOCUMENT_REPORT_COUNTRY = "document_report_country";
    private static final String DOCUMENT_REPORT_ERRAND_END_DATE = "document_report_errandEndDate";
    private static final String DOCUMENT_REPORT_ERRAND_BEGIN_DATE = "document_report_errandBeginDate";
    private static final String DOCUMENT_REPORT_APPLICANT_NAME = "document_report_applicantName";
    private static final String DOCUMENT_REPORT_COST_MANAGER = "document_report_costManager";

    private static final List<String> NOT_MANDATORY_FIELDS_ORDERED = Collections.unmodifiableList(Arrays.asList(DocumentCommonModel.Props.KEYWORDS.getLocalName(),
            DocumentDynamicModel.Props.FIRST_KEYWORD_LEVEL.getLocalName(), DocumentSpecificModel.Props.COST_MANAGER.getLocalName(),
            DocumentSpecificModel.Props.APPLICANT_NAME.getLocalName(), DocumentSpecificModel.Props.ERRAND_BEGIN_DATE.getLocalName(),
            DocumentSpecificModel.Props.ERRAND_END_DATE.getLocalName(), DocumentSpecificModel.Props.ERRAND_COUNTRY.getLocalName(),
            DocumentSpecificModel.Props.ERRAND_COUNTY.getLocalName(), DocumentSpecificModel.Props.ERRAND_CITY.getLocalName(),
            DocumentSpecificModel.Props.DELIVERER_NAME.getLocalName(), DocumentSpecificModel.Props.PROCUREMENT_TYPE.getLocalName(),
            DocumentSpecificModel.Props.INVOICE_NUMBER.getLocalName(), DocumentSpecificModel.Props.INVOICE_DATE.getLocalName(),
            DocumentSpecificModel.Props.SELLER_PARTY_NAME.getLocalName(), DocumentSpecificModel.Props.SELLER_PARTY_REG_NUMBER.getLocalName(),
            DocumentSpecificModel.Props.TOTAL_SUM.getLocalName()));

    /** Map containing main heading keys for different report types */
    private static final Map<TemplateReportType, List<String>> REPORT_HEADER_MSG_KEYS;
    /** Additional headings diplayed in document report with subnodes */
    private static final List<String> DOC_REPORT_WITH_SUBNODES_HEADER_MSG_KEYS = Collections.unmodifiableList(Arrays.asList("document_report_recipients",
            "document_report_partyName", "document_report_partyContactPerson", "document_report_firstPartyContactPersonName", "document_report_sendInfoRecipient",
            "document_report_sendDateTime", "document_report_sendMode", "document_report_sendStatus", DOCUMENT_REPORT_COST_MANAGER, DOCUMENT_REPORT_APPLICANT_NAME,
            DOCUMENT_REPORT_ERRAND_BEGIN_DATE, DOCUMENT_REPORT_ERRAND_END_DATE, DOCUMENT_REPORT_COUNTRY, DOCUMENT_REPORT_COUNTY, DOCUMENT_REPORT_CITY,
            DOCUMENT_REPORT_DELIVERER_NAME, DOCUMENT_REPORT_PROCUREMENT_TYPE, DOCUMENT_REPORT_INVOICE_NUMBER, DOCUMENT_REPORT_INVOICE_DATE, DOCUMENT_REPORT_SELLER_PARTY_NAME,
            DOCUMENT_REPORT_SELLER_PARTY_REG_NUMBER, DOCUMENT_REPORT_TOTAL_SUM));
    /**
     * Contains mappings for heading keys which may or may not be displayed depending of the used state of connected field definition.
     */
    private static final Map<String, String> HEADER_MSG_KEY_TO_FIELD_ID;

    /** List contains: 1) filter type 2) report template name prop 3) saved filter name prop 4) filter assoc 5) filter output type prop (may be null) */
    private static final Map<TemplateReportType, List<QName>> REPORT_FILTER_TYPE_AND_PROPS;

    static {
        Map<TemplateReportType, List<QName>> reportFilterTypeAndProps = new HashMap<TemplateReportType, List<QName>>();
        reportFilterTypeAndProps
                .put(TemplateReportType.TASKS_REPORT,
                        Arrays.asList(TaskReportModel.Types.FILTER, TaskReportModel.Props.REPORT_TEMPLATE, TaskSearchModel.Props.NAME, TaskReportModel.Assocs.FILTER, null));
        reportFilterTypeAndProps.put(TemplateReportType.DOCUMENTS_REPORT,
                Arrays.asList(DocumentReportModel.Types.FILTER, DocumentReportModel.Props.REPORT_TEMPLATE, DocumentSearchModel.Props.NAME, DocumentReportModel.Assocs.FILTER,
                        DocumentReportModel.Props.REPORT_OUTPUT_TYPE));
        REPORT_FILTER_TYPE_AND_PROPS = Collections.unmodifiableMap(reportFilterTypeAndProps);

        Map<TemplateReportType, List<String>> reportHeaderMsgKeys = new HashMap<TemplateReportType, List<String>>();
        reportHeaderMsgKeys.put(TemplateReportType.TASKS_REPORT,
                Arrays.asList("task_search_result_regNum", "task_search_result_regDate", "task_search_result_createdDate", "task_search_result_docType",
                        "task_search_result_docName", "task_search_result_creatorName", "task_search_result_startedDate", "task_search_result_ownerName",
                        "task_search_result_ownerOrganizationName", "task_search_result_ownerJobTitle", "task_search_result_taskType", "task_search_result_dueDate",
                        "task_search_result_completedDate", "task_search_result_comment", "task_search_result_responsible", "task_search_result_stoppedDate",
                        "task_search_result_resolution", "task_search_result_overdue", "task_search_result_status", "task_search_result_function", "task_search_result_series",
                        "task_search_result_volume", "task_search_result_case"));
        reportHeaderMsgKeys.put(TemplateReportType.DOCUMENTS_REPORT,
                Arrays.asList("document_report_regNumber", "document_report_regDate", "document_report_docType", "document_report_function", "document_report_series",
                        "document_report_volume", "document_report_case", "document_report_docName", "document_report_accessRestriction",
                        "document_report_accessRestrictionReason", "document_report_accessRestrictionBeginDate", "document_report_accessRestrictionEndDate",
                        "document_report_accessRestrictionEndDesc", "document_report_ownerName", "document_report_ownerStructUnit", "document_report_ownerJobTitle",
                        "document_report_docStatus", "document_report_senderName", "document_report_senderRegDate", "document_report_senderRegNumber",
                        "document_report_transmittalMode", "document_report_dueDate", "document_report_complienceDate", "document_report_signerName",
                        "document_report_signerJobTitle", DOCUMENT_REPORT_KEYWORDS, DOCUMENT_REPORT_HIERARCHICAL_KEYWORDS, "document_report_storageType",
                        "document_report_created"));

        Map<String, String> headerMsgKeyToFieldId = new HashMap<String, String>();
        headerMsgKeyToFieldId.put(DOCUMENT_REPORT_APPLICANT_NAME, DocumentSpecificModel.Props.APPLICANT_NAME.getLocalName());
        headerMsgKeyToFieldId.put(DOCUMENT_REPORT_CITY, DocumentSpecificModel.Props.ERRAND_CITY.getLocalName());
        headerMsgKeyToFieldId.put(DOCUMENT_REPORT_COST_MANAGER, DocumentSpecificModel.Props.COST_MANAGER.getLocalName());
        headerMsgKeyToFieldId.put(DOCUMENT_REPORT_COUNTRY, DocumentSpecificModel.Props.ERRAND_COUNTRY.getLocalName());
        headerMsgKeyToFieldId.put(DOCUMENT_REPORT_COUNTY, DocumentSpecificModel.Props.ERRAND_COUNTY.getLocalName());
        headerMsgKeyToFieldId.put(DOCUMENT_REPORT_DELIVERER_NAME, DocumentSpecificModel.Props.DELIVERER_NAME.getLocalName());
        headerMsgKeyToFieldId.put(DOCUMENT_REPORT_ERRAND_BEGIN_DATE, DocumentSpecificModel.Props.ERRAND_BEGIN_DATE.getLocalName());
        headerMsgKeyToFieldId.put(DOCUMENT_REPORT_ERRAND_END_DATE, DocumentSpecificModel.Props.ERRAND_END_DATE.getLocalName());
        // check thesauri group by not removable systematic field id in the group
        headerMsgKeyToFieldId.put(DOCUMENT_REPORT_HIERARCHICAL_KEYWORDS, DocumentDynamicModel.Props.FIRST_KEYWORD_LEVEL.getLocalName());
        headerMsgKeyToFieldId.put(DOCUMENT_REPORT_INVOICE_DATE, DocumentSpecificModel.Props.INVOICE_DATE.getLocalName());
        headerMsgKeyToFieldId.put(DOCUMENT_REPORT_INVOICE_NUMBER, DocumentSpecificModel.Props.INVOICE_NUMBER.getLocalName());
        headerMsgKeyToFieldId.put(DOCUMENT_REPORT_KEYWORDS, DocumentCommonModel.Props.KEYWORDS.getLocalName());
        headerMsgKeyToFieldId.put(DOCUMENT_REPORT_PROCUREMENT_TYPE, DocumentSpecificModel.Props.PROCUREMENT_TYPE.getLocalName());
        headerMsgKeyToFieldId.put(DOCUMENT_REPORT_SELLER_PARTY_NAME, DocumentSpecificModel.Props.SELLER_PARTY_NAME.getLocalName());
        headerMsgKeyToFieldId.put(DOCUMENT_REPORT_SELLER_PARTY_REG_NUMBER, DocumentSpecificModel.Props.SELLER_PARTY_REG_NUMBER.getLocalName());
        headerMsgKeyToFieldId.put(DOCUMENT_REPORT_TOTAL_SUM, DocumentSpecificModel.Props.TOTAL_SUM.getLocalName());

        HEADER_MSG_KEY_TO_FIELD_ID = Collections.unmodifiableMap(headerMsgKeyToFieldId);

        REPORT_HEADER_MSG_KEYS = Collections.unmodifiableMap(reportHeaderMsgKeys);
    }

    public static QName getFilterType(TemplateReportType reportType) {
        return REPORT_FILTER_TYPE_AND_PROPS.get(reportType).get(0);
    }

    public static QName getTemplateNameProp(TemplateReportType reportType) {
        return REPORT_FILTER_TYPE_AND_PROPS.get(reportType).get(1);
    }

    public static QName getFilterNameProp(TemplateReportType reportType) {
        return REPORT_FILTER_TYPE_AND_PROPS.get(reportType).get(2);
    }

    public static QName getFilterAssoc(TemplateReportType reportType) {
        return REPORT_FILTER_TYPE_AND_PROPS.get(reportType).get(3);
    }

    public static void setReportResultOutputType(TemplateReportType reportType, Map<String, Object> filterProps, Map<QName, Serializable> reportResultProps) {
        QName filterOutputTypePropQName = REPORT_FILTER_TYPE_AND_PROPS.get(reportType).get(4);
        if (filterOutputTypePropQName != null) {
            reportResultProps.put(ReportModel.Props.REPORT_OUTPUT_TYPE, (String) filterProps.get(filterOutputTypePropQName));
        }

    }

    public static List<String> getReportHeaderMsgKeys(TemplateReportType reportType) {
        return REPORT_HEADER_MSG_KEYS.get(reportType);
    }

    public static List<String> getDocumentReportHeaderAdditionalMsgKeys() {
        return DOC_REPORT_WITH_SUBNODES_HEADER_MSG_KEYS;
    }

    /** Return field id if this fields needs to be checked for used state */
    public static String getCheckableFieldByMsgKey(String msgKey) {
        return HEADER_MSG_KEY_TO_FIELD_ID.get(msgKey);
    }

    public static List<String> getDocumentReportNotMandatoryFieldsInOrder() {
        return NOT_MANDATORY_FIELDS_ORDERED;
    }

}
