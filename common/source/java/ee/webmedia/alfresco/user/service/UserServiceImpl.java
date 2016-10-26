package ee.webmedia.alfresco.user.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAuthorityService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentSearchService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getOrganizationStructureService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowService;

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
import org.alfresco.repo.cache.SimpleCache;
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
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.NoSuchPersonException;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import ee.webmedia.alfresco.common.service.ApplicationConstantsBean;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.log.PropDiffHelper;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.report.model.ReportModel;
import ee.webmedia.alfresco.user.model.Authority;
import ee.webmedia.alfresco.user.model.UserModel;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.utils.UserUtil;

public class UserServiceImpl implements UserService {

    private AuthenticationService authenticationService;
    private GeneralService generalService;
    private NodeService nodeService;
    private DictionaryService dictionaryService;
    private SearchService searchService;
    private ParametersService parametersService;
    private PersonService personService;
    private OrganizationStructureService organizationStructureService;
    private ConfigurableService configurableService;
    private NamespaceService namespaceService;
    private LogService logService;
    private ApplicationConstantsBean applicationConstantsBean;
    private Set<String> systematicGroups;
    private SimpleCache<String, Authority> authorityCache;

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
            }
            return null;
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
            personService.updateCache(username);
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
    public NodeRef retrieveCurrentUserForNotification(QName aspectQName) {
        String currentUserName = getCurrentUserName();
        NodeRef currentUserRef = getPerson(currentUserName);
        if (!nodeService.getAspects(currentUserRef).contains(aspectQName)) {
            nodeService.addAspect(currentUserRef, aspectQName, null);
            personService.updateCache(currentUserName);
        }
        return currentUserRef;
    }

    @Override
    public boolean isCurrentStructUnitUser() {
        String taskOwnerStructUnit = parametersService.getStringParameter(Parameters.TASK_OWNER_STRUCT_UNIT);
        Set<String> authorities = getAuthorityService().getAuthorities();
        return StringUtils.isBlank(taskOwnerStructUnit) || isDocumentManager() || isSupervisor() || authorities.contains(getGroup(taskOwnerStructUnit))
                || authorities.contains(taskOwnerStructUnit);
    }

    @Override
    public boolean isAdministrator() {
        return AuthenticationUtil.isRunAsUserTheSystemUser() || getAuthorityService().hasAdminAuthority();
    }

    @Override
    public boolean isAdministrator(String userName) {
        return (StringUtils.isNotBlank(userName) && (AuthenticationUtil.SYSTEM_USER_NAME.equals(userName) || getAuthorityService().getAuthoritiesForUser(userName).contains(
                PermissionService.ADMINISTRATOR_AUTHORITY)));
    }

    @Override
    public boolean isDocumentManager() {
        if (isAdministrator()) {
            return true;
        }

        return getAuthorityService().getAuthorities().contains(getDocumentManagersGroup());
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
        return getAuthorityService().getAuthorities().contains(getAccountantsGroup());
    }

    @Override
    public boolean isArchivist() {
        return isAdministrator() || getAuthorityService().getAuthorities().contains(getArchivistsGroup());
    }
    
    @Override
    public boolean isGuest() {
    	Set<String> groups = getAuthorityService().getAuthorities();
    	boolean isGuest  = false;
    	if (groups.contains(getGuestsGroup()) && !groups.contains(getAdministratorsGroup()) && !groups.contains(getArchivistsGroup()) 
    			&& !groups.contains(getAccountantsGroup()) && !groups.contains(getDocumentManagersGroup()) 
    			&& !groups.contains(getSupervisionGroup())) {
    		isGuest = true;
    	}
        return isGuest;
    }
    
    @Override
    public boolean isGuest(String username) {
    	Set<String> groups = getAuthorityService().getAuthoritiesForUser(username);
    	boolean isGuest  = false;
    	if (groups.contains(getGuestsGroup()) && !groups.contains(getAdministratorsGroup()) && !groups.contains(getArchivistsGroup()) 
    			&& !groups.contains(getAccountantsGroup()) && !groups.contains(getDocumentManagersGroup()) 
    			&& !groups.contains(getSupervisionGroup())) {
    		isGuest = true;
    	}
        return isGuest;
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
        return getAuthorityService().getAuthorities().contains(getSupervisionGroup());
    }

    @Override
    public boolean isDocumentManager(String userName) {
        if (isAdministrator(userName)) {
            return true;
        }
        return getAuthorityService().getAuthoritiesForUser(userName).contains(getDocumentManagersGroup());
    }

    @Override
    public String getDocumentManagersGroup() {
        return getGroup(DOCUMENT_MANAGERS_GROUP);
    }

    @Override
    public String getAccountantsGroup() {
        return getGroup(ACCOUNTANTS_GROUP);
    }

    @Override
    public String getSupervisionGroup() {
        return getGroup(SUPERVISION_GROUP);
    }

    private String getArchivistsGroup() {
        return getGroup(ARCHIVIST_GROUP);
    }

    @Override
    public String getAdministratorsGroup() {
        return getGroup(ADMINISTRATORS_GROUP);
    }
    
    @Override
    public String getGuestsGroup() {
        return getGroup(GUESTS_GROUP);
    }

    @Override
    public void addUserToGroup(String group, String username) {
        addUserToGroup(group, getUser(username));
    }

    @Override
    public void addUserToGroup(String group, Node user) {
    	String username = (String) user.getProperties().get(ContentModel.PROP_USERNAME);
        getAuthorityService().addAuthority(group, username);
        logUserGroupAction(group, user, "applog_group_user_add");
        String groupDisplayName = getAuthorityService().getAuthorityDisplayName(group);
        getWorkflowService().addUserToCompoundWorkflowDefinitions(groupDisplayName, username);
    }

    private void logUserGroupAction(String group, Node user, String logMessageKey) {
        String userFullInfo = UserUtil.getUserFullNameAndId(RepoUtil.toQNameProperties(user.getProperties()));
        String groupName = getAuthorityService().getAuthorityDisplayName(group);
        logService.addLogEntry(LogEntry.create(LogObject.USER_GROUP, getUserService(), user.getNodeRef(), logMessageKey, groupName, userFullInfo));
    }

    @Override
    public void removeUserFromGroup(String group, String username) {
        removeUserFromGroup(group, getUser(username));
    }

    @Override
    public void removeUserFromGroup(String group, Node user) {
    	String username = (String) user.getProperties().get(ContentModel.PROP_USERNAME);
    	String groupDisplayName = getAuthorityService().getAuthorityDisplayName(group);
        getAuthorityService().removeAuthority(group, username);
        getWorkflowService().removeUserOrGroupFromCompoundWorkflowDefinitions(groupDisplayName, username);
        logUserGroupAction(group, user, "applog_group_user_rem");
    }

    private String getGroup(String name) {
        return getAuthorityService().getName(AuthorityType.GROUP, name);
    }

    @Override
    public List<Node> searchUsers(String input, boolean returnAllUsers, int limit) {
        return searchUsers(input, returnAllUsers, null, limit);
    }

    @Override
    public List<Node> searchUsers(String input, boolean returnAllUsers, String group, int limit) {
        return searchUsers(input, returnAllUsers, group, limit, null);
    }

    @Override
    public List<Pair<String, String>> searchUserNamesAndIdsWithoutCurrentUser(String param, int limit) {
        return searchUserNamesAndIds(param, false, limit);
    }

    @Override
    public List<Pair<String, String>> searchUserNamesAndIds(String param, int limit) {
        return searchUserNamesAndIds(param, true, limit);
    }

    private List<Pair<String, String>> searchUserNamesAndIds(String param, boolean withCurrenUser, int limit) {
        List<Pair<String, String>> results = new ArrayList<>();
        List<Node> nodes = searchUsers(param, false, null, limit, null, false);
        String currentUser = withCurrenUser ? null : AuthenticationUtil.getRunAsUser();
        for (Node node : nodes) {
            Map<String, Object> props = node.getProperties();
            String userName = (String) props.get(ContentModel.PROP_USERNAME);
            if ((!withCurrenUser && StringUtils.equals(userName, currentUser)) || node.hasAspect(UserModel.Aspects.LEAVING)) {
                continue;
            }
            String firstName = (String) props.get(ContentModel.PROP_FIRSTNAME);
            String lastName = (String) props.get(ContentModel.PROP_LASTNAME);
            String name = UserUtil.getPersonFullName(firstName, lastName);
            results.add(Pair.newInstance(name, userName));
        }
        return results;
    }

    @Override
    public List<Node> searchUsers(String input, boolean returnAllUsers, String group, int limit, String exactGroup) {
        return searchUsers(input, returnAllUsers, group, limit, exactGroup, true);
    }

    private List<Node> searchUsers(String input, boolean returnAllUsers, String group, int limit, String exactGroup, boolean withJobTitle) {
        Set<QName> props = new HashSet<QName>(3);
        props.add(ContentModel.PROP_FIRSTNAME);
        props.add(ContentModel.PROP_LASTNAME);
        if (withJobTitle) {
            props.add(ContentModel.PROP_JOBTITLE);
        }
        return searchUsersByProps(input, returnAllUsers, group, props, limit, exactGroup);
    }

    private Set<String> limitSearchParameters(int limit, Set<String> original) {
        if (limit > -1 && original != null && original.size() > limit) {
            return ImmutableSet.copyOf(Iterables.limit(original, limit));
        }
        return original;
    }

    private List<Node> searchUsersByProps(String input, boolean returnAllUsers, String group, Set<QName> props, int limit, String exactGroup) {
        List<String> groupNames = null;
        List<String> queryAndAdditions = new ArrayList<>();
        Set<String> userNamesInGroup = new HashSet<>();
        boolean isUsersInGroupSearch = StringUtils.isNotBlank(group);
        if (isUsersInGroupSearch) {
            userNamesInGroup.addAll(getUserNamesInGroup(group));
        }
        if (StringUtils.isNotBlank(exactGroup)) {
            groupNames = BeanHelper.getDocumentSearchService().searchAuthorityGroupsByExactName(exactGroup);
            userNamesInGroup.retainAll(getUserNamesInGroup(groupNames));
        }
        if (!userNamesInGroup.isEmpty()) {
            Set<String> userNames = limitSearchParameters(limit, userNamesInGroup);
            queryAndAdditions.add(SearchUtil.generatePropertyExactQuery(ContentModel.PROP_USERNAME, userNames));
        }
        if (userNamesInGroup.isEmpty() && isUsersInGroupSearch) {
            return Collections.emptyList();
        }
        List<String> userNames = getDocumentSearchService().searchUserNamesByTypeAndProps(input, ContentModel.TYPE_PERSON, props, limit,
                SearchUtil.joinQueryPartsAnd(queryAndAdditions));
        if (userNames == null) {
            if (returnAllUsers) {
                List<Node> users = personService.getPersonNodeList(limit);
                filterByGroup(users, groupNames);
                return users;
            }
            return Collections.emptyList();
        }

        List<Node> users = new ArrayList<Node>(userNames.size());
        for (String userName : userNames) {
            Node personNode = personService.getPersonNode(userName);
            if (personNode == null) {
                continue;
            }
            users.add(personNode);
        }
        return users;
    }

    @Override
    public List<Node> getPersonsList() {
        return personService.getPersonNodeList(-1);
    }

    @Override
    public Node getUser(String userName) {
        return personService.getPersonNode(userName);
    }

    @Override
    public List<Authority> getAuthorities(final NodeRef nodeRef, final Privilege privilege) {

        // We need to run this in elevated rights, so regular users could use PermissionListDialog
        return AuthenticationUtil.runAs(new RunAsWork<List<Authority>>() {
            @Override
            public List<Authority> doWork() throws Exception {
                List<Authority> authorities = new ArrayList<Authority>();
                List<String> authorityNames = BeanHelper.getPrivilegeService().getAuthoritiesWithDirectPrivilege(nodeRef, privilege);
                if (authorityNames != null) {
                    for (String authorityName : authorityNames) {
                        authorities.add(getAuthority(authorityName, false));
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
        return (String) personService.getPersonProperty(userName, ContentModel.PROP_EMAIL);
    }

    @Override
    public String getDefaultTelephoneForSigning(String userName) {
        return (String) personService.getPersonProperty(userName, ContentModel.DEFAULT_TELEPHONE_FOR_SIGNING);
    }

    public Map<QName, Serializable> getPersonProperties(String userName) {
        if (StringUtils.isBlank(userName)) {
            return null;
        }
        try {
            if (personService.personExists(userName)) {
                return getUserProperties(userName);
            }
            return null;
        } catch (NoSuchPersonException e) {
            return null;
        }
    }

    @Override
    public String getCurrentUsersStructUnitId() {
        Map<QName, Serializable> userProperties = getUserProperties(AuthenticationUtil.getRunAsUser());
        String orgId = (String) userProperties.get(ContentModel.PROP_ORGID);
        return orgId;
    }

    @Override
    public Set<String> getUsernamesByStructUnit(List<String> structUnits) {
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
                String structUnit = (String) nodeService.getProperty(personRef, ContentModel.PROP_ORGID);
                if (StringUtils.isNotBlank(structUnit) && structUnits.contains(structUnit)) {
                    String userName = (String) nodeService.getProperty(personRef, ContentModel.PROP_USERNAME);
                    Assert.notNull(userName);
                    users.add(userName);
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
        if (StringUtils.isBlank(userName)) {
            return userName;
        }
        userName = StringUtils.substringBefore(userName, "_");
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
    public List<String> getUserOrgPathOrOrgName(Map<QName, Serializable> props) {
        @SuppressWarnings("unchecked")
        List<String> orgPaths = (List<String>) props.get(ContentModel.PROP_ORGANIZATION_PATH);
        if (orgPaths != null && !orgPaths.isEmpty()) {
            return orgPaths;
        }
        String orgId = (String) props.get(ContentModel.PROP_ORGID);
        if (StringUtils.isBlank(orgId)) {
            return new ArrayList<String>();
        }
        List<String> orgPath = getOrganizationStructureService().getOrganizationStructurePaths(orgId);
        if (orgPath == null || orgPath.isEmpty()) {
            orgPath = Collections.singletonList(getOrganizationStructureService().getOrganizationStructureName(orgId));
        }
        return orgPath;
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
    public void setCurrentUserProperty(QName property, Serializable value) {
        String userName = getCurrentUserName();
        personService.setPersonProperty(userName, property, value);
    }

    @Override
    public boolean isGroupDeleteAllowed(String group) {
        return applicationConstantsBean.isGroupsEditingAllowed() && !getSystematicGroups().contains(group);
    }

    @Override
    public Set<String> getSystematicGroups() {
        if (systematicGroups == null) {
            systematicGroups = new HashSet<String>(Arrays.asList(getAdministratorsGroup(), getDocumentManagersGroup(), getAccountantsGroup(), getSupervisionGroup(),
                    getArchivistsGroup(), getGuestsGroup()));
        }
        return systematicGroups;
    }

    @Override
    public boolean markUserLeaving(String leavingUserId, String replacementUserId, boolean isLeaving) {
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
        personService.updateCache(leavingUserId);

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
    public Set<String> getUserNamesInGroup(String groupName) {
        return getUserNamesInGroup(Arrays.asList(groupName));
    }

    @Override
    public Set<String> getUserNamesInGroup(List<String> groupNames) {
        Set<String> usersInGroup = new HashSet<String>();
        for (String groupName : groupNames) {
            if (getAuthorityService().authorityExists(groupName)) {
                usersInGroup.addAll(getAuthorityService().getContainedAuthorities(AuthorityType.USER, groupName, true));
            }
        }
        return usersInGroup;
    }

    @Override
    public Set<String> getUsersGroups(String userName) {
        if (userName == null) {
            return new HashSet<String>(0);
        }

        Set<String> authoritiesForUser = getAuthorityService().getAuthoritiesForUser(userName);
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
        Authority auth = authorityCache.get(authority);
        if (auth != null) {
            return auth;
        }
        if (authorityType == AuthorityType.USER) {
            NodeRef person = getPerson(authority);
            String name = authority;
            if (person != null) {
                name = getUserFullNameWithUnitName(authority);
            } else if (returnNull) {
                return null;
            }
            auth = new Authority(authority, false, name);
            authorityCache.put(authority, auth);
            return auth;
        } else if (authorityType == AuthorityType.GROUP) {
            String name = getAuthorityService().getAuthorityDisplayName(authority);
            auth = new Authority(authority, true, name);
            authorityCache.put(authority, auth);
            return auth;
        } else {
            throw new RuntimeException("Authority type must be USER or GROUP: " + authorityType);
        }
    }

    private void filterByGroup(List<Node> users, List<String> groupNames) {
        if (groupNames == null) {
            return;
        }
        Set<String> auths = getUserNamesInGroup(groupNames);
        for (Iterator<Node> i = users.iterator(); i.hasNext();) {
            Node user = i.next();
            Object userName = user.getProperties().get(ContentModel.PROP_USERNAME);
            if (!auths.contains(userName)) {
                i.remove();
            }
        }
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

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    public void setAuthorityCache(SimpleCache<String, Authority> authorityCache) {
        this.authorityCache = authorityCache;
    }

    public void setApplicationConstantsBean(ApplicationConstantsBean applicationConstantsBean) {
        this.applicationConstantsBean = applicationConstantsBean;
    }

    // END: setters/getters

}
