package ee.webmedia.alfresco.substitute.service;

import java.util.Date;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.substitute.model.Substitute;

<<<<<<< HEAD
/**
 * @author Romet Aidla
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public interface SubstituteService {
    public static final String BEAN_NAME = "SubstituteService";

    /**
     * Gets list of substitutes.
     * 
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
     * 
     * @return
     */
    List<Substitute> findActiveSubstitutionDuties(String userName);

    /**
     * @param otherUserName
     * @return true if authenticated user can substitute <code>otherUserName</code> at the moment
     */
    boolean canBeSubstituting(String otherUserName);

    /**
     * Finds list of substitution duties for given user that overlap given time-frame.
     * 
     * @param user nodeRef
     * @param startDate if null, return empty list
     * @param endDate if null, return empty list
     * @return list of substitutes
     */
    List<Substitute> findSubstitutionDutiesInPeriod(NodeRef userNodeRef, Date startDate, Date endDate);

}
