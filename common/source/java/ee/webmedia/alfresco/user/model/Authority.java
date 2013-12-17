package ee.webmedia.alfresco.user.model;

import java.io.Serializable;

import org.alfresco.web.ui.repo.WebResources;

public class Authority implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String authority;
    private final boolean group;
    private final String name;

    public Authority(String authority, boolean group, String name) {
        this.authority = authority;
        this.group = group;
        this.name = name;
    }

    public String getAuthority() {
        return authority;
    }

    public boolean isGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return group ? WebResources.IMAGE_GROUP : WebResources.IMAGE_PERSON;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return authority.equals(obj);
        }
        if (!(obj instanceof Authority)) {
            return false;
        }
        return authority.equals(((Authority) obj).getAuthority());
    }

    @Override
    public int hashCode() {
        return authority.hashCode();
    }

}
