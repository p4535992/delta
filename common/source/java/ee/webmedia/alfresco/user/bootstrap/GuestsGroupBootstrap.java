package ee.webmedia.alfresco.user.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;

import ee.webmedia.alfresco.user.service.UserService;

/**
 * Create group for guests
 */
public class GuestsGroupBootstrap extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        AccountantsGroupBootstrap.createGroup(UserService.GUESTS_GROUP, UserService.GUESTS_DISPLAY_NAME);
    }

}
