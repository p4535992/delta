<<<<<<< HEAD
package ee.webmedia.alfresco.document.assignresponsibility.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Alar Kvell
 */
public interface AssignResponsibilityModel {
    String URI = "http://alfresco.webmedia.ee/model/assignresponsibility/common/1.0";
    String PREFIX = "asr:";

    public interface Types {
        QName ASSIGN_RESPONSIBILITY = QName.createQName(URI, "assignResponsibility");
    }

    public interface Props {
        QName OWNER_NAME = QName.createQName(URI, "ownerName");
    }

}
=======
package ee.webmedia.alfresco.document.assignresponsibility.model;

import org.alfresco.service.namespace.QName;

public interface AssignResponsibilityModel {
    String URI = "http://alfresco.webmedia.ee/model/assignresponsibility/common/1.0";
    String PREFIX = "asr:";

    public interface Types {
        QName ASSIGN_RESPONSIBILITY = QName.createQName(URI, "assignResponsibility");
    }

    public interface Props {
        QName OWNER_NAME = QName.createQName(URI, "ownerName");
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
