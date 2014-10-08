<<<<<<< HEAD
package ee.webmedia.alfresco.user.bootstrap;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;

import ee.webmedia.alfresco.user.service.UserService;

//Handle 2 special groups - their system name is different from display name
// The same names are used in ChainingUserRegistrySynchronizer
public class DocumentManagersGroupBootstrap extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        AuthorityService authorityService = serviceRegistry.getAuthorityService();
        // Create document managers group
        authorityService.createAuthority(AuthorityType.GROUP, UserService.DOCUMENT_MANAGERS_GROUP, I18NUtil.getMessage(UserService.DOCUMENT_MANAGERS_DISPLAY_NAME),
                authorityService.getDefaultZones());
        // Change the display name of administrator and email contributors group
        authorityService.setAuthorityDisplayName(authorityService.getName(AuthorityType.GROUP, UserService.ADMINISTRATORS_GROUP),
                I18NUtil.getMessage(UserService.ALFRESCO_ADMINISTRATORS_DISPLAY_NAME));
    }

}
=======
package ee.webmedia.alfresco.user.bootstrap;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;

import ee.webmedia.alfresco.user.service.UserService;

//Handle 2 special groups - their system name is different from display name
// The same names are used in ChainingUserRegistrySynchronizer
public class DocumentManagersGroupBootstrap extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        AuthorityService authorityService = serviceRegistry.getAuthorityService();
        // Create document managers group
        authorityService.createAuthority(AuthorityType.GROUP, UserService.DOCUMENT_MANAGERS_GROUP, I18NUtil.getMessage(UserService.DOCUMENT_MANAGERS_DISPLAY_NAME),
                authorityService.getDefaultZones());
        // Change the display name of administrator and email contributors group
        authorityService.setAuthorityDisplayName(authorityService.getName(AuthorityType.GROUP, UserService.ADMINISTRATORS_GROUP),
                I18NUtil.getMessage(UserService.ALFRESCO_ADMINISTRATORS_DISPLAY_NAME));
    }

}
>>>>>>> develop-5.1
