package ee.webmedia.alfresco.privilege.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * VO that maintains information about user privileges(permissions, group belongings)
 */
public class UserPrivileges implements Serializable {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(UserPrivileges.class);
    private static final long serialVersionUID = 1L;

    /** used for grouping consecutive rows with same value into same tbody element */
    private final String userName;
    private final String userDisplayName;
    private final Set<String> groups = new HashSet<String>();

    private boolean readOnly;
    /** all permissions related to this user should be deleted when updating permissions of a node */
    private boolean deleted;

    /** privileges added dynamically */
    private Map<String /* privilege */, String /* reason */> dynamicPrivReasonsCached = new LinkedHashMap<String, String>();
    private final Map<String /* privilege */, Set<String> /* reason */> dynamicPrivReasons = new LinkedHashMap<String, Set<String>>();
    private final Map<String /* privilege */, Boolean /* alsoStatic */> dynamicPrivileges = new HashMap<String, Boolean>();

    /** static privileges (already saved) */
    private Set<String> staticPrivilegesBeforeChanges;
    /** static & dynamic privileges (some privileges that have neither been added nor removed are not included in this map) */
    private final Map<String/* privilege */, Boolean/* active */> privileges = new HashMap<String, Boolean>();

    public UserPrivileges(String userName, String userDisplayName) {
        this.userName = userName;
        this.userDisplayName = userDisplayName;
    }

    public void markBaseState() {
        staticPrivilegesBeforeChanges = getStaticPrivileges();
    }

    public void addDynamicPrivilege(String privilege, String reason) {
        boolean hasPriv = BooleanUtils.isTrue(privileges.get(privilege));
        boolean hasStaticPriv = hasPriv && (!dynamicPrivileges.containsKey(privilege) || dynamicPrivileges.get(privilege));
        dynamicPrivileges.put(privilege, hasStaticPriv);
        Set<String> privReasons = dynamicPrivReasons.get(privilege);
        if (privReasons == null) {
            privReasons = new LinkedHashSet<String>();
            dynamicPrivReasons.put(privilege, privReasons);
        }
        if (privReasons.add(reason)) {
            dynamicPrivReasonsCached = null;
        }
        if (hasPriv) {
            return;
        }
        addPrivilege(privilege);
    }

    public void addPrivilege(String... privsToAdd) {
        for (String privilege : privsToAdd) {
            privileges.put(privilege, true);
        }
    }

    public void addPrivileges(Collection<String> privsToAdd) {
        for (String privilege : privsToAdd) {
            privileges.put(privilege, true);
        }
    }

    public void deletePrivileges(Collection<String> privsToDelete) {
        for (String privilege : privsToDelete) {
            boolean hasDynamicPriv = dynamicPrivileges.containsKey(privilege);
            if (hasDynamicPriv) {
                dynamicPrivileges.put(privilege, false);
            } else {
                privileges.put(privilege, false);
            }
        }
    }

    public Set<String> getPrivilegesToAdd() {
        if (deleted) {
            return Collections.<String> emptySet();// don't add any privileges
        }
        Set<String> privilegesToDelete = getStaticPrivileges();
        if (staticPrivilegesBeforeChanges != null) {
            privilegesToDelete.removeAll(staticPrivilegesBeforeChanges);
        }
        return privilegesToDelete;
    }

    /**
     * {@link #getStaticPrivileges()}
     * 
     * @return privileges that user would have when {@link #getPrivilegesToDelete()} are removed and
     */
    public Set<String> getActivePrivileges() {
        Set<String> activePrivs = getStaticPrivileges();
        activePrivs.addAll(dynamicPrivileges.keySet());
        return activePrivs;
    }

    /**
     * @return static privileges that user had, but should be deleted when saving
     */
    public Set<String> getPrivilegesToDelete() {
        if (staticPrivilegesBeforeChanges == null) {
            return Collections.<String> emptySet();
        }
        if (deleted) {
            return staticPrivilegesBeforeChanges;// remove all static privileges that user had
        }
        Set<String> privilegesToDelete = new HashSet<String>(staticPrivilegesBeforeChanges);
        privilegesToDelete.removeAll(getActivePrivileges(false));
        return privilegesToDelete;
    }

    public Set<String> getStaticPrivileges() {
        return getActivePrivileges(true);
    }

    /**
     * @param staticOnly - could be also read as "staticOnly"
     * @return
     *         if !considerDynamic then all active privileges are returned,
     *         if considerDynamic (no dynamic priv or both bynamic and static)
     */
    private Set<String> getActivePrivileges(boolean staticOnly) {
        Set<String> activePrivileges = new HashSet<String>();
        for (Entry<String, Boolean> entry : privileges.entrySet()) {
            String privilege = entry.getKey();
            if (entry.getValue()) {
                if (!staticOnly || !dynamicPrivileges.containsKey(privilege) || dynamicPrivileges.get(privilege)) {
                    activePrivileges.add(privilege);
                }
            }
        }
        return activePrivileges;
    }

    /**
     * @return true if user has at least one static privilege(managed by this VO)
     */
    public boolean hasManageablePrivileges() {
        return !getActivePrivileges(true).isEmpty();
    }

    public void addGroup(String group) {
        groups.add(group);
    }

    // START: getters / setters
    public Map<String, Boolean> getPrivileges() {
        return privileges;
    }

    public Map<String, String> getDynamicPrivReasons() {
        if (dynamicPrivReasonsCached == null) {
            Set<Entry<String, Set<String>>> entrySet = dynamicPrivReasons.entrySet();
            dynamicPrivReasonsCached = new HashMap<String, String>(entrySet.size());
            for (Entry<String, Set<String>> entry : entrySet) {
                String priv = entry.getKey();
                Set<String> reasons = entry.getValue();
                dynamicPrivReasonsCached.put(priv, StringUtils.join(reasons, "; "));
            }
        }
        return dynamicPrivReasonsCached;
    }

    public Map<String, Boolean> getDynamicPrivileges() {
        return dynamicPrivileges;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserDisplayName() {
        return userDisplayName;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    // END: getters / setters
    @Override
    public String toString() {
        return ReflectionToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

}
