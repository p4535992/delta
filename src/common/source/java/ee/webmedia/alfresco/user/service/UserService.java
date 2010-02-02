package ee.webmedia.alfresco.user.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.user.model.Authority;

public interface UserService {

    String BEAN_NAME = "UserService";

    String DOCUMENT_MANAGERS_GROUP = "DOCUMENT_MANAGERS";

    String DOCUMENT_MANAGERS_DISPLAY_NAME = "document_managers_display_name";
    String ALFRESCO_ADMINISTRATORS_DISPLAY_NAME = "alfresco_administrators_display_name";

    /**
     * Checks if user has administrative privileges
     * 
     * @return true if has
     */
    boolean isAdministrator();

    /**
     * Checks if user belongs to document managers group or is an administrator, because administrators have document managers privileges.
     * 
     * @return true if belongs
     */
    boolean isDocumentManager();

    /**
     * Return group name for document administrators group
     * 
     * @return group name with group type prefix
     */
    String getDocumentManagersGroup();
    
    /**
     * Return group name for Alfresco administrators group  
     * 
     * @return group name with group type prefix
     */
    String getAdministratorsGroup();

    /**
     * Searches for users by first name and last name. If {@code input} is empty, all users are returned if {@code returnAllUsers} is {@code true}, otherwise an
     * empty list is returned. The results from this method should be processed by {@link OrganizationStructureService#setUsersUnit(List)} if correct unit name
     * is desired.
     */
    List<Node> searchUsers(String input, boolean returnAllUsers);

    /**
     * Searches for users from a specified group.
     * @see #searchUsers(String, boolean)
     */
    List<Node> searchUsers(String input, boolean returnAllUsers, String group);

    /**
     * Fetches the users node 
     * @param userName
     * @return node representing the user
     */
    Node getUser(String userName);

    /**
     * Searches for groups by name. If {@code input} is empty, all groups are returned if {@code returnAllGroups} is {@code true}, otherwise an empty list is
     * returned.
     */
    List<Authority> searchGroups(String input, boolean returnAllGroups);

    Authority getAuthority(String authority);

    Authority getAuthorityOrNull(String authority);

    List<Authority> getAuthorities(NodeRef nodeRef, String permission);

    /**
     * Returns the full name of the user.
     * 
     * @param userName
     * @return
     */
    String getUserFullName(String userName);

    String getUserFullNameWithUnitName(String userName);

    /**
     * Returns a map with the user's properties.
     * @param userName
     * @return
     */
    Map<QName, Serializable> getUserProperties(String userName);

    /**
     * Returns the name of the authenticated user. 
     * @return
     */
    String getCurrentUserName();

}
