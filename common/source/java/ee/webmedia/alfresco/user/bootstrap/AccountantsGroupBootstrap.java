<<<<<<< HEAD
package ee.webmedia.alfresco.user.bootstrap;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.user.service.UserService;

// Create accountants group
public class AccountantsGroupBootstrap extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        createGroup(UserService.ACCOUNTANTS_GROUP, UserService.ACCOUNTANTS_DISPLAY_NAME);
    }

    public static void createGroup(String groupCode, String groupCodeMsgKey) {
        AuthorityService authorityService = BeanHelper.getAuthorityService();
        authorityService.createAuthority(AuthorityType.GROUP, groupCode, I18NUtil.getMessage(groupCodeMsgKey), authorityService.getDefaultZones());
    }

}
=======
package ee.webmedia.alfresco.user.bootstrap;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.user.service.UserService;

// Create accountants group
public class AccountantsGroupBootstrap extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        createGroup(UserService.ACCOUNTANTS_GROUP, UserService.ACCOUNTANTS_DISPLAY_NAME);
    }

    public static void createGroup(String groupCode, String groupCodeMsgKey) {
        AuthorityService authorityService = BeanHelper.getAuthorityService();
        authorityService.createAuthority(AuthorityType.GROUP, groupCode, I18NUtil.getMessage(groupCodeMsgKey), authorityService.getDefaultZones());
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
