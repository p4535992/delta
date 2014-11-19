<<<<<<< HEAD
package ee.webmedia.alfresco.user.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;

import ee.webmedia.alfresco.user.service.UserService;

/**
 * Create group for archivists
 * 
 * @author Ats Uiboupin
 */
public class ArchivistsGroupBootstrap extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        AccountantsGroupBootstrap.createGroup(UserService.ARCHIVIST_GROUP, UserService.ARCHIVISTS_DISPLAY_NAME);
    }

}
=======
package ee.webmedia.alfresco.user.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;

import ee.webmedia.alfresco.user.service.UserService;

/**
 * Create group for archivists
 */
public class ArchivistsGroupBootstrap extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        AccountantsGroupBootstrap.createGroup(UserService.ARCHIVIST_GROUP, UserService.ARCHIVISTS_DISPLAY_NAME);
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
