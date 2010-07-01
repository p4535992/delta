package ee.webmedia.alfresco.user.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.alfresco.model.ApplicationModel;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.configuration.ConfigurableService;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
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
import org.alfresco.util.SearchLanguageConversion;
import org.alfresco.web.bean.repository.MapNode;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.user.model.Authority;
import ee.webmedia.alfresco.utils.UserUtil;

public class UserServiceImpl implements UserService {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(UserServiceImpl.class);

    private AuthenticationService authenticationService;
    private AuthorityService authorityService;
    private GeneralService generalService;
    private NodeService nodeService;
    private SearchService searchService;
    private PersonService personService;
    private PermissionService permissionService;
    private OrganizationStructureService organizationStructureService;
    private ConfigurableService configurableService;
    private NamespaceService namespaceService;

    @Override
    public NodeRef getUsersPreferenceNodeRef(String userName) {
        if(userName == null) {
            userName = AuthenticationUtil.getRunAsUser();
        }

        NodeRef prefRef = null;
        NodeRef person = personService.getPerson(userName);
        if (nodeService.hasAspect(person, ApplicationModel.ASPECT_CONFIGURABLE) == false) {
            // create the configuration folder for this Person node
            configurableService.makeConfigurable(person);
        }

        // target of the assoc is the configurations folder ref
        NodeRef configRef = configurableService.getConfigurationFolder(person);
        if (configRef == null) {
            throw new IllegalStateException("Unable to find associated 'configurations' folder for node: "
                    + person);
        }

        String xpath = NamespaceService.APP_MODEL_PREFIX + ":" + "preferences";
        List<NodeRef> nodes = searchService.selectNodes(configRef, xpath, null, namespaceService, false);

        if (nodes.size() == 1) {
            prefRef = nodes.get(0);
        } else {
            // create the preferences Node for this user
            ChildAssociationRef childRef = nodeService.createNode(configRef, ContentModel.ASSOC_CONTAINS, QName.createQName(
                    NamespaceService.APP_MODEL_1_0_URI, "preferences"), ContentModel.TYPE_CMOBJECT);
            prefRef = childRef.getChildRef();

        }
        return prefRef;
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
    public String getAdministratorsGroup() {
        return authorityService.getName(AuthorityType.GROUP, "ALFRESCO_ADMINISTRATORS");
    }

    @Override
    public List<Node> searchUsers(String input, boolean returnAllUsers) {
        return searchUsers(input, returnAllUsers, null);
    }

    @Override
    // XXX filtering by group is not optimal - it is done after searching/getting all users
    public List<Node> searchUsers(String input, boolean returnAllUsers, String group) {
        Set<QName> props = new HashSet<QName>(1);
        props.add(ContentModel.PROP_FIRSTNAME);
        props.add(ContentModel.PROP_LASTNAME);

        List<NodeRef> nodeRefs = generalService.searchNodes(input, ContentModel.TYPE_PERSON, props);
        if (nodeRefs == null) {
            if (returnAllUsers) {
                // XXX use alfresco services instead
                List<Node> users = getUsers(FacesContext.getCurrentInstance(), nodeService, searchService);
                filterByGroup(users, group);
                return users;
            }
            return Collections.emptyList();
        }

        List<Node> users = new ArrayList<Node>(nodeRefs.size());
        for (NodeRef nodeRef : nodeRefs) {
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
        return new Node(personService.getPerson(userName));
    }

    @Override
    public List<Authority> searchGroups(String input, boolean returnAllGroups) {
        input = StringUtils.trimToEmpty(input);
        Set<String> results;
        if (input.length() == 0) {
            if (!returnAllGroups) {
                return Collections.emptyList();
            }
            results = authorityService.getAllAuthorities(AuthorityType.GROUP);
        } else {
            String query = "*" + SearchLanguageConversion.escapeForLucene(input) + "*";
            log.debug("Query in Lucene format: " + query);
            results = authorityService.findAuthoritiesByShortName(AuthorityType.GROUP, query);
        }
        List<Authority> authorities = new ArrayList<Authority>(results.size());
        for (String result : results) {
            if (!getAdministratorsGroup().equals(result) && !getDocumentManagersGroup().equals(result)) {
                authorities.add(getAuthority(result, AuthorityType.GROUP, false));
            }
        }
        return authorities;
    }

    @Override
    public List<Authority> getAuthorities(NodeRef nodeRef, String permission) {
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
        try {
            if(personService.personExists(userName)) {
                NodeRef person = personService.getPerson(userName);
                return nodeService.getProperties(person);
            } else {
                //FIXME: workaround, et transaktsiooni Rollbakc-only'ks ei märgitaks
                return null;
            }
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
        return nodeService.getProperty(personService.getPerson(userName), ContentModel.PROP_EMAIL).toString();
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
        String unitName = organizationStructureService.getOrganizationStructure(unitId);
        return UserUtil.getPersonFullNameWithUnitName(props, unitName);
    }
    
    @Override
    public NodeRef getCurrentUser() {
        return personService.getPerson(authenticationService.getCurrentUserName());
    }
    
    @Override
    public String getCurrentUserName() {
        return authenticationService.getCurrentUserName();
    }

    private Authority getAuthority(String authority, boolean returnNull) {
        AuthorityType authorityType = AuthorityType.getAuthorityType(authority);
        return getAuthority(authority, authorityType, returnNull);
    }

    private Authority getAuthority(String authority, AuthorityType authorityType, boolean returnNull) {
        if (authorityType == AuthorityType.USER) {
            NodeRef person = personService.getPerson(authority);
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
        Set<String> auths = authorityService.getContainedAuthorities(AuthorityType.USER, authorityService.getName(AuthorityType.GROUP, "SIGNERS"), true);
        for (Iterator<Node> i = users.iterator(); i.hasNext(); ) {
            Node user = i.next();
            Object userName = user.getProperties().get(ContentModel.PROP_USERNAME);
            if (!auths.contains(userName)) {
                i.remove();
            }
        }
    }

    private List<Node> getUsers(FacesContext context, NodeService nodeService, SearchService searchService) {
        List<Node> personNodes = null;

        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(personService.getPeopleContainer());
        personNodes = new ArrayList<Node>(childRefs.size());
        for (ChildAssociationRef ref: childRefs)
        {
            // create our Node representation from the NodeRef
            NodeRef nodeRef = ref.getChildRef();
            if (nodeService.getType(nodeRef).equals(ContentModel.TYPE_PERSON))
            {
                // create our Node representation
                MapNode node = new MapNode(nodeRef);

                // Load eagerly
                Map<String, Object> props = node.getProperties(); 
                String lastName = (String)props.get("lastName");
                props.put("fullName", ((String)props.get("firstName")) + ' ' + (lastName != null ? lastName : ""));
                NodeRef homeFolderNodeRef = (NodeRef)props.get("homeFolder");
                if (homeFolderNodeRef != null)
                {
                    props.put("homeSpace", homeFolderNodeRef);
                }

                personNodes.add(node);
            }
        }
        return personNodes;
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

    // END: setters/getters

}