package ee.webmedia.alfresco.privilege.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.privilege.model.PrivMappings;
import ee.webmedia.alfresco.privilege.model.UserPrivileges;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Ats Uiboupin
 */
public class PrivilegeServiceImpl implements PrivilegeService {
    private PermissionService permissionService;
    private NodeService nodeService;
    private UserService userService;
    private AuthorityService authorityService;
    public static final String GROUPLESS_GROUP = "<groupless>";

    @Override
    public boolean hasPermissions(NodeRef nodeRef, String... permissions) {
        if (permissions != null) {
            for (String permission : permissions) {
                if (permissionService.hasPermission(nodeRef, permission) == AccessStatus.DENIED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public PrivMappings getPrivMappings(NodeRef manageableRef, Collection<String> manageablePermissions) {
        PrivMappings privMappings = new PrivMappings(manageableRef);// fillMembersByGroup(manageableRef);
        Map<String/* userName */, UserPrivileges> privilegesByUsername = new HashMap<String, UserPrivileges>();
        Map<String/* groupName */, UserPrivileges> privilegesByGroup = new HashMap<String, UserPrivileges>();
        NodeRef parentRef = nodeService.getPrimaryParent(manageableRef).getParentRef();

        { // regular admin group
            String adminGroup = UserService.AUTH_ADMINISTRATORS_GROUP;
            UserPrivileges adminPrivs = new UserPrivileges(adminGroup, authorityService.getAuthorityDisplayName(adminGroup));
            privilegesByGroup.put(adminGroup, adminPrivs);
            adminPrivs.setReadOnly(true);
            for (String permission : manageablePermissions) {
                adminPrivs.addDynamicPrivilege(permission, MessageUtil.getMessage("manage_permissions_extraInfo_adminGroupHasAllPermissions"));
            }
        }

        Set<String> defaultAdmins = BeanHelper.getAuthenticationService().getDefaultAdministratorUserNames();

        boolean inheritParentPermissions = BeanHelper.getPermissionService().getInheritParentPermissions(manageableRef);

        for (AccessPermission accessPermission : permissionService.getAllSetPermissions(privMappings.getManageableRef())) {
            String authority = accessPermission.getAuthority();
            String permission = accessPermission.getPermission();
            if (StringUtils.startsWith(authority, AuthorityType.ROLE.getPrefixString()) || !manageablePermissions.contains(permission)) {
                continue; // not interested in roles added directly to the manageableRef nor permissions that are not requested
            }
            boolean inherited = accessPermission.isInherited();
            if (!inherited && inheritParentPermissions) {
                // permission is assigned directly, but we need to know if it is also given dynamically
                boolean hasPermissionForParentRef = hasPermission(parentRef, permission, authority);
//@formatter:off
/*
                if (inherited != hasPermissionForParentRef && !defaultAdmins.contains(authority)) {
                    // hasPermissionForParentRef doesn't mean that this permission is set directly to parentRef
                    // - it might be granted dynamically as well as inherited from parent of parent
                    // - for example to users listed by external.authentication.defaultAdministratorUserNames
                    // FIXME PRIV2 Ats - teadet võidakse kuvada, kui sama permission on antud ka parent-nodele (kasutajale või mõnele kasutaja grupile)
                    MessageUtil.addWarningMessage(authority + " has permission " + permission + " - it is not inherited, but it is still somehow granted");
                }
*/
//@formatter:on
                inherited = hasPermissionForParentRef;
            }
            if (StringUtils.startsWith(authority, AuthorityType.GROUP.getPrefixString())) {
                UserPrivileges authPrivileges = privilegesByGroup.get(authority);
                if (authPrivileges == null) {
                    authPrivileges = new UserPrivileges(authority, authorityService.getAuthorityDisplayName(authority));
                    privilegesByGroup.put(authority, authPrivileges);
                }
                boolean allowed = AccessStatus.ALLOWED.equals(accessPermission.getAccessStatus());
                Assert.isTrue(allowed, "Expected to see only allowed permissions. accessPermission=" + accessPermission + "\nmanageableRef=" + privMappings.getManageableRef());
                authPrivileges.addPrivilege(permission, inherited);
            } else {
                UserPrivileges authPrivileges = privilegesByUsername.get(authority);
                if (authPrivileges == null) {
                    authPrivileges = new UserPrivileges(authority, userService.getUserFullNameWithOrganizationPath(authority));
                    privilegesByUsername.put(authority, authPrivileges);

                    Set<String> curUserGroups = privMappings.getUserGroups().get(authority);
                    if (curUserGroups != null) {
                        authPrivileges.getGroups().addAll(curUserGroups);
                    }
                }
                boolean allowed = AccessStatus.ALLOWED.equals(accessPermission.getAccessStatus());
                Assert.isTrue(allowed, "Expected to see only allowed permissions. accessPermission=" + accessPermission + "\nmanageableRef=" + privMappings.getManageableRef());
                if (defaultAdmins.contains(authority)) {
                    authPrivileges.addDynamicPrivilege(permission, MessageUtil.getMessage("manage_permissions_extraInfo_defaultAdmin"));
                } else {
                    authPrivileges.addPrivilege(permission, inherited);
                }
            }
        }
        privMappings.setPrivilegesByUsername(privilegesByUsername);
        privMappings.setPrivilegesByGroup(privilegesByGroup);

        // add all manageable permissions to all default administrators
        for (String defaultAdmin : defaultAdmins) {
            UserPrivileges authPrivileges = privMappings.getOrCreateUserPrivilegesVO(defaultAdmin);
            authPrivileges.setReadOnly(true);
            for (String permission : manageablePermissions) {
                authPrivileges.addDynamicPrivilege(permission, MessageUtil.getMessage("manage_permissions_extraInfo_defaultAdmin"));
            }
        }
        return privMappings;
    }

    private boolean hasPermission(final NodeRef targetRef, final String permission, String userName) {
        return AuthenticationUtil.runAs(new RunAsWork<Boolean>() {
            @Override
            public Boolean doWork() throws Exception {
                AccessStatus hasPermission = permissionService.hasPermission(targetRef, permission);
                return AccessStatus.ALLOWED.equals(hasPermission);
            }
        }, userName);
    }

    @Override
    public void savePrivileges(NodeRef manageableRef, Map<String, UserPrivileges> privilegesByUsername, Map<String, UserPrivileges> privilegesByGroup, QName listenerCode) {
        // FIXME PRIV2 Ats - at the moment (at least for documents) there is no need for listeners
        // notifyListeners(manageableRef, privilegesByUsername, listenerCode);
        updatePrivileges(manageableRef, privilegesByUsername);
        updatePrivileges(manageableRef, privilegesByGroup);
    }

    private void updatePrivileges(NodeRef manageableRef, Map<String, UserPrivileges> privilegesByAuthority) {
        for (Iterator<Entry<String, UserPrivileges>> it = privilegesByAuthority.entrySet().iterator(); it.hasNext();) {
            Entry<String, UserPrivileges> entry = it.next();
            String authority = entry.getKey();
            UserPrivileges vo = entry.getValue();
            for (String permission : vo.getPrivilegesToDelete()) {
                permissionService.deletePermission(manageableRef, authority, permission);
            }
            if (vo.isDeleted()) {
                it.remove();
            } else {
                if (vo.hasManageablePrivileges()) {
                    setPermissions(manageableRef, authority, vo.getPrivilegesToAdd());
                }
            }
        }
    }

    @Override
    public Set<String> setPermissions(NodeRef manageableRef, String authority, String... privilegesToAdd) {
        if (privilegesToAdd == null || privilegesToAdd.length == 0) {
            throw new IllegalArgumentException("setPermissions() called without any privilegesToAdd");
        }
        return setPermissions(manageableRef, authority, new HashSet<String>(Arrays.asList(privilegesToAdd)));
    }

    @Override
    public Set<String> setPermissions(NodeRef manageableRef, String authority, Set<String> privilegesToAdd) {
        Assert.notNull(manageableRef, "setPermissions() called manageableRef");
        Assert.notNull(authority, "setPermissions() called without authority");
        Set<String> permissionsWithDependencies = PrivilegeUtil.getPrivsWithDependencies(privilegesToAdd);
        for (String permission : permissionsWithDependencies) {
            permissionService.setPermission(manageableRef, authority, permission, true);
        }
        return permissionsWithDependencies;
    }

    // START: getters / setters
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    // END: getters / setters

}
