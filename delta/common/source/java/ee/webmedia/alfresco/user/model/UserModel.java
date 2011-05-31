package ee.webmedia.alfresco.user.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Kaarel Jõgeva
 */
public interface UserModel {
    String URI = "http://alfresco.webmedia.ee/model/user/1.0";
    String NAMESPACE_PREFFIX = "usr:";

    interface Aspects {
        QName LEAVING = QName.createQName(URI, "leaving");
    }

    public interface Props {
        QName LEAVING_DATE_TIME = QName.createQName(URI, "leavingDateTime");
        QName LIABILITY_GIVEN_TO_PERSON_ID = QName.createQName(URI, "liabilityGivenToPersonId");
    }
}
