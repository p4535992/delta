package ee.webmedia.alfresco.versions.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.versions.model.Version;

public interface VersionsService {
    String BEAN_NAME = "VersionsService";
    
    /**
     * Returns the full name of the user if nodeRef has VersionsModel.Aspects.VERSION_MODIFIED aspect.
     * @param nodeRef
     * @return
     */
    String getPersonFullNameFromAspect(NodeRef nodeRef, String userName);
    
    /**
     * Returns all versions of this nodeRef.
     * @param nodeRef
     * @return
     */
    List<Version> getAllVersions(NodeRef nodeRef, String fileName);
}
