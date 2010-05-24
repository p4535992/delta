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
        /** Siselähetuse taotleja ja lähetuse bloki vaheline seos (dokument->taotleja => dokument->taotleja->lähetus) */
        QName ERRAND_DOMESTIC = QName.createQName(URI, "errandsDomestic");
        /** Seos välisLähetuse dokumendi ja välisLähetuse taotleja vahel (dokument => dokument->taotleja) */
        QName ERRAND_ORDER_APPLICANTS_ABROAD = QName.createQName(URI, "errandOrderApplicantsAbroad");
        /** Seos siseLähetuse dokumendi ja siseLähetuse taotleja vahel (dokument => dokument->taotleja) */
        QName ERRAND_APPLICATION_DOMESTIC_APPLICANTS = QName.createQName(URI, "errandApplicationDomesticApplicants");
        /** Seos koolitustaotluse dokumendi ja taotleja vahel (dokument => dokument->taotleja) */
        QName TRAINING_APPLICATION_APPLICANTS = QName.createQName(URI, "trainingApplicationApplicants");
    }

    interface Types {
        /** VälisLähetuse blokk (dokument->taotleja->lähetus) */
        QName ERRAND_ABROAD_TYPE = QName.createQName(URI, "errandsAbroadType");
        /** Siselähetuse blokk (dokument->taotleja->lähetus) */
        QName ERRANDS_DOMESTIC_TYPE = QName.createQName(URI, "errandsDomesticType");
        /** VälisLähetuse taotleja(dokument->taotleja) */
        QName ERRAND_ORDER_APPLICANT_ABROAD = QName.createQName(URI, "errandOrderApplicantAbroadType");
        /** SiseLähetuse taotleja(dokument->taotleja) */
        QName ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE = QName.createQName(URI, "errandApplicationDomesticApplicantType");
        /** Koolitustaotluse taotleja(dokument->taotleja) */
        QName TRAINING_APPLICATION_APPLICANT_TYPE = QName.createQName(URI, "trainingApplicationApplicantType");
    }

    interface Aspects {
        QName SENDER = QName.createQName(URI, "sender");
        QName SENDER_DETAILS = QName.createQName(URI, "senderDetails");
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
        QName VACATION_ORDER_SMIT = QName.createQName(URI, "vacationOrderSmit");
        QName COST_MANAGER = QName.createQName(URI, "costManager");
        /** aspect for common fields of errand applicant (lähetuse taotleja (välislähetuse ja siselähetuse puhul taotleja ühised väljad)) */
        QName ERRAND_APPLICANT = QName.createQName(URI, "errandApplicant");//TODO: pole veel kasutuses
        QName ERRAND_ORDER_APPLICANT_ABROAD = QName.createQName(URI, "errandOrderApplicantAbroad");
        /** Välis- ja siseLähetuse dokumentide tüübi ühine aspekt */
        QName ERRAND_DOC = QName.createQName(URI, "errandDoc");
        /** Välislähetuse dokumenndi tüübi aspekt */
         QName ERRAND_ORDER_ABROAD = QName.createQName(URI, "errandOrderAbroad");
         /** SiseLähetuse dokumenndi tüübi aspekt */
         QName ERRAND_APPLICATION_DOMESTIC = QName.createQName(URI, "errandApplicationDomestic");
         /** Koolituslähetuse dokumenndi tüübi aspekt */
         QName TRAINING_APPLICATION = QName.createQName(URI, "trainingApplication");
        /** Välislähetus -> taotleja -> lähetus tüübi aspekt */
        QName ERRAND_ORDER_ABROAD_BLOCK = QName.createQName(URI, "errandBlockAbroad");
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
        QName SECOND_PARTY_EMAIL = QName.createQName(URI, "secondPartyEmail");
        QName THIRD_PARTY_CONTACT_PERSON = QName.createQName(URI, "thirdPartyContactPerson");
        QName THIRD_PARTY_NAME = QName.createQName(URI, "thirdPartyName");
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
        QName SUBSTITUTION_BEGIN_DATE = QName.createQName(URI, "substitutionBeginDate");
        QName SUBSTITUTION_END_DATE = QName.createQName(URI, "substitutionEndDate");

        QName CONTRACT_SIM_END_DATE = QName.createQName(URI, "contractSimEndDate");
        QName CONTRACT_SMIT_END_DATE = QName.createQName(URI, "contractSmitEndDate");

        QName MANAGEMENTS_ORDER_DUE_DATE = QName.createQName(URI, "managementsOrderDueDate");

        QName PROCUREMENT_TYPE = QName.createQName(URI, "procurementType"); // TODO not in data model yet, because docsub:tenderingApplication is not yet implemented
        
        QName APPLICATION_RECIPIENT = QName.createQName(URI, "applicationRecipient");
    }

}
