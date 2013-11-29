package ee.webmedia.alfresco.privilege.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.repo.search.IndexerAndSearcher;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.permissions.impl.AccessPermissionImpl;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.common.service.ApplicationService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.privilege.model.PrivMappings;
import ee.webmedia.alfresco.privilege.model.PrivilegeActionType;
import ee.webmedia.alfresco.privilege.model.PrivilegeModel;
import ee.webmedia.alfresco.privilege.model.UserPrivileges;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.CalendarUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * @author Ats Uiboupin
 */
public class PrivilegeServiceImpl implements PrivilegeService {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(PrivilegeServiceImpl.class);

    public static final String GROUPLESS_GROUP = "<groupless>";

    private PermissionService permissionService;
    private NodeService nodeService;
    private UserService userService;
    private GeneralService generalService;
    private AuthorityService authorityService;
    private IndexerAndSearcher indexerAndSearcher;
    private ApplicationService applicationService;
    private LogService logService;
    private boolean privilegeActionsEnabled;

    private boolean privilegeActionsPaused = false;

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
    public boolean hasPermissionOnAuthority(NodeRef targetRef, String authority, String... permissions) {
        Set<AccessPermission> accessPermissions = permissionService.getAllSetPermissions(targetRef);
        int matches = 0;
        for (String permission : permissions) {
            for (AccessPermission accessPermission : accessPermissions) {
                if (accessPermission.getAuthority().equals(authority) && accessPermission.getPermission().equals(permission)
                        && accessPermission.getAccessStatus() == AccessStatus.ALLOWED) {
                    matches++;
                    break;
                }
            }
        }
        return matches >= permissions.length;
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
    public boolean savePrivileges(NodeRef manageableRef, Map<String, UserPrivileges> privilegesByUsername, Map<String, UserPrivileges> privilegesByGroup, QName listenerCode) {
        List<Node> privilegeActions = new ArrayList<Node>();
        updatePrivileges(manageableRef, privilegesByUsername, false, privilegeActions);
        updatePrivileges(manageableRef, privilegesByGroup, true, privilegeActions);

        int childDocumentsCount = 0; // volumes count is not taken into account, we don't need to be that accurate
        QName manageableType = nodeService.getType(manageableRef);
        if (SeriesModel.Types.SERIES.equals(manageableType)) {
            Integer count = (Integer) nodeService.getProperty(manageableRef, SeriesModel.Props.CONTAINING_DOCS_COUNT);
            childDocumentsCount = count == null ? 0 : count;
        } else if (VolumeModel.Types.VOLUME.equals(manageableType) || CaseFileModel.Types.CASE_FILE.equals(manageableType)) {
            Integer count = (Integer) nodeService.getProperty(manageableRef, VolumeModel.Props.CONTAINING_DOCS_COUNT);
            childDocumentsCount = count == null ? 0 : count;
        }
        int permissionsCount = 0;
        for (Node privilegeAction : privilegeActions) {
            @SuppressWarnings("unchecked")
            List<String> permissions = (List<String>) privilegeAction.getProperties().get(PrivilegeModel.Props.PERMISSIONS);
            permissionsCount += permissions.size();
        }
        // 5768 was 64 sec, so 100 should be 1,1 sec
        if (childDocumentsCount * permissionsCount <= 100) {
            for (Node privilegeAction : privilegeActions) {
                doPrivilegeAction(privilegeAction, manageableRef);
            }
            return true;
        }
        for (Node privilegeAction : privilegeActions) {
            savePrivilegeAction(privilegeAction, manageableRef);
        }
        return false;
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

                if (permissionService.getInheritParentPermissions(childRef)) {
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

    private void updatePrivileges(NodeRef manageableRef, Map<String, UserPrivileges> privilegesByAuthority, boolean group, List<Node> privilegeActions) {
        for (Iterator<Entry<String, UserPrivileges>> it = privilegesByAuthority.entrySet().iterator(); it.hasNext();) {
            Entry<String, UserPrivileges> entry = it.next();
            String authority = entry.getKey();
            UserPrivileges vo = entry.getValue();

            Set<String> privilegesToDelete = vo.getPrivilegesToDelete();
            if (!privilegesToDelete.isEmpty()) {
                privilegeActions.add(createPrivilegeAction(PrivilegeActionType.REMOVE, authority, privilegesToDelete));
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
                    Set<String> permissionsWithDependencies = PrivilegeUtil.getPrivsWithDependencies(privilegesToAdd);
                    privilegeActions.add(createPrivilegeAction(PrivilegeActionType.ADD, authority, permissionsWithDependencies));
                }
            }
        }
    }

    private void deletePermissions(NodeRef manageableRef, String authority, Set<String> privilegesToDelete) {
        for (String permission : privilegesToDelete) {
            if (!hasPermissionOnAuthority(manageableRef, authority, permission)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Previously deleted permission " + permission + " from " + authority + " on " + manageableRef + " - nothing to do");
                }
                continue;
            }
            long startTime = 0L;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Deleting permission " + permission + " from " + authority + " on " + manageableRef);
                startTime = System.nanoTime();
            }
            permissionService.deletePermission(manageableRef, authority, permission);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Deleted permission " + permission + " from " + authority + " on " + manageableRef + " - took " + CalendarUtil.duration(startTime) + " ms");
            }
        }
    }

    @Override
    public void setPermissions(NodeRef manageableRef, String authority, String... privilegesToAdd) {
        if (privilegesToAdd == null || privilegesToAdd.length == 0) {
            throw new IllegalArgumentException("setPermissions() called without any privilegesToAdd");
        }
        setPermissions(manageableRef, authority, new HashSet<String>(Arrays.asList(privilegesToAdd)));
    }

    @Override
    public void setPermissions(NodeRef manageableRef, String authority, Set<String> privilegesToAdd) {
        Assert.notNull(manageableRef, "setPermissions() called manageableRef");
        Assert.notNull(authority, "setPermissions() called without authority");
        Set<String> permissionsWithDependencies = PrivilegeUtil.getPrivsWithDependencies(privilegesToAdd);
        for (String permission : permissionsWithDependencies) {
            if (hasPermissionOnAuthority(manageableRef, authority, permission)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Previously set permission " + permission + " to " + authority + " on " + manageableRef + " - nothing to do");
                }
                continue;
            }
            try {
                long startTime = 0L;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Setting permission " + permission + " to " + authority + " on " + manageableRef);
                    startTime = System.nanoTime();
                }
                permissionService.setPermission(manageableRef, authority, permission, true);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Set permission " + permission + " to " + authority + " on " + manageableRef + " - took " + CalendarUtil.duration(startTime) + " ms");
                }
            } catch (Exception e) {
                throw new RuntimeException("failed to set permission " + permission + " to authority " + authority + " on node " + manageableRef, e);
            }
        }
    }

    @Override
    public boolean isPrivilegeActionsEnabled() {
        return privilegeActionsEnabled;
    }

    @Override
    public boolean isPrivilegeActionsPaused() {
        return privilegeActionsPaused;
    }

    @Override
    public void setPrivilegeActionsPaused(boolean privilegeActionsPaused) {
        this.privilegeActionsPaused = privilegeActionsPaused;
    }

    @Override
    public void doPausePrivilegeActions() {
        while (privilegeActionsPaused) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Do nothing
            }
        }
    }

    private NodeRef getPrivilegeActionsSpaceRef() {
        return generalService.getNodeRef(PrivilegeModel.Repo.PRIVILEGE_ACTIONS_SPACE);
    }

    @Override
    public List<Node> getAllInQueuePrivilegeActions() {
        List<ChildAssociationRef> childAssocRefs = nodeService.getChildAssocs(getPrivilegeActionsSpaceRef());
        List<Node> privilegeActions = new ArrayList<Node>(childAssocRefs.size());
        for (ChildAssociationRef childAssocRef : childAssocRefs) {
            WmNode privilegeAction = getPrivilegeAction(childAssocRef.getChildRef());
            privilegeActions.add(privilegeAction);
        }
        return privilegeActions;
    }

    private WmNode getPrivilegeAction(NodeRef privilegeActionRef) {
        return generalService.fetchObjectNode(privilegeActionRef, PrivilegeModel.Types.PRIVILEGE_ACTION);
    }

    @Override
    public List<Node> getAllInQueuePrivilegeActions(NodeRef manageableRef) {
        List<AssociationRef> assocRefs = nodeService.getSourceAssocs(manageableRef, PrivilegeModel.Assocs.PRIVILEGE_ACTION_2_NODE);
        List<Node> privilegeActions = new ArrayList<Node>(assocRefs.size());
        for (AssociationRef assocRef : assocRefs) {
            WmNode privilegeAction = getPrivilegeAction(assocRef.getSourceRef());
            privilegeActions.add(privilegeAction);
        }
        return privilegeActions;
    }

    private Node createPrivilegeAction(PrivilegeActionType actionType, String authority, Set<String> permissions) {
        HashMap<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(PrivilegeModel.Props.PRIVILEGE_ACTION_TYPE, actionType.name());
        props.put(PrivilegeModel.Props.PERMISSIONS, new ArrayList<String>(permissions));
        props.put(PrivilegeModel.Props.AUTHORITY, authority);
        return generalService.createNewUnSaved(PrivilegeModel.Types.PRIVILEGE_ACTION, props);
    }

    private NodeRef savePrivilegeAction(Node privilegeAction, NodeRef manageableRef) {
        NodeRef privilegeActionRef = nodeService.createNode(getPrivilegeActionsSpaceRef(), PrivilegeModel.Assocs.PRIVILEGE_ACTION, PrivilegeModel.Assocs.PRIVILEGE_ACTION,
                privilegeAction.getType(),
                RepoUtil.toQNameProperties(privilegeAction.getProperties())).getChildRef();
        nodeService.createAssociation(privilegeActionRef, manageableRef, PrivilegeModel.Assocs.PRIVILEGE_ACTION_2_NODE);
        return privilegeActionRef;
    }

    @Override
    public void doPrivilegeAction(Node privilegeAction) {
        NodeRef manageableRef = null;
        List<AssociationRef> assocs = nodeService.getTargetAssocs(privilegeAction.getNodeRef(), PrivilegeModel.Assocs.PRIVILEGE_ACTION_2_NODE);
        if (!assocs.isEmpty()) {
            manageableRef = assocs.get(0).getTargetRef();
        }
        doPrivilegeAction(privilegeAction, manageableRef);
    }

    private void doPrivilegeAction(Node privilegeAction, NodeRef manageableRef) {
        NodeRef actionRef = privilegeAction.getNodeRef();
        LOG.debug("Processing privilege action " + actionRef);
        if (manageableRef != null) {
            String actionType = (String) privilegeAction.getProperties().get(PrivilegeModel.Props.PRIVILEGE_ACTION_TYPE);
            @SuppressWarnings("unchecked")
            List<String> permissions = (List<String>) privilegeAction.getProperties().get(PrivilegeModel.Props.PERMISSIONS);
            Set<String> permissionsSet = new HashSet<String>(permissions);
            String authority = (String) privilegeAction.getProperties().get(PrivilegeModel.Props.AUTHORITY);
            if (PrivilegeActionType.ADD.name().equals(actionType)) {
                setPermissions(manageableRef, authority, permissionsSet);
            } else if (PrivilegeActionType.REMOVE.name().equals(actionType)) {
                deletePermissions(manageableRef, authority, permissionsSet);
            }
        }
        if (RepoUtil.isSaved(privilegeAction)) {
            nodeService.deleteNode(privilegeAction.getNodeRef());
        }
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

    private void logMemberPrivAdd(NodeRef manageableRef, String authority, boolean group, Set<String> privs) {
        if (group) {
            log(manageableRef, "applog_priv_add_group", getAuthorityName(authority, group), getPrivilegesDisplayNames(privs));
        } else {
            log(manageableRef, "applog_priv_add_user", getAuthorityName(authority, group), getPrivilegesDisplayNames(privs));
        }
    }

    private void logMemberPrivRem(NodeRef manageableRef, String authority, boolean group, Set<String> privs) {
        if (group) {
            log(manageableRef, "applog_priv_rem_group", getAuthorityName(authority, group), getPrivilegesDisplayNames(privs));
        } else {
            log(manageableRef, "applog_priv_rem_user", getAuthorityName(authority, group), getPrivilegesDisplayNames(privs));
        }
    }

    private String getPrivilegesDisplayNames(Set<String> privs) {
        Set<String> displayNames = new HashSet<String>(privs.size());
        for (String priv : privs) {
            displayNames.add(MessageUtil.getMessage("permission_" + priv));
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

    public void setPrivilegeActionsEnabled(boolean privilegeActionsEnabled) {
        this.privilegeActionsEnabled = privilegeActionsEnabled;
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
        private final String adminGroup;

        ThoroughInheritanceChecker(NodeRef manageableRef, String adminGroup) {
            parentRef = nodeService.getPrimaryParent(manageableRef).getParentRef();
            this.adminGroup = adminGroup;
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
            if (origAccessPermission.getAuthorityType() == AuthorityType.ADMIN || userNamesInAdminsGroup.contains(authority) || defaultAdmins.contains(authority)
                    || authority.equals(adminGroup)) {
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
                // XXX FROM KAAREL: When document is saved under case file for the first time then case file ownerId is added to document rights with editDocument permissions.
                // Ats had inserted this verification here that now seems obsolete but is left as a comment for future wanderers...
                // MessageUtil.addErrorMessage(authority + " has permission " + permission + " on parentNode, however it is NOT INHERITED, but it is still somehow granted");
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
