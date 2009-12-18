package ee.webmedia.alfresco.user.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;

import ee.webmedia.alfresco.user.service.UserService;

public class DocumentManagersGroupBootstrap extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        AuthorityService authorityService = serviceRegistry.getAuthorityService();
        // Create document managers group
        authorityService.createAuthority(AuthorityType.GROUP, UserService.DOCUMENT_MANAGERS, UserService.DOCUMENT_MANAGERS_DISPLAY_NAME, authorityService.getDefaultZones());
        // Change the display name of administrator and email contributors group
        authorityService.setAuthorityDisplayName("GROUP_ALFRESCO_ADMINISTRATORS", UserService.ALFRESCO_ADMINISTRATORS_DISPLAY_NAME);
        authorityService.setAuthorityDisplayName("GROUP_EMAIL_CONTRIBUTORS", UserService.EMAIL_CONTRIBUTORS_DISPLAY_NAME);
        // Remove addressbook group
        authorityService.deleteAuthority("GROUP_ADDRESSBOOK");
        
    }

}
