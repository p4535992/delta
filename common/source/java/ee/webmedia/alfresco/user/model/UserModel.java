package ee.webmedia.alfresco.user.model;

import org.alfresco.service.namespace.QName;

<<<<<<< HEAD
/**
 * @author Kaarel Jõgeva
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public interface UserModel {
    String URI = "http://alfresco.webmedia.ee/model/user/1.0";
    String NAMESPACE_PREFFIX = "usr:";

    interface Aspects {
        QName LEAVING = QName.createQName(URI, "leaving");
<<<<<<< HEAD
        QName CASE_FILE_WORKFLOW_NOTIFICATIONS = QName.createQName(URI, "caseFileWorkflowNotifications");
        QName INDEPENDENT_WORKFLOW_NOTIFICATIONS = QName.createQName(URI, "independentWorkflowNotifications");
        QName DOCUMENT_NOTIFICATIONS = QName.createQName(URI, "documentNotifications");
        QName CASE_FILE_NOTIFICATIONS = QName.createQName(URI, "caseFileNotifications");
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    public interface Props {
        QName LEAVING_DATE_TIME = QName.createQName(URI, "leavingDateTime");
        QName LIABILITY_GIVEN_TO_PERSON_ID = QName.createQName(URI, "liabilityGivenToPersonId");
    }
<<<<<<< HEAD

    public interface Assocs {
        QName CASE_FILE_WORKFLOW_NOTIFICATION = QName.createQName(URI, "caseFileWorkflowNotification");
        QName INDEPENDENT_WORKFLOW_NOTIFICATION = QName.createQName(URI, "independentWorkflowNotification");
        QName DOCUMENT_NOTIFICATION = QName.createQName(URI, "documentNotification");
        QName CASE_FILE_NOTIFICATION = QName.createQName(URI, "caseFileNotification");
    }
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}
