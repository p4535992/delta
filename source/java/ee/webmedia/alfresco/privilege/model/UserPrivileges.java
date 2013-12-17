package ee.webmedia.alfresco.privilege.model;

import static ee.webmedia.alfresco.privilege.service.PrivilegeServiceImpl.GROUPLESS_GROUP;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.service.cmr.security.AccessPermission;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * VO that maintains information about user privileges(permissions, group belongings)
 * 
 * @author Ats Uiboupin
 */
// FIXME PRIV2 Ats - rename to AuthPrivileges - algselt oli see ainult kasutajate jaoks, nüüd ka gruppide jaoks sama objekt
public class UserPrivileges implements Serializable {
    private static final long serialVersionUID = 1L;

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

    /** static privileges (already saved) */
    private Set<String> staticPrivilegesBeforeChanges;
    private String inheritedMsg;
    private String staticMsg;
    private final Map<String /* privilege */, PrivPosition> positionByPrivilege = new HashMap<String, PrivPosition>();

    private final Map<String/* privilege */, Boolean/* active */> privileges = new PrivilegesMap();

    class PrivilegesMap extends AbstractMap<String, Boolean> implements Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public Set<Entry<String, Boolean>> entrySet() {
            LinkedHashSet<Entry<String, Boolean>> entrySet = new LinkedHashSet<Entry<String, Boolean>>();
            Set<Entry<String, PrivPosition>> wrapped = positionByPrivilege.entrySet();
            for (final Entry<String, PrivPosition> wrappedEntry : wrapped) {
                entrySet.add(new StaticPermissionModifyingEntry(wrappedEntry));
            }
            return entrySet;
        }

        @Override
        public Boolean put(String privilege, Boolean value) {
            // to be used by JSF when applying request values from checkBoxes of the privileges table
            PrivPosition privPosition = positionByPrivilege.get(privilege);
            Boolean oldValue = privPosition.isStatic;
            privPosition.setStatic(value);
            return oldValue;
        }

        @Override
        public Boolean remove(Object privilege) {
            throw new RuntimeException("Don't touch it! Use positionByPrivilege to remove privileges! " + privilege + " " + UserPrivileges.this.toString());
        }
    }

    protected final Map<String/* privilege */, String/* explanation */> explanationByPrivilege = new HashMap<String, String>() {
        private static final long serialVersionUID = 1L;

        @Override
        public String get(Object privilege) {
            String explanation = getDynamicPrivReasons().get(privilege);
            PrivPosition privPosition = getOrCreatePrivPosition((String) privilege);
            if (privPosition.isStatic()) {
                if (staticMsg == null) {
                    staticMsg = MessageUtil.getMessage("manage_permissions_extraInfo_static");
                }
                explanation = staticMsg + (StringUtils.isBlank(explanation) ? "" : "; " + explanation);
            }
            if (privPosition.isInherited()) {
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
            PrivPosition privPosition = getOrCreatePrivPosition((String) privilege);
            return privPosition.isDynamic() || privPosition.isInherited();
        }
    };

    private PrivPosition getOrCreatePrivPosition(String privilege) {
        PrivPosition privPosition = positionByPrivilege.get(privilege);
        if (privPosition == null) {
            privPosition = new PrivPosition();
            positionByPrivilege.put(privilege, privPosition);
        }
        return privPosition;
    }

    public UserPrivileges(String authName, String userDisplayName) {
        this.authName = authName;
        this.userDisplayName = userDisplayName;
    }

    public void markBaseState() {
        staticPrivilegesBeforeChanges = Collections.unmodifiableSet(getStaticPrivileges());
    }

    public boolean isNew() {
        return staticPrivilegesBeforeChanges == null;
    }

    public void addPrivilegeDynamic(String privilege, String reason) {
        PrivPosition privPosition = getOrCreatePrivPosition(privilege);
        privPosition.setDynamic();

        Set<String> privReasons = dynamicPrivReasons.get(privilege);
        if (privReasons == null) {
            privReasons = new LinkedHashSet<String>();
            dynamicPrivReasons.put(privilege, privReasons);
        }
        if (privReasons.add(reason)) {
            dynamicPrivReasonsCached = null;
        }
    }

    public void addPrivilegeInherited(String privToAdd) {
        getOrCreatePrivPosition(privToAdd).setInherited(true);
    }

    public void addPrivilegeStatic(String privToAdd) {
        getOrCreatePrivPosition(privToAdd).setStatic(true);
    }

    public void addPrivilegesStatic(Collection<String> privsToAdd) {
        for (String privilege : privsToAdd) {
            addPrivilegeStatic(privilege);
        }
    }

    public void deletePrivilegesStatic(Collection<String> privsToDelete) {
        for (String privilege : privsToDelete) {
            getOrCreatePrivPosition(privilege).setStatic(false);
        }
    }

    public Set<String> getPrivilegesToAdd() {
        if (deleted) {
            return Collections.<String> emptySet();// don't add any privileges
        }
        Set<String> privilegesToAdd = getStaticPrivileges();
        if (staticPrivilegesBeforeChanges != null) { // null when authority was added after rendering dialog
            privilegesToAdd.removeAll(staticPrivilegesBeforeChanges);
        }
        if (!privilegesToAdd.isEmpty()) {
            // rule from spec:
            // if at least one privilege was manually added, then add statically all privileges that authority has at the moment
            // (in case some of those dynamic privileges shall be lost later)
            privilegesToAdd.addAll(filterDynamicPrivileges());
        }
        return privilegesToAdd;
    }

    public void makeInheritedPrivilegesAsStatic() {
        for (Entry<String, PrivPosition> entry : positionByPrivilege.entrySet()) {
            PrivPosition position = entry.getValue();
            if (position.isInherited() && !position.isStatic()) {
                position.setStatic(true);
            }
        }
    }

    /** @return set of static privileges (some of them may also be dynamic or inherited) */
    private Set<String> getStaticPrivileges() {
        return filterPrivileges(true, null, null);
    }

    private Set<String> filterDynamicPrivileges() {
        return filterPrivileges(null, true, null);
    }

    private Set<String> filterInheritedPrivileges() {
        return filterPrivileges(null, null, true);
    }

    private Set<String> filterPrivileges(Boolean isStatic, Boolean dynamic, Boolean inherited) {
        Set<String> filteredPrivs = new HashSet<String>();
        for (Entry<String, PrivPosition> entry : positionByPrivilege.entrySet()) {
            String privilege = entry.getKey();
            PrivPosition position = entry.getValue();
            if (position.matches(isStatic, dynamic, inherited)) {
                filteredPrivs.add(privilege);
            }
        }
        return filteredPrivs;
    }

    /**
     * @return privileges that user would have when {@link #getPrivilegesToDelete()} are removed and
     */
    public Set<String> getActivePrivileges() {
        Set<String> activePrivs = new HashSet<String>();
        for (Entry<String, PrivPosition> entry : positionByPrivilege.entrySet()) {
            String privilege = entry.getKey();
            PrivPosition position = entry.getValue();
            if (position.isActive()) {
                activePrivs.add(privilege);
            }
        }
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
        privilegesToDelete.removeAll(getActivePrivileges());
        return privilegesToDelete;
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
                dynamicPrivReasonsCached.put(priv, StringUtils.join(reasons, "; "));
            }
        }
        return dynamicPrivReasonsCached;
    }

    public Map<String, String> getExplanationByPrivilege() {
        return explanationByPrivilege;
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

    /** used by JSF to determine if checkBox should be readRnly */
    public boolean isRemovable() {
        return !readOnly && (groups.isEmpty() || groups.size() == 1 && groups.contains(GROUPLESS_GROUP)) && filterInheritedPrivileges().isEmpty();
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
        return authName + " [" + userDisplayName + "]";
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

    /**
     * Using this class You can tell if given permission is static and/or inherited and/or dynamic - smth like improved {@link AccessPermission#getPosition()}.
     * State of this class could be refactored into single byte using bit shifting:
     * static=1;
     * dynamic=2;
     * inherited=4;
     * ...but since I didn't have that much time on my last day of work it is just an idea for optimization
     * 
     * @author Ats Uiboupin
     */
    static class PrivPosition implements Serializable {
        private static final long serialVersionUID = 1L;

        private Boolean isStatic;
        private Boolean dynamic;
        private Boolean inherited;

        public boolean isStatic() {
            return isStatic != null && isStatic;
        }

        public boolean isDynamic() {
            return dynamic != null && dynamic;
        }

        public boolean isInherited() {
            return inherited != null && inherited;
        }

        public boolean isStatic(boolean pureStatic) {
            if (pureStatic) {
                return isStatic() && !isDynamic() && !isInherited();
            }
            return isStatic();
        }

        /**
         * don't allow removing dynamic privilege
         * set dynamic = true;
         */
        public void setDynamic() {
            dynamic = true;
        }

        public void setInherited(boolean inherited) {
            this.inherited = inherited;
        }

        public void reset() {
            isStatic = false;
            dynamic = false;
            inherited = false;
        }

        public void setStatic(boolean isStatic) {
            this.isStatic = isStatic;
        }

        public Boolean isActive() {
            return isStatic() || isDynamic() || isInherited();
        }

        public boolean matches(Boolean isSt, Boolean dyn, Boolean inh) {
            return (isSt == null || isSt == isStatic())
                    && (dyn == null || dyn == isDynamic())
                    && (inh == null || inh == isInherited());
        }

        @Override
        public String toString() {
            return "PrivPosition " + (isStatic() ? "static" : "") + (isDynamic() ? " dynamic" : " ") + (isInherited() ? " inherited" : " ");
        }
    }

    /**
     * {@link Map} entry that is used to show and modify static privilege granted to the authority
     * 
     * @author Ats Uiboupin
     */
    class StaticPermissionModifyingEntry implements Entry<String, Boolean>, Serializable {
        private static final long serialVersionUID = 1L;

        private final Entry<String, PrivPosition> wrappedEntry;

        public StaticPermissionModifyingEntry(Entry<String, PrivPosition> wrappedEntry) {
            this.wrappedEntry = wrappedEntry;
        }

        @Override
        public String getKey() {
            return wrappedEntry.getKey();
        }

        @Override
        public Boolean getValue() {
            return wrappedEntry.getValue().isActive();
        }

        @Override
        public Boolean setValue(Boolean value) {
            throw new RuntimeException("Don't touch it! Use put method of underlying map to add/remove static privilege");
            // FIXME PRIV2 - if noone sees this exception message during testing, then it can probably be removed(as JSF in its infinite wisdom uses put method)
            // Boolean oldValue = getValue();
            // wrappedEntry.getValue().setStatic(true);
            // return oldValue;
        }
    }

}
