package ee.webmedia.alfresco.privilege.model;

import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Holds information about user and group privileges
 */
public class PrivMappings extends PrivilegeMappings {
    private static final long serialVersionUID = 1L;
    private Map<String/* userName */, UserPrivileges> privilegesByGroup;

    public PrivMappings(NodeRef manageableRef) {
        super(manageableRef);
    }

    public void setPrivilegesByGroup(Map<String, UserPrivileges> privilegesByGroup) {
        this.privilegesByGroup = privilegesByGroup;
    }

    public Map<String, UserPrivileges> getPrivilegesByGroup() {
        return privilegesByGroup;
    }
}