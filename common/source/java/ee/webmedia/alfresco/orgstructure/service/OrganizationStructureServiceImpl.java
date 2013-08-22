package ee.webmedia.alfresco.orgstructure.service;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.lang.StringUtils;

import smit.ametnik.services.Yksus;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.orgstructure.amr.service.AMRService;
import ee.webmedia.alfresco.orgstructure.model.OrganizationStructure;
import ee.webmedia.alfresco.orgstructure.model.OrganizationStructureModel;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;

public class OrganizationStructureServiceImpl implements OrganizationStructureService {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(OrganizationStructureServiceImpl.class);

    @SuppressWarnings("unchecked")
    private static Comparator<OrganizationStructure> nameComparator = new NullComparator(new OrganizationStructure.NameComparator());
    private static BeanPropertyMapper<OrganizationStructure> organizationStructureBeanPropertyMapper = BeanPropertyMapper
            .newInstance(OrganizationStructure.class);

    private GeneralService generalService;
    private NodeService nodeService;
    private AMRService amrService;

    @Override
    public int updateOrganisationStructures() {
        Yksus[] yksusArray = amrService.getYksusByAsutusId();
        List<OrganizationStructure> orgStructures = new ArrayList<OrganizationStructure>(yksusArray.length);
        for (Yksus yksus : yksusArray) {
            orgStructures.add(yksusToOrganizationStructure(yksus));
        }
        Set<QName> childNodeTypeQnames = new HashSet<QName>();
        childNodeTypeQnames.add(OrganizationStructureModel.Assocs.ORGSTRUCT);
        String orgStructXPath = OrganizationStructureModel.Repo.SPACE;
        NodeRef orgsNodeRef = generalService.getNodeRef(orgStructXPath);
        // save old organization structures, that will be removed if everything goes right
        List<ChildAssociationRef> oldOrganizations = nodeService.getChildAssocs(orgsNodeRef, childNodeTypeQnames);
        for (OrganizationStructure org : orgStructures) {
            Map<QName, Serializable> properties = organizationStructureBeanPropertyMapper.toProperties(org);
            nodeService.createNode(orgsNodeRef, OrganizationStructureModel.Assocs.ORGSTRUCT, //
                    QName.createQName(OrganizationStructureModel.URI, String.valueOf(org.getUnitId())), OrganizationStructureModel.Types.ORGSTRUCT, properties);
        }
        for (ChildAssociationRef oldOrganization : oldOrganizations) { // remove all old organizations
            nodeService.removeChildAssociation(oldOrganization);
        }
        return orgStructures.size();
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
    public String getOrganizationStructure(String value) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }
        try {
            Integer unitId = DefaultTypeConverter.INSTANCE.convert(Integer.class, value);
            OrganizationStructure os = getOrganizationStructure(unitId);
            if (os == null) {
                return value.toString();
            }
            return os.getName();
        } catch (NumberFormatException e) {
            log.debug("Conversion failed, input cannot be parsed as integer: '" + value.toString() + "' " + value.getClass().getCanonicalName());
            return value.toString();
        }
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
    public List<OrganizationStructure> searchOrganizationStructures(String input) {
        Set<QName> props = new HashSet<QName>(1);
        props.add(OrganizationStructureModel.Props.NAME);

        // why doesn't lucene sorting work? as a workaround we sort in java
        List<NodeRef> nodes = generalService.searchNodes(input, OrganizationStructureModel.Types.ORGSTRUCT, props);

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
                orgStruct = getOrganizationStructure(unitId);
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
        // TODO os.setSuperValueName
        return os;
    }

    private OrganizationStructure yksusToOrganizationStructure(Yksus yksus) {
        OrganizationStructure org = new OrganizationStructure();
        org.setUnitId(yksus.getId().intValue());
        org.setName(yksus.getNimetus());
        BigInteger ylemYksusId = yksus.getYlemYksusId();
        if (ylemYksusId != null) {
            org.setSuperUnitId(ylemYksusId.intValue());
        }
        return org;
    }

    // END: private methods

    // START: getters / setters
    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setAmrService(AMRService amrService) {
        this.amrService = amrService;
    }
    // END: getters / setters
}
