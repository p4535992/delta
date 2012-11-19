package ee.webmedia.alfresco.document.assignresponsibility.service;

/**
 * @author Alar Kvell
 */
public interface AssignResponsibilityService {

    String BEAN_NAME = "AssignResponsibilityService";

    void changeOwnerOfAllDesignatedObjects(String fromOwnerId, String toOwnerId, boolean isLeaving);

}
