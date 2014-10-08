package ee.webmedia.alfresco.user.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;

import ee.webmedia.alfresco.user.service.UserService;

public class SupervisionGroupBootstrap extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        AccountantsGroupBootstrap.createGroup(UserService.SUPERVISION_GROUP, UserService.SUPERVISION_DISPLAY_NAME);
    }

}
