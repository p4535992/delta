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

    void changeOwnerOfAllDesignatedObjects(String fromOwnerId, String toOwnerId, boolean isLeaving);

}
>>>>>>> develop-5.1
