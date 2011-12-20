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

import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * VO that maintains information about user privileges(permissions, group belongings)
 * 
 * @author Ats Uiboupin
 */
public class UserPrivileges implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String EMPTY_DYN_PRIV_REASON = "";

    /** used for grouping consecutive rows with same value into same tbody element */
    private final String authName;
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
    protected final Map<String/* privilege */, Boolean/* inherited */> inheritanceByPrivilege = new HashMap<String, Boolean>();
    private String inheritedMsg;
    protected final Map<String/* privilege */, String/* explanation */> explanationByPrivilege = new HashMap<String, String>() {
        private static final long serialVersionUID = 1L;

        @Override
        public String get(Object privilege) {
            String explanation = getDynamicPrivReasons().get(privilege);
            Boolean inherited = inheritanceByPrivilege.get(privilege);
            if (inherited != null && inherited) {
                if (inheritedMsg == null) {
                    inheritedMsg = MessageUtil.getMessage("manage_permissions_extraInfo_inherited");
                }
                explanation = explanation != null ? explanation + "; " : "";
                return explanation + inheritedMsg;
            }
            return explanation;
        }
    };

    private final Map<String/* privilege */, Boolean /* checkboxDisabled */> disabledByPrivilege = new HashMap<String, Boolean>() {
        private static final long serialVersionUID = 1L;

        @Override
        public Boolean get(Object privilege) {
            if (dynamicPrivileges.get(privilege) != null) {
                return true; // has dynamic privilege (doesn't matter if also static or not)
            }
            Boolean inherited = inheritanceByPrivilege.get(privilege);
            // return (inherited != null && inherited);
            if (inherited != null && inherited) {
                return true;
            }
            return false;
        }
    };

    public UserPrivileges(String authName, String userDisplayName) {
        this.authName = authName;
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
        boolean inherited = false; // FIXME PRIV2 Ats ok?
        addPrivilege(privilege, inherited);
    }

    public void addPrivilege(String privToAdd, boolean inherited) {
        privileges.put(privToAdd, true);
        inheritanceByPrivilege.put(privToAdd, inherited);
    }

    public void addPrivileges(Collection<String> privsToAdd) {
        for (String privilege : privsToAdd) {
            addPrivilege(privilege, false);
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
    /** Used for JSF binding */
    public Map<String, Boolean> getPrivileges() {
        return privileges;
    }

    public Map<String, String> getDynamicPrivReasons() {
        if (dynamicPrivReasonsCached == null) {
            Set<Entry<String, Set<String>>> entrySet = dynamicPrivReasons.entrySet();
            dynamicPrivReasonsCached = new HashMap<String, String>(entrySet.size());
            for (Entry<String, Set<String>> entry : entrySet) {
                String priv = entry.getKey();
                Set<String> reasons = new HashSet<String>(entry.getValue());
                reasons.remove(EMPTY_DYN_PRIV_REASON);
                dynamicPrivReasonsCached.put(priv, StringUtils.join(reasons, "; "));
            }
        }
        return dynamicPrivReasonsCached;
    }

    public Map<String, String> getExplanationByPrivilege() {
        return explanationByPrivilege;
    }

    @Deprecated
    public Map<String, Boolean> getDynamicPrivileges() {
        return dynamicPrivileges;
    }

    /** used by JSF to determine if checkBox should be readRnly */
    public Map<String, Boolean> getDisabledByPrivilege() {
        return disabledByPrivilege;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public String getUserName() {
        return authName;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((authName == null) ? 0 : authName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        UserPrivileges other = (UserPrivileges) obj;
        if (authName == null) {
            if (other.authName != null) {
                return false;
            }
        } else if (!authName.equals(other.authName)) {
            return false;
        }
        return true;
    }

}
