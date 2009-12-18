package ee.webmedia.alfresco.addressbook.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.security.AuthorityType;

public class AddressBookGroupsBootstrap extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        serviceRegistry.getAuthorityService().createAuthority(AuthorityType.GROUP, "ADDRESSBOOK");
    }

}
