<<<<<<< HEAD
package ee.webmedia.alfresco.user.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;

import ee.webmedia.alfresco.user.service.UserService;

/**
 * @author Kaarel Jõgeva
 */
public class SupervisionGroupBootstrap extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        AccountantsGroupBootstrap.createGroup(UserService.SUPERVISION_GROUP, UserService.SUPERVISION_DISPLAY_NAME);
    }

}
=======
package ee.webmedia.alfresco.user.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;

import ee.webmedia.alfresco.user.service.UserService;

public class SupervisionGroupBootstrap extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        AccountantsGroupBootstrap.createGroup(UserService.SUPERVISION_GROUP, UserService.SUPERVISION_DISPLAY_NAME);
    }

}
>>>>>>> develop-5.1
