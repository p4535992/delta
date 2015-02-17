package ee.webmedia.alfresco.orgstructure.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentSearchService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPersonService;
import static java.util.Arrays.asList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.sync.NodeDescription;
import org.alfresco.repo.security.sync.UserRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.service.ApplicationConstantsBean;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.orgstructure.model.OrganizationStructure;
import ee.webmedia.alfresco.orgstructure.model.OrganizationStructureModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;

public class OrganizationStructureServiceImpl implements OrganizationStructureService {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(OrganizationStructureServiceImpl.class);

    @SuppressWarnings("unchecked")
    private static Comparator<OrganizationStructure> nameComparator = new NullComparator(new OrganizationStructure.NameComparator());
    private static BeanPropertyMapper<OrganizationStructure> organizationStructureBeanPropertyMapper = BeanPropertyMapper
            .newInstance(OrganizationStructure.class);

    private GeneralService generalService;
    private NodeService nodeService;
    private UserRegistry userRegistry;
    private AuthorityService authorityService;
    private ApplicationConstantsBean applicationConstantsBean;
    // START: properties that would cause dependency cycle when trying to inject them
    private UserService _userService;
    // END: properties that would cause dependency cycle when trying to inject them

    /** a transactionally-safe cache to be injected */
    private SimpleCache<String, OrganizationStructure> orgStructPropertiesCache;

    private NodeRef orgStructsRoot;

    @Override
    public int updateOrganisationStructures() {
        Iterator<NodeDescription> orgStructs = userRegistry.getOrganizationStructures();
        // save old organization structures, that will be removed if everything goes right
        List<ChildAssociationRef> oldOrganizations = nodeService.getChildAssocs(getOrgStructsRoot(), OrganizationStructureModel.Assocs.ORGSTRUCT, RegexQNamePattern.MATCH_ALL);
        int orgStructuresCount = 0;
        while (orgStructs.hasNext()) {
            NodeDescription orgStruct = orgStructs.next();
            String unitId = (String) orgStruct.getProperties().get(OrganizationStructureModel.Props.UNIT_ID);
            nodeService.createNode(getOrgStructsRoot(), OrganizationStructureModel.Assocs.ORGSTRUCT, //
                    QName.createQName(OrganizationStructureModel.URI, unitId), OrganizationStructureModel.Types.ORGSTRUCT, orgStruct.getProperties());
            orgStructuresCount++;
        }
        for (ChildAssociationRef oldOrganization : oldOrganizations) { // remove all old organizations
            orgStructPropertiesCache.remove((String) nodeService.getProperty(oldOrganization.getChildRef(), OrganizationStructureModel.Props.UNIT_ID));
            nodeService.deleteNode(oldOrganization.getChildRef());
        }
        return orgStructuresCount;
    }

    @Override
    public void updateOrganisationStructures(ActionEvent event) {
        updateOrganisationStructures();
    }

    @Override
    public int updateOrganisationStructureBasedGroups() {
        if (!applicationConstantsBean.isGroupsEditingAllowed()) {
            return 0; // System uses Active Directory
        }

        NodeRef zone = authorityService.getOrCreateZone(STRUCT_UNIT_BASED);
        String structUnitZoneName = (String) nodeService.getProperty(zone, ContentModel.PROP_NAME);
        Set<String> generatedGroups = new HashSet<String>(authorityService.getAllAuthoritiesInZone(structUnitZoneName, AuthorityType.GROUP));

        Set<NodeRef> allPeople = getPersonService().getAllPeople();
        List<Map<QName, Serializable>> users = new ArrayList<Map<QName, Serializable>>(allPeople.size());
        for (NodeRef personRef : allPeople) {
            users.add(nodeService.getProperties(personRef));
        }

        // Fetch all groups that user has added manually or are automatically generated by the application
        Set<String> defaultGroups = new HashSet<String>(authorityService.getAllAuthoritiesInZone(AuthorityService.ZONE_APP_DEFAULT, AuthorityType.GROUP));

        List<OrganizationStructure> allOrganizationStructures = getAllOrganizationStructures();
        Map<String, OrganizationStructure> orgStructById = new HashMap<String, OrganizationStructure>(allOrganizationStructures.size());
        for (OrganizationStructure organizationStructure : allOrganizationStructures) {
            orgStructById.put(organizationStructure.getUnitId(), organizationStructure);
        }

        for (OrganizationStructure os : allOrganizationStructures) {
            String organizationPath = os.getOrganizationDisplayPath();
            String groupName = StringUtils.isBlank(organizationPath) ? os.getName() : organizationPath;
            String groupAuthority = AuthorityType.GROUP.getPrefixString() + groupName;

            // User has manually created a group that is named after an organization structure
            boolean isGeneratedGroupAuthority = generatedGroups.contains(groupAuthority);
            if (defaultGroups.contains(groupAuthority) && !isGeneratedGroupAuthority) {
                // Mark this group as generated by adding it to the zone and to generated groups list
                authorityService.addAuthorityToZones(groupAuthority, new HashSet<String>(asList(structUnitZoneName)));
            }

            // Create the groups from AMR
            if (!isGeneratedGroupAuthority) {
                groupAuthority = authorityService.createAuthority(AuthorityType.GROUP, groupName, groupName,
                        new HashSet<String>(Arrays.asList(STRUCT_UNIT_BASED, AuthorityService.ZONE_AUTH_ALFRESCO, AuthorityService.ZONE_APP_DEFAULT)));
            }

            // Get current users for this group
            Set<String> orgStructGroupMembers = new HashSet<String>(authorityService.getContainedAuthorities(AuthorityType.USER, groupAuthority, true));

            // Update groups users
            OUTER: for (Map<QName, Serializable> props : users) {
                String username = (String) props.get(ContentModel.PROP_USERNAME);
                boolean isAlreadyGroupMember = orgStructGroupMembers.contains(username);

                // Users who have organization path
                @SuppressWarnings("unchecked")
                List<String> orgPath = (List<String>) props.get(ContentModel.PROP_ORGANIZATION_PATH);
                if (orgPath != null) {
                    for (String op : orgPath) {
                        if (StringUtils.equals(op, groupName)) {
                            if (!isAlreadyGroupMember) {
                                authorityService.addAuthority(groupAuthority, username);
                            }
                            orgStructGroupMembers.remove(username);
                            continue OUTER;
                        }
                    }
                }

                // Users by organization ID
                String orgId = (String) props.get(ContentModel.PROP_ORGID);
                if (StringUtils.isBlank(orgId)) {
                    continue;
                }
                OrganizationStructure orgStruct = orgStructById.get(orgId);
                if (orgStruct == null) {
                    continue;
                }
                if (StringUtils.equals(groupName, orgStruct.getName())) {
                    if (!isAlreadyGroupMember) {
                        authorityService.addAuthority(groupAuthority, username);
                    }
                    orgStructGroupMembers.remove(username);
                }

            }

            // Remove processed groups
            generatedGroups.remove(groupAuthority);

            // Remove users that have been removed from this organization structure
            for (String username : orgStructGroupMembers) {
                authorityService.removeAuthority(groupAuthority, username);
            }
        }

        // Remove missing organization structures
        for (String missingGeneratedGroup : generatedGroups) {
            authorityService.deleteAuthority(missingGeneratedGroup);

        }

        return 0;
    }

    @Override
    public void updateOrganisationStructureBasedGroups(ActionEvent event) {
        AuthenticationUtil.runAs(new RunAsWork<Integer>() {
            @Override
            public Integer doWork() throws Exception {
                return updateOrganisationStructureBasedGroups();
            }
        }, AuthenticationUtil.getSystemUserName());

    }

    @Override
    public void createOrganisationStructure(OrganizationStructure org) {
        Map<QName, Serializable> properties = organizationStructureBeanPropertyMapper.toProperties(org);
        nodeService.createNode(getOrgStructsRoot(), OrganizationStructureModel.Assocs.ORGSTRUCT, //
                QName.createQName(OrganizationStructureModel.URI, org.getUnitId()), OrganizationStructureModel.Types.ORGSTRUCT, properties);
        orgStructPropertiesCache.put(org.getUnitId(), org);
    }

    @Override
    public OrganizationStructure getOrganizationStructure(String unitId) {
        OrganizationStructure os = orgStructPropertiesCache.get(unitId);
        if (os == null && StringUtils.isNotBlank(unitId)) {
            List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(getOrgStructsRoot(),
                    OrganizationStructureModel.Assocs.ORGSTRUCT, QName.createQName(OrganizationStructureModel.URI, unitId));
            for (ChildAssociationRef childAssociationRef : childAssocs) {
                OrganizationStructure orgStruct = getOrganizationStructure(childAssociationRef.getChildRef());
                if (StringUtils.equals(unitId, orgStruct.getUnitId())) {
                    os = orgStruct;
                    orgStructPropertiesCache.put(unitId, os);
                    return os;
                }
            }
        }
        return os;
    }

    @Override
    public String getOrganizationStructureName(String value) {
        OrganizationStructure orgStruct = getOrganizationStructure(value);
        if (orgStruct == null) {
            return value;
        }
        return orgStruct.getName();
    }

    @Override
    public List<String> getOrganizationStructurePaths(String value) {
        OrganizationStructure orgStruct = getOrganizationStructure(value);
        if (orgStruct == null) {
            return null;
        }
        return orgStruct.getOrganizationPath();
    }

    @Override
    public List<OrganizationStructure> getAllOrganizationStructures() {
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(getOrgStructsRoot());
        List<OrganizationStructure> orgstructs = new ArrayList<OrganizationStructure>(childRefs.size());
        Map<String, String> superNames = new HashMap<String, String>(childRefs.size());
        for (ChildAssociationRef childAssocRef : childRefs) {
            NodeRef nodeRef = childAssocRef.getChildRef();
            OrganizationStructure os = getOrganizationStructure(nodeRef);
            orgstructs.add(os);
            superNames.put(os.getUnitId(), os.getName());
        }
        for (OrganizationStructure os : orgstructs) {
            if (superNames.containsKey(os.getSuperUnitId())) {
                os.setSuperValueName(superNames.get(os.getSuperUnitId()));
            } else {
                os.setSuperValueName("");
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("OrganizationStructures found: " + orgstructs);
        }
        return orgstructs;
    }

    @Override
    public List<NodeRef> getAllOrganizationStructureRefs() {
        NodeRef root = generalService.getNodeRef(OrganizationStructureModel.Repo.SPACE);
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(root);
        List<NodeRef> orgStructRefs = new ArrayList<NodeRef>(childRefs.size());
        for (ChildAssociationRef childAssoc : childRefs) {
            orgStructRefs.add(childAssoc.getChildRef());
        }
        return orgStructRefs;
    }

    @Override
    public List<OrganizationStructure> searchOrganizationStructures(String input, int limit) {
        Set<QName> props = new HashSet<QName>(1);
        props.add(OrganizationStructureModel.Props.NAME);
        props.add(OrganizationStructureModel.Props.ORGANIZATION_PATH);

        // why doesn't lucene sorting work? as a workaround we sort in java
        List<NodeRef> nodeRefs = getDocumentSearchService().searchNodesByTypeAndProps(input, OrganizationStructureModel.Types.ORGSTRUCT, props, limit);

        if (nodeRefs == null) {
            return sortByName(getAllOrganizationStructures());
        }

        List<OrganizationStructure> structs = new ArrayList<OrganizationStructure>(nodeRefs.size());
        for (NodeRef nodeRef : nodeRefs) {
            structs.add(getOrganizationStructure(nodeRef));
        }
        return sortByName(structs);
    }

    @Override
    public List<Node> setUsersUnit(List<Node> users) {
        for (Node user : users) {
            loadUserUnit(user);
        }
        return users;
    }

    @Override
    public void loadUserUnit(Node user) {
        Map<String, Object> props = user.getProperties();

        String unitId = (String) props.get(ContentModel.PROP_ORGID);
        String orgStruct;
        if (StringUtils.isBlank(unitId)) {
            unitId = "";
            orgStruct = "";
        } else {
            orgStruct = getOrganizationStructureName(unitId);
        }

        props.put(UNIT_PROP, unitId + (StringUtils.equals(unitId, orgStruct) ? "" : " " + orgStruct));
        props.put(UNIT_NAME_PROP, orgStruct);
    }

    // public String getOrganizationStructureByUser(Map<QName, Serializable> userProps) {
    // return getOrganizationStructure((String) userProps.get(ContentModel.PROP_ORGID));
    // }

    // START: private methods
    private List<OrganizationStructure> sortByName(List<OrganizationStructure> structs) {
        Collections.sort(structs, nameComparator);
        return structs;
    }

    @Override
    public OrganizationStructure getOrganizationStructure(NodeRef nodeRef) {
        OrganizationStructure os = organizationStructureBeanPropertyMapper.toObject(nodeService.getProperties(nodeRef));
        os.setNodeRef(nodeRef);
        return os;
    }

    private NodeRef getOrgStructsRoot() {
        if (orgStructsRoot == null) {
            String orgStructXPath = OrganizationStructureModel.Repo.SPACE;
            orgStructsRoot = generalService.getNodeRef(orgStructXPath);
        }
        return orgStructsRoot;
    }

    // END: private methods

    // START: getters / setters
    public UserService getUserService() {
        if (_userService == null) {
            _userService = BeanHelper.getUserService();
        }
        return _userService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setUserRegistry(UserRegistry userRegistry) {
        this.userRegistry = userRegistry;
    }

    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    public void setOrgStructPropertiesCache(SimpleCache<String, OrganizationStructure> orgStructPropertiesCache) {
        this.orgStructPropertiesCache = orgStructPropertiesCache;
    }

    public void setApplicationConstantsBean(ApplicationConstantsBean applicationConstantsBean) {
        this.applicationConstantsBean = applicationConstantsBean;
    }

    // END: getters / setters
}
