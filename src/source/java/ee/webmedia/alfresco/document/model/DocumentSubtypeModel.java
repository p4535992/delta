package ee.webmedia.alfresco.document.model;

import org.alfresco.service.namespace.QName;

public interface DocumentSubtypeModel {
    String URI = "http://alfresco.webmedia.ee/model/document/subtype/1.0";
    String PREFIX = "docsub:";

    public interface Types {
        QName INCOMING_LETTER = QName.createQName(URI, "incomingLetter");
        QName OUTGOING_LETTER = QName.createQName(URI, "outgoingLetter");
        QName MEMO = QName.createQName(URI, "memo");
        QName SUPERVISION_REPORT = QName.createQName(URI, "supervisionReport");
        QName CHANCELLORS_ORDER = QName.createQName(URI, "chancellorsOrder");
        QName LEAVING_LETTER = QName.createQName(URI, "leavingLetter");
        QName CONTRACT_SIM = QName.createQName(URI, "contractSim");
        QName CONTRACT_SMIT = QName.createQName(URI, "contractSmit");
    }
}
