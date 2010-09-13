package ee.webmedia.alfresco.document.scanned.model;

import org.alfresco.service.namespace.QName;

public interface ScannedModel {
    String URI = "http://alfresco.webmedia.ee/model/scanned/1.0";
    String PREFIX = "scan:";

    public interface Types {
        QName SCANNED = QName.createQName(URI, "scanned");
    }

}
