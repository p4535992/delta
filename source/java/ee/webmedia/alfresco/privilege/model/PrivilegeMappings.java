package ee.webmedia.alfresco.privilege.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.common.web.BeanHelper;

/**
 * Holds information about user privileges and user-group mappings
 */
public class PrivilegeMappings implements Serializable {
    private static final long serialVersionUID = 1L;
    private final NodeRef manageableRef;
    private final Map<String/* user */, Set<String> /* groups */> userGroups = new HashMap<String, Set<String>>();
    private Map<String/* groupCode */, Set<String> /* members */> membersByGroups;
    private Map<String/* userName */, UserPrivileges> privilegesByUsername;

    public PrivilegeMappings(NodeRef manageableRef) {
        this.manageableRef = manageableRef;
    }

    public NodeRef getManageableRef() {
        return manageableRef;
    }

    public Set<String> getMembersByGroup(String group) {
        Set<String> curGroupMembers = getMembersByGroups().get(group);
        if (curGroupMembers == null) {
            curGroupMembers = new HashSet<String>();
            getMembersByGroups().put(group, curGroupMembers);
        }
        return curGroupMembers;
    }

    public UserPrivileges getOrCreateUserPrivilegesVO(String userName) {
        UserPrivileges privs = getPrivilegesByUsername().get(userName);
        if (privs == null) {
            privs = new UserPrivileges(userName, BeanHelper.getUserService().getUserFullName(userName));
            getPrivilegesByUsername().put(userName, privs);
        }
        return privs;
    }

    public Map<String, Set<String>> getMembersByGroups() {
        return membersByGroups;
    }

    public Map<String, UserPrivileges> getPrivilegesByUsername() {
        return privilegesByUsername;
    }

    public Map<String/* user */, Set<String> /* groups */> getUserGroups() {
        return userGroups;
    }

    public void setMembersByGroups(Map<String/* groupCode */, Set<String> /* members */> membersByGroups) {
        this.membersByGroups = membersByGroups;
    }

    public void setPrivilegesByUsername(Map<String/* userName */, UserPrivileges> privilegesByUsername) {
        this.privilegesByUsername = privilegesByUsername;
    }
}
