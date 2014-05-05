package ee.webmedia.alfresco.versions.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;

import org.alfresco.repo.version.Version2Model;
import org.alfresco.repo.version.Version2ServiceImpl;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.web.BeanHelper;

public class VersionServiceExtImpl extends Version2ServiceImpl implements VersionServiceExt {

    @Override
    public String calculateNextVersionLabel(NodeRef nodeRef) {
        QName classRef = getNodeService().getType(nodeRef);
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
