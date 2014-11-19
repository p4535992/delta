<<<<<<< HEAD
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
=======
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
