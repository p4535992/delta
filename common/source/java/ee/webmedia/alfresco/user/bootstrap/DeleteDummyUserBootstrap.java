package ee.webmedia.alfresco.user.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.security.PersonService;

public class DeleteDummyUserBootstrap extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        PersonService personService = serviceRegistry.getPersonService();
        personService.deletePerson("dummy");
    }

}
