package ee.webmedia.alfresco.document.model;

import org.alfresco.service.namespace.QName;

public interface DocumentSubtypeModel {
    String URI = "http://alfresco.webmedia.ee/model/document/subtype/1.0";
    String PREFIX = "docsub:";

    public interface Types {
        QName INCOMING_LETTER = QName.createQName(URI, "incomingLetter");
        QName OUTGOING_LETTER = QName.createQName(URI, "outgoingLetter");
        QName ERRAND_ORDER_ABROAD = QName.createQName(URI, "errandOrderAbroad");
        QName ERRAND_APPLICATION_DOMESTIC = QName.createQName(URI, "errandApplicationDomestic");
        QName MEMO = QName.createQName(URI, "memo");
        QName SUPERVISION_REPORT = QName.createQName(URI, "supervisionReport");
        QName CHANCELLORS_ORDER = QName.createQName(URI, "chancellorsOrder");
        QName MINISTERS_ORDER = QName.createQName(URI, "ministersOrder");
        QName INSTRUMENT_OF_DELIVERY_AND_RECEIPT = QName.createQName(URI, "instrumentOfDeliveryAndReceipt");
        QName LEAVING_LETTER = QName.createQName(URI, "leavingLetter");
        QName TRAINING_APPLICATION = QName.createQName(URI, "trainingApplication");
        QName TENDERING_APPLICATION = QName.createQName(URI, "tenderingApplication");
        QName INTERNAL_APPLICATION = QName.createQName(URI, "internalApplication");
        QName REPORT = QName.createQName(URI, "report");
        QName REGULATION = QName.createQName(URI, "regulation");
        QName PERSONELLE_ORDER_SIM = QName.createQName(URI, "personelleOrderSim");
        QName PERSONELLE_ORDER_SMIT = QName.createQName(URI, "personelleOrderSmit");
        QName DECREE = QName.createQName(URI, "decree");
        QName CONTRACT_SIM = QName.createQName(URI, "contractSim");
        QName CONTRACT_SMIT = QName.createQName(URI, "contractSmit");
        QName LICENCE = QName.createQName(URI, "licence");
        QName MANAGEMENTS_ORDER = QName.createQName(URI, "managementsOrder");
        QName VACATION_ORDER = QName.createQName(URI, "vacationOrder");
        QName VACATION_ORDER_SMIT = QName.createQName(URI, "vacationOrderSmit");
    }

}
