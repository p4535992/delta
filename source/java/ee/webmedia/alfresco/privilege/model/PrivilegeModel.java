package ee.webmedia.alfresco.privilege.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Ats Uiboupin
 */
public interface PrivilegeModel {
    String URI = "http://alfresco.webmedia.ee/model/privilege/1.0";

    public interface Aspects {
        QName USER_GROUP_MAPPING = QName.createQName(URI, "userGroupMapping");
    }

    public interface Props {
        QName USER = QName.createQName(URI, "user");
        QName GROUP = QName.createQName(URI, "group");
    }
}
