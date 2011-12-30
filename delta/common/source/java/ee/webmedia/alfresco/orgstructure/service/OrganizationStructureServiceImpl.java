package ee.webmedia.alfresco.orgstructure.service;

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
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.QName;
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
    // START: properties that would cause dependency cycle when trying to inject them

    private NodeRef orgStructsRoot;

    @Override
    public int updateOrganisationStructures() {
        if (!organizationStructureUpdateEnabled) {
            return 0;
        }

        YksusExt[] yksusArray = amrService.getYksusByAsutusId();
        List<OrganizationStructure> orgStructures = new ArrayList<OrganizationStructure>(yksusArray.length);
        for (YksusExt yksus : yksusArray) {
            orgStructures.add(yksusToOrganizationStructure(yksus));
        }
        Set<QName> childNodeTypeQnames = new HashSet<QName>();
        childNodeTypeQnames.add(OrganizationStructureModel.Assocs.ORGSTRUCT);
        // save old organization structures, that will be removed if everything goes right
        List<ChildAssociationRef> oldOrganizations = nodeService.getChildAssocs(getOrgStructsRoot(), childNodeTypeQnames);
        for (OrganizationStructure org : orgStructures) {
            createOrganisationStructure(org);
        }
        for (ChildAssociationRef oldOrganization : oldOrganizations) { // remove all old organizations
            nodeService.removeChildAssociation(oldOrganization);
        }
        return orgStructures.size();
    }

    @Override
    public int updateOrganisationStructureBasedGroups() {
        if (!getUserService().isGroupsEditingAllowed()) {
            return 0; // System uses Active Directory
        }

        NodeRef zone = authorityService.getOrCreateZone(STRUCT_UNIT_BASED);
        String zoneName = (String) nodeService.getProperty(zone, ContentModel.PROP_NAME);
        Set<String> allAuthoritiesInZone = authorityService.getAllAuthoritiesInZone(zoneName, AuthorityType.GROUP);
        // Delete previously generated groups and members
        for (String structUnitBasedAuthority : allAuthoritiesInZone) {
            authorityService.deleteAuthority(structUnitBasedAuthority, true);
        }

        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(BeanHelper.getPersonService().getPeopleContainer(),
                new HashSet<QName>(Arrays.asList(ContentModel.TYPE_PERSON)));
        List<Map<QName, Serializable>> users = new ArrayList<Map<QName, Serializable>>(childAssocs.size());
        for (ChildAssociationRef childAssociationRef : childAssocs) {
            users.add(nodeService.getProperties(childAssociationRef.getChildRef()));
        }

        // Fetch all groups that user has added manually
        Set<String> authorities = authorityService.getAllAuthoritiesInZone(AuthorityService.ZONE_APP_DEFAULT, AuthorityType.GROUP);

        List<OrganizationStructure> allOrganizationStructures = getAllOrganizationStructures();
        Map<Integer, OrganizationStructure> orgStructById = new HashMap<Integer, OrganizationStructure>(allOrganizationStructures.size());
        for (OrganizationStructure organizationStructure : allOrganizationStructures) {
            orgStructById.put(organizationStructure.getUnitId(), organizationStructure);
        }

        for (OrganizationStructure os : allOrganizationStructures) {
            List<String> organizationPath = os.getOrganizationPath();
            String longestPath = "";
            for (String path : organizationPath) {
                longestPath = (path.length() > longestPath.length()) ? path : longestPath;
            }
            String groupName = StringUtils.isEmpty(longestPath) ? os.getName() : longestPath;
            String groupAuthority = AuthorityType.GROUP.getPrefixString() + groupName;

            // User has manually created a group that is named after an organization structure
            if (authorities.contains(groupAuthority)) {
                authorityService.deleteAuthority(groupAuthority, true); // Delete the group and all users that have been added to it
            }

            // (Re)Create the groups from AMR
            String newGroup = authorityService.createAuthority(AuthorityType.GROUP, groupName, groupName,
                    new HashSet<String>(Arrays.asList(STRUCT_UNIT_BASED, AuthorityService.ZONE_AUTH_ALFRESCO, AuthorityService.ZONE_APP_DEFAULT)));
            // Add users to the group
            OUTER: for (Map<QName, Serializable> props : users) {
                @SuppressWarnings("unchecked")
                List<String> orgPath = (List<String>) props.get(ContentModel.PROP_ORGANIZATION_PATH);
                Serializable orgId = props.get(ContentModel.PROP_ORGID);
                if (orgPath != null) {
                    for (String op : orgPath) {
                        if (StringUtils.equals(op, groupName)) {
                            authorityService.addAuthority(newGroup, (String) props.get(ContentModel.PROP_USERNAME));
                            continue OUTER;
                        }
                    }
                }

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
                    authorityService.addAuthority(newGroup, (String) props.get(ContentModel.PROP_USERNAME));
                }
            }
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
                QName.createQName(OrganizationStructureModel.URI, String.valueOf(org.getUnitId())), OrganizationStructureModel.Types.ORGSTRUCT, properties);
    }

    @Override
    public OrganizationStructure getOrganizationStructure(int unitId) {
        String xPath = OrganizationStructureModel.Repo.SPACE + "/" + OrganizationStructureModel.NAMESPACE_PREFFIX + unitId;
        NodeRef nodeRef = generalService.getNodeRef(xPath);
        if (nodeRef == null) {
            return null;
        }
        return getOrganizationStructure(nodeRef);
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
            OrganizationStructure os = organizationStructureBeanPropertyMapper.toObject(nodeService.getProperties(childRef.getChildRef()));
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
    public List<OrganizationStructure> searchOrganizationStructures(String input, int limit) {
        Set<QName> props = new HashSet<QName>(1);
        props.add(OrganizationStructureModel.Props.NAME);
        props.add(OrganizationStructureModel.Props.ORGANIZATION_PATH);

        // why doesn't lucene sorting work? as a workaround we sort in java
        List<NodeRef> nodes = generalService.searchNodes(input, OrganizationStructureModel.Types.ORGSTRUCT, props, limit);

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

    private OrganizationStructure getOrganizationStructure(NodeRef nodeRef) {
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

    // END: getters / setters
}
