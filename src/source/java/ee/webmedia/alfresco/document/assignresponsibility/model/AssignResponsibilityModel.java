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
