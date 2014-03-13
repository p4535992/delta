package ee.webmedia.alfresco.orgstructure.service;

import java.util.List;

import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.orgstructure.model.OrganizationStructure;

/**
 * Service for searching and listing organization structures.
 */
public interface OrganizationStructureService {

    String BEAN_NAME = "OrganizationStructureService";

    String UNIT_PROP = "unit";
    String UNIT_NAME_PROP = "unitName";

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
    OrganizationStructure getOrganizationStructure(int unitId);

    /**
     * Return one organization structure.
     * 
     * @param unitId
     * @return organization structure or {@code null} of not found
     */
    String getOrganizationStructure(String unitId);

    /**
     * @return number of organisations updated
     */
    int updateOrganisationStructures();

    /**
     * Find organization structures by name. If input is empty, all organization structures are returned.
     */
    List<OrganizationStructure> searchOrganizationStructures(String input);

    /**
     * Sets correct and up to date unit name for users, based on data found in organization structure list.
     * 
     * @param users list of user nodes to be processed
     * @return processed nodes
     */
    List<Node> setUsersUnit(List<Node> users);

}
