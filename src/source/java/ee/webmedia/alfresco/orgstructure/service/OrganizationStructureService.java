package ee.webmedia.alfresco.orgstructure.service;

import java.util.List;

import ee.webmedia.alfresco.orgstructure.model.OrganizationStructure;

/**
 * Service for searching and listing organization structures.
 * 
 * @author Dmitri Melnikov
 */
public interface OrganizationStructureService {

    public static final String BEAN_NAME = "OrganizationStructureService";

    /**
     * Returns a list of all found organization structures.
     * @return
     */
    List<OrganizationStructure> getAllOrganizationStructures();
    
    /**
     * Return one organization structure.
     * @param unitId
     * @return organization structure or {@code null} of not found
     */
    OrganizationStructure getOrganizationStructure(int unitId);

    /**
     * Return one organization structure.
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

}
