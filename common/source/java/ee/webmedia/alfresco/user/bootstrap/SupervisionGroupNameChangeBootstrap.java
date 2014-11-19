<<<<<<< HEAD
package ee.webmedia.alfresco.user.bootstrap;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;

import ee.webmedia.alfresco.user.service.UserService;

/**
 * @author Keit Tehvan
 */
public class SupervisionGroupNameChangeBootstrap extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        AuthorityService authorityService = serviceRegistry.getAuthorityService();
        authorityService.setAuthorityDisplayName(AuthorityType.GROUP.getPrefixString() + UserService.SUPERVISION_GROUP, I18NUtil.getMessage(UserService.SUPERVISION_DISPLAY_NAME));
    }

}
=======
package ee.webmedia.alfresco.user.bootstrap;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;

import ee.webmedia.alfresco.user.service.UserService;

public class SupervisionGroupNameChangeBootstrap extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        AuthorityService authorityService = serviceRegistry.getAuthorityService();
        authorityService.setAuthorityDisplayName(AuthorityType.GROUP.getPrefixString() + UserService.SUPERVISION_GROUP, I18NUtil.getMessage(UserService.SUPERVISION_DISPLAY_NAME));
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
