package ee.webmedia.alfresco.document.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.security.PermissionService;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel.Privileges;

/**
 * Set all document privileges to drafts folder.
 */
public class DraftsPermissionsUpdater extends AbstractModuleComponent {

    private boolean disabled;

    @Override
    protected void executeInternal() throws Throwable {
        if (!disabled) {
            BeanHelper.getPrivilegeService().setPermissions(BeanHelper.getDocumentService().getDrafts(), PermissionService.ALL_AUTHORITIES, Privileges.EDIT_DOCUMENT);
        }
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

}
