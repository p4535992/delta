package ee.webmedia.alfresco.docdynamic.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Alar Kvell
 */
public interface DocumentDynamicModel {
    String URI = "http://alfresco.webmedia.ee/model/document/dynamic/1.0";
    String PREFIX = "docdyn:";

    QName MODEL = QName.createQName(URI, "documentDynamicModel");

    interface Types {
        QName DOCUMENT_DYNAMIC = QName.createQName(URI, "documentDynamic");
    }

    interface Props {
        QName DOCUMENT_TYPE_ID = QName.createQName(URI, "documentTypeId");
        QName DOCUMENT_TYPE_VERSION_NR = QName.createQName(URI, "documentTypeVersionNr");
    }

}
