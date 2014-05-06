package ee.webmedia.alfresco.privilege.service;

import ee.webmedia.alfresco.privilege.model.Privilege;

public class Permission {

    private final String authority;
    private final boolean direct;
    private final Privilege privilege;

    public Permission(String authority, boolean direct, Privilege privilege) {
        this.authority = authority;
        this.direct = direct;
        this.privilege = privilege;
    }

    public String getAuthority() {
        return authority;
    }

    public boolean isDirect() {
        return direct;
    }

    public Privilege getPrivilege() {
        return privilege;
    }

}
