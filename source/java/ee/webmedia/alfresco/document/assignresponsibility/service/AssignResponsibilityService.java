package ee.webmedia.alfresco.document.assignresponsibility.service;

public interface AssignResponsibilityService {

    String BEAN_NAME = "AssignResponsibilityService";

    void changeOwnerOfAllDesignatedObjects(String fromOwnerId, String toOwnerId, boolean isLeaving);

}
