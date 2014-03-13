package ee.webmedia.alfresco.document.scanned.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.user.service.UserService;

/**
 * reverts what ScannedDocumentManagersPermissionsBootstrap used to do
 */
public class UndoScannedDocumentManagersPermissionsBootstrap extends AbstractModuleComponent {
    protected final Log LOG = LogFactory.getLog(getClass());

    private GeneralService generalService;
    private String scannedFilesPath;

    @Override
    protected void executeInternal() throws Throwable {
        LOG.info("Executing " + getName());

        AuthorityService authorityService = serviceRegistry.getAuthorityService();
        PermissionService permissionService = serviceRegistry.getPermissionService();

        NodeRef nodeRef = generalService.getNodeRef(scannedFilesPath);
        String authority = authorityService.getName(AuthorityType.GROUP, UserService.DOCUMENT_MANAGERS_GROUP);
        permissionService.deletePermission(nodeRef, authority, "DocumentFileRead");
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setScannedFilesPath(String scannedFilesPath) {
        this.scannedFilesPath = scannedFilesPath;
    }

}
