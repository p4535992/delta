package ee.webmedia.alfresco.privilege.service;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.privilege.model.PrivMappings;
import ee.webmedia.alfresco.privilege.model.UserPrivileges;

/**
 * Service that helps to manage privileges(permissions and group belongings of a user)
 * 
 * @author Ats Uiboupin
 */
public interface PrivilegeService {
    String BEAN_NAME = "PrivilegeService";

    /**
     * @param nodeRef
     * @param permissions
     * @return true if current user has all permissions for given nodeRef
     */
    public boolean hasPermissions(NodeRef nodeRef, String... permissions);

    boolean hasPermission(final NodeRef targetRef, final String permission, String userName);

    /**
     * @param manageableRef
     * @param manageablePermissions - only ACL entries with given permissions are used to fill {@link PrivMappings} that is returned
     * @return
     */
    PrivMappings getPrivMappings(NodeRef manageableRef, Collection<String> manageablePermissions);

    /**
     * @param manageableRef
     * @param privilegesByUsername - VO's that contain information about user and his/her static privileges to add/remove for <code>manageableRef</code>
     * @param privilegesByGroup
     * @param ignoredGroups
     * @param listener - optional listener(must have been registered using this QName) to be called, that can cause side-effects, such as adding privileges to related nodes
     */
    void savePrivileges(NodeRef manageableRef, Map<String, UserPrivileges> privilegesByUsername
            , Map<String, UserPrivileges> privilegesByGroup, QName listenerCode);

    /**
     * Set <code>privilegesToAdd</code> permissions with dependencies for those permissions for given <code>authority</code> on given node
     * 
     * @param manageableRef
     * @param userName
     * @param privilegesToAdd
     * @return permissions with dependencies
     */
    public Set<String> setPermissions(NodeRef manageableRef, String authority, String... privilegesToAdd);

    /** @see #setPermissions(NodeRef, String, String...) */
    Set<String> setPermissions(NodeRef manageableRef, String authority, Set<String> privilegesToAdd);

}
