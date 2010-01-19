package ee.webmedia.alfresco.user.bootstrap;

import javax.faces.context.FacesContext;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;

import ee.webmedia.alfresco.user.service.UserService;

public class DocumentManagersGroupBootstrap extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        AuthorityService authorityService = serviceRegistry.getAuthorityService();
        // Create document managers group
        FacesContext context = FacesContext.getCurrentInstance();
        authorityService.createAuthority(AuthorityType.GROUP, UserService.DOCUMENT_MANAGERS_GROUP, I18NUtil.getMessage(UserService.DOCUMENT_MANAGERS_DISPLAY_NAME), authorityService.getDefaultZones());
        // Change the display name of administrator and email contributors group
        authorityService.setAuthorityDisplayName(authorityService.getName(AuthorityType.GROUP, "ALFRESCO_ADMINISTRATORS"), I18NUtil.getMessage(UserService.ALFRESCO_ADMINISTRATORS_DISPLAY_NAME));
    }

}
