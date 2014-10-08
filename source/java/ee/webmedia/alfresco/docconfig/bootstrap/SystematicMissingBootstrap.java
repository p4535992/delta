package ee.webmedia.alfresco.docconfig.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

import ee.webmedia.alfresco.common.bootstrap.ImporterModuleComponent;
import ee.webmedia.alfresco.common.service.GeneralService;

/**
 * Fix for task 180940 - in already existing deployments, two bootstap component names were used in 3.2 but then deleted, so they did not execute again when reused in 3.3.
<<<<<<< HEAD
 * 
 * @author Alar Kvell
=======
>>>>>>> develop-5.1
 */
public class SystematicMissingBootstrap extends AbstractModuleComponent implements BeanFactoryAware {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(SystematicMissingBootstrap.class);

    private GeneralService generalService;
    private BeanFactory beanFactory;

    @Override
    protected void executeInternal() throws Throwable {
        if (generalService.getNodeRef("/docadmin:fieldDefinitions/docdyn:publishToAdr") == null) {
            LOG.info("Executing systematicFieldDefinitionsBootstrap2");
            ((ImporterModuleComponent) beanFactory.getBean("systematicFieldDefinitionsBootstrap2")).executeInternal();
        }
        if (generalService.getNodeRef("/docadmin:fieldGroupDefinitions/docadmin:contractParties") == null
                && generalService.getNodeRef("/docadmin:fieldGroupDefinitions/docadmin:firstPartyContactPerson") == null) {
            LOG.info("Executing systematicFieldGroupDefinitionsBootstrap2");
            ((ImporterModuleComponent) beanFactory.getBean("systematicFieldGroupDefinitionsBootstrap2")).executeInternal();
        }
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

}
