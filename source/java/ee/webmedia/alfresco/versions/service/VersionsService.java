package ee.webmedia.alfresco.versions.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.versions.model.Version;

public interface VersionsService {
    String BEAN_NAME = "VersionsService";

    /**
     * Returns the full name of the user if nodeRef has VersionsModel.Aspects.VERSION_MODIFIED aspect.
     * 
     * @param nodeRef
     * @return
     */
    String getPersonFullNameFromAspect(NodeRef nodeRef, String userName);

    /**
     * Returns all versions of this nodeRef.
     * 
     * @param nodeRef
     * @return
     */
<<<<<<< HEAD
    List<Version> getAllVersions(NodeRef nodeRef);
=======
    List<Version> getAllVersions(NodeRef nodeRef, String fileName);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    /**
     * Adds the user and date information to the custom aspect properties.
     * 
     * @param nodeRef
     */
    void updateVersionModifiedAspect(NodeRef nodeRef);

<<<<<<< HEAD
    void addVersionModifiedAspect(NodeRef nodeRef);

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    /**
     * Updates the version if the node is unlocked and last version was not saved.. (.. today or ..by authenticated user)
     * Sets VersionsModel.Props.VersionLockable.LOCKED property of the VersionsModel.Aspects.VERSION_LOCKABLE aspect to true.
     * 
     * @param nodeRef
     * @param filename TODO
     * @return true if new version was created
     */
    boolean updateVersion(NodeRef nodeRef, String filename, boolean updateOnlyIfNeeded);

    /**
     * Returns the value of the VersionsModel.Props.VersionLockable.LOCKED property if the node has VersionsModel.Aspects.VERSION_LOCKABLE aspect.
     * False if there is no VersionsModel.Aspects.VERSION_LOCKABLE on the node.
     * 
     * @param nodeRef
     * @return
     */
    boolean getVersionLockableAspect(NodeRef nodeRef);

    /**
     * If the node has VersionsModel.Aspects.VERSION_LOCKABLE aspect, sets the VersionsModel.Props.VersionLockable.LOCKED property to flag.
     * 
     * @param lockNode
     * @param flag
     */
    void setVersionLockableAspect(NodeRef lockNode, boolean flag);

    /**
     * Adds the VersionsModel.Aspects.VERSION_LOCKABLE aspect to the node.
     * Does NOT set any value for the VersionsModel.Props.VersionLockable.LOCKED property.
     * 
     * @param lockNode
     */
<<<<<<< HEAD
    boolean addVersionLockableAspect(NodeRef lockNode);
=======
    void addVersionLockableAspect(NodeRef lockNode);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    /**
     * Calculate next version label based on current version (no new version is actually created)
     * 
     * @param nodeRef
     * @return
     */
    String calculateNextVersionLabel(NodeRef fileRef);

<<<<<<< HEAD
    void activateVersion(NodeRef versionRef);

    boolean addVersionableAspect(NodeRef nodeRef);

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}
