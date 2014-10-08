<<<<<<< HEAD
package ee.webmedia.alfresco.document.scanned.model;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;

public interface ScannedModel {
    String URI = "http://alfresco.webmedia.ee/model/scanned/1.0";
    String SCANNED_MODEL_PREFIX = "scan:";

    public interface Repo {
        String SCANNED_PARENT = "/";
        String SCANNED_ROOT = "scannedDocs";
        String SCANNED_SPACE = SCANNED_PARENT + DocumentCommonModel.DOCCOM_PREFIX + SCANNED_ROOT;
    }

    public interface Types {
        QName SCANNED = QName.createQName(URI, "scanned");
    }

}
=======
package ee.webmedia.alfresco.document.scanned.model;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;

public interface ScannedModel {
    String URI = "http://alfresco.webmedia.ee/model/scanned/1.0";
    String SCANNED_MODEL_PREFIX = "scan:";

    public interface Repo {
        String SCANNED_PARENT = "/";
        String SCANNED_ROOT = "scannedDocs";
        String SCANNED_SPACE = SCANNED_PARENT + DocumentCommonModel.DOCCOM_PREFIX + SCANNED_ROOT;
    }

    public interface Types {
        QName SCANNED = QName.createQName(URI, "scanned");
    }

}
>>>>>>> develop-5.1
