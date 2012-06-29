package ee.webmedia.alfresco.document.model;

import org.alfresco.service.namespace.QName;

@Deprecated
public interface DocumentSubtypeModel {
    String URI = "http://alfresco.webmedia.ee/model/document/subtype/1.0";
    String PREFIX = "docsub:";
    QName MODEL_NAME = QName.createQName(URI, "documentSubtypeModel");

    // TODO DLSeadist remove these and rewrite all places to use SystematicDocumentType enum
    public interface Types {
        QName INCOMING_LETTER = QName.createQName(URI, "incomingLetter");
        QName INCOMING_LETTER_MV = QName.createQName(URI, "incomingLetterMv");
        QName OUTGOING_LETTER = QName.createQName(URI, "outgoingLetter");
        QName OUTGOING_LETTER_MV = QName.createQName(URI, "outgoingLetterMv");
        QName ERRAND_ORDER_ABROAD = QName.createQName(URI, "errandOrderAbroad");
        QName ERRAND_ORDER_ABROAD_MV = QName.createQName(URI, "errandOrderAbroadMv");
        QName ERRAND_APPLICATION_DOMESTIC = QName.createQName(URI, "errandApplicationDomestic");
        QName MEMO = QName.createQName(URI, "memo");
        QName SUPERVISION_REPORT = QName.createQName(URI, "supervisionReport");
        QName CHANCELLORS_ORDER = QName.createQName(URI, "chancellorsOrder");
        QName MINISTERS_ORDER = QName.createQName(URI, "ministersOrder");
        QName INSTRUMENT_OF_DELIVERY_AND_RECEIPT = QName.createQName(URI, "instrumentOfDeliveryAndReceipt");
        QName INSTRUMENT_OF_DELIVERY_AND_RECEIPT_MV = QName.createQName(URI, "instrumentOfDeliveryAndReceiptMv");
        QName LEAVING_LETTER = QName.createQName(URI, "leavingLetter");
        QName TRAINING_APPLICATION = QName.createQName(URI, "trainingApplication");
        QName TENDERING_APPLICATION = QName.createQName(URI, "tenderingApplication");
        QName INTERNAL_APPLICATION = QName.createQName(URI, "internalApplication");
        QName INTERNAL_APPLICATION_MV = QName.createQName(URI, "internalApplicationMv");
        QName REPORT = QName.createQName(URI, "report");
        QName REPORT_MV = QName.createQName(URI, "reportMv");
        QName REGULATION = QName.createQName(URI, "regulation");
        QName PERSONELLE_ORDER_SIM = QName.createQName(URI, "personelleOrderSim");
        QName PERSONELLE_ORDER_SMIT = QName.createQName(URI, "personelleOrderSmit");
        QName DECREE = QName.createQName(URI, "decree");
        QName CONTRACT_SIM = QName.createQName(URI, "contractSim");
        QName CONTRACT_SMIT = QName.createQName(URI, "contractSmit");
        QName CONTRACT_MV = QName.createQName(URI, "contractMv");
        QName LICENCE = QName.createQName(URI, "licence");
        QName MANAGEMENTS_ORDER = QName.createQName(URI, "managementsOrder");
        QName VACATION_ORDER = QName.createQName(URI, "vacationOrder");
        QName VACATION_ORDER_SMIT = QName.createQName(URI, "vacationOrderSmit");
        QName VACATION_APPLICATION = QName.createQName(URI, "vacationApplication");
        /** protokoll */
        QName MINUTES = QName.createQName(URI, "minutes");
        QName MINUTES_MV = QName.createQName(URI, "minutesMv");
        QName PERSONAL_VEHICLE_USAGE_COMPENSATION_MV = QName.createQName(URI, "personalVehicleUsageCompensationMv");
        QName PROJECT_APPLICATION = QName.createQName(URI, "projectApplication");
        QName INVOICE = QName.createQName(URI, "invoice");
        QName RESOLUTION_MV = QName.createQName(URI, "resolutionMv");
        QName ORDER_MV = QName.createQName(URI, "orderMv");
        QName OTHER_DOCUMENT_MV = QName.createQName(URI, "otherDocumentMv");
    }

}
