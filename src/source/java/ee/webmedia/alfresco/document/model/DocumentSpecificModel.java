package ee.webmedia.alfresco.document.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Alar Kvell
 */
public interface DocumentSpecificModel {
    String URI = "http://alfresco.webmedia.ee/model/document/specific/1.0";
    String PREFIX = "docspec:";

    interface Aspects {
        QName SENDER = QName.createQName(URI, "sender");
    }

    interface Props {
        QName SENDER_REG_NUMBER = QName.createQName(URI, "senderRegNumber");
        QName SENDER_REG_DATE = QName.createQName(URI, "senderRegDate");
    }

}
