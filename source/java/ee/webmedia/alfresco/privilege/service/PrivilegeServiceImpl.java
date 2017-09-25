package ee.webmedia.alfresco.privilege.service;

import static ee.webmedia.alfresco.common.search.DbSearchUtil.getQuestionMarks;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.repo.search.IndexerAndSearcher;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.common.search.DbSearchUtil;
import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.privilege.model.PrivMappings;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.privilege.model.UserPrivileges;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;

public class PrivilegeServiceImpl implements PrivilegeService {
    private static final String COLUMN_AUTHORITY = "authority";

    private static final String COLUMN_NODE_UUID = "node_uuid";

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(PrivilegeServiceImpl.class);

    public static final String GROUPLESS_GROUP = "<groupless>";

    private NodeService nodeService;
    private UserService userService;
    private GeneralService generalService;
    private AuthorityService authorityService;
    private IndexerAndSearcher indexerAndSearcher;
    private LogService logService;
    List<DynamicAuthority> dynamicAuthorities;
    private SimpleJdbcTemplate jdbcTemplate;

    private Set<String> defaultAdmins;

    private static final String HIERARCHY_QUERY_NAME = "node_inherit_hierarchy";
    // TODO: unify with getNodeRefWithSetViewPrivilege hierarchyQuery
    private static final String HIERARCHY_QUERY = "WITH RECURSIVE " + HIERARCHY_QUERY_NAME + " (id, store_id, uuid, child_inherits) AS ( " +
            "    (SELECT id, node.store_id, uuid, CASE WHEN node_permission.inherits IS NULL OR node_permission.inherits = TRUE THEN TRUE ELSE FALSE END " +
            "    FROM alf_node node " +
            "    LEFT JOIN delta_node_inheritspermissions node_permission on (node_permission.node_uuid = node.uuid) " +
            "    WHERE node.uuid = ? " +
            "    AND node.store_id = (SELECT id from alf_store WHERE protocol = ? AND identifier = ?) " +
            "    LIMIT 1) " +
            "     UNION ALL " +
            "    (SELECT parent.id, parent.store_id, parent.uuid, CASE WHEN node_permission.inherits IS NULL OR node_permission.inherits = TRUE THEN TRUE ELSE FALSE END  " +
            "    FROM node_inherit_hierarchy " +
            "    JOIN alf_child_assoc child_assoc ON (child_assoc.child_node_id = node_inherit_hierarchy.id) " +
            "    JOIN alf_node parent ON (parent.id = child_assoc.parent_node_id) " +
            "    LEFT JOIN delta_node_inheritspermissions node_permission on (node_permission.node_uuid = parent.uuid) " +
            "    WHERE child_inherits = TRUE " +
            "    LIMIT 1 ) " +
            ") ";

    @Override
    public boolean hasPermission(final NodeRef targetRef, String userName, final Privilege... permissions) {
        if (userService.isAdministrator()) {
            return true;
        }
        return hasPermission(targetRef, userName, true, permissions);
    }

    private boolean hasPermission(final NodeRef targetRef, String userName, boolean addContainingAuthorities, final Privilege... permissions) {
        List<Object> params = new ArrayList<Object>();
        String sql = getInheritHierarchyQuery(targetRef, params)
                + " SELECT count(1) FROM delta_node_permission permission "
                + " WHERE permission.node_uuid IN (SELECT uuid FROM " + HIERARCHY_QUERY_NAME + ") "
                + " AND " + getAuthorityCondition(userName, addContainingAuthorities, params)
                + " AND " + getPrivilegeCondition(permissions)
                + " LIMIT 1";
        Object[] paramArray = params.toArray();
        int count = jdbcTemplate.queryForInt(sql, paramArray);
        explainQuery(sql, false, paramArray);
        if (count > 0) {
            return true;
        }
        if (!addContainingAuthorities) {
            return false;
        }
        QName nodeType = nodeService.getType(targetRef);
        Set<Privilege> dynamicallyGranted = new HashSet<Privilege>();
        List<Privilege> permissionList = Arrays.asList(permissions);
        for (DynamicAuthority dynamicAuthority : dynamicAuthorities) {
            Set<Privilege> requiredFor = dynamicAuthority.getGrantedPrivileges();
            if (org.apache.commons.collections.CollectionUtils.containsAny(requiredFor, permissionList)
                    && dynamicAuthority.hasAuthority(targetRef, nodeType, userName, null)) {
                dynamicallyGranted.addAll(requiredFor);
                if (dynamicallyGranted.containsAll(permissionList)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getInheritHierarchyQuery(final NodeRef targetRef, List<Object> params) {
        params.add(targetRef.getId());
        StoreRef storeRef = targetRef.getStoreRef();
        params.add(storeRef.getProtocol());
        params.add(storeRef.getIdentifier());
        return HIERARCHY_QUERY;
    }

    private String getAuthorityCondition(String userName, boolean addContainingAuthorities, List<Object> params) {
        Set<String> allAuthorities = new HashSet<String>();
        if (addContainingAuthorities) {
            allAuthorities.addAll(authorityService.getContainingAuthorities(AuthorityType.GROUP, userName, false));
        }
        allAuthorities.add(userName);
        String sql = "authority IN (" + getQuestionMarks(allAuthorities.size()) + ") ";
        params.addAll(allAuthorities);
        return sql;
    }

    private String getPrivilegeCondition(final Privilege... permissions) {
        Assert.isTrue(permissions != null && permissions.length > 0);
        StringBuffer privilegeCondition = new StringBuffer();
        for (Privilege permission : permissions) {
            privilegeCondition.append((privilegeCondition.length() > 0 ? " AND " : " ") + permission.getDbFieldName() + " = TRUE ");
        }
        return privilegeCondition.toString();
    }

    @Override
    public boolean hasPermissionOnAuthority(NodeRef targetRef, String authority, Privilege... permissions) {
        return hasPermission(targetRef, authority, false, permissions);
    }

    @Override
    public PrivMappings getPrivMappings(NodeRef manageableRef, final Collection<Privilege> manageablePermissions) {

        final PrivMappings privMappings = new PrivMappings(manageableRef);// fillMembersByGroup(manageableRef);
        final Map<String/* userName */, UserPrivileges> privilegesByUsername = new HashMap<String, UserPrivileges>();
        final Map<String/* groupName */, UserPrivileges> privilegesByGroup = new HashMap<String, UserPrivileges>();

        { // regular admin group
            String adminGroup = UserService.AUTH_ADMINISTRATORS_GROUP;
            UserPrivileges adminPrivs = new UserPrivileges(adminGroup, authorityService.getAuthorityDisplayName(adminGroup));
            privilegesByGroup.put(adminGroup, adminPrivs);
            adminPrivs.setReadOnly(true);
            for (Privilege permission : manageablePermissions) {
                adminPrivs.addPrivilegeDynamic(permission, MessageUtil.getMessage("manage_permissions_extraInfo_adminGroupHasAllPermissions"));
            }
        }
        if (defaultAdmins == null) {
            defaultAdmins = BeanHelper.getAuthenticationService().getDefaultAdministratorUserNames();
        }

        final String manageableNodeId = manageableRef.getId();
        List<Object> parameters = new ArrayList<Object>();
        String sql = getInheritHierarchyQuery(manageableRef, parameters)
                + " SELECT * FROM delta_node_permission WHERE "
                + " node_uuid IN (SELECT uuid FROM " + HIERARCHY_QUERY_NAME + ")";

        jdbcTemplate.query(sql, new ParameterizedRowMapper<Object>() {

            @Override
            public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
                String nodeId = rs.getString(COLUMN_NODE_UUID);
                String authority = rs.getString(COLUMN_AUTHORITY);
                for (Privilege privilege : manageablePermissions) {
                    Boolean hasPermission = rs.getBoolean(privilege.getDbFieldName());
                    if (!hasPermission) {
                        continue;
                    }
                    boolean setDirectly = manageableNodeId.equals(nodeId);
                    if (StringUtils.startsWith(authority, AuthorityType.GROUP.getPrefixString())) {
                        if (!authorityService.authorityExists(authority)) {
                            continue;
                        }
                        UserPrivileges authPrivileges = privilegesByGroup.get(authority);
                        if (authPrivileges == null) {
                            authPrivileges = new UserPrivileges(authority, authorityService.getAuthorityDisplayName(authority));
                            privilegesByGroup.put(authority, authPrivileges);
                        }
                        addPrivilege(authPrivileges, privilege, setDirectly);
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
                        if (defaultAdmins.contains(authority)) {
                            authPrivileges.addPrivilegeDynamic(privilege, MessageUtil.getMessage("manage_permissions_extraInfo_defaultAdmin"));
                        } else {
                            addPrivilege(authPrivileges, privilege, setDirectly);
                        }
                    }
                }
                return null;
            }
        }, parameters.toArray());

        privMappings.setPrivilegesByUsername(privilegesByUsername);
        privMappings.setPrivilegesByGroup(privilegesByGroup);

        // add all manageable permissions to all default administrators
        for (String defaultAdmin : defaultAdmins) {
            UserPrivileges authPrivileges = privMappings.getOrCreateUserPrivilegesVO(defaultAdmin);
            authPrivileges.setReadOnly(true);
            for (Privilege permission : manageablePermissions) {
                authPrivileges.addPrivilegeDynamic(permission, MessageUtil.getMessage("manage_permissions_extraInfo_defaultAdmin"));
            }
        }
        return privMappings;
    }

    @Override
    public List<Permission> getAllSetPrivileges(NodeRef nodeRef) {
        final List<Permission> setPrivileges = new ArrayList<Permission>();

        final String manageableNodeId = nodeRef.getId();
        List<Object> parameters = new ArrayList<Object>();
        String sql = getInheritHierarchyQuery(nodeRef, parameters)
                + " SELECT * FROM delta_node_permission WHERE "
                + " node_uuid IN (SELECT uuid FROM " + HIERARCHY_QUERY_NAME + ")"
                + " ORDER BY authority";

        jdbcTemplate.query(sql, new ParameterizedRowMapper<Object>() {

            @Override
            public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
                String nodeId = rs.getString(COLUMN_NODE_UUID);
                String authority = rs.getString(COLUMN_AUTHORITY);
                for (Privilege privilege : Privilege.values()) {
                    Boolean hasPermission = rs.getBoolean(privilege.getDbFieldName());
                    if (!hasPermission) {
                        continue;
                    }
                    boolean setDirectly = manageableNodeId.equals(nodeId);
                    setPrivileges.add(new Permission(authority, setDirectly, privilege));

                }
                return null;
            }
        }, parameters.toArray());

        return setPrivileges;
    }
    
    @Override
    public Set<NodeRef> getAllNodesWithPermissions(final StoreRef storeRef) {
        final Set<NodeRef> nodes = new HashSet<NodeRef>();

        String sql = "SELECT node_uuid FROM delta_node_permission GROUP BY node_uuid";

        jdbcTemplate.query(sql, new ParameterizedRowMapper<Object>() {

            @Override
            public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
                String nodeId = rs.getString(COLUMN_NODE_UUID);
                NodeRef nodeRef = new NodeRef(storeRef, nodeId);
                if (nodeService.exists(nodeRef)) {
                	nodes.add(nodeRef);
                }
                return null;
            }
        });

        return nodes;
    }

    @Override
    public Set<Privilege> getAllCurrentUserPermissions(NodeRef nodeRef, QName type, Map<String, Object> properties) {
        final Set<Privilege> userPrivileges = new HashSet<Privilege>();
        if (userService.isAdministrator()) {
            userPrivileges.addAll(Arrays.asList(Privilege.values()));
            return userPrivileges;
        }
        List<Object> params = new ArrayList<Object>();
        List<String> privilegeAggregateClause = new ArrayList<String>(Privilege.values().length);
        for (Privilege privilege : Privilege.values()) {
            String privilegeDbColumn = privilege.getDbFieldName();
            privilegeAggregateClause.add("bool_or(" + privilegeDbColumn + ") as " + privilegeDbColumn);
        }
        String userName = AuthenticationUtil.getRunAsUser();
        String sql = getInheritHierarchyQuery(nodeRef, params)
                + " SELECT " + TextUtil.joinNonBlankStringsWithComma(privilegeAggregateClause) + " FROM delta_node_permission permission "
                + " WHERE permission.node_uuid IN (SELECT uuid FROM " + HIERARCHY_QUERY_NAME + ") "
                + " AND " + getAuthorityCondition(userName, true, params);

        Object[] paramArray = params.toArray();
        jdbcTemplate.query(sql, new ParameterizedRowMapper<Object>() {

            @Override
            public T mapRow(ResultSet rs, int rowNum) throws SQLException {
                for (Privilege privilege : Privilege.values()) {
                    if (rs.getBoolean(privilege.getDbFieldName())) {
                        userPrivileges.add(privilege);
                    }
                }
                return null;
            }
        }, paramArray);
        explainQuery(sql, true, paramArray);
        int allPrivilegesCount = Privilege.values().length;
        if (userPrivileges.size() < allPrivilegesCount) {
            QName nodeType = nodeService.getType(nodeRef);
            for (DynamicAuthority dynamicAuthority : dynamicAuthorities) {
                if (dynamicAuthority.hasAuthority(nodeRef, nodeType, userName, properties)) {
                    userPrivileges.addAll(dynamicAuthority.getGrantedPrivileges());
                    if (userPrivileges.size() >= allPrivilegesCount) {
                        break;
                    }
                }
            }
        }
        return userPrivileges;

    }

    @Override
    public List<String> getAuthoritiesWithPrivilege(NodeRef nodeRef, Privilege... privileges) {
        List<Object> parameters = new ArrayList<Object>();
        String sql = getInheritHierarchyQuery(nodeRef, parameters)
                + " SELECT DISTINCT authority FROM delta_node_permission WHERE "
                + " node_uuid IN (SELECT uuid FROM " + HIERARCHY_QUERY_NAME + ")"
                + " AND " + getPrivilegeCondition(privileges);
        Object[] paramArray = parameters.toArray();
        List<String> authorities = jdbcTemplate.query(sql, new ParameterizedRowMapper<String>() {

            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getString(COLUMN_AUTHORITY);
            }
        }, paramArray);
        explainQuery(sql, false, paramArray);
        return authorities;
    }

    @Override
    public List<String> getAuthoritiesWithDirectPrivilege(NodeRef nodeRef, Privilege... privileges) {
        String sql = " SELECT DISTINCT authority FROM delta_node_permission WHERE "
                + " node_uuid = ? "
                + " AND " + getPrivilegeCondition(privileges);
        String nodeId = nodeRef.getId();
        List<String> authorities = jdbcTemplate.query(sql, new ParameterizedRowMapper<String>() {

            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getString(COLUMN_AUTHORITY);
            }
        }, nodeId);
        explainQuery(sql, false, nodeId);
        return authorities;
    }

    private void addPrivilege(UserPrivileges authPrivileges, Privilege privilege, boolean setDirectly) {
        if (setDirectly) {
            authPrivileges.addPrivilegeStatic(privilege);
        } else {
            authPrivileges.addPrivilegeInherited(privilege);
        }
    }

    @Override
    public void updateIndexedPermissions(NodeRef nodeRef) {
        NodeRef seriesRef = generalService.getAncestorNodeRefWithType(nodeRef, SeriesModel.Types.SERIES, false, false);
        if (seriesRef != null && !Boolean.FALSE.equals(nodeService.getProperty(seriesRef, SeriesModel.Props.DOCUMENTS_VISIBLE_FOR_USERS_WITHOUT_ACCESS))) {
            // Optimization: no need to reindex documents, if series is public or is changed from hidden -> public
            return;
        }
        updateIndexedPermissionsImpl(nodeRef);
    }

    private void updateIndexedPermissionsImpl(NodeRef nodeRef) {
        QName type = nodeService.getType(nodeRef);
        if (DocumentCommonModel.Types.DOCUMENT.equals(type)) {
            indexerAndSearcher.getIndexer(nodeRef.getStoreRef()).updateNode(nodeRef);
            return;
        }

        for (QName assoc : getDocumentTreeNodeAssocs(type)) {
            for (ChildAssociationRef childAssoc : nodeService.getChildAssocs(nodeRef, assoc, assoc)) {
                NodeRef childRef = childAssoc.getChildRef();

                if (getInheritParentPermissions(childRef)) {
                    updateIndexedPermissionsImpl(childRef);
                }
            }
        }
    }

    private QName[] getDocumentTreeNodeAssocs(QName type) {
        if (SeriesModel.Types.SERIES.equals(type)) {
            return new QName[] { VolumeModel.Types.VOLUME, CaseFileModel.Types.CASE_FILE };
        } else if (VolumeModel.Types.VOLUME.equals(type)) {
            return new QName[] { CaseModel.Associations.CASE, DocumentCommonModel.Assocs.DOCUMENT };
        } else if (CaseModel.Associations.CASE.equals(type) || CaseFileModel.Types.CASE_FILE.equals(type)) {
            return new QName[] { DocumentCommonModel.Assocs.DOCUMENT };
        }
        return new QName[0];
    }

    @Override
    public void savePrivileges(NodeRef manageableRef, Map<String, UserPrivileges> privilegesByUsername, Map<String, UserPrivileges> privilegesByGroup, QName listenerCode) {
        Map<String, Pair<Set<Privilege>, Set<Privilege>>> privilegeActions = new HashMap<String, Pair<Set<Privilege>, Set<Privilege>>>();
        Set<String> authoritiesToRemove = new HashSet<String>();
        collectPrivileges(manageableRef, privilegesByUsername, false, privilegeActions, authoritiesToRemove);
        collectPrivileges(manageableRef, privilegesByGroup, true, privilegeActions, authoritiesToRemove);
        for (Map.Entry<String, Pair<Set<Privilege>, Set<Privilege>>> entry : privilegeActions.entrySet()) {
            String authority = entry.getKey();
            Pair<Set<Privilege>, Set<Privilege>> permissions = entry.getValue();
            setPermissions(manageableRef, authority, permissions.getFirst(), permissions.getSecond());
        }
        if (!authoritiesToRemove.isEmpty()) {
            removeAllPermissions(manageableRef, authoritiesToRemove.toArray(new String[authoritiesToRemove.size()]));
        }
    }

    private void collectPrivileges(NodeRef manageableRef, Map<String, UserPrivileges> privilegesByAuthority, boolean group,
            Map<String, Pair<Set<Privilege>, Set<Privilege>>> privilegeActions, Set<String> authoritiesToRemove) {
        for (Iterator<Entry<String, UserPrivileges>> it = privilegesByAuthority.entrySet().iterator(); it.hasNext();) {
            Entry<String, UserPrivileges> entry = it.next();
            String authority = entry.getKey();
            UserPrivileges vo = entry.getValue();

            Set<Privilege> privilegesToDelete = vo.getPrivilegesToDelete();
            Set<Privilege> privilegesToAdd = null;

            if (!vo.isDeleted() && !privilegesToDelete.isEmpty()) {
                logMemberPrivRem(manageableRef, authority, group, privilegesToDelete);
            }

            if (vo.isDeleted()) {
                it.remove();
                logMemberRemove(manageableRef, authority, group);
                authoritiesToRemove.add(authority);
            } else {
                Set<Privilege> voPrivilegesToAdd = vo.getPrivilegesToAdd();

                if (vo.isNew()) {
                    logMemberAdd(manageableRef, authority, group);
                } else if (!voPrivilegesToAdd.isEmpty()) {
                    logMemberPrivAdd(manageableRef, authority, group, voPrivilegesToAdd);
                }
                privilegesToAdd = PrivilegeUtil.getPrivsWithDependencies(voPrivilegesToAdd);
                privilegeActions.put(authority, Pair.newInstance(privilegesToAdd, privilegesToDelete));
            }
        }
    }

    @Override
    public boolean getInheritParentPermissions(NodeRef manageableRef) {
        String sql = "SELECT inherits FROM delta_node_inheritspermissions WHERE node_uuid = ?";
        List<Boolean> inherits = jdbcTemplate.query(sql, new ParameterizedRowMapper<Boolean>() {

            @Override
            public Boolean mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getBoolean("inherits");
            }
        }, manageableRef.getId());
        return (inherits.isEmpty() || inherits.contains(Boolean.TRUE));
    }

    @Override
    public void setInheritParentPermissions(NodeRef manageableRef, boolean inherits) {
        Object[] params = new Object[] { manageableRef.getId(), inherits };
        String sql = "WITH new_values (node_uuid, inherits, acl_id) AS (values (?, ?::boolean, -1)),"
                + "     upsert AS (UPDATE delta_node_inheritspermissions SET inherits = nv.inherits "
                + "                 FROM new_values nv "
                + "                 WHERE delta_node_inheritspermissions.node_uuid = nv.node_uuid"
                + "                 RETURNING delta_node_inheritspermissions.node_uuid ) "
                + " INSERT INTO delta_node_inheritspermissions (node_uuid, inherits, acl_id) SELECT * FROM new_values WHERE NOT EXISTS (SELECT 1 FROM upsert WHERE upsert.node_uuid = new_values.node_uuid)";
        jdbcTemplate.update(sql, params);
        explainQuery(sql, false, params);
    }

    @Override
    public void removeAllPermissions(NodeRef manageableRef, String... authorities) {
        Assert.notNull(manageableRef, "removeAllPermissions() called without manageableRef");
        Assert.isTrue(authorities != null && authorities.length > 0, "removeAllPermissions() called without authority");
        String sql = "DELETE FROM delta_node_permission WHERE node_uuid = ? AND authority IN (" + DbSearchUtil.getQuestionMarks(authorities.length) + ")";
        List<Object> params = new ArrayList<Object>();
        params.add(manageableRef.getId());
        params.addAll(Arrays.asList(authorities));
        Object[] paramArray = params.toArray();
        jdbcTemplate.update(sql, paramArray);
        explainQuery(sql, false, paramArray);
    }

    @Override
    public void removeNodePermissionData(NodeRef manageableRef) {
        Assert.notNull(manageableRef, "removePermissions() called manageableRef");
        String sql = "DELETE FROM delta_node_permission WHERE node_uuid = ? ";
        String nodeRefId = manageableRef.getId();
        jdbcTemplate.update(sql, nodeRefId);
        explainQuery(sql, false, nodeRefId);

        sql = "DELETE FROM delta_node_inheritspermissions WHERE node_uuid = ? ";
        jdbcTemplate.update(sql, nodeRefId);
        explainQuery(sql, false, nodeRefId);
    }

    @Override
    public void removeAuthorityPermissions(String userName) {
        Assert.notNull(userName, "Username cannot be null");
        String sql = "DELETE FROM delta_node_permission WHERE authority = ? ";
        jdbcTemplate.update(sql, userName);
        explainQuery(sql, false, userName);
    }

    @Override
    public void setPermissions(NodeRef manageableRef, String authority, Privilege... privilegesToAdd) {
        if (privilegesToAdd == null || privilegesToAdd.length == 0) {
            throw new IllegalArgumentException("setPermissions() called without any privilegesToAdd");
        }
        setPermissions(manageableRef, authority, new HashSet<Privilege>(Arrays.asList(privilegesToAdd)));
    }

    @Override
    public void setPermissions(NodeRef manageableRef, String authority, Set<Privilege> privilegesToAdd) {
        setPermissions(manageableRef, authority, privilegesToAdd, null);
    }

    private void setPermissions(NodeRef manageableRef, String authority, Set<Privilege> privilegesToAdd, Set<Privilege> privilegesToRemove) {
        List<String> authorities = new ArrayList<String>(1);
        authorities.add(authority);
        Assert.notNull(manageableRef, "setPermissions() called manageableRef");
        Assert.notNull(authority, "setPermissions() called without authority");
        List<Privilege> privilegesToAddList = new ArrayList<Privilege>(PrivilegeUtil.getPrivsWithDependencies(privilegesToAdd));

        if (privilegesToAddList.isEmpty() && (privilegesToRemove == null || privilegesToRemove.isEmpty())) {
            return;
        }

        Assert.isTrue(
                privilegesToAdd == null || privilegesToRemove == null
                        || org.apache.commons.collections.CollectionUtils.intersection(privilegesToAddList, privilegesToRemove).size() == 0,
                "Cannot add and remove privileges at same time: adding " + privilegesToAddList + ", removing: " + privilegesToRemove);

        StringBuffer permissionColumns = new StringBuffer();
        StringBuffer permissionSetters = new StringBuffer();
        boolean isFirst = true;
        int privilegesToRemoveSize = 0;
        if (privilegesToRemove != null) {
            privilegesToRemoveSize = privilegesToRemove.size();
            for (Privilege privilegeToRemove : privilegesToRemove) {
                String privilegeColumnName = privilegeToRemove.getDbFieldName();
                permissionColumns.append(", " + privilegeColumnName);
                if (!isFirst) {
                    permissionSetters.append(", ");
                }
                isFirst = false;
                permissionSetters.append(privilegeColumnName + " = FALSE ");
            }
        }

        for (Privilege permission : privilegesToAddList) {
            String privilegeColumnName = permission.getDbFieldName();
            permissionColumns.append(", " + privilegeColumnName);
            if (!isFirst) {
                permissionSetters.append(", ");
            }
            isFirst = false;
            permissionSetters.append(privilegeColumnName + " = TRUE ");
        }

        List<String> typeParameters = Collections.nCopies(privilegesToAddList.size() + privilegesToRemoveSize, "?::boolean");
        String sql = "WITH new_values (node_uuid, authority" + permissionColumns + ") as (values  (?, ?, " + TextUtil.joinNonBlankStringsWithComma(typeParameters) + "))," +
                "upsert as (update delta_node_permission list "
                + " SET " + permissionSetters
                + " FROM new_values nv"
                + " WHERE list.node_uuid = nv.node_uuid "
                + " AND list.authority = nv.authority "
                + "   RETURNING list.* "
                + ")"
                + " INSERT INTO delta_node_permission (node_uuid, authority" + permissionColumns + ")" +
                "   SELECT * " +
                "   FROM new_values" +
                "   WHERE NOT EXISTS (SELECT 1" +
                "       FROM upsert up" +
                "       WHERE up.node_uuid = new_values.node_uuid"
                + "     AND up.authority = new_values.authority)";

        List<List<Boolean>> authorityPrivileges = new ArrayList<List<Boolean>>(1);
        List<Boolean> authPriv = new ArrayList<Boolean>();
        authorityPrivileges.add(authPriv);
        if (privilegesToRemove != null) {
            authPriv.addAll(Collections.nCopies(privilegesToRemoveSize, Boolean.FALSE));
        }
        authPriv.addAll(Collections.nCopies(privilegesToAddList.size(), Boolean.TRUE));
        jdbcTemplate.getJdbcOperations().batchUpdate(sql, new PermissionUpdateOrCreateParamSetter(manageableRef.getId(), authorities, authorityPrivileges));
    }

    @Override
    public void copyPermissions(NodeRef sourceNodeRef, NodeRef destinationNodeRef) {
        setInheritParentPermissions(destinationNodeRef, getInheritParentPermissions(sourceNodeRef));

        List<String> privilegeColumns = new ArrayList<String>();
        for (Privilege privilege : Privilege.values()) {
            privilegeColumns.add(privilege.getDbFieldName());
        }
        String permissionFields = TextUtil.joinNonBlankStringsWithComma(privilegeColumns);

        String sql = " INSERT INTO delta_node_permission (node_uuid, authority, " + permissionFields + ")"
                + " SELECT ?, authority, " + permissionFields + " FROM delta_node_permission "
                + " WHERE node_uuid = ? ";

        String sourceNodeId = sourceNodeRef.getId();
        String targetNodeId = destinationNodeRef.getId();

        jdbcTemplate.update(sql, targetNodeId, sourceNodeId);
        explainQuery(sql, false, targetNodeId, sourceNodeId);

    }

    private class PermissionUpdateOrCreateParamSetter implements BatchPreparedStatementSetter {

        private final String nodeId;
        private final List<String> authorities;
        private final List<List<Boolean>> auhtorityPermissions;

        public PermissionUpdateOrCreateParamSetter(String nodeId, List<String> authorities, List<List<Boolean>> list) {
            this.nodeId = nodeId;
            this.authorities = authorities;
            auhtorityPermissions = list;
        }

        @Override
        public void setValues(PreparedStatement stmt, int i) throws SQLException {
            stmt.setString(1, nodeId);
            Iterator<String> authoritiesIterator = authorities.iterator();
            stmt.setString(2, authoritiesIterator.next());
            Iterator<List<Boolean>> permissionsIterator = auhtorityPermissions.iterator();
            List<Boolean> permissions = permissionsIterator.next();
            int k = 3;
            for (Boolean permission : permissions) {
                stmt.setBoolean(k++, permission);
            }
            authoritiesIterator.remove();
            permissionsIterator.remove();
        }

        @Override
        public int getBatchSize() {
            return authorities.size();
        }
    }

    @Override
    public void addDynamicAuthority(DynamicAuthority dynamicAuthority) {
        if (dynamicAuthorities == null) {
            dynamicAuthorities = new ArrayList<DynamicAuthority>();
        }
        dynamicAuthorities.add(dynamicAuthority);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Set<NodeRef> getNodeRefWithSetViewPrivilege(List<NodeRef> nodeRefsToCheck, List<String> authorities) {
        int nodeRefsSize = nodeRefsToCheck.size();
        List<Object> arguments = new ArrayList<>();
        String hierarchyQuery = "WITH RECURSIVE " + HIERARCHY_QUERY_NAME + " (id, store_id, uuid, base_uuid, base_store_id, child_inherits) AS ( " +
                "    (SELECT id, node.store_id, uuid, node.uuid as base_uuid, node.store_id as base_store_id, "
                + "         CASE WHEN node_permission.inherits IS NULL OR node_permission.inherits = TRUE THEN TRUE ELSE FALSE END " +
                "    FROM " + BeanHelper.getBulkLoadNodeService().getNodeTableConditionalJoin(nodeRefsToCheck, arguments) +
                "    LEFT JOIN delta_node_inheritspermissions node_permission on (node_permission.node_uuid = node.uuid) " +
                "    LIMIT " + nodeRefsSize + ") " +
                "     UNION ALL " +
                "    (SELECT parent.id, parent.store_id, parent.uuid, node_inherit_hierarchy.base_uuid as base_uuid, "
                + "         node_inherit_hierarchy.base_store_id as base_store_id, CASE WHEN node_permission.inherits IS NULL OR node_permission.inherits = TRUE THEN TRUE ELSE FALSE END  " +
                "    FROM node_inherit_hierarchy " +
                "    JOIN alf_child_assoc child_assoc ON (child_assoc.child_node_id = node_inherit_hierarchy.id) " +
                "    JOIN alf_node parent ON (parent.id = child_assoc.parent_node_id) " +
                "    LEFT JOIN delta_node_inheritspermissions node_permission on (node_permission.node_uuid = parent.uuid) " +
                "    WHERE child_inherits = TRUE " +
                "    LIMIT " + nodeRefsSize + " ) " +
                ") ";
        arguments.addAll(authorities);
        String sql = hierarchyQuery
                + " SELECT * FROM " + HIERARCHY_QUERY_NAME + " inheritance "
                + " join delta_node_permission permission on permission.node_uuid = inheritance.uuid "
                + " WHERE permission.node_uuid IN (SELECT uuid FROM " + HIERARCHY_QUERY_NAME + ") "
                + " AND authority IN (" + getQuestionMarks(authorities.size()) + ") "
                + " AND " + getPrivilegeCondition(Privilege.VIEW_DOCUMENT_META_DATA);
        final Set<NodeRef> allowedNodeRefs = new HashSet<>();
        final BulkLoadNodeService bulkLoadNodeService = BeanHelper.getBulkLoadNodeService();
        jdbcTemplate.query(sql, new ParameterizedRowMapper<Void>() {

            @Override
            public Void mapRow(ResultSet rs, int rowNum) throws SQLException {
                allowedNodeRefs.add(new NodeRef(bulkLoadNodeService.getStoreRefByDbId(rs.getLong("base_store_id")), rs.getString("base_uuid")));
                return null;
            }
        }, arguments.toArray());
                return allowedNodeRefs;
    }

    @Override
    public Map<String, List<String>> getCreateDocumentPrivileges(Set<String> nodeRefIds) {
        return getCreateTypePrivileges(nodeRefIds, Privilege.CREATE_DOCUMENT);
    }

    @Override
    public Map<String, List<String>> getCreateCaseFilePrivileges(Set<String> nodeRefIds) {
        return getCreateTypePrivileges(nodeRefIds, Privilege.CREATE_CASE_FILE);
    }

    private Map<String, List<String>> getCreateTypePrivileges(Set<String> nodeRefIds, Privilege privilege) {
        Assert.isTrue(nodeRefIds != null && !nodeRefIds.isEmpty());
        String query = "SELECT node_uuid, authority FROM delta_node_permission WHERE node_uuid IN ( " + DbSearchUtil.getQuestionMarks(nodeRefIds.size())
                + " ) AND " + privilege.getDbFieldName() + " = TRUE";
        Object[] ids = nodeRefIds.toArray();
        final Map<String, List<String>> privileges = new HashMap<String, List<String>>();
        jdbcTemplate.query(query, new ParameterizedRowMapper<Object>() {

            @Override
            public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
                String uuid = rs.getString(COLUMN_NODE_UUID);
                String authority = rs.getString(COLUMN_AUTHORITY);
                List<String> authorities = privileges.get(uuid);
                if (authorities == null) {
                    authorities = new ArrayList<String>();
                    privileges.put(uuid, authorities);
                }
                authorities.add(authority);
                return null;
            }

        }, ids);
        explainQuery(query, false, ids);
        return privileges;
    }

    private void explainQuery(String sqlQuery, boolean analyze, Object... args) {
        generalService.explainAnalyzeQuery(sqlQuery, LOG, analyze && BeanHelper.getApplicationService().isTest(), args);
    }

    private void log(NodeRef manageableRef, String messageCode, Object... params) {
        LogObject obj = LogObject.DOCUMENT;
        QName nodeType = nodeService.getType(manageableRef);
        if (SeriesModel.Types.SERIES.equals(nodeType)) {
            obj = LogObject.RIGHTS_SERIES;
        } else if (VolumeModel.Types.VOLUME.equals(nodeType) || CaseFileModel.Types.CASE_FILE.equals(nodeType)) {
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

    private void logMemberPrivAdd(NodeRef manageableRef, String authority, boolean group, Set<Privilege> privs) {
        if (group) {
            log(manageableRef, "applog_priv_add_group", getAuthorityName(authority, group), getPrivilegesDisplayNames(privs));
        } else {
            log(manageableRef, "applog_priv_add_user", getAuthorityName(authority, group), getPrivilegesDisplayNames(privs));
        }
    }

    private void logMemberPrivRem(NodeRef manageableRef, String authority, boolean group, Set<Privilege> privs) {
        if (group) {
            log(manageableRef, "applog_priv_rem_group", getAuthorityName(authority, group), getPrivilegesDisplayNames(privs));
        } else {
            log(manageableRef, "applog_priv_rem_user", getAuthorityName(authority, group), getPrivilegesDisplayNames(privs));
        }
    }

    private String getPrivilegesDisplayNames(Set<Privilege> privs) {
        Set<String> displayNames = new HashSet<String>(privs.size());
        for (Privilege priv : privs) {
            displayNames.add(MessageUtil.getMessage("permission_" + priv.getPrivilegeName()));
        }
        return StringUtils.join(displayNames, ", ");
    }

    // START: getters / setters
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setIndexerAndSearcher(IndexerAndSearcher indexerAndSearcher) {
        this.indexerAndSearcher = indexerAndSearcher;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setLogService(LogService logService) {
        this.logService = logService;
    }

    public SimpleJdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;

    }

}
