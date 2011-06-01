package ee.webmedia.alfresco.privilege.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ParameterCheck;
import org.apache.commons.collections.comparators.ComparableComparator;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.privilege.model.PrivilegeModel;
import ee.webmedia.alfresco.privilege.model.UserPrivileges;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * @author Ats Uiboupin
 */
public class PrivilegeServiceImpl implements PrivilegeService {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(PrivilegeServiceImpl.class);
    private PermissionService permissionService;
    private NodeService nodeService;
    private UserService userService;
    private GeneralService generalService;
    private final Map<QName, PrivilegesChangedListener> privilegesChangedListeners = new HashMap<QName, PrivilegeServiceImpl.PrivilegesChangedListener>();
    @SuppressWarnings("unchecked")
    private static final Comparator<String> MEMBERS_BY_GROUP_COMPARATOR = new NullComparator(ComparableComparator.getInstance());
    public static final String GROUPLESS_GROUP = "<groupless>";

    @Override
    public void registerListener(QName listenerCode, PrivilegesChangedListener listener) {
        privilegesChangedListeners.put(listenerCode, listener);
    }

    @Override
    public PrivilegeMappings getPrivilegeMappings(NodeRef manageableRef) {
        PrivilegeMappings privMappings = fillMembersByGroup(manageableRef);
        fillPrivilegesByUserName(privMappings);
        return privMappings;
    }

    private PrivilegeMappings fillMembersByGroup(NodeRef manageableRef) {
        PrivilegeMappings privMappings = new PrivilegeMappings(manageableRef);
        Map<QName, Serializable> manageableNodeProps = nodeService.getProperties(manageableRef);
        Map<String, Set<String>> membersByGroup = new TreeMap<String, Set<String>>(MEMBERS_BY_GROUP_COMPARATOR);
        { // figure out memberships
            @SuppressWarnings("unchecked")
            List<String> privUsers = (List<String>) manageableNodeProps.get(PrivilegeModel.Props.USER);
            @SuppressWarnings("unchecked")
            List<String> privGroups = (List<String>) manageableNodeProps.get(PrivilegeModel.Props.GROUP);
            RepoUtil.validateSameSize(privUsers, privGroups, "users", "groups");
            if (privGroups != null) {
                for (int i = 0; i < privGroups.size(); i++) {
                    String group = privGroups.get(i);
                    if (group == null) {
                        group = GROUPLESS_GROUP;
                    }
                    String user = privUsers.get(i);

                    Set<String> curUserGroups = privMappings.userGroups.get(user);
                    if (curUserGroups == null) {
                        curUserGroups = new HashSet<String>();
                        privMappings.userGroups.put(user, curUserGroups);
                    }
                    curUserGroups.add(group);

                    Set<String> curGroupMembers = membersByGroup.get(group);
                    if (curGroupMembers == null) {
                        curGroupMembers = new HashSet<String>();
                        membersByGroup.put(group, curGroupMembers);
                    }

                    curGroupMembers.add(user);
                }
            }
        }
        privMappings.membersByGroups = membersByGroup;
        return privMappings;
    }

    private Map<String/* userName */, UserPrivileges> fillPrivilegesByUserName(PrivilegeMappings privMappings) {
        Map<String/* userName */, UserPrivileges> privilegesByUsername = new HashMap<String, UserPrivileges>();
        for (AccessPermission accessPermission : permissionService.getAllSetPermissions(privMappings.getManageableRef())) {
            LOG.debug("accessPermission=" + accessPermission);
            String authority = accessPermission.getAuthority();
            if (StringUtils.startsWith(authority, AuthorityType.GROUP.getPrefixString()) || StringUtils.startsWith(authority, AuthorityType.ROLE.getPrefixString())) {
                continue; // not interested in groups and roles added directly to the manageableRef
            }
            UserPrivileges userPrivileges = privilegesByUsername.get(authority);
            if (userPrivileges == null) {
                userPrivileges = new UserPrivileges(authority, userService.getUserFullName(authority));
                privilegesByUsername.put(authority, userPrivileges);

                Set<String> curUserGroups = privMappings.userGroups.get(authority);
                if (curUserGroups != null) {
                    userPrivileges.getGroups().addAll(curUserGroups);
                }
            }
            boolean allowed = AccessStatus.ALLOWED.equals(accessPermission.getAccessStatus());
            Assert.isTrue(allowed, "Expected to see only allowed permissions. accessPermission=" + accessPermission + "\nmanageableRef=" + privMappings.getManageableRef());
            userPrivileges.addPrivilege(accessPermission.getPermission());
        }
        privMappings.privilegesByUsername = privilegesByUsername;
        return privilegesByUsername;
    }

    @Override
    public void savePrivileges(NodeRef manageableRef, Map<String/* userName */, UserPrivileges> privilegesByUsername, Set<String> ignoredGroups, QName listener) {
        PrivilegesChangedListener privilegesChangedListener = privilegesChangedListeners.get(listener);
        if (privilegesChangedListener != null) {
            privilegesChangedListener.onSavePrivileges(manageableRef, privilegesByUsername);
        }
        save(manageableRef, privilegesByUsername, ignoredGroups);
    }

    private void save(NodeRef manageableRef, Map<String/* userName */, UserPrivileges> privilegesByUsername, Set<String> ignoredGroups) {
        ArrayList<String> privUsers = new ArrayList<String>();
        ArrayList<String> privGroups = new ArrayList<String>();
        RepoUtil.validateSameSize(privUsers, privGroups, "users", "groups");

        for (Iterator<Entry<String, UserPrivileges>> it = privilegesByUsername.entrySet().iterator(); it.hasNext();) {
            Entry<String, UserPrivileges> entry = it.next();
            String userName = entry.getKey();
            UserPrivileges vo = entry.getValue();
            for (String permission : vo.getPrivilegesToDelete()) {
                permissionService.deletePermission(manageableRef, userName, permission);
            }
            boolean deleted = vo.isDeleted();
            if (deleted) {
                it.remove();
            } else {
                if (vo.hasManageablePrivileges()) {
                    for (String permission : vo.getPrivilegesToAdd()) {
                        permissionService.setPermission(manageableRef, userName, permission, true);
                    }
                    Set<String> groups = vo.getGroups();
                    for (String group : groups) {
                        if (!ignoredGroups.contains(group) && !deleted) {
                            privUsers.add(userName);
                            privGroups.add(group);
                        }
                    }
                }
            }
        }
        RepoUtil.validateSameSize(privUsers, privGroups, "users", "groups");
        Map<QName, Serializable> userGroupMappingProps = new HashMap<QName, Serializable>();
        userGroupMappingProps.put(PrivilegeModel.Props.USER, privUsers);
        userGroupMappingProps.put(PrivilegeModel.Props.GROUP, privGroups);
        nodeService.addProperties(manageableRef, userGroupMappingProps);
    }

    @Override
    public void addPrivilege(NodeRef nodeRef, Map<String, Object> nodeProps, QName listener, String authority, String... permissions) {
        Set<String> privileges = new HashSet<String>();
        if (permissions != null) {
            for (String permission : permissions) {
                privileges.add(permission);
            }
        }
        addPrivilege(nodeRef, nodeProps, listener, authority, null, privileges);
    }

    @Override
    public void addPrivilege(NodeRef nodeRef, Map<String, Object> nodeProps, QName listener, String authority, String group, Set<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return;
        }
        ParameterCheck.mandatory("authority", authority);

        PrivilegesChangedListener privilegesChangedListener = privilegesChangedListeners.get(listener);
        if (privilegesChangedListener != null) {
            privilegesChangedListener.onAddPrivileges(nodeRef, permissions);
        }

        boolean mustSaveUserGroupProps = false;
        if (nodeProps == null) {
            nodeProps = new HashMap<String, Object>();
            mustSaveUserGroupProps = true;
            nodeProps.put(PrivilegeModel.Props.USER.toString(), nodeService.getProperty(nodeRef, PrivilegeModel.Props.USER));
            nodeProps.put(PrivilegeModel.Props.GROUP.toString(), nodeService.getProperty(nodeRef, PrivilegeModel.Props.GROUP));
        }

        for (String permission : permissions) {
            @SuppressWarnings("unchecked")
            Set<String> addedPrivileges = (Set<String>) nodeProps.get("{temp}addedPrivileges");
            if (addedPrivileges == null) {
                addedPrivileges = new HashSet<String>();
                nodeProps.put("{temp}addedPrivileges", addedPrivileges);
            }
            String key = authority + permission;
            if (!addedPrivileges.contains(key)) {
                addedPrivileges.add(key);
                permissionService.setPermission(nodeRef, authority, permission, true);
            }
        }
        @SuppressWarnings("unchecked")
        List<String> privUsers = (List<String>) nodeProps.get(PrivilegeModel.Props.USER.toString());
        @SuppressWarnings("unchecked")
        List<String> privGroups = (List<String>) nodeProps.get(PrivilegeModel.Props.GROUP.toString());
        RepoUtil.validateSameSize(privUsers, privGroups, "users", "groups");
        if (privUsers == null) {
            privUsers = new ArrayList<String>();
            nodeProps.put(PrivilegeModel.Props.USER.toString(), privUsers);
        }
        if (privGroups == null) {
            privGroups = new ArrayList<String>();
            nodeProps.put(PrivilegeModel.Props.GROUP.toString(), privGroups);
        }
        RepoUtil.validateSameSize(privUsers, privGroups, "users", "groups");

        if (group == null) {
            group = GROUPLESS_GROUP;
        }
        boolean found = false;
        for (int i = 0; i < privUsers.size(); i++) {
            if (authority.equals(privUsers.get(i)) && group.equals(privGroups.get(i))) {
                found = true;
                break;
            }
        }
        // Add, if same user+group combination is not yet added
        if (!found) {
            privUsers.add(authority);
            privGroups.add(group);
        } else {
            mustSaveUserGroupProps = false;
        }
        if (mustSaveUserGroupProps) {
            nodeService.addProperties(nodeRef, generalService.getPropertiesIgnoringSystem(nodeProps));
        }
    }

    /**
     * Holds information about user privileges and user-group mappings
     * 
     * @author Ats Uiboupin
     */
    public class PrivilegeMappings implements Serializable {
        private static final long serialVersionUID = 1L;
        private final NodeRef manageableRef;
        private final Map<String/* user */, Set<String> /* groups */> userGroups = new HashMap<String, Set<String>>();
        private Map<String/* groupCode */, Set<String> /* members */> membersByGroups;
        private Map<String/* userName */, UserPrivileges> privilegesByUsername;

        private PrivilegeMappings(NodeRef manageableRef) {
            this.manageableRef = manageableRef;
        }

        private NodeRef getManageableRef() {
            return manageableRef;
        }

        public Set<String> getMembersByGroup(String group) {
            Set<String> curGroupMembers = membersByGroups.get(group);
            if (curGroupMembers == null) {
                curGroupMembers = new HashSet<String>();
                membersByGroups.put(group, curGroupMembers);
            }
            return curGroupMembers;
        }

        public UserPrivileges getOrCreateUserPrivilegesVO(String userName) {
            UserPrivileges privs = privilegesByUsername.get(userName);
            if (privs == null) {
                privs = new UserPrivileges(userName, userService.getUserFullName(userName));
                privilegesByUsername.put(userName, privs);
            }
            return privs;
        }

        public Map<String, Set<String>> getMembersByGroups() {
            return membersByGroups;
        }

        public Map<String, UserPrivileges> getPrivilegesByUsername() {
            return privilegesByUsername;
        }
    }

    // START: getters / setters
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }
    // END: getters / setters

}
