package ee.webmedia.alfresco.privilege.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

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
import org.alfresco.repo.security.permissions.impl.AccessPermissionImpl;
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

import ee.webmedia.alfresco.common.service.ApplicationService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.privilege.model.PrivMappings;
import ee.webmedia.alfresco.privilege.model.UserPrivileges;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * @author Ats Uiboupin
 */
public class PrivilegeServiceImpl implements PrivilegeService {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(PrivilegeServiceImpl.class);

    private PermissionService permissionService;
    private NodeService nodeService;
    private UserService userService;
    private AuthorityService authorityService;
    public static final String GROUPLESS_GROUP = "<groupless>";
    private ApplicationService applicationService;
    private LogService logService;

    // cache some values that shouldn't change during application runtime
    private Boolean isTest;
    private Set<String> defaultAdmins;

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
    public boolean hasPermission(final NodeRef targetRef, final String permission, String userName) {
        return AuthenticationUtil.runAs(new RunAsWork<Boolean>() {
            @Override
            public Boolean doWork() throws Exception {
                AccessStatus hasPermission = permissionService.hasPermission(targetRef, permission);
                return AccessStatus.ALLOWED.equals(hasPermission);
            }
        }, userName);
    }

    @Override
    public PrivMappings getPrivMappings(NodeRef manageableRef, Collection<String> manageablePermissions) {
        PrivMappings privMappings = new PrivMappings(manageableRef);// fillMembersByGroup(manageableRef);
        Map<String/* userName */, UserPrivileges> privilegesByUsername = new HashMap<String, UserPrivileges>();
        Map<String/* groupName */, UserPrivileges> privilegesByGroup = new HashMap<String, UserPrivileges>();

        ThoroughInheritanceChecker inheritanceChecker;
        { // regular admin group
            String adminGroup = UserService.AUTH_ADMINISTRATORS_GROUP;
            inheritanceChecker = new ThoroughInheritanceChecker(manageableRef, adminGroup);
            UserPrivileges adminPrivs = new UserPrivileges(adminGroup, authorityService.getAuthorityDisplayName(adminGroup));
            privilegesByGroup.put(adminGroup, adminPrivs);
            adminPrivs.setReadOnly(true);
            for (String permission : manageablePermissions) {
                adminPrivs.addPrivilegeDynamic(permission, MessageUtil.getMessage("manage_permissions_extraInfo_adminGroupHasAllPermissions"));
            }
        }

        for (AccessPermission accessPermission : permissionService.getAllSetPermissions(manageableRef)) {
            String authority = accessPermission.getAuthority();
            String permission = accessPermission.getPermission();
            if (StringUtils.startsWith(authority, AuthorityType.ROLE.getPrefixString()) || !manageablePermissions.contains(permission)) {
                continue; // not interested in roles added directly to the manageableRef nor permissions that are not requested
            }
            boolean setDirectly = accessPermission.isSetDirectly();
            boolean inherited = inheritanceChecker.checkInheritance(accessPermission);
            if (StringUtils.startsWith(authority, AuthorityType.GROUP.getPrefixString())) {
                UserPrivileges authPrivileges = privilegesByGroup.get(authority);
                if (authPrivileges == null) {
                    authPrivileges = new UserPrivileges(authority, authorityService.getAuthorityDisplayName(authority));
                    privilegesByGroup.put(authority, authPrivileges);
                }
                boolean allowed = AccessStatus.ALLOWED.equals(accessPermission.getAccessStatus());
                Assert.isTrue(allowed, "Expected to see only allowed permissions. accessPermission=" + accessPermission + "\nmanageableRef=" + manageableRef);
                addPrivilege(authPrivileges, permission, setDirectly, inherited);
            } else {
                UserPrivileges authPrivileges = privilegesByUsername.get(authority);
                if (authPrivileges == null) {
                    authPrivileges = new UserPrivileges(authority, userService.getUserFullNameWithOrganizationPath(authority));
                    privilegesByUsername.put(authority, authPrivileges);
                    Set<String> curUserGroups = privMappings.getUserGroups().get(authority);
                    if (curUserGroups != null) {
                        authPrivileges.getGroups().addAll(curUserGroups);
                    }
                    if (curUserGroups == null || curUserGroups.isEmpty()) {
                        authPrivileges.getGroups().add(GROUPLESS_GROUP);
                    }
                }
                boolean allowed = AccessStatus.ALLOWED.equals(accessPermission.getAccessStatus());
                Assert.isTrue(allowed, "Expected to see only allowed permissions. accessPermission=" + accessPermission + "\nmanageableRef=" + manageableRef);
                if (defaultAdmins.contains(authority)) {
                    authPrivileges.addPrivilegeDynamic(permission, MessageUtil.getMessage("manage_permissions_extraInfo_defaultAdmin"));
                } else {
                    addPrivilege(authPrivileges, permission, setDirectly, inherited);
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
                authPrivileges.addPrivilegeDynamic(permission, MessageUtil.getMessage("manage_permissions_extraInfo_defaultAdmin"));
            }
        }
        return privMappings;
    }

    private void addPrivilege(UserPrivileges authPrivileges, String permission, boolean setDirectly, boolean inherited) {
        if (setDirectly) {
            authPrivileges.addPrivilegeStatic(permission);
        }
        // inherited and setDirectly is here non-exclusive
        if (inherited) {
            authPrivileges.addPrivilegeInherited(permission);
        }
    }

    @Override
    public void savePrivileges(NodeRef manageableRef, Map<String, UserPrivileges> privilegesByUsername, Map<String, UserPrivileges> privilegesByGroup, QName listenerCode) {
        updatePrivileges(manageableRef, privilegesByUsername, false);
        updatePrivileges(manageableRef, privilegesByGroup, true);
    }

    private void updatePrivileges(NodeRef manageableRef, Map<String, UserPrivileges> privilegesByAuthority, boolean group) {
        for (Iterator<Entry<String, UserPrivileges>> it = privilegesByAuthority.entrySet().iterator(); it.hasNext();) {
            Entry<String, UserPrivileges> entry = it.next();
            String authority = entry.getKey();
            UserPrivileges vo = entry.getValue();

            Set<String> privilegesToDelete = vo.getPrivilegesToDelete();
            for (String permission : privilegesToDelete) {
                permissionService.deletePermission(manageableRef, authority, permission);
            }
            if (!vo.isDeleted() && !privilegesToDelete.isEmpty()) {
                logMemberPrivRem(manageableRef, authority, group, privilegesToDelete);
            }

            if (vo.isDeleted()) {
                it.remove();
                logMemberRemove(manageableRef, authority, group);
            } else {
                Set<String> privilegesToAdd = vo.getPrivilegesToAdd();

                if (vo.isNew()) {
                    logMemberAdd(manageableRef, authority, group);
                } else if (!privilegesToAdd.isEmpty()) {
                    logMemberPrivAdd(manageableRef, authority, group, privilegesToAdd);
                }

                if (!privilegesToAdd.isEmpty()) {
                    setPermissions(manageableRef, authority, privilegesToAdd);
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
            try {
                permissionService.setPermission(manageableRef, authority, permission, true);
            } catch (Exception e) {
                throw new RuntimeException("failed to set permission " + permission + " to authority " + authority + " on node " + manageableRef, e);
            }
        }
        return permissionsWithDependencies;
    }

    private void log(NodeRef manageableRef, String messageCode, Object... params) {
        LogObject obj = LogObject.DOCUMENT;
        QName nodeType = nodeService.getType(manageableRef);
        if (SeriesModel.Types.SERIES.equals(nodeType)) {
            obj = LogObject.RIGHTS_SERIES;
        } else if (VolumeModel.Types.VOLUME.equals(nodeType)) {
            obj = LogObject.RIGHTS_VOLUME;
        }
        logService.addLogEntry(LogEntry.create(obj, userService, manageableRef, messageCode, params));
    }

    private String getAuthorityName(String authority, boolean group) {
        return group ? authorityService.getAuthorityDisplayName(authority) : userService.getUserFullNameAndId(authority);
    }

    private void logMemberRemove(NodeRef manageableRef, String authority, boolean group) {
        if (group) {
            log(manageableRef, "applog_rights_rem_group", getAuthorityName(authority, group));
        } else {
            log(manageableRef, "applog_rights_rem_user", getAuthorityName(authority, group));
        }
    }

    private void logMemberAdd(NodeRef manageableRef, String authority, boolean group) {
        if (group) {
            log(manageableRef, "applog_rights_add_group", getAuthorityName(authority, group));
        } else {
            log(manageableRef, "applog_rights_add_user", getAuthorityName(authority, group));
        }
    }

    private void logMemberPrivAdd(NodeRef manageableRef, String authority, boolean group, Set<String> privs) {
        if (group) {
            log(manageableRef, "applog_priv_add_group", getAuthorityName(authority, group), StringUtils.join(privs, ", "));
        } else {
            log(manageableRef, "applog_priv_add_user", getAuthorityName(authority, group), StringUtils.join(privs, ", "));
        }
    }

    private void logMemberPrivRem(NodeRef manageableRef, String authority, boolean group, Set<String> privs) {
        if (group) {
            log(manageableRef, "applog_priv_rem_group", getAuthorityName(authority, group), StringUtils.join(privs, ", "));
        } else {
            log(manageableRef, "applog_priv_rem_user", getAuthorityName(authority, group), StringUtils.join(privs, ", "));
        }
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

    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public void setLogService(LogService logService) {
        this.logService = logService;
    }

    /**
     * {@link AccessPermission#isInherited()} == false does not mean, that the same permission is not also inherited - it just says that this permission is set directly as well
     * 
     * @author Ats Uiboupin
     */
    class ThoroughInheritanceChecker {
        private final Set<String> userNamesInAdminsGroup;
        private final NodeRef parentRef;
        private final boolean inheritParentPermissions;
        private final Map<String, Set<String>> groupsByUser = new HashMap<String, Set<String>>();
        private Map<String, AccessPermission> ancestorAccessPermissionsMap;

        ThoroughInheritanceChecker(NodeRef manageableRef, String adminGroup) {
            parentRef = nodeService.getPrimaryParent(manageableRef).getParentRef();
            userNamesInAdminsGroup = getUserService().getUserNamesInGroup(adminGroup);
            inheritParentPermissions = BeanHelper.getPermissionService().getInheritParentPermissions(manageableRef);
            if (isTest == null) {
                isTest = applicationService.isTest();
            }
            if (defaultAdmins == null) {
                defaultAdmins = BeanHelper.getAuthenticationService().getDefaultAdministratorUserNames();
            }
        }

        private void lazyInit() {
            if (ancestorAccessPermissionsMap == null) {
                Set<AccessPermission> ancestorAccessPermissions = permissionService.getAllSetPermissions(parentRef);
                ancestorAccessPermissionsMap = new HashMap<String, AccessPermission>();
                Set<String> visitedGroups = new HashSet<String>();
                for (AccessPermission accessPermission : ancestorAccessPermissions) {
                    String key = customHash(accessPermission);
                    AccessPermission previousValue = ancestorAccessPermissionsMap.put(key, accessPermission);
                    if (previousValue != null && isTest) {
                        // I assume that there are not two AccessPermissions with same customHash, but if I'm wrong, then maybe I'm missing something
                        MessageUtil.addErrorMessage("Weird, several AccessPermissions on parent node with same hash - check code");
                    }
                    // for debugging
                    if (isTest && accessPermission.getAuthorityType() == AuthorityType.GROUP) {
                        String authority = accessPermission.getAuthority();
                        if (!visitedGroups.contains(authority)) {
                            for (String userName : getUserService().getUserNamesInGroup(authority)) {
                                Set<String> userGroups = groupsByUser.get(userName);
                                if (userGroups == null) {
                                    userGroups = visitedGroups;
                                    groupsByUser.put(userName, userGroups);
                                }
                                userGroups.add(authority);
                            }
                        }
                    }
                }
            }
        }

        /**
         * @param accessPermission
         * @return true if the same permission is granted to parent node(either directly or inherited from any other ancestor)
         */
        boolean checkInheritance(AccessPermission accessPermission) {
            boolean inherited = accessPermission.isInherited();
            if (inherited || !inheritParentPermissions) {
                return inherited;
            }
            Assert.isTrue(accessPermission.isSetDirectly(), "expected that set directly");
            /**
             * AccessPermission it is set directly(and based on this AccessPermission not inherited)
             * THIS DOES NOT MEAN THAT SAME PERMISSION IS NOT INHERITED,
             * because if permission is set directly then you don't see another record saying that it is also inherited (in case ancestor node has the same permission set directly
             * or inherited).
             * THEREFORE WE NEED TO CHECK FIRST PARENT NODE ACCESSPERMISSIONS AS WELL
             */
            String authority = accessPermission.getAuthority();
            String permission = accessPermission.getPermission();
            // permission is set directly, but we need to know if it is also inherited from ancestor nodes
            boolean hasPermissionForParentRef = hasPermission(parentRef, permission, authority);
            if (hasPermissionForParentRef) {
                Boolean inheritedFromParent = isInherited(accessPermission);
                if (inheritedFromParent != null) {
                    inherited = inheritedFromParent;
                }
            }
            return inherited;
        }

        private Boolean isInherited(AccessPermission origAccessPermission) {
            String authority = origAccessPermission.getAuthority();
            lazyInit();
            String permission = origAccessPermission.getPermission();
            if (hasAccessPermission(authority, permission)) {
                return true;
            }
            // check being admin after inspecting AccessPermissions
            if (origAccessPermission.getAuthorityType() == AuthorityType.ADMIN || userNamesInAdminsGroup.contains(authority) || defaultAdmins.contains(authority)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("NOT inherited isAdmin: " + authority + " " + permission);
                }
                return false;
            }
            if (isTest) {
                // doing check through group only in test environments - in other environments expecting that this permission is not inherited
                if (origAccessPermission.getAuthorityType() == AuthorityType.USER) {
                    Set<String> userGroups = groupsByUser.get(authority);
                    if (userGroups != null) {
                        for (String group : userGroups) {
                            if (hasAccessPermission(group, permission)) {
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("this permission " + permission + " it self is not inherited,"
                                            + " but it is granted to " + origAccessPermission.getAuthority() + " through parent node and user group " + group);
                                }
                                return false; // this permission it self is not inherited, but it is granted through parent node and user group
                            }
                        }
                    }
                }
                MessageUtil.addErrorMessage(authority + " has permission " + permission + " on parentNode, however it is NOT INHERITED, but it is still somehow granted");
            }
            return null;
        }

        private boolean hasAccessPermission(String authority, String permission) {
            String key = customHash(new AccessPermissionImpl(permission, AccessStatus.ALLOWED, authority, /* position doesn't matter */0));
            return ancestorAccessPermissionsMap.get(key) != null;
        }

        private String customHash(AccessPermission accessPermission) {
            return accessPermission.getPermission() + " ¤ " + accessPermission.getAccessStatus().name() + " ¤ " + accessPermission.getAuthority();
        }
    }
}
