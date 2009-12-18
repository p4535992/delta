package ee.webmedia.alfresco.user.service;

import java.util.List;

import org.alfresco.web.bean.repository.Node;

public interface UserService {
    static final String BEAN_NAME = "UserService";
    static final String DOCUMENT_MANAGERS = "DOCUMENT_MANAGERS";
    static final String DOCUMENT_MANAGERS_DISPLAY_NAME = "Dokumendihaldurid";
    static final String ALFRESCO_ADMINISTRATORS_DISPLAY_NAME = "Administraatorid";
    static final String EMAIL_CONTRIBUTORS_DISPLAY_NAME = "E-kirja saatjad";
    static final String CUSTOM_UNIT_PROP = "unit";
    static final String CUSTOM_UNIT_NAME_PROP = "unitName";

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
    String getAlfrescoAdministratorsGroup();

    /**
     * Searches for users by first name and last name. If {@code input} is empty, an empty list is returned. The results from this method should be processed by
     * {@link #setUsersUnit(List)} if correct unit name is desired.
     */
    List<Node> searchUsers(String input, boolean returnAllUsers);

    /**
     * Sets correct and up to date unit name for users, based on data found in organization structure list. 
     * 
     * @param users list of user nodes to be processed
     * @return processed nodes
     */
    List<Node> setUsersUnit(List<Node> users);
}
