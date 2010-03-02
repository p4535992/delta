package ee.webmedia.alfresco.substitute.service;

import ee.webmedia.alfresco.substitute.model.Substitute;
import org.alfresco.service.cmr.repository.NodeRef;

import java.util.List;

/**
 * @author Romet Aidla
 */
public interface SubstituteService {
    public static final String BEAN_NAME = "SubstituteService";

    /**
     * Gets list of substitutes.
     * @return List
     */
    List<Substitute> getSubstitutes(NodeRef userNodeRef);

    Substitute getSubstitute(NodeRef substituteRef);

    /**
     * Adds new substitute.
     *
     * @param substitute Substitute to be added
     * @return reference to added substitute
     */
    NodeRef addSubstitute(NodeRef userNodeRef, Substitute substitute);

    /**
     * Updates existing substitute.
     *
     * @param substitute Substitute to be updated
     */
    void updateSubstitute(Substitute substitute);

    /**
     * Deletes substitute
     *
     * @param substituteNodeRef Reference to substitute
     */
    void deleteSubstitute(NodeRef substituteNodeRef);

    /**
     * Finds list of currently active substitution duties for given person.
     * @return
     */
    List<Substitute> findActiveSubstitutionDuties(String userName);

}
