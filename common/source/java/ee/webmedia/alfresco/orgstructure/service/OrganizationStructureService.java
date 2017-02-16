package ee.webmedia.alfresco.orgstructure.service;

import ee.webmedia.alfresco.orgstructure.model.OrganizationStructure;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

import javax.faces.event.ActionEvent;
import java.util.List;

/**
 * Service for searching and listing organization structures.
 */
public interface OrganizationStructureService {

    String BEAN_NAME = "OrganizationStructureService";

    String UNIT_PROP = "unit";
    String UNIT_NAME_PROP = "unitName";
    String STRUCT_UNIT_BASED = "STRUCT.UNIT.BASED";

    /**
     * Returns a list of all found organization structures.
     *
     * @return
     */
    List<OrganizationStructure> getAllOrganizationStructures();

    /**
     * Return one organization structure.
     *
     * @param unitId
     * @return organization structure or {@code null} of not found
     */
    OrganizationStructure getOrganizationStructure(String unitId);

    /**
     * Return one organization structure.
     *
     * @param unitId
     * @return organization structure or {@code null} of not found
     */
    String getOrganizationStructureName(String unitId);

    /**
     * @return number of organisations updated
     */
    int updateOrganisationStructures();

    void updateOrganisationStructures(ActionEvent event);

    /**
     * Find organization structures by name. If input is empty, all organization structures are returned.
     *
     * @param limit
     */
    List<OrganizationStructure> searchOrganizationStructures(String input, int limit);

    /**
     * Sets correct and up to date unit name for users, based on data found in organization structure list.
     *
     * @param users list of user nodes to be processed
     * @return processed nodes
     */
    List<Node> setUsersUnit(List<Node> users);

    void createOrganisationStructure(OrganizationStructure org);

    int updateOrganisationStructureBasedGroups();

    void updateOrganisationStructureBasedGroups(ActionEvent event);

    List<String> getOrganizationStructurePaths(String value);

    List<NodeRef> getAllOrganizationStructureRefs();

    OrganizationStructure getOrganizationStructure(NodeRef nodeRef);

    void loadUserUnit(Node user);

    public String getSyncActiveStatus();

}
