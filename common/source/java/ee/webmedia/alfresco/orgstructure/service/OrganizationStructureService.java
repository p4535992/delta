package ee.webmedia.alfresco.orgstructure.service;

import java.util.List;

import javax.faces.event.ActionEvent;

<<<<<<< HEAD
=======
import org.alfresco.service.cmr.repository.NodeRef;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.orgstructure.model.OrganizationStructure;

/**
 * Service for searching and listing organization structures.
<<<<<<< HEAD
 * 
 * @author Dmitri Melnikov
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
    OrganizationStructure getOrganizationStructure(String unitId);
=======
    OrganizationStructure getOrganizationStructure(int unitId);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

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

<<<<<<< HEAD
=======
    List<NodeRef> getAllOrganizationStructureRefs();

    OrganizationStructure getOrganizationStructure(NodeRef nodeRef);

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}
