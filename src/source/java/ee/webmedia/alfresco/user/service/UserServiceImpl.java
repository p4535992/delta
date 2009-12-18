package ee.webmedia.alfresco.user.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.MapNode;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.orgstructure.model.OrganizationStructure;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;

public class UserServiceImpl implements UserService {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(UserServiceImpl.class);

    private AuthorityService authorityService;
    private OrganizationStructureService organizationStructureService;
    private GeneralService generalService;
    private NodeService nodeService;
    private SearchService searchService;

    @Override
    public boolean isAdministrator() {
        return authorityService.hasAdminAuthority();
    }

    @Override
    public boolean isDocumentManager() {
        if (isAdministrator()) {
            return true;
        }

        return authorityService.getAuthorities().contains(getDocumentManagersGroup());
    }

    @Override
    public String getDocumentManagersGroup() {
        return PermissionService.GROUP_PREFIX + DOCUMENT_MANAGERS;
    }

    @Override
    public String getAlfrescoAdministratorsGroup() {
        return PermissionService.GROUP_PREFIX + "ALFRESCO_ADMINISTRATORS";
    }

    @Override
    public List<Node> searchUsers(String input, boolean returnAllUsers) {
        Set<QName> props = new HashSet<QName>(1);
        props.add(ContentModel.PROP_FIRSTNAME);
        props.add(ContentModel.PROP_LASTNAME);

        List<NodeRef> nodeRefs = generalService.searchNodes(input, ContentModel.TYPE_PERSON, props);
        if (nodeRefs == null) {
            if (returnAllUsers) {
                //TODO XXX FIXME use service instead
                return Repository.getUsers(FacesContext.getCurrentInstance(), nodeService, searchService);
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

        return users;
    }

    public List<Node> setUsersUnit(List<Node> users) {
        for (Node user : users) {
            // Eagerly load node properties from repository
            Map<String, Object> prop = user.getProperties();

            String unitIdProp = (String) prop.get(ContentModel.PROP_ORGID);
            if (!StringUtils.isEmpty(unitIdProp)) {

                int unitId;
                try {
                    unitId = Integer.parseInt(unitIdProp);
                } catch (NumberFormatException nfe) {
                    throw new RuntimeException("Tried to get unitId for user (NodeRef: " + user.getNodeRefAsString() + "), but failed to parse it!", nfe);
                }

                OrganizationStructure orgStruct = organizationStructureService.getOrganizationStructure(unitId);
                
                prop.put(CUSTOM_UNIT_PROP, orgStruct == null ? unitIdProp : unitIdProp + " " + orgStruct.getName());
                prop.put(CUSTOM_UNIT_NAME_PROP, orgStruct == null ? null : orgStruct.getName());
            }
        }
        return users;
    }

    // START: setters/getters
    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    public void setOrganizationStructureService(OrganizationStructureService organizationStructureService) {
        this.organizationStructureService = organizationStructureService;
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
    // END: setters/getters

}
