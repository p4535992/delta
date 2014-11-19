<<<<<<< HEAD
package ee.webmedia.alfresco.dvk.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Ats Uiboupin
 */
public interface DvkModel {
    String URI = "http://alfresco.webmedia.ee/model/dvk/1.0";

    interface Types {
        QName FAILED_DOC = QName.createQName(URI, "failedDoc");
    }

    interface Aspects {
        QName RECEIVED_DVK_DOCUMENT = QName.createQName(URI, "receivedDvkDocument");
        QName ACCESS_RIGHTS = QName.createQName(URI, "accessRights");
    }

    interface Props {
        QName DVK_ID = QName.createQName(URI, "dvkId");
    }
}
=======
package ee.webmedia.alfresco.dvk.model;

import org.alfresco.service.namespace.QName;

public interface DvkModel {
    String URI = "http://alfresco.webmedia.ee/model/dvk/1.0";

    interface Types {
        QName FAILED_DOC = QName.createQName(URI, "failedDoc");
    }

    interface Aspects {
        QName RECEIVED_DVK_DOCUMENT = QName.createQName(URI, "receivedDvkDocument");
        QName ACCESS_RIGHTS = QName.createQName(URI, "accessRights");
    }

    interface Props {
        QName DVK_ID = QName.createQName(URI, "dvkId");
    }
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
