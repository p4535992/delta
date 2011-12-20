package ee.webmedia.alfresco.privilege.service;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.privilege.model.PrivMappings;
import ee.webmedia.alfresco.privilege.model.PrivilegeMappings;
import ee.webmedia.alfresco.privilege.model.PrivilegeModel;
import ee.webmedia.alfresco.privilege.model.UserPrivileges;
import ee.webmedia.alfresco.privilege.web.ManagePrivilegesDialog;

/**
 * Service that helps to manage privileges(permissions and group belongings of a user)
 * 
 * @author Ats Uiboupin
 */
public interface PrivilegeService {
    String BEAN_NAME = "PrivilegeService";

    @Deprecated
    PrivilegeMappings getPrivilegeMappings(NodeRef manageableRef);

    /**
     * @param manageableRef
     * @param manageablePermissions - only ACL entries with given permissions are used to fill {@link PrivMappings} that is returned
     * @return
     */
    PrivMappings getPrivMappings(NodeRef manageableRef, Collection<String> manageablePermissions);

    /**
     * FIXME PRIV2 Ats - vana privileegide halduse meetod
     * 
     * @param manageableRef
     * @param privilegesByUsername - VO's that contain information about user and his/her static privileges to add/remove for <code>manageableRef</code>
     * @param ignoredGroups
     * @param listener - optional listener(must have been registered using this QName) to be called, that can cause side-effects, such as adding privileges to related nodes
     */
    @Deprecated
    void savePrivileges(NodeRef manageableRef, Map<String, UserPrivileges> privilegesByUsername, Set<String> ignoredGroups, QName listenerCode);

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
     * adds permission <code>permission</code> to <code>nodeRef</code> and adds <code>autority</code> to {@link PrivilegeModel.Props#USER} if it didn't already contain it
     * 
     * @param nodeRef
     * @param nodeProps optional - not null, then {@link NodeService#setProperties(NodeRef, Map)} must be called by the caller of this method, otherwise this method also calls
     *            {@link NodeService#addProperties(NodeRef, Map)} to update userGroup info if user wasn't in {@link PrivilegeModel.Props#USER}
     * @param listener - optional listener(must have been registered using this QName) to be called, that can cause side-effects, such as adding privileges to related nodes
     * @param authority
     * @param group - used to visually group users in {@link ManagePrivilegesDialog}. If no group is set and user doesn't belong to any group, then
     *            {@link PrivilegeServiceImpl#GROUPLESS_GROUP} is used as a group
     * @param requiredPrivileges
     */
    void addPrivilege(NodeRef nodeRef, Map<String, Object> nodeProps, QName listener, String authority, String group, Set<String> requiredPrivileges);

    /**
     * @see #addPrivilege(NodeRef, Object, QName, String, Set)
     */
    void addPrivilege(NodeRef nodeRef, Map<String, Object> nodeProps, QName listenerCode, String authority, String... permissions);

    void registerListener(QName listenerCode, PrivilegesChangedListener listener);

    public interface PrivilegesChangedListener {
        void onSavePrivileges(NodeRef manageableRef, Map<String, UserPrivileges> privilegesByUsername);

        void onAddPrivileges(NodeRef nodeRef, Set<String> permissions);
    }

}
