package ee.webmedia.alfresco.dvk.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.user.service.UserService;

public class DvkCorruptDocumentPermissionsBootstrap extends AbstractModuleComponent {

    private GeneralService generalService;
    private String dvkCorruptDocumentsPath;

    @Override
    protected void executeInternal() throws Throwable {
        AuthorityService authorityService = serviceRegistry.getAuthorityService();
        PermissionService permissionService = serviceRegistry.getPermissionService();

        NodeRef nodeRef = generalService.getNodeRef(dvkCorruptDocumentsPath);
        String authority = authorityService.getName(AuthorityType.GROUP, UserService.DOCUMENT_MANAGERS_GROUP);
        permissionService.setPermission(nodeRef, authority, "DocumentDelete", true);
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setDvkCorruptDocumentsPath(String dvkCorruptDocumentsPath) {
        this.dvkCorruptDocumentsPath = dvkCorruptDocumentsPath;
    }

}
