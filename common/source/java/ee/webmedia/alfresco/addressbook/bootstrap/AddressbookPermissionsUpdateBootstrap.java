<<<<<<< HEAD
package ee.webmedia.alfresco.addressbook.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.user.service.UserService;

/**
 * Adds AddressbookManage permission to GROUP_DOCUMENT_MANAGERS if it is missing
 * 
 * @author Ats Uiboupin
 */
public class AddressbookPermissionsUpdateBootstrap extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        final NodeRef addressbookNodeRef = BeanHelper.getAddressbookService().getAddressbookRoot();
        PermissionService permissionService = BeanHelper.getPermissionService();
        permissionService.setPermission(addressbookNodeRef, UserService.AUTH_DOCUMENT_MANAGERS_GROUP, "AddressbookManage", true);
    }

}
=======
package ee.webmedia.alfresco.addressbook.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.user.service.UserService;

/**
 * Adds AddressbookManage permission to GROUP_DOCUMENT_MANAGERS if it is missing
 */
public class AddressbookPermissionsUpdateBootstrap extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        final NodeRef addressbookNodeRef = BeanHelper.getAddressbookService().getAddressbookRoot();
        PermissionService permissionService = BeanHelper.getPermissionService();
        permissionService.setPermission(addressbookNodeRef, UserService.AUTH_DOCUMENT_MANAGERS_GROUP, "AddressbookManage", true);
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
