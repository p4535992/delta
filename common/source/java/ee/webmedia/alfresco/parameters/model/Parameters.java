package ee.webmedia.alfresco.parameters.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.parameters.model.ParametersModel.Repo;

public enum Parameters {
    /** Maximal number of documents that is fetched with single service call(multiple calls will be created if needed) */
    DVK_MAX_RECEIVE_DOCUMENTS_NR("dvkReceiveDocumentsNumber"),
    /** Number of days the document will be retained */
    DVK_RETAIN_PERIOD("dvkRetainPeriod"),
    /** Cron expression - expresses when or how often */
    DVK_RECEIVE_DOCUMENTS_INTERVAL("dvkReceiveDocumentsInterval"),
    /** (H:mm) when should dvkCapability of orgainzation(addresbook entry) be updated */
    DVK_RECEIVE_ORGANIZATIONS("dvkReceiveOrganizations"),
    /** period in minutes */
    DVK_RECEIVE_DOC_SEND_STATUSES("dvkReceiveDocSendStatusesInterval"),
    DVK_ORGANIZATION_NAME("dvkOrganizationName"), // kui tühi, kasutatakse xtee confist
    ADIT_RECEIVE_DOC_SEND_STATUSES("aditReceiveDocSendStatusesInterval"),
    SYNC_USERS_GROUPS_STRUCT_UNITS_TIME("syncUsersGroupsStructUnitsTime"),
    EXTERNAL_REVIEW_AUTOMATIC_FOLDER("externalReviewAutomaticFolder"),
    // Default sender email value when sending out a document
    DOC_SENDER_EMAIL("docSenderEmail"),
    // Info message for document sendout view
    DOC_SENDOUT_INFO("docSendoutInfo"),
    // Default max number of email recipients
    MAX_EMAIL_RECIPIENTS("maxEmailRecipients"),
    // Default sender email value when sending out a task
    TASK_SENDER_EMAIL("taskSenderEmail"),
    // Maximum total size of attachments when sending out a document
    MAX_ATTACHED_FILE_SIZE("maxAttachedFilesSize"),
    /** Leaving letter */
    LEAVING_LETTER_CONTENT("leavingLetterContent"),
    /** Advisers, Deputies and Officials on the leavingLetter form */
    TRAINING_ADVISER("trainingAdviser"),
    FINANCIAL_DEPARTMENT_HEAD_ON_ACCOUNTING("financialDepartmentHeadOnAccounting"),
    ADVISER_GENERAL_DEPARTMENT_ON_ARCHIVES("adviserGeneralDepartmentOnArchives"),
    ADVISER_GENERAL_DEPARTMENT_ON_CLASSIFIED_RECORDS("adviserGeneralDepartmentOnClassifiedRecords"),
    DEPUTY_HEAD_OF_THE_GENERAL_DEPARTMENT("deputyHeadOfTheGeneralDepartment"),
    DOCUMENTARY_PROCEDURES_RECIPIENT("documentaryProceduresRecipient"),
    ADVISER_ON_ICT("adviserOnICT"),
    DEPUTY_HEAD_OF_THE_ADM_DEPT_ON_MAINTENANCE("deputyHeadOfTheAdmDeptOnMaintenance"),
    OFFICIALS_DIRECT_MANAGER("officialsDirectManager"),
    OFFICIAL_ON_PERSONNEL_DEPARTMENT("officialOnPersonnelDepartment"),
    // START: of procurement parameters (riigihanke läbiviimise taotlus)
    PROCUREMENT_LEGAL_BASIS("procurementLegalBasis"),
    PROCUREMENT_OBJECT_CLASS_CODE("procurementObjectClassCode"),
    PROCUREMENT_TENDER_DATA("procurementTenderData"),
    PROCUREMENT_EVALUATION_CRITERIA("procurementEvaluationCriteria"),
    PROCUREMENT_QUALIFICATION_TERMS_FOR_TENDERS("procurementQualificationTermsForTenders"),
    // END: of procurement parameters (riigihanke läbiviimise taotlus)
    // START: errandOrderAbroad (välisLähetuse taotlus)
    ERRAND_ORDER_LEGAL_BASIS_FOR_OFFICIALS("errandOrderLegalBasisForOfficials"),
    ERRAND_ORDER_LEGAL_BASIS_FOR_SUPPORT_STAFF("errandOrderLegalBasisForSupportStaff"),
    // END: errandOrderAbroad (välisLähetuse taotlus)
    VACATION_ORDER_LEGAL_BASIS("vacationOrderLegalBasis"),
    WELCOME_TEXT("welcomeText"),
    EMAIL_FOR_SUBSTITUTE_SUBJECT("emailForSubstituteSubject"),
    TASK_DUE_DATE_NOTIFICATION_TIME("taskDueDateNotificationTime"),
    TASK_DUE_DATE_NOTIFICATION_DAYS("taskDueDateNotificationDays"),
    VOLUME_DISPOSITION_DATE_NOTIFICATION_DAYS("volumeDispositionDateNotificationDays"),
    ACCESS_RESTRICTION_END_DATE_NOTIFICATION_DAYS("accessRestrictionEndDateNotificationDays"),
    CONTRACT_DUE_DATE_NOTIFICATION_DAYS("contractDueDateNotificationDays"),
    ASSIGN_RESPONSIBILITY_INSTRUCTION("assignResponsibilityInstruction"),
    DAYS_FOR_SUBSTITUTION_TASKS_CALC("daysForSubstitutionTasksCalc"),
    DOC_PROP_LICENCE_DOC_NAME("docPropLicenceDocName"),
    DOC_PROP_TENDERING_APPLICATION_DOC_NAME("docPropTenderingApplicationDocName"),
    DOC_PROP_ERRAND_ORDER_ABROAD_DOC_NAME("docPropErrandOrderAbroadDocName"),
    DOC_PROP_ERRAND_APPLICATION_DOMESTIC_DOC_NAME("docPropErrandApplicationDomesticDocName"),
    DOC_PROP_MANAGEMENTS_ORDER_DOC_NAME("docPropManagementsOrderDocName"),
    DOC_PROP_SUPERVISION_REPORT_DOC_NAME("docPropSupervisionReportDocName"),
    DOC_PROP_VACATION_ORDER_DOC_NAME("docPropVacationOrderDocName"),
    DOC_PROP_VACATION_ORDER_LEGAL_BASIS_NAME("docPropVacationOrderLegalBasisName"),
    DOC_PROP_CHANCELLORS_ORDER_SIGNER_NAME("docPropChancellorsOrderSignerName"),
    MAX_MODAL_SEARCH_RESULT_ROWS("maxModalSearchResultRows"),
    TASK_OWNER_STRUCT_UNIT("taskOwnerStructUnit"),
    MAX_SEARCH_RESULT_ROWS("maxSearchResultRows"),
    MAX_SEARCH_SHOW_ALL_RESULT_ROWS("maxSearchShowAllResultRows"),
    HEADER_TEXT("headerText"),
    FOOTER_TEXT("footerText"),
    M_DELTA_FOOTER_TEXT("mDeltaFooterText"),
    VOLUME_AUTOMATIC_CLOSING_TIME("volumeAutomaticClosingTime"),
    SIGNER_NAME("signerName"),
    CONTRACT_FIRST_PARTY_NAME("contractFirstPartyName"),
    EXTERNAL_REVIEW_WORKFLOW_ENABLED("externalReviewWorkflowEnabled"),
    DVK_RECEIVE_DOCUMENTS_INVOICE_FOLDER("dvkReceiveDocumentsInvoiceFolder"),
    COST_MANAGER_FIELD_NAME("costManagerFieldName"),
    TRAINING_APPLICATION_DAILY_ALLOWANCE_SUM("trainingApplicationDailyAllowanceSum"),
    ERRAND_ORDER_ABROAD_DAILY_ALLOWANCE_SUM("errandOrderAbroadDailyAllowanceSum"),
    SAP_FINANCIAL_DIMENSIONS_FOLDER_IN_DVK("sapFinancialDimensionsFolderInDvk"),
    SAP_CONTACT_UPDATE_TIME("sapContactUpdateTime"),
    SAP_DVK_CODE("sapDvkCode"),
    SEND_XXL_INVOICE_TO_DVK_FOLDER("sendXxlInvoiceToDvkFolder"),
    SEND_INVOICE_TO_DVK_FOLDER("sendInvoiceToDvkFolder"),
    SEND_PURCHASE_ORDER_INVOICE_TO_DVK_FOLDER("sendPurchaseOrderInvoiceToDvkFolder"),
    REVIEW_WORKFLOW_COST_MANAGER_WORKFLOW_NUMBER("reviewWorkflowCostManagerWorkflowNumber"),
    INVOICE_ENTRY_DATE_ONLY_IN_CURRENT_YEAR("invoiceEntryDateOnlyInCurrentYear"),

    EXTERNAL_SESSION_EXPIRATION_TIME("externalSessionExpirationTime"),

    // dimension parameters
    DIMENSION_CODE_INVOICE_FUNDS_CENTERS("dimensionCodeInvoiceFundsCenters"),
    DIMENSION_CODE_INVOICE_COST_CENTERS("dimensionCodeInvoiceCostCenters"),
    DIMENSION_CODE_INVOICE_FUNDS("dimensionCodeInvoiceFunds"),
    DIMENSION_CODE_INVOICE_COMMITMENT_ITEM("dimensionCodeInvoiceCommitmentItem"),
    DIMENSION_CODE_INVOICE_INTERNAL_ORDERS("dimensionCodeInvoiceInternalOrders"),
    DIMENSION_CODE_INVOICE_ASSET_INVENTORY_NUMBERS("dimensionCodeInvoiceAssetInventoryNumbers"),
    DIMENSION_CODE_INVOICE_TRADING_PARTNER_CODES("dimensionCodeInvoiceTradingPartnerCodes"),
    DIMENSION_CODE_INVOICE_FUNCTIONAL_AREA_CODE("dimensionCodeInvoiceFunctionalAreaCode"),
    DIMENSION_CODE_INVOICE_CASH_FLOW_CODES("dimensionCodeInvoiceCashFlowCodes"),
    DIMENSION_CODE_INVOICE_SOURCE_CODES("dimensionCodeInvoiceSourceCodes"),
    DIMENSION_CODE_INVOICE_POSTING_KEY("dimensionCodeInvoicePostingKey"),
    DIMENSION_CODE_INVOICE_PAYMENT_METHOD_CODES("dimensionCodeInvoicePaymentMethodCodes"),
    DIMENSION_CODE_INVOICE_HOUSE_BANK_CODES("dimensionCodeInvoiceHouseBankCodes"),

    DIMENSION_DELETION_PERIOD("dimensionDeletionPeriod"),
    DIMENSION_DELETION_TIME("dimensionDeletionTime"),

    WORKING_DOCUMENTS_ADDRESS("workingDocumentsAddress"),
    ONE_STEP_ALLOWED_IN_COMPOUND_WORKFLOW_MESSAGE("oneStepAllowedInCompoundWorkflowMessage"),
    LARGE_WORKFLOW_WARNING_TASK_COUNT("largeWorkflowWarningTaskCount"),

    QUICK_SEARCH_WORDS_COUNT("quickSearchWordsCount"),

    UPLOAD_FILE_MAX_SIZE("uploadFileMaxSize"),
    DOC_MANAGER_EMAIL("docManagerEmail"),
    DVK_FAILED_SENDS_RESEND_TIME("dvkFailedSendsResendTime"),
    MOVE_TO_ARCHIVE_LIST_DEFAULT_MONTHS("moveToArchiveListDefaultMonths"),
    ARCHIVAL_ACTIVITY_DOC_TYPE_ID("archivalActivityDocTypeId"),
	DOC_SEND_FAIL_VIA_DVK_NOTIFICATION_EMAILS("docSendFailViaDvkNotificationEmails"),
    ACCESS_RESTRICTION_END_DATE_NOTIFICATION_MAX_DOCUMENTS("accessRestrictionEndDateNotificationMaxDocuments"),
    
    ARCHIVING_BEGIN_TIME("archivingBeginTime"),
    ARCHIVING_END_TIME("archivingEndTime"),
    CONTINUE_ARCIVING_OVER_WEEKEND("continueArchivingOverWeekend"),
    
    DESTRUCTION_BEGIN_TIME("destructionBeginTime"),
    DESTRUCTION_END_TIME("destructionEndTime"),
    CONTINUE_DESTRUCTION_OVER_WEEKEND("continueDestructionOverWeekend"),
    
    DIGIDOC_FILE_FORMAT("digidocFileFormat"),
	ALLOW_CLOSE_FOR_ARCIVED_VOLUMES("allowCloseForArchivedVolumes"),
	ONE_MULTISTEP_WORKFLOW_ALLOWED("oneMultistepWorkflowAllowed");

    private String xPath;
    private String parameterName;
    private static final Map<String, Parameters> params = new HashMap<>();

    Parameters(String parameterName) {
        xPath = Repo.PARAMETERS_SPACE + "/" + ParametersModel.PREFIX + parameterName;
        this.parameterName = parameterName;
    }

    public static Parameters get(String parameterName) {
        if (params.containsKey(parameterName)) {
            return params.get(parameterName);
        }
        final Parameters[] values = Parameters.values();
        for (Parameters parameter : values) {
            if (parameter.parameterName.equals(parameterName)) {
                params.put(parameterName, parameter);
                return parameter;
            }
        }
        throw new IllegalArgumentException("Unknown parameterName: " + parameterName + ". Known values: " + StringUtils.join(Parameters.values(), ", "));
    }

    public static Parameters get(Parameter<?> parameter) {
        return get(parameter.getParamName());
    }

    @Override
    public String toString() {
        return xPath;
    }

    public String getParameterName() {
        return parameterName;
    }

}