<<<<<<< HEAD
package ee.webmedia.alfresco.document.assignresponsibility.service;

/**
 * @author Alar Kvell
 */
public interface AssignResponsibilityService {

    String BEAN_NAME = "AssignResponsibilityService";

    void changeOwnerOfAllDesignatedObjects(String fromOwnerId, String toOwnerId, boolean isLeaving);

}
=======
package ee.webmedia.alfresco.document.assignresponsibility.service;

public interface AssignResponsibilityService {

    String BEAN_NAME = "AssignResponsibilityService";

    void changeOwnerOfAllDocumentsAndTasks(String fromOwnerId, String toOwnerId, boolean isLeaving);

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
