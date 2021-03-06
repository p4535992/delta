package ee.webmedia.alfresco.substitute.service;

import java.util.Date;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.substitute.model.Substitute;
import ee.webmedia.alfresco.substitute.model.UnmodifiableSubstitute;

public interface SubstituteService {
    public static final String BEAN_NAME = "SubstituteService";

    /**
     * Gets list of substitutes from repo.
     *
     * @return List
     */
    List<Substitute> getSubstitutes(NodeRef userNodeRef);

    /**
     * Get list of {@code UnmodifiableSubstitute}s from cache
     */
    List<UnmodifiableSubstitute> getUnmodifiableSubstitutes(NodeRef userNodeRef);

    String getSubstituteLabel(String userName);

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
    List<Substitute> searchActiveSubstitutionDuties(String userName);

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

    String clearCache();

}
