package ee.webmedia.alfresco.user.bootstrap;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;

import ee.webmedia.alfresco.user.service.UserService;

/**
 * @author Kaarel Jõgeva
 */
public class SupervisionGroupBootstrap extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        AuthorityService authorityService = serviceRegistry.getAuthorityService();
        // Create supervisors group
        authorityService.createAuthority(AuthorityType.GROUP, UserService.SUPERVISION_GROUP, I18NUtil.getMessage(UserService.SUPERVISION_DISPLAY_NAME),
                authorityService.getDefaultZones());
    }

}
