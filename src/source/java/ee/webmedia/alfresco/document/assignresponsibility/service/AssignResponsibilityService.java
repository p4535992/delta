package ee.webmedia.alfresco.document.assignresponsibility.service;


/**
 * @author Alar Kvell
 */
public interface AssignResponsibilityService {

    String BEAN_NAME = "AssignResponsibilityService";

    void changeOwnerOfAllDocumentsAndTasks(String fromOwnerId, String toOwnerId);

}
