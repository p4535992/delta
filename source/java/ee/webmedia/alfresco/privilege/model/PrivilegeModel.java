<<<<<<< HEAD
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
=======
package ee.webmedia.alfresco.privilege.model;

import org.alfresco.service.namespace.QName;

public interface PrivilegeModel {
    String URI = "http://alfresco.webmedia.ee/model/privilege/1.0";
    String PREFIX = "priv:";

    interface Repo {
        String PRIVILEGE_ACTIONS_PARENT = "/";
        String PRIVILEGE_ACTIONS_SPACE = PRIVILEGE_ACTIONS_PARENT + PREFIX + "privilegeActionsQueue";
    }

    interface Types {
        QName PRIVILEGE_ACTION = QName.createQName(URI, "privilegeAction");
    }

    interface Assocs {
        QName PRIVILEGE_ACTION = QName.createQName(URI, "privilegeAction");
        QName PRIVILEGE_ACTION_2_NODE = QName.createQName(URI, "privilegeAction2Node");
    }

    interface Aspects {
        QName USER_GROUP_MAPPING = QName.createQName(URI, "userGroupMapping");
    }

    interface Props {
        QName USER = QName.createQName(URI, "user");
        QName GROUP = QName.createQName(URI, "group");

        QName PRIVILEGE_ACTION_TYPE = QName.createQName(URI, "privilegeActionType");
        QName AUTHORITY = QName.createQName(URI, "authority");
        QName PERMISSIONS = QName.createQName(URI, "permissions");
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
