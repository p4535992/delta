package ee.webmedia.alfresco.document.model;

import org.alfresco.service.namespace.QName;

public interface DocumentSubtypeModel {
    String URI = "http://alfresco.webmedia.ee/model/document/subtype/1.0";
    String PREFIX = "docsub:";

    public interface Types {
        QName INCOMING_LETTER = QName.createQName(URI, "incomingLetter");
        QName OUTGOING_LETTER = QName.createQName(URI, "outgoingLetter");
    }
}
