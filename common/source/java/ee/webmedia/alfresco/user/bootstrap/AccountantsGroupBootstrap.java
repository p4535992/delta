package ee.webmedia.alfresco.user.bootstrap;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;

import ee.webmedia.alfresco.user.service.UserService;

public class AccountantsGroupBootstrap extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        AuthorityService authorityService = serviceRegistry.getAuthorityService();
        // Create accountants group
        authorityService.createAuthority(AuthorityType.GROUP, UserService.ACCOUNTANTS_GROUP, I18NUtil.getMessage(UserService.ACCOUNTANTS_DISPLAY_NAME),
                authorityService.getDefaultZones());
    }

}
