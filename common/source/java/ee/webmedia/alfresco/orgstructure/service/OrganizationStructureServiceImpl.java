package ee.webmedia.alfresco.orgstructure.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentSearchService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPersonService;
import static java.util.Arrays.asList;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.lang.StringUtils;

import smit.ametnik.services.YksusExt;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.orgstructure.amr.service.AMRService;
import ee.webmedia.alfresco.orgstructure.model.OrganizationStructure;
import ee.webmedia.alfresco.orgstructure.model.OrganizationStructureModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;

public class OrganizationStructureServiceImpl implements OrganizationStructureService {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(OrganizationStructureServiceImpl.class);

    @SuppressWarnings("unchecked")
    private static Comparator<OrganizationStructure> nameComparator = new NullComparator(new OrganizationStructure.NameComparator());
    private static BeanPropertyMapper<OrganizationStructure> organizationStructureBeanPropertyMapper = BeanPropertyMapper
            .newInstance(OrganizationStructure.class);

    private boolean organizationStructureUpdateEnabled;
    private GeneralService generalService;
    private NodeService nodeService;
    private AMRService amrService;
    private AuthorityService authorityService;
    // START: properties that would cause dependency cycle when trying to inject them
    private UserService _userService;
    // END: properties that would cause dependency cycle when trying to inject them

    /** a transactionally-safe cache to be injected */
    private SimpleCache<Integer, OrganizationStructure> orgStructPropertiesCache;

    private NodeRef orgStructsRoot;

    @Override
    public int updateOrganisationStructures() {
        if (!organizationStructureUpdateEnabled) {
        	log.debug("Organization structure update not enabled! Exiting...");
            return 0;
        }
        
        log.debug("Starting AMR getYksusByAsutusId() update process...");
        
        YksusExt[] yksusArray = amrService.getYksusByAsutusId();
        List<OrganizationStructure> orgStructures = new ArrayList<OrganizationStructure>(yksusArray.length);
        for (YksusExt yksus : yksusArray) {
            orgStructures.add(yksusToOrganizationStructure(yksus));
        }
        
        log.debug("Found " + orgStructures.size() + " organiztion structure groups for adding.");
        // save old organization structures, that will be removed if everything goes right
        List<ChildAssociationRef> oldOrganizations = nodeService.getChildAssocs(getOrgStructsRoot(), OrganizationStructureModel.Assocs.ORGSTRUCT, RegexQNamePattern.MATCH_ALL);
        
        log.debug("Found old " + oldOrganizations.size() + " organiztion structure groups for removing.");
        for (OrganizationStructure org : orgStructures) {
            createOrganisationStructure(org);
        }
        
        
        for (ChildAssociationRef oldOrganization : oldOrganizations) { // remove all old organizations
            orgStructPropertiesCache.remove((Integer) nodeService.getProperty(oldOrganization.getChildRef(), OrganizationStructureModel.Props.UNIT_ID));
            log.debug("Remove old org structure: " + oldOrganization.getQName().getLocalName().toString());
            nodeService.removeChildAssociation(oldOrganization);
        }
        
        log.debug("Returing AMR org size: " + orgStructures.size());
        return orgStructures.size();
    }

    @Override
    public void updateOrganisationStructures(ActionEvent event) {
        updateOrganisationStructures();
    }

    @Override
    public int updateOrganisationStructureBasedGroups() {
        if (!getUserService().isGroupsEditingAllowed()) {
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
        Map<Integer, OrganizationStructure> orgStructById = new HashMap<Integer, OrganizationStructure>(allOrganizationStructures.size());
        for (OrganizationStructure organizationStructure : allOrganizationStructures) {
            orgStructById.put(organizationStructure.getUnitId(), organizationStructure);
        }

        for (OrganizationStructure os : allOrganizationStructures) {
            String organizationPath = os.getOrganizationDisplayPath();
            String groupName = StringUtils.isEmpty(organizationPath) ? os.getName() : organizationPath;
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
                Serializable orgId = props.get(ContentModel.PROP_ORGID);
                if (orgId == null) {
                    continue;
                }
                OrganizationStructure orgStruct = null;
                try {
                    orgStruct = orgStructById.get(Integer.valueOf((String) orgId));
                } catch (NumberFormatException e) {
                    // Ignore and continue
                }
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
        
        log.debug("(1) Creating org strucure: " + org.getOrganizationPath().toString());
        log.debug("(2) getOrgStructsRoot(): " + getOrgStructsRoot().toString());
        log.debug("(3) OrganizationStructureModel.Assocs.ORGSTRUCT: " + OrganizationStructureModel.Assocs.ORGSTRUCT.toString());
        //log.debug("(4) ");
        
        nodeService.createNode(getOrgStructsRoot(), OrganizationStructureModel.Assocs.ORGSTRUCT, //
                QName.createQName(OrganizationStructureModel.URI, String.valueOf(org.getUnitId())), OrganizationStructureModel.Types.ORGSTRUCT, properties);
        
        
        orgStructPropertiesCache.put(org.getUnitId(), org);
    }

    @Override
    public OrganizationStructure getOrganizationStructure(int unitId) {
        OrganizationStructure os = orgStructPropertiesCache.get(unitId);
        if (os == null) {
            String xPath = OrganizationStructureModel.Repo.SPACE + "/" + OrganizationStructureModel.NAMESPACE_PREFFIX + unitId;
            NodeRef nodeRef = generalService.getNodeRef(xPath);
            if (nodeRef == null) {
                return null;
            }
            os = getOrganizationStructure(nodeRef);
            orgStructPropertiesCache.put(unitId, os);
        }
        return os;
    }

    @Override
    public String getOrganizationStructureName(String value) {
        OrganizationStructure orgStruct = getOrganizationStructur(value);
        if (orgStruct == null) {
            return value;
        }
        return orgStruct.getName();
    }

    protected OrganizationStructure getOrganizationStructur(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        try {
            Integer unitId = DefaultTypeConverter.INSTANCE.convert(Integer.class, value);
            return getOrganizationStructure(unitId);
        } catch (NumberFormatException e) {
            log.debug("Conversion failed, input cannot be parsed as integer: '" + value.toString() + "' " + value.getClass().getCanonicalName());
            return null;
        }
    }

    @Override
    public List<String> getOrganizationStructurePaths(String value) {
        OrganizationStructure orgStruct = getOrganizationStructur(value);
        if (orgStruct == null) {
            return null;
        }
        return orgStruct.getOrganizationPath();
    }

    @Override
    public List<OrganizationStructure> getAllOrganizationStructures() {
        NodeRef root = generalService.getNodeRef(OrganizationStructureModel.Repo.SPACE);
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(root);
        List<OrganizationStructure> orgstructs = new ArrayList<OrganizationStructure>(childRefs.size());
        Map<Integer, String> superNames = new HashMap<Integer, String>(childRefs.size());
        for (ChildAssociationRef childRef : childRefs) {
            OrganizationStructure os = getOrganizationStructure(childRef.getChildRef());
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
        List<NodeRef> nodes = getDocumentSearchService().searchNodesByTypeAndProps(input, OrganizationStructureModel.Types.ORGSTRUCT, props, limit);

        if (nodes == null) {
            return sortByName(getAllOrganizationStructures());
        }

        List<OrganizationStructure> structs = new ArrayList<OrganizationStructure>(nodes.size());
        for (NodeRef node : nodes) {
            structs.add(getOrganizationStructure(node));
        }
        return sortByName(structs);
    }

    @Override
    public List<Node> setUsersUnit(List<Node> users) {
        for (Node user : users) {
            Map<String, Object> props = user.getProperties();

            String unitId = (String) props.get(ContentModel.PROP_ORGID);
            String orgStruct;
            if (StringUtils.isEmpty(unitId)) {
                unitId = "";
                orgStruct = "";
            } else {
                orgStruct = getOrganizationStructureName(unitId);
            }

            props.put(UNIT_PROP, unitId + (StringUtils.equals(unitId, orgStruct) ? "" : " " + orgStruct));
            props.put(UNIT_NAME_PROP, orgStruct);
        }
        return users;
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
        String displayUnit = UserUtil.getDisplayUnit(os.getOrganizationPath());
        if (StringUtils.isBlank(displayUnit)) {
            displayUnit = os.getName();
        }
        os.setOrganizationDisplayPath(displayUnit);

        // TODO os.setSuperValueName
        return os;
    }

    private OrganizationStructure yksusToOrganizationStructure(YksusExt yksus) {
        OrganizationStructure org = new OrganizationStructure();
        org.setUnitId(yksus.getId().intValue());
        org.setName(yksus.getNimetus());
        BigInteger ylemYksusId = yksus.getYlemYksusId();
        if (ylemYksusId != null) {
            org.setSuperUnitId(ylemYksusId.intValue());
        }
        
        log.debug("formatYksusRadaToOrganizationPath: " + UserUtil.formatYksusRadaToOrganizationPath(yksus.getYksusRada()));
        org.setOrganizationPath(UserUtil.formatYksusRadaToOrganizationPath(yksus.getYksusRada()));
        return org;
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

    public void setAmrService(AMRService amrService) {
        this.amrService = amrService;
    }

    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    public void setOrganizationStructureUpdateEnabled(boolean organizationStructureUpdateEnabled) {
        this.organizationStructureUpdateEnabled = organizationStructureUpdateEnabled;
    }

    public void setOrgStructPropertiesCache(SimpleCache<Integer, OrganizationStructure> orgStructPropertiesCache) {
        this.orgStructPropertiesCache = orgStructPropertiesCache;
    }

    // END: getters / setters
}
