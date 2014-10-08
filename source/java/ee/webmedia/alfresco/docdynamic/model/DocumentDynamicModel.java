package ee.webmedia.alfresco.docdynamic.model;

import org.alfresco.service.namespace.QName;

public interface DocumentDynamicModel {
    String URI = "http://alfresco.webmedia.ee/model/document/dynamic/1.0";
    String PREFIX = "docdyn:";

    QName MODEL = QName.createQName(URI, "documentDynamicModel");

    interface Props {
        QName OWNER_SERVICE_RANK = QName.createQName(URI, "ownerServiceRank");
        QName OWNER_WORK_ADDRESS = QName.createQName(URI, "ownerWorkAddress");
        QName SIGNER_SERVICE_RANK = QName.createQName(URI, "signerServiceRank");
        QName SIGNER_WORK_ADDRESS = QName.createQName(URI, "signerWorkAddress");
        QName SIGNER_ORG_STRUCT_UNIT = QName.createQName(URI, "signerOrgStructUnit");
        QName SIGNER_EMAIL = QName.createQName(URI, "signerEmail");
        QName SIGNER_PHONE = QName.createQName(URI, "signerPhone");

        QName OWNER_NAME = QName.createQName(URI, "ownerName");
        QName OWNER_ID = QName.createQName(URI, "ownerId");
        QName RECIPIENT_PERSON_NAME = QName.createQName(URI, "recipientPersonName");
        QName RECIPIENT_STREET_HOUSE = QName.createQName(URI, "recipientStreetHouse");
        QName RECIPIENT_POSTAL_CITY = QName.createQName(URI, "recipientPostalCity");
        QName RECIPIENT_ID = QName.createQName(URI, "recipientId");
        QName ADDITIONAL_RECIPIENT_PERSON_NAME = QName.createQName(URI, "additionalRecipientPersonName");
        QName ADDITIONAL_RECIPIENT_STREET_HOUSE = QName.createQName(URI, "additionalRecipientStreetHouse");
        QName ADDITIONAL_RECIPIENT_POSTAL_CITY = QName.createQName(URI, "additionalRecipientPostalCity");
        QName ADDITIONAL_RECIPIENT_ID = QName.createQName(URI, "additionalRecipientId");

        QName SENDER_PERSON_NAME = QName.createQName(URI, "senderPersonName");
        QName SENDER_INITIALS_TO_ADR = QName.createQName(URI, "senderInitialsToAdr");
        QName SENDER_STREET_HOUSE = QName.createQName(URI, "senderStreetHouse");
        QName SENDER_POSTAL_CITY = QName.createQName(URI, "senderPostalCity");
        QName SENDER_FAX = QName.createQName(URI, "senderFax");

        /** document name used for ADR */
        QName DOC_NAME_ADR = QName.createQName(URI, "docNameAdr");
        QName PUBLISH_TO_ADR = QName.createQName(URI, "publishToAdr");
        QName SIGNER_ID = QName.createQName(URI, "signerId");
        QName SIGNER_NAME = QName.createQName(URI, "signerName");
        QName SUBSTITUTE_ID = QName.createQName(URI, "substituteId");
        QName PREVIOUS_OWNER_ID = QName.createQName(URI, "previousOwnerId");

        QName USER_NAME = QName.createQName(URI, "userName");
        QName USER_SERVICE_RANK = QName.createQName(URI, "userServiceRank");
        QName USER_JOB_TITLE = QName.createQName(URI, "userJobTitle");
        QName USER_ORG_STRUCT_UNIT = QName.createQName(URI, "userOrgStructUnit");
        QName USER_WORK_ADDRESS = QName.createQName(URI, "userWorkAddress");
        QName USER_EMAIL = QName.createQName(URI, "userEmail");
        QName USER_PHONE = QName.createQName(URI, "userPhone");
        QName USER_ID = QName.createQName(URI, "userId");

        QName APPLICANT_NAME = QName.createQName(URI, "applicantName");
        QName APPLICANT_ID = QName.createQName(URI, "applicantId");
        QName APPLICANT_SERVICE_RANK = QName.createQName(URI, "applicantServiceRank");
        QName APPLICANT_JOB_TITLE = QName.createQName(URI, "applicantJobTitle");
        QName APPLICANT_ORG_STRUCT_UNIT = QName.createQName(URI, "applicantOrgStructUnit");
        QName APPLICANT_WORK_ADDRESS = QName.createQName(URI, "applicantWorkAddress");
        QName APPLICANT_EMAIL = QName.createQName(URI, "applicantEmail");
        QName APPLICANT_PHONE = QName.createQName(URI, "applicantPhone");
        QName COST_CENTER = QName.createQName(URI, "costCenter");
        QName EXPENSE_TYPE = QName.createQName(URI, "expenseType");
        QName REPORT_DUE_DATE = QName.createQName(URI, "reportDueDate");

        QName APPLICANT_TYPE = QName.createQName(URI, "applicantType");
        QName APPLICANT_AREA = QName.createQName(URI, "applicantArea");
        QName APPLICATION_LANGUAGE = QName.createQName(URI, "applicationLanguage");
        QName CASE_RESULT = QName.createQName(URI, "caseResult");
        QName SUPERVISION_VISIT = QName.createQName(URI, "supervisionVisit");
        QName OPCAT = QName.createQName(URI, "opcat");
        QName COMMENT = QName.createQName(URI, "comment");
        QName FIRST_LEVEL_RESPONDENT = QName.createQName(URI, "firstLevelRespondent");
        QName SECOND_LEVEL_RESPONDENT = QName.createQName(URI, "secondLevelRespondent");
        QName LEGISLATION = QName.createQName(URI, "legislation");
        QName GENERAL_RIGHT_TO_EQUALITY = QName.createQName(URI, "generalRightToEquality");
        QName DISCRIMINATION = QName.createQName(URI, "discrimination");
        QName GOOD_ADMINISTRATION = QName.createQName(URI, "goodAdministration");
        QName CHILD_RIGHTS = QName.createQName(URI, "childRights");
        QName CHILD_APPLICANT = QName.createQName(URI, "childApplicant");
        QName TO_SURVEY = QName.createQName(URI, "toSurvey");
        QName PROCEDURE_STATUS = QName.createQName(URI, "procedureStatus");
        QName EQUALITY_OF_TREATMENT = QName.createQName(URI, "equalityOfTreatment");

        QName FIRST_PARTY_CONTACT_PERSON_NAME = QName.createQName(URI, "firstPartyContactPersonName");
        QName FIRST_PARTY_CONTACT_PERSON_SERVICE_RANK = QName.createQName(URI, "firstPartyContactPersonServiceRank");
        QName FIRST_PARTY_CONTACT_PERSON_JOB_TITLE = QName.createQName(URI, "firstPartyContactPersonJobTitle");
        QName FIRST_PARTY_CONTACT_PERSON_ORG_STRUCT_UNIT = QName.createQName(URI, "firstPartyContactPersonOrgStructUnit");
        QName FIRST_PARTY_CONTACT_PERSON_WORK_ADDRESS = QName.createQName(URI, "firstPartyContactPersonWorkAddress");
        QName FIRST_PARTY_CONTACT_PERSON_EMAIL = QName.createQName(URI, "firstPartyContactPersonEmail");
        QName FIRST_PARTY_CONTACT_PERSON_PHONE = QName.createQName(URI, "firstPartyContactPersonPhone");
        QName FIRST_PARTY_CONTACT_PERSON_ID = QName.createQName(URI, "firstPartyContactPersonId");

        QName CONTACT_NAME = QName.createQName(URI, "contactName");
        QName CONTACT_ADDRESS = QName.createQName(URI, "contactAddress");
        QName CONTACT_STREET_HOUSE = QName.createQName(URI, "contactStreetHouse");
        QName CONTACT_POSTAL_CITY = QName.createQName(URI, "contactPostalCity");
        QName CONTACT_EMAIL = QName.createQName(URI, "contactEmail");
        QName CONTACT_FIRST_ADDITIONAL_EMAIL = QName.createQName(URI, "contactFirstAdditionalEmail");
        QName CONTACT_SECOND_ADDITIONAL_EMAIL = QName.createQName(URI, "contactSecondAdditionalEmail");
        QName CONTACT_PHONE = QName.createQName(URI, "contactPhone");
        QName CONTACT_FIRST_ADDITIONAL_PHONE = QName.createQName(URI, "contactFirstAdditionalPhone");
        QName CONTACT_SECOND_ADDITIONAL_PHONE = QName.createQName(URI, "contactSecondAdditionalPhone");
        QName CONTACT_FAX_NUMBER = QName.createQName(URI, "contactFaxNumber");
        QName CONTACT_WEB_PAGE = QName.createQName(URI, "contactWebPage");
        QName CONTACT_REG_NUMBER = QName.createQName(URI, "contactRegNumber");

        QName BEGIN_DATE = QName.createQName(URI, "beginDate");
        QName END_DATE = QName.createQName(URI, "endDate");
        QName CALCULATED_DURATION_DAYS = QName.createQName(URI, "calculatedDurationDays");
        QName FIRST_KEYWORD_LEVEL = QName.createQName(URI, "firstKeywordLevel");
        QName SECOND_KEYWORD_LEVEL = QName.createQName(URI, "secondKeywordLevel");
        /** hidden field */
        QName THESAURUS = QName.createQName(URI, "thesaurus");

        QName LEAVE_BEGIN_DATE = QName.createQName(URI, "leaveBeginDate");
        QName LEAVE_END_DATE = QName.createQName(URI, "leaveEndDate");
        QName LEAVE_WORK_YEAR = QName.createQName(URI, "leaveWorkYear");
        QName LEAVE_CHANGED_DAYS = QName.createQName(URI, "leaveChangedDays");
        QName LEAVE_NEW_WORK_YEAR = QName.createQName(URI, "leaveNewWorkYear");

        QName STATUS = QName.createQName(URI, "status");
        QName TITLE = QName.createQName(URI, "title");

        QName EXPENSE_TYPE_CHOICE = QName.createQName(URI, "expenseTypeChoice");
        QName ERRAND_REPORT_SUM = QName.createQName(URI, "errandReportSum");
        QName ERRAND_REPORT_TOTAL_SUM = QName.createQName(URI, "errandReportTotalSum");
        QName ERRAND_SUMMARY_DEBIT = QName.createQName(URI, "errandSummaryDebit");
        QName ERRAND_SUMMARY_DEBIT_TOTAL = QName.createQName(URI, "errandSummaryDebitTotal");
        QName ERRAND_SUMMARY_CREDIT = QName.createQName(URI, "errandSummaryCredit");
        QName ERRAND_SUMMARY_CREDIT_TOTAL = QName.createQName(URI, "errandSummaryCreditTotal");
        QName DRIVE_TOTAL_KM = QName.createQName(URI, "driveTotalKm");
        QName DRIVE_COMPENSATION_RATE = QName.createQName(URI, "driveCompensationRate");
        QName DRIVE_TOTAL_COMPENSATION = QName.createQName(URI, "driveTotalCompensation");

        QName DOC_TITLE = QName.createQName(URI, "title");
        QName WORKFLOW_DUE_DATE = QName.createQName(URI, "workflowDueDate");
        QName WORKFLOW_END_DATE = QName.createQName(URI, "workflowEndDate");
        QName VALID_FROM = QName.createQName(URI, "validFrom");
        QName VALID_TO = QName.createQName(URI, "validTo");
        QName DOCUMENT_TEMPLATE = QName.createQName(URI, "documentTemplate");

        QName VOLUME_MARK = QName.createQName(URI, "volumeMark");
    }
}