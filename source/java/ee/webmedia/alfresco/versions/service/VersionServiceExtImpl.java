<<<<<<< HEAD
package ee.webmedia.alfresco.versions.service;

import org.alfresco.repo.version.Version2Model;
import org.alfresco.repo.version.Version2ServiceImpl;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

/**
 * @author Riina Tens
 */
public class VersionServiceExtImpl extends Version2ServiceImpl implements VersionServiceExt {

    @Override
    public String calculateNextVersionLabel(NodeRef nodeRef) {
        QName classRef = nodeService.getType(nodeRef);
        org.alfresco.service.cmr.version.Version preceedingVersion = getCurrentVersion(nodeRef);
        int versionNumber = 1;
        if (preceedingVersion != null) {
            Integer currentVersionNumber = (Integer) preceedingVersion.getVersionProperty(Version2Model.PROP_VERSION_NUMBER);
            if (currentVersionNumber != null) {
                versionNumber = currentVersionNumber + 1;
            }
        }
        return invokeCalculateVersionLabel(classRef, preceedingVersion, versionNumber, null);
    }

}
=======
package ee.webmedia.alfresco.versions.service;

import org.alfresco.repo.version.Version2Model;
import org.alfresco.repo.version.Version2ServiceImpl;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

public class VersionServiceExtImpl extends Version2ServiceImpl implements VersionServiceExt {

    @Override
    public String calculateNextVersionLabel(NodeRef nodeRef) {
        QName classRef = nodeService.getType(nodeRef);
        org.alfresco.service.cmr.version.Version preceedingVersion = getCurrentVersion(nodeRef);
        int versionNumber = 1;
        if (preceedingVersion != null) {
            Integer currentVersionNumber = (Integer) preceedingVersion.getVersionProperty(Version2Model.PROP_VERSION_NUMBER);
            if (currentVersionNumber != null) {
                versionNumber = currentVersionNumber + 1;
            }
        }
        return invokeCalculateVersionLabel(classRef, preceedingVersion, versionNumber, null);
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
