<<<<<<< HEAD
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

    boolean hasPermissionOnAuthority(NodeRef targetRef, String authority, String... permissions);

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
     * Triggers re-indexing of all descendant nodes of documents under given node that inherit privileges from given node.
     * 
     * @param nodeRef The node (usually series) under which documents are re-indexed.
     */
    void updateIndexedPermissions(NodeRef nodeRef);

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
=======
package ee.webmedia.alfresco.privilege.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.privilege.model.PrivMappings;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.privilege.model.UserPrivileges;

/**
 * Service that helps to manage privileges(permissions and group belongings of a user)
 */
public interface PrivilegeService {
    String BEAN_NAME = "PrivilegeService";

    boolean hasPermission(final NodeRef targetRef, String userName, final Privilege... permission);

    boolean hasPermissionOnAuthority(NodeRef targetRef, String authority, Privilege... permissions);

    /**
     * @param manageableRef
     * @param collection - only ACL entries with given permissions are used to fill {@link PrivMappings} that is returned
     * @param inheritRef TODO
     * @param grandInheritRef TODO
     * @return
     */
    PrivMappings getPrivMappings(NodeRef manageableRef, Collection<Privilege> collection);

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
     * Triggers re-indexing of all descendant nodes of documents under given node that inherit privileges from given node.
     *
     * @param nodeRef The node (usually series) under which documents are re-indexed.
     */
    void updateIndexedPermissions(NodeRef nodeRef);

    /**
     * Set <code>privilegesToAdd</code> permissions with dependencies for those permissions for given <code>authority</code> on given node
     *
     * @param manageableRef
     * @param userName
     * @param privilegesToAdd
     * @return permissions with dependencies
     */
    public void setPermissions(NodeRef manageableRef, String authority, Privilege... privilegesToAdd);

    /** @see #setPermissions(NodeRef, String, String...) */
    void setPermissions(NodeRef manageableRef, String authority, Set<Privilege> privilegesToAdd);

    void addDynamicAuthority(DynamicAuthority dynamicAuthority);

    Map<String, List<String>> getCreateDocumentPrivileges(Set<String> nodeRefIds);

    void removeAllPermissions(NodeRef manageableRef, String... authorities);

    Set<Privilege> getAllCurrentUserPermissions(NodeRef nodeRef, QName type);

    List<String> getAuthoritiesWithPrivilege(NodeRef nodeRef, Privilege... privilege);

    void setInheritParentPermissions(NodeRef manageableRef, boolean inherits);

    void removeNodePermissionData(NodeRef manageableRef);

    boolean getInheritParentPermissions(NodeRef manageableRef);

    void removeAuthorityPermissions(String userName);

    List<Permission> getAllSetPrivileges(NodeRef nodeRef);

    /** Used to copy permissions from existing node to new node. Assumes that no permissions have been added to new node */
    void copyPermissions(NodeRef sourceNodeRef, NodeRef destinationNodeRef);

    List<String> getAuthoritiesWithDirectPrivilege(NodeRef nodeRef, Privilege... privileges);

    Map<String, List<String>> getCreateCaseFilePrivileges(Set<String> nodeRefIds);

}
>>>>>>> develop-5.1
