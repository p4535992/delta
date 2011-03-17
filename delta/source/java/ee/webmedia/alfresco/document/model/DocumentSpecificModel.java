package ee.webmedia.alfresco.document.model;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.classificator.enums.TransmittalMode;

/**
 * @author Alar Kvell
 */
public interface DocumentSpecificModel {
    String URI = "http://alfresco.webmedia.ee/model/document/specific/1.0";
    String PREFIX = "docspec:";

    QName MODEL = QName.createQName(URI, "documentSpecificModel");

    interface Assocs {
        /** Välislähetuse taotleja ja lähetuse bloki vaheline seos (dokument->taotleja => dokument->taotleja->lähetus) */
        QName ERRAND_ABROAD = QName.createQName(URI, "errandsAbroad");
        /** Välislähetuse (MV) taotleja ja lähetuse bloki vaheline seos (dokument->taotleja => dokument->taotleja->lähetus) */
        QName ERRAND_ABROAD_MV = QName.createQName(URI, "errandsAbroadMv");
        /** Siselähetuse taotleja ja lähetuse bloki vaheline seos (dokument->taotleja => dokument->taotleja->lähetus) */
        QName ERRAND_DOMESTIC = QName.createQName(URI, "errandsDomestic");
        /** Seos välisLähetuse dokumendi ja välisLähetuse taotleja vahel (dokument => dokument->taotleja) */
        QName ERRAND_ORDER_APPLICANTS_ABROAD = QName.createQName(URI, "errandOrderApplicantsAbroad");
        /** Seos välisLähetuse (MV) dokumendi ja välisLähetuse taotleja vahel (dokument => dokument->taotleja) */
        QName ERRAND_ORDER_ABROAD_MV_APPLICANTS = QName.createQName(URI, "errandOrderAbroadMvApplicants");
        /** Seos siseLähetuse dokumendi ja siseLähetuse taotleja vahel (dokument => dokument->taotleja) */
        QName ERRAND_APPLICATION_DOMESTIC_APPLICANTS = QName.createQName(URI, "errandApplicationDomesticApplicants");
        /** Seos koolitustaotluse dokumendi ja taotleja vahel (dokument => dokument->taotleja) */
        QName TRAINING_APPLICATION_APPLICANTS = QName.createQName(URI, "trainingApplicationApplicants");
        /** Seos leping (MV) ja osapoole vahel(dokument => dokument->osapool) */
        QName CONTRACT_MV_PARTIES = QName.createQName(URI, "contractMvParties");
        /** Seos leping (SIM + SMIT) ja osapoole vahel(dokument => dokument->osapool) */
        QName CONTRACT_PARTIES = QName.createQName(URI, "contractParties");
    }

    interface Types {
        /** VälisLähetuse blokk (dokument->taotleja->lähetus) */
        QName ERRAND_ABROAD_TYPE = QName.createQName(URI, "errandsAbroadType");
        /** VälisLähetuse (MV) blokk (dokument->taotleja->lähetus) */
        QName ERRAND_ABROAD_MV_TYPE = QName.createQName(URI, "errandsAbroadMvType");
        /** Siselähetuse blokk (dokument->taotleja->lähetus) */
        QName ERRANDS_DOMESTIC_TYPE = QName.createQName(URI, "errandsDomesticType");
        /** VälisLähetuse taotleja(dokument->taotleja) */
        QName ERRAND_ORDER_APPLICANT_ABROAD = QName.createQName(URI, "errandOrderApplicantAbroadType");
        /** VälisLähetuse (MV) taotleja(dokument->taotleja) */
        QName ERRAND_ORDER_ABROAD_MV_APPLICANT_MV = QName.createQName(URI, "errandOrderAbroadMvApplicantType");
        /** SiseLähetuse taotleja(dokument->taotleja) */
        QName ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE = QName.createQName(URI, "errandApplicationDomesticApplicantType");
        /** Koolitustaotluse taotleja(dokument->taotleja) */
        QName TRAINING_APPLICATION_APPLICANT_TYPE = QName.createQName(URI, "trainingApplicationApplicantType");
        /** Leping (MV) osapool (dokument->osapool) */
        QName CONTRACT_MV_PARTY_TYPE = QName.createQName(URI, "contractMvPartyType");
        QName CONTRACT_PARTY_TYPE = QName.createQName(URI, "contractPartyType");
    }

    interface Aspects {
        QName SENDER = QName.createQName(URI, "sender");
        QName SENDER_DETAILS = QName.createQName(URI, "senderDetails");
        QName SENDER_DETAILS_MV = QName.createQName(URI, "senderDetailsMv");
        QName WHOM = QName.createQName(URI, "whom");
        QName WHOSE = QName.createQName(URI, "whose");
        QName TEMPLATE = QName.createQName(URI, "template");
        QName COMPLIENCE = QName.createQName(URI, "complience");
        QName DELIVERER = QName.createQName(URI, "deliverer");
        QName RECEIVER = QName.createQName(URI, "receiver");
        QName SECOND_PARTY_REG = QName.createQName(URI, "secondPartyReg");
        QName LICENCE_DETAILS = QName.createQName(URI, "licenceDetails");
        QName TRANSMITTAL_MODE = QName.createQName(URI, "transmittalMode");
        QName MANAGEMENTS_ORDER_DETAILS = QName.createQName(URI, "managementsOrderDetails");
        QName VACATION_ORDER = QName.createQName(URI, "vacationOrder");
        QName VACATION_ORDER_COMMON = QName.createQName(URI, "vacationOrderCommon");
        QName VACATION_ORDER_SMIT = QName.createQName(URI, "vacationOrderSmit");
        QName COST_MANAGER = QName.createQName(URI, "costManager");
        /** aspect for common fields of errand applicant (lähetuse taotleja (välislähetuse ja siselähetuse puhul taotleja ühised väljad)) */
        QName ERRAND_APPLICANT = QName.createQName(URI, "errandApplicant");//TODO: pole veel kasutuses
        QName ERRAND_ORDER_APPLICANT_ABROAD = QName.createQName(URI, "errandOrderApplicantAbroad");
        /** Välis- ja siseLähetuse dokumentide tüübi ühine aspekt */
        QName ERRAND_DOC = QName.createQName(URI, "errandDoc");
        QName ERRAND_DOC_COMMON = QName.createQName(URI, "errandDocCommon");
        /** Välislähetuse dokumendi tüübi aspekt */
        QName ERRAND_ORDER_ABROAD = QName.createQName(URI, "errandOrderAbroad");
        /** Välislähetuse (MV) dokumendi tüübi aspekt */
        QName ERRAND_ORDER_ABROAD_MV = QName.createQName(URI, "errandOrderAbroadMv");
        /** SiseLähetuse dokumendi tüübi aspekt */
        QName ERRAND_APPLICATION_DOMESTIC = QName.createQName(URI, "errandApplicationDomestic");
        /** Koolituslähetuse dokumenndi tüübi aspekt */
        QName TRAINING_APPLICATION = QName.createQName(URI, "trainingApplication");
        /** Välislähetus -> taotleja -> lähetus tüübi aspekt */
        QName ERRAND_ORDER_ABROAD_BLOCK = QName.createQName(URI, "errandBlockAbroad");
        QName CONTENT = QName.createQName(URI, "content");
        QName CONTRACT_MV_DETAILS = QName.createQName(URI, "contractMvDetails");
        QName CONTRACT_SIM_DETAILS = QName.createQName(URI, "contractSimDetails");
        QName CONTRACT_COMMON_DETAILS = QName.createQName(URI, "contractCommonDetails");
        QName MINUTES_MV = QName.createQName(URI, "minutesMv");
        QName PERSONAL_VEHICLE_USAGE_COMPENSATION_MV = QName.createQName(URI, "personalVehicleUsageCompensationMv");
        QName PROJECT_APPLICATION = QName.createQName(URI, "projectApplication");
        QName OUTGOING_LETTER_MV = QName.createQName(URI, "outgoingLetterMv");
        QName CONTRACT_DETAILS = QName.createQName(URI, "contractDetails");
        QName CONTRACT_DETAILS_V1 = QName.createQName(URI, "contractDetailsV1");
        QName CONTRACT_DETAILS_V2 = QName.createQName(URI, "contractDetailsV2");
        QName CONTRACT_PARTY = QName.createQName(URI, "contractParty");
        QName NOT_EDITABLE = QName.createQName(URI, "notEditable");
    }

    interface Props {
        // START: properties of (docspec:errandDoc)
        QName EVENT_BEGIN_DATE = QName.createQName(URI, "eventBeginDate");
        QName EVENT_END_DATE = QName.createQName(URI, "eventEndDate");
        // END: properties of (docspec:errandDoc)
        // START: properties of aspect ERRAND_APPLICANT (docspec:errandApplicant)
        QName APPLICANT_NAME = QName.createQName(URI, "applicantName");
        QName APPLICANT_JOB_TITLE = QName.createQName(URI, "applicantJobTitle");
        QName APPLICANT_STRUCT_UNIT_NAME = QName.createQName(URI, "applicantStructUnitName");
        // END: properties of aspect ERRAND_APPLICANT (docspec:errandApplicant)
        // START: properties of aspect ERRAND_ORDER_APPLICANT_ABROAD (errandOrderApplicantAbroad)
        QName EXPENDITURE_ITEM = QName.createQName(URI, "expenditureItem");
        // END: properties of aspect ERRAND_ORDER_APPLICANT_ABROAD (errandOrderApplicantAbroad)

        // START: properties of aspect docspec:errandBlockCommon
        QName ERRAND_BEGIN_DATE = QName.createQName(URI, "errandBeginDate");
        QName ERRAND_END_DATE = QName.createQName(URI, "errandEndDate");
        QName ADVANCE_PAYMENT_DESC = QName.createQName(URI, "advancePaymentDesc"); // FIXME: vist veel ei kasutata
        QName ERRAND_SUBSTITUTE_NAME = QName.createQName(URI, "errandSubstituteName");
        // END: properties of aspect docspec:errandBlockCommon
        // START: properties of aspect ERRAND_ORDER_ABROAD_BLOCK (errandBlockAbroad)
        QName ERRAND_COUNTRY = QName.createQName(URI, "country");
        QName ERRAND_COUNTY = QName.createQName(URI, "county");
        QName ERRAND_CITY = QName.createQName(URI, "city");
        QName DAILY_ALLOWANCE_CATERING_COUNT = QName.createQName(URI, "dailyAllowanceCateringCount");
        QName DAILY_ALLOWANCE_DAYS = QName.createQName(URI, "dailyAllowanceDays");
        QName DAILY_ALLOWANCE_RATE = QName.createQName(URI, "dailyAllowanceRate");
        // END: properties of aspect ERRAND_ORDER_ABROAD_BLOCK (errandBlockAbroad)

        // START: properties of aspect docspec:procurementApplicants
        QName PROCUREMENT_APPLICANT_NAME = QName.createQName(URI, "procurementApplicantName");
        QName PROCUREMENT_APPLICANT_JOB_TITLE = QName.createQName(URI, "procurementApplicantJobTitle");
        QName PROCUREMENT_APPLICANT_ORG_STRUCT_UNIT = QName.createQName(URI, "procurementApplicantOrgStructUnit");
        // END: properties of aspect docspec:procurementApplicants
        // START: properties of aspect docspec:tenderingApplication"
        QName PROCUREMENT_OFFICIAL_RESPONSIBLE = QName.createQName(URI, "procurementOfficialResponsible");
        // END: properties of aspect docspec:tenderingApplication"

        //START: properties of type CONTRACT_MV_PARTIES
        QName PARTY_NAME = QName.createQName(URI, "partyName");
        QName PARTY_SIGNER = QName.createQName(URI, "partySigner");
        QName PARTY_EMAIL = QName.createQName(URI, "partyEmail");
        QName PARTY_CONTACT_PERSON = QName.createQName(URI, "partyContactPerson");
        //END: properties of type CONTRACT_MV_PARTIES
        //START: properties of aspect SENDER_DETAILS_MV
        QName SENDER_SIGNER = QName.createQName(URI, "senderSigner");
        QName SENDER_WRITER = QName.createQName(URI, "senderWriter");
        QName SENDER_PHONE = QName.createQName(URI, "senderPhone");
        QName SENDER_ADDRESS1 = QName.createQName(URI, "senderAddress1");
        QName SENDER_ADDRESS2 = QName.createQName(URI, "senderAddress2");
        QName SENDER_POSTAL_CODE = QName.createQName(URI, "senderPostalCode");
        //END: properties of aspect SENDER_DETAILS_MV
        QName SENDER_REG_NUMBER = QName.createQName(URI, "senderRegNumber");
        QName SENDER_REG_DATE = QName.createQName(URI, "senderRegDate");

        QName SENDER_DETAILS_NAME = QName.createQName(URI, "senderName");
        QName SENDER_DETAILS_EMAIL = QName.createQName(URI, "senderEmail");

        QName WHOM_NAME = QName.createQName(URI, "whomName");
        QName WHOM_JOB_TITLE = QName.createQName(URI, "whomJobTitle");

        QName WHOSE_NAME = QName.createQName(URI, "whose");

        QName TEMPLATE_NAME = QName.createQName(URI, "templateName");
        QName RAPPORTEUR_NAME = QName.createQName(URI, "rapporteur");

        QName COMPLIENCE_DATE = QName.createQName(URI, "complienceDate");
        QName DUE_DATE = QName.createQName(URI, "dueDate");

        QName DELIVERER_NAME = QName.createQName(URI, "delivererName");
        QName DELIVERER_JOB_TITLE = QName.createQName(URI, "delivererJobTitle");
        QName DELIVERER_STRUCT_UNIT = QName.createQName(URI, "delivererStructUnit");

        /**
         * Classificator - Known values for this classificator are defined in Enum {@link TransmittalMode}
         */
        QName TRANSMITTAL_MODE = QName.createQName(URI, "transmittalMode");

        QName RECEIVER_NAME = QName.createQName(URI, "receiverName");
        QName RECEIVER_JOB_TITLE = QName.createQName(URI, "receiverJobTitle");
        QName RECEIVER_STRUCT_UNIT = QName.createQName(URI, "receiverStructUnit");

        QName SECOND_PARTY_REG_NUMBER = QName.createQName(URI, "secondPartyRegNumber");
        QName SECOND_PARTY_REG_DATE = QName.createQName(URI, "secondPartyRegDate");

        QName CONTENT = QName.createQName(URI, "contentValue");

        QName SECOND_PARTY_CONTRACT_DATE = QName.createQName(URI, "secondPartyContractDate");
        QName SECOND_PARTY_CONTRACT_NUMBER = QName.createQName(URI, "secondPartyContractNumber");

        QName RESPONSIBLE_NAME = QName.createQName(URI, "responsibleName");
        QName RESPONSIBLE_STRUCT_UNIT = QName.createQName(URI, "responsibleStructUnit");
        QName RESPONSIBLE_ORGANIZATION = QName.createQName(URI, "responsibleOrganization");
        QName CO_RESPONSIBLES = QName.createQName(URI, "coResponsibles");

        QName FIRST_PARTY_CONTACT_PERSON = QName.createQName(URI, "firstPartyContactPerson");
        QName FIRST_PARTY_NAME = QName.createQName(URI, "firstPartyName");
        QName SECOND_PARTY_CONTACT_PERSON = QName.createQName(URI, "secondPartyContactPerson");
        QName SECOND_PARTY_NAME = QName.createQName(URI, "secondPartyName");
        QName SECOND_PARTY_SIGNER = QName.createQName(URI, "secondPartySigner");
        QName SECOND_PARTY_EMAIL = QName.createQName(URI, "secondPartyEmail");
        QName THIRD_PARTY_CONTACT_PERSON = QName.createQName(URI, "thirdPartyContactPerson");
        QName THIRD_PARTY_NAME = QName.createQName(URI, "thirdPartyName");
        QName THIRD_PARTY_SIGNER = QName.createQName(URI, "thirdPartySigner");
        QName THIRD_PARTY_EMAIL = QName.createQName(URI, "thirdPartyEmail");
        QName COST_MANAGER = QName.createQName(URI, "costManager");
        QName FINAL_TERM_OF_DELIVERY_AND_RECEIPT = QName.createQName(URI, "finalTermOfDeliveryAndReceipt");

        QName VACATION_ADD = QName.createQName(URI, "vacationAdd");
        QName VACATION_CHANGE = QName.createQName(URI, "vacationChange");
        QName VACATION_SUBSTITUTE = QName.createQName(URI, "vacationSubstitute");

        QName LEAVE_ANNUAL = QName.createQName(URI, "leaveAnnual");
        QName LEAVE_ANNUAL_BEGIN_DATE = QName.createQName(URI, "leaveAnnualBeginDate");
        QName LEAVE_ANNUAL_END_DATE = QName.createQName(URI, "leaveAnnualEndDate");
        QName LEAVE_ANNUAL_DAYS = QName.createQName(URI, "leaveAnnualDays");

        QName LEAVE_WITHOUT_PAY = QName.createQName(URI, "leaveWithoutPay");
        QName LEAVE_WITHOUT_PAY_BEGIN_DATE = QName.createQName(URI, "leaveWithoutPayBeginDate");
        QName LEAVE_WITHOUT_PAY_END_DATE = QName.createQName(URI, "leaveWithoutPayEndDate");
        QName LEAVE_WITHOUT_PAY_DAYS = QName.createQName(URI, "leaveWithoutPayDays");

        QName LEAVE_CHILD = QName.createQName(URI, "leaveChild");
        QName LEAVE_CHILD_BEGIN_DATE = QName.createQName(URI, "leaveChildBeginDate");
        QName LEAVE_CHILD_END_DATE = QName.createQName(URI, "leaveChildEndDate");
        QName LEAVE_CHILD_DAYS = QName.createQName(URI, "leaveChildDays");

        QName LEAVE_STUDY = QName.createQName(URI, "leaveStudy");
        QName LEAVE_STUDY_BEGIN_DATE = QName.createQName(URI, "leaveStudyBeginDate");
        QName LEAVE_STUDY_END_DATE = QName.createQName(URI, "leaveStudyEndDate");
        QName LEAVE_STUDY_DAYS = QName.createQName(URI, "leaveStudyDays");

        QName LEAVE_CHANGE = QName.createQName(URI, "leaveChange");
        QName LEAVE_INITIAL_BEGIN_DATE = QName.createQName(URI, "leaveInitialBeginDate");
        QName LEAVE_INITIAL_END_DATE = QName.createQName(URI, "leaveInitialEndDate");
        QName LEAVE_NEW_BEGIN_DATE = QName.createQName(URI, "leaveNewBeginDate");
        QName LEAVE_NEW_END_DATE = QName.createQName(URI, "leaveNewEndDate");
        QName LEAVE_NEW_DAYS = QName.createQName(URI, "leaveNewDays");

        QName LEAVE_CANCEL = QName.createQName(URI, "leaveCancel");
        QName LEAVE_CANCEL_BEGIN_DATE = QName.createQName(URI, "leaveCancelBeginDate");
        QName LEAVE_CANCEL_END_DATE = QName.createQName(URI, "leaveCancelEndDate");
        QName LEAVE_CANCEL_DAYS = QName.createQName(URI, "leaveCancelDays");

        QName SUBSTITUTE_NAME = QName.createQName(URI, "substituteName");
        QName SUBSTITUTE_JOB_TITLE = QName.createQName(URI, "substituteJobTitle");
        QName SUBSTITUTION_BEGIN_DATE = QName.createQName(URI, "substitutionBeginDate");
        QName SUBSTITUTION_END_DATE = QName.createQName(URI, "substitutionEndDate");

        QName CONTRACT_SIM_END_DATE = QName.createQName(URI, "contractSimEndDate");
        QName CONTRACT_SMIT_END_DATE = QName.createQName(URI, "contractSmitEndDate");
        QName CONTRACT_MV_END_DATE = QName.createQName(URI, "contractMvEndDate");

        QName MANAGEMENTS_ORDER_DUE_DATE = QName.createQName(URI, "managementsOrderDueDate");

        QName PROCUREMENT_TYPE = QName.createQName(URI, "procurementType"); // TODO not in data model yet, because docsub:tenderingApplication is not yet implemented
        QName PROCUREMENT_LEGAL_BASIS = QName.createQName(URI, "procurementLegalBasis");
        QName PROCUREMENT_DESC = QName.createQName(URI, "procurementDesc");
        QName PROCUREMENT_SUM_ESTIMATED = QName.createQName(URI, "procurementSumEstimated");
        QName PROCUREMENT_BUDGET_CLASSIFICATION = QName.createQName(URI, "procurementBudgetClassification");
        QName PROCUREMENT_OBJECT_CLASS_CODE = QName.createQName(URI, "procurementObjectClassCode");
        QName PROCUREMENT_TENDER_DATA = QName.createQName(URI, "procurementTenderData");
        QName PROCUREMENT_CONTRACT_DATE_ESTIMATED = QName.createQName(URI, "procurementContractDateEstimated");
        QName LINKED_TO_EU_PROJECT = QName.createQName(URI, "linkedToEuProject");
        QName EU_PROJECT_DESC = QName.createQName(URI, "euProjectDesc");
        QName STRUCTURAL_AID_ID_OUTSIDE_PROJECT = QName.createQName(URI, "structuralAidIdOutsideProject");
        QName OBJECT_TECHNICAL_DESC = QName.createQName(URI, "objectTechnicalDesc");
        QName EVALUATION_CRITERIA = QName.createQName(URI, "evaluationCriteria");
        QName OFFERING_END_DATE = QName.createQName(URI, "offeringEndDate");
        QName QUALIFICATION_TERMS_FOR_TENDERS = QName.createQName(URI, "qualificationTermsForTenders");
        QName CONTRACT_BEGIN_DATE = QName.createQName(URI, "contractBeginDate");
        QName PROCUREMENT_NUMBER = QName.createQName(URI, "procurementNumber");


        QName APPLICATION_RECIPIENT = QName.createQName(URI, "applicationRecipient");

        QName MINUTES_DIRECTOR = QName.createQName(URI, "minutesDirector");
        QName MINUTES_RECORDER = QName.createQName(URI, "minutesRecorder");

        QName COMPENSATION_APPLICANT_NAME = QName.createQName(URI, "compensationApplicantName");
        QName COMPENSATION_APPLICANT_JOB_TITLE = QName.createQName(URI, "compensationApplicantJobTitle");
        QName COMPENSATION_APPLICANT_STRUCT_UNIT_NAME = QName.createQName(URI, "compensationApplicantStructUnitName");

        QName APPLICANT_INSTITUTION = QName.createQName(URI, "applicantInstitution");
        QName APPLICANT_PERSON = QName.createQName(URI, "applicantPerson");
        QName CO_APPLICANT_INSTITUTION = QName.createQName(URI, "coApplicantInstitution");
        QName CO_APPLICANT_PERSON = QName.createQName(URI, "coApplicantPerson");
        QName CO_FINANCER_INSTITUTION = QName.createQName(URI, "coFinancerInstitution");
        QName CO_FINANCER_PERSONA = QName.createQName(URI, "coFinancerPersona");
        QName CO_FINANCER_SUM = QName.createQName(URI, "coFinancerSum");
        QName REPLY_DATE = QName.createQName(URI, "replyDate");
        QName ANNEX = QName.createQName(URI, "annex");
        
        QName NOT_EDITABLE = QName.createQName(URI, "notEditable");
    }

}
