package ee.webmedia.alfresco.user.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.user.model.Authority;

public interface UserService {

    String BEAN_NAME = "UserService";

    String DOCUMENT_MANAGERS_GROUP = "DOCUMENT_MANAGERS";
    String ADMINISTRATORS_GROUP = "ALFRESCO_ADMINISTRATORS";
    String ACCOUNTANTS_GROUP = "ACCOUNTANTS";

    String AUTH_DOCUMENT_MANAGERS_GROUP = AuthorityType.GROUP.getPrefixString() + DOCUMENT_MANAGERS_GROUP;
    String AUTH_ADMINISTRATORS_GROUP = AuthorityType.GROUP.getPrefixString() + ADMINISTRATORS_GROUP;

    String DOCUMENT_MANAGERS_DISPLAY_NAME = "document_managers_display_name";
    String ALFRESCO_ADMINISTRATORS_DISPLAY_NAME = "alfresco_administrators_display_name";
    String ACCOUNTANTS_DISPLAY_NAME = "accountants_display_name";

    /**
     * Fetches the node reference, where user preferences are kept
     * 
     * @param userName if null, defaults to AuthenticationUtil.getRunAsUser()
     * @return
     */
    NodeRef getUsersPreferenceNodeRef(String userName);

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

    boolean isDocumentManager(String userName);

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
     * 
     * @see #searchUsers(String, boolean)
     */
    List<Node> searchUsers(String input, boolean returnAllUsers, String group);

    /**
     * Fetches the users node
     * 
     * @param userName
     * @return node representing the user
     */
    Node getUser(String userName);

    Authority getAuthority(String authority);

    Authority getAuthorityOrNull(String authority);

    List<Authority> getAuthorities(NodeRef nodeRef, String permission);

    /**
     * Returns full name of the authenticated user
     */
    String getUserFullName();

    /**
     * Returns full name of the specified user. If user doesn't have full name, returns username.
     * 
     * @param userName
     * @return full name of user or {@code null} if user does not exist
     */
    String getUserFullName(String userName);

    String getUserFullNameWithUnitName(String userName);

    /**
     * Returns a map with the user's properties.
     * 
     * @param userName
     * @return
     */
    Map<QName, Serializable> getUserProperties(String userName);

    Map<QName, Serializable> getCurrentUserProperties();

    /**
     * Returns username of the authenticated user.
     * 
     * @return
     */
    String getCurrentUserName();

    NodeRef getCurrentUser();

    String getUserEmail(String userName);

    Integer getCurrentUsersStructUnitId();

    /**
     * @param structUnits - organization structure units
     * @return userNames of the users directly in given structUnits (not in some descendant structUnit of given structUnits)
     */
    Set<String> getUsernamesByStructUnit(List<Integer> structUnits);

    boolean isGroupsEditingAllowed();

    void setOwnerPropsFromUser(Map<QName, Serializable> docProps, Map<QName, Serializable> userProps);

    void setOwnerPropsFromUser(Map<String, Object> docProps);

    String getAccountantsGroup();

    /**
     * @return true if user is in admin or accountant group
     */
    boolean isAccountant();

    /**
     * @return true if user is in accountant group
     */
    boolean isInAccountantGroup();

    /**
     * Adds leaving aspect to leavingUserId.
     * 
     * @param leavingUserId resigning user
     * @param replacementUserId user to whom liability is given to
     * @return true if successful, false otherwise
     */
    boolean markUserLeaving(String leavingUserId, String replacementUserId, boolean isLeaving);

    void updateUser(Node user);

    NodeRef getPerson(String userName);

}
