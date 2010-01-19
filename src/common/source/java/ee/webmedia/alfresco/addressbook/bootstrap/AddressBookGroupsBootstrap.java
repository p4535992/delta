package ee.webmedia.alfresco.addressbook.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.security.AuthorityType;

import ee.webmedia.alfresco.addressbook.service.AddressbookService;

public class AddressBookGroupsBootstrap extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        serviceRegistry.getAuthorityService().createAuthority(AuthorityType.GROUP, AddressbookService.ADDRESSBOOK_GROUP);
    }

}
