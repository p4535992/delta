package ee.webmedia.alfresco.user.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentSearchService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ApplicationModel;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.configuration.ConfigurableService;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.NoSuchPersonException;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.bean.repository.MapNode;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.log.PropDiffHelper;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.report.model.ReportModel;
import ee.webmedia.alfresco.user.model.Authority;
import ee.webmedia.alfresco.user.model.UserModel;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.utils.UserUtil;

public class UserServiceImpl implements UserService {

    private AuthenticationService authenticationService;
    private AuthorityService authorityService;
    private GeneralService generalService;
    private NodeService nodeService;
    private DictionaryService dictionaryService;
    private SearchService searchService;
    private PersonService personService;
    private PermissionService permissionService;
    private OrganizationStructureService organizationStructureService;
    private ConfigurableService configurableService;
    private NamespaceService namespaceService;
    private LogService logService;
    private boolean groupsEditingAllowed;
    private Set<String> systematicGroups;

    @Override
    public NodeRef retrieveUsersPreferenceNodeRef(String userName) {
        return retrieveUserPreferencesNode(userName, true);
    }

    @Override
    public NodeRef getUsersPreferenceNodeRef(String userName) {
        return retrieveUserPreferencesNode(userName, false);
    }

    private NodeRef retrieveUserPreferencesNode(String userName, boolean createIfMissing) {
        if (userName == null) {
            userName = AuthenticationUtil.getRunAsUser();
        }

        NodeRef prefRef = null;
        NodeRef person = getPerson(userName);
        if (person == null) {
            return null;
        }
        if (createIfMissing && !nodeService.hasAspect(person, ApplicationModel.ASPECT_CONFIGURABLE)) {
            // create the configuration folder for this Person node
            configurableService.makeConfigurable(person);
        }

        // target of the assoc is the configurations folder ref
        NodeRef configRef = configurableService.getConfigurationFolder(person);
        if (configRef == null) {
            if (createIfMissing) {
                // tried to create the folder, but failed
                throw new IllegalStateException("Unable to find associated 'configurations' folder for node: " + person);
            } else {
                return null;
            }
        }

        String xpath = NamespaceService.APP_MODEL_PREFIX + ":" + "preferences";
        List<NodeRef> nodes = searchService.selectNodes(configRef, xpath, null, namespaceService, false);

        if (nodes.size() == 1) {
            prefRef = nodes.get(0);
        } else if (createIfMissing) {
            // create the preferences Node for this user
            ChildAssociationRef childRef = nodeService.createNode(configRef, ContentModel.ASSOC_CONTAINS, QName.createQName(
                    NamespaceService.APP_MODEL_1_0_URI, "preferences"), ContentModel.TYPE_CMOBJECT);
            prefRef = childRef.getChildRef();

        }
        return prefRef;
    }

    @Override
    public NodeRef retrieveUserReportsFolderRef(String username) {
        Assert.isTrue(StringUtils.isNotBlank(username));
        NodeRef personRef = getPerson(username);
        if (personRef == null) {
            return null;
        }
        if (!nodeService.hasAspect(personRef, ReportModel.Aspects.REPORTS_QUEUE_CONTAINER)) {
            nodeService.addAspect(personRef, ReportModel.Aspects.REPORTS_QUEUE_CONTAINER, null);
        }
        NodeRef reportsFolder = getReportsFolder(personRef);
        if (reportsFolder == null) {
            reportsFolder = nodeService.createNode(personRef, ReportModel.Assocs.REPORTS_QUEUE, ReportModel.Assocs.REPORTS_QUEUE, ReportModel.Types.REPORTS_QUEUE_ROOT)
                    .getChildRef();
        }
        return reportsFolder;
    }

    private NodeRef getReportsFolder(NodeRef userRef) {
        NodeRef reportsFolderRef = null;
        List<ChildAssociationRef> assocs = nodeService.getChildAssocs(userRef, RegexQNamePattern.MATCH_ALL, ReportModel.Types.REPORTS_QUEUE_ROOT);
        if (assocs != null && !assocs.isEmpty()) {
            reportsFolderRef = assocs.get(0).getChildRef();
        }
        return reportsFolderRef;
    }

    @Override
    public boolean isAdministrator() {
        return authorityService.hasAdminAuthority();
    }

    private boolean isAdministrator(String userName) {
        return ((userName != null) && authorityService.getAuthoritiesForUser(userName).contains(PermissionService.ADMINISTRATOR_AUTHORITY));
    }

    @Override
    public boolean isDocumentManager() {
        if (isAdministrator()) {
            return true;
        }

        return authorityService.getAuthorities().contains(getDocumentManagersGroup());
    }

    @Override
    public boolean isAccountant() {
        if (isAdministrator()) {
            return true;
        }

        return isInAccountantGroup();
    }

    @Override
    public boolean isInAccountantGroup() {
        return authorityService.getAuthorities().contains(getAccountantsGroup());
    }

    @Override
    public boolean isSupervisor() {
        if (isAdministrator()) {
            return true;
        }

        return isInSupervisionGroup();
    }

    @Override
    public boolean isInSupervisionGroup() {
        return authorityService.getAuthorities().contains(getSupervisionGroup());
    }

    @Override
    public boolean isDocumentManager(String userName) {
        if (isAdministrator(userName)) {
            return true;
        }
        return authorityService.getAuthoritiesForUser(userName).contains(getDocumentManagersGroup());
    }

    @Override
    public String getDocumentManagersGroup() {
        return authorityService.getName(AuthorityType.GROUP, DOCUMENT_MANAGERS_GROUP);
    }

    @Override
    public String getAccountantsGroup() {
        return authorityService.getName(AuthorityType.GROUP, ACCOUNTANTS_GROUP);
    }

    @Override
    public String getSupervisionGroup() {
        return authorityService.getName(AuthorityType.GROUP, SUPERVISION_GROUP);
    }

    private String getArchivistsGroup() {
        return authorityService.getName(AuthorityType.GROUP, ARCHIVIST_GROUP);
    }

    @Override
    public String getAdministratorsGroup() {
        return authorityService.getName(AuthorityType.GROUP, "ALFRESCO_ADMINISTRATORS");
    }

    @Override
    public void addUserToGroup(String group, String username) {
        addUserToGroup(group, getUser(username));
    }

    @Override
    public void addUserToGroup(String group, Node user) {
        authorityService.addAuthority(group, (String) user.getProperties().get(ContentModel.PROP_USERNAME));
        logUserGroupAction(group, user, "applog_group_user_add");
    }

    private void logUserGroupAction(String group, Node user, String logMessageKey) {
        String userFullInfo = UserUtil.getUserFullNameAndId(RepoUtil.toQNameProperties(user.getProperties()));
        String groupName = authorityService.getAuthorityDisplayName(group);
        logService.addLogEntry(LogEntry.create(LogObject.USER_GROUP, getUserService(), user.getNodeRef(), logMessageKey, groupName, userFullInfo));
    }

    @Override
    public void removeUserFromGroup(String group, String username) {
        removeUserFromGroup(group, getUser(username));
    }

    @Override
    public void removeUserFromGroup(String group, Node user) {
        authorityService.removeAuthority(group, (String) user.getProperties().get(ContentModel.PROP_USERNAME));
        logUserGroupAction(group, user, "applog_group_user_rem");
    }

    @Override
    public List<Node> searchUsers(String input, boolean returnAllUsers, int limit) {
        return searchUsers(input, returnAllUsers, null, limit);
    }

    @Override
    public List<Node> searchUsers(String input, boolean returnAllUsers, String group, int limit) {
        Set<QName> props = new HashSet<QName>(2);
        props.add(ContentModel.PROP_FIRSTNAME);
        props.add(ContentModel.PROP_LASTNAME);

        return searchUsersByProps(input, returnAllUsers, group, props, limit);
    }

    // XXX filtering by group is not optimal - it is done after searching/getting all users
    private List<Node> searchUsersByProps(String input, boolean returnAllUsers, String group, Set<QName> props, int limit) {
        List<NodeRef> nodeRefs = getDocumentSearchService().searchNodesByTypeAndProps(input, ContentModel.TYPE_PERSON, props, limit);
        if (nodeRefs == null) {
            if (returnAllUsers) {
                // XXX use alfresco services instead
                List<Node> users = getUsers(limit);
                filterByGroup(users, group);
                return users;
            }
            return Collections.emptyList();
        }

        List<Node> users = new ArrayList<Node>(nodeRefs.size());
        for (NodeRef nodeRef : nodeRefs) {
            if (!nodeService.exists(nodeRef)) {
                continue;
            }

            MapNode node = new MapNode(nodeRef);

            // Eagerly load node properties from repository
            node.getProperties();

            users.add(node);
        }

        filterByGroup(users, group);
        return users;
    }

    @Override
    public Node getUser(String userName) {
        NodeRef personRef = getPerson(userName);
        if (personRef == null) {
            return null;
        }
        return new Node(personRef);
    }

    @Override
    public List<Authority> getAuthorities(final NodeRef nodeRef, final String permission) {

        // We need to run this in elevated rights, so regular users could use PermissionListDialog
        return AuthenticationUtil.runAs(new RunAsWork<List<Authority>>() {
            @Override
            public List<Authority> doWork() throws Exception {
                List<Authority> authorities = new ArrayList<Authority>();

                Set<AccessPermission> permissions = permissionService.getAllSetPermissions(nodeRef);
                for (AccessPermission accessPermission : permissions) {

                    if (accessPermission.isSetDirectly() && accessPermission.getAccessStatus() == AccessStatus.ALLOWED && accessPermission.getPermission().equals(permission) &&
                            (accessPermission.getAuthorityType() == AuthorityType.USER || accessPermission.getAuthorityType() == AuthorityType.GROUP)) {

                        authorities.add(getAuthority(accessPermission.getAuthority(), accessPermission.getAuthorityType(), false));
                    }
                }
                return authorities;
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    @Override
    public Authority getAuthority(String authority) {
        return getAuthority(authority, false);
    }

    @Override
    public Authority getAuthorityOrNull(String authority) {
        return getAuthority(authority, true);
    }

    @Override
    public Map<QName, Serializable> getUserProperties(String userName) {
        return personService.getPersonProperties(userName);
    }

    @Override
    public NodeRef getPerson(String userName) {
        if (StringUtils.isBlank(userName)) {
            return null;
        }
        try {
            if (personService.personExists(userName)) {
                return personService.getPerson(userName);
            }
            return null;
        } catch (NoSuchPersonException e) {
            return null;
        }
    }

    @Override
    public Map<QName, Serializable> getCurrentUserProperties() {
        return getUserProperties(getCurrentUserName());
    }

    @Override
    public String getUserEmail(String userName) {
        NodeRef personRef = getPerson(userName);
        if (personRef == null) {
            return null;
        }
        return (String) nodeService.getProperty(personRef, ContentModel.PROP_EMAIL);
    }

    @Override
    public Integer getCurrentUsersStructUnitId() {
        Map<QName, Serializable> userProperties = getUserProperties(AuthenticationUtil.getRunAsUser());
        Serializable orgId = userProperties.get(ContentModel.PROP_ORGID);
        if (StringUtils.isBlank((String) orgId)) {
            return null;
        }
        return Integer.parseInt(orgId.toString());
    }

    @Override
    public Set<String> getUsernamesByStructUnit(List<Integer> structUnits) {
        if (structUnits == null || structUnits.isEmpty()) {
            return Collections.<String> emptySet();
        }
        String query = SearchUtil.generateTypeQuery(ContentModel.TYPE_PERSON);
        ResultSet resultSet = searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query);
        try {
            Set<String> users = new HashSet<String>();
            for (ResultSetRow resultSetRow : resultSet) {
                NodeRef personRef = resultSetRow.getNodeRef();
                if (!nodeService.exists(personRef)) {
                    continue;
                }
                String strStructUnit = (String) nodeService.getProperty(personRef, ContentModel.PROP_ORGID);
                Integer structUnit = null;
                if (StringUtils.isNotBlank(strStructUnit)) {
                    structUnit = Integer.valueOf(strStructUnit);
                    if (structUnits.contains(structUnit)) {
                        String userName = (String) nodeService.getProperty(personRef, ContentModel.PROP_USERNAME);
                        Assert.notNull(userName);
                        users.add(userName);
                    }
                }
            }
            return users;
        } finally {
            resultSet.close();
        }
    }

    @Override
    public String getUserFullName() {
        return getUserFullName(getCurrentUserName());
    }

    @Override
    public String getUserFullName(String userName) {
        Map<QName, Serializable> props = getUserProperties(userName);
        if (props == null) {
            return userName;
        }
        return UserUtil.getPersonFullName1(props);
    }

    @Override
    public String getUserFullNameWithUnitName(String userName) {
        Map<QName, Serializable> props = getUserProperties(userName);
        if (props == null) {
            return userName;
        }
        String unitId = (String) props.get(ContentModel.PROP_ORGID);
        String unitName = organizationStructureService.getOrganizationStructureName(unitId);
        return UserUtil.getPersonFullNameWithUnitName(props, unitName);
    }

    @Override
    public String getUserFullNameWithOrganizationPath(String userName) {
        Map<QName, Serializable> props = getUserProperties(userName);
        if (props == null) {
            return userName;
        }
        String organizationPath = UserUtil.getUserDisplayUnit(RepoUtil.toStringProperties(props));
        return UserUtil.getPersonFullNameWithUnitName(props, organizationPath);
    }

    @Override
    public String getUserFullNameAndId(String userName) {
        Map<QName, Serializable> props = getUserProperties(userName);
        if (props == null) {
            return userName;
        }
        return UserUtil.getUserFullNameAndId(props);
    }

    @Override
    public NodeRef getCurrentUser() {
        return getPerson(authenticationService.getCurrentUserName());
    }

    @Override
    public String getCurrentUserName() {
        return authenticationService.getCurrentUserName();
    }

    @Override
    public boolean isGroupsEditingAllowed() {
        return groupsEditingAllowed;
    }

    @Override
    public boolean isGroupDeleteAllowed(String group) {
        return isGroupsEditingAllowed() && !getSystematicGroups().contains(group);
    }

    @Override
    public Set<String> getSystematicGroups() {
        if (systematicGroups == null) {
            systematicGroups = new HashSet<String>(Arrays.asList(getAdministratorsGroup(), getDocumentManagersGroup(), getAccountantsGroup(), getSupervisionGroup(),
                    getArchivistsGroup()));
        }
        return systematicGroups;
    }

    @Override
    public boolean markUserLeaving(String leavingUserId, String replacementUserId, boolean isLeaving) {
        if (StringUtils.isBlank(leavingUserId) || StringUtils.isBlank(replacementUserId)) {
            return false;
        }
        if (!personService.personExists(leavingUserId) || !personService.personExists(replacementUserId)) {
            return false;
        }
        Node leavingUser = getUser(leavingUserId);
        if (leavingUser == null) {
            return false;
        }

        if (isLeaving) {
            Map<QName, Serializable> properties = new HashMap<QName, Serializable>(2);
            properties.put(UserModel.Props.LEAVING_DATE_TIME, new Date());
            properties.put(UserModel.Props.LIABILITY_GIVEN_TO_PERSON_ID, replacementUserId);
            nodeService.addAspect(leavingUser.getNodeRef(), UserModel.Aspects.LEAVING, properties);
            logService.addLogEntry(LogEntry.create(LogObject.USER, this, leavingUser.getNodeRef(), "applog_user_rights_transfer",
                    getUserFullNameAndId(leavingUserId), getUserFullNameAndId(replacementUserId)));
        } else {
            nodeService.removeAspect(leavingUser.getNodeRef(), UserModel.Aspects.LEAVING);
            logService.addLogEntry(LogEntry.create(LogObject.USER, this, leavingUser.getNodeRef(), "applog_user_rights_return",
                    getUserFullNameAndId(leavingUserId)));
        }
        personService.removeFromPersonPropertiesCache(leavingUserId);
        return true;
    }

    @Override
    public void updateUser(Node user) {
        // Remove protected properties
        Map<QName, Serializable> props = RepoUtil.toQNameProperties(user.getProperties());
        props.remove(ContentModel.PROP_SIZE_CURRENT);
        props.remove(ContentModel.PROP_SIZE_QUOTA);

        String diff = new PropDiffHelper()
                .watchUser()
                .diff(RepoUtil.getPropertiesIgnoringSystem(nodeService.getProperties(user.getNodeRef()), dictionaryService), props);
        if (diff != null) {
            logService.addLogEntry(LogEntry.create(LogObject.USER, this, user.getNodeRef(), "applog_user_edit", UserUtil.getUserFullNameAndId(props), diff));
        }

        // Update user node
        personService.setPersonProperties((String) props.get(ContentModel.PROP_USERNAME), props);
    }

    @Override
    public Set<String> getUserNamesInGroup(String group) {
        return authorityService.getContainedAuthorities(AuthorityType.USER, group, true);
    }

    @Override
    public Set<String> getUsersGroups(String userName) {
        Set<String> authoritiesForUser = authorityService.getAuthoritiesForUser(userName);
        Set<String> groupNames = new HashSet<String>(authoritiesForUser.size());
        for (String authority : authoritiesForUser) {
            if (authority.startsWith(PermissionService.GROUP_PREFIX)) {
                groupNames.add(authority);
            }
        }
        return groupNames;
    }

    private Authority getAuthority(String authority, boolean returnNull) {
        AuthorityType authorityType = AuthorityType.getAuthorityType(authority);
        return getAuthority(authority, authorityType, returnNull);
    }

    private Authority getAuthority(String authority, AuthorityType authorityType, boolean returnNull) {
        if (authorityType == AuthorityType.USER) {
            NodeRef person = getPerson(authority);
            String name = authority;
            if (person != null) {
                name = getUserFullNameWithUnitName(authority);
            } else if (returnNull) {
                return null;
            }
            return new Authority(authority, false, name);
        } else if (authorityType == AuthorityType.GROUP) {
            String name = authorityService.getAuthorityDisplayName(authority);
            return new Authority(authority, true, name);
        } else {
            throw new RuntimeException("Authority type must be USER or GROUP: " + authorityType);
        }
    }

    private void filterByGroup(List<Node> users, String group) {
        if (group == null) {
            return;
        }
        Set<String> auths = getUserNamesInGroup(authorityService.getName(AuthorityType.GROUP, "SIGNERS"));
        for (Iterator<Node> i = users.iterator(); i.hasNext();) {
            Node user = i.next();
            Object userName = user.getProperties().get(ContentModel.PROP_USERNAME);
            if (!auths.contains(userName)) {
                i.remove();
            }
        }
    }

    private List<Node> getUsers(int limit) {
        List<Node> personNodes = null;

        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(personService.getPeopleContainer());
        personNodes = new ArrayList<Node>(childRefs.size());
        for (ChildAssociationRef ref : childRefs) {
            // create our Node representation from the NodeRef
            NodeRef nodeRef = ref.getChildRef();
            if (nodeService.getType(nodeRef).equals(ContentModel.TYPE_PERSON)) {
                // create our Node representation
                MapNode node = new MapNode(nodeRef);

                // Load eagerly
                Map<String, Object> props = node.getProperties();
                String lastName = (String) props.get("lastName");
                props.put("fullName", ((String) props.get("firstName")) + ' ' + (lastName != null ? lastName : ""));
                NodeRef homeFolderNodeRef = (NodeRef) props.get("homeFolder");
                if (homeFolderNodeRef != null) {
                    props.put("homeSpace", homeFolderNodeRef);
                }

                personNodes.add(node);
            }
            if (limit > -1 && personNodes.size() == limit) {
                break;
            }
        }
        return personNodes;
    }

    @Override
    public Set<String> getAllUsersUsernames() {
        Set<String> usernames = null;
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(personService.getPeopleContainer());
        usernames = new HashSet<String>(childRefs.size());
        for (ChildAssociationRef ref : childRefs) {
            NodeRef nodeRef = ref.getChildRef();
            if (nodeService.getType(nodeRef).equals(ContentModel.TYPE_PERSON)) {
                usernames.add((String) nodeService.getProperty(nodeRef, ContentModel.PROP_USERNAME));
            }
        }
        return usernames;
    }

    // START: setters/getters

    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }

    public void setOrganizationStructureService(OrganizationStructureService organizationStructureService) {
        this.organizationStructureService = organizationStructureService;
    }

    public void setConfigurableService(ConfigurableService configurableService) {
        this.configurableService = configurableService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setLogService(LogService logService) {
        this.logService = logService;
    }

    public void setGroupsEditingAllowed(boolean groupsEditingAllowed) {
        this.groupsEditingAllowed = groupsEditingAllowed;
    }

    // END: setters/getters

}
