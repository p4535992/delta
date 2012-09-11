package ee.webmedia.alfresco.parameters.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;

import ee.webmedia.alfresco.parameters.service.ParametersService;

public class ParameterRescheduledJobsBootstrap extends AbstractModuleComponent {

    private ParametersService parametersService;

    @Override
    protected void executeInternal() throws Throwable {
        parametersService.applicationStarted();
    }

    // START: getters / setters
    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }
    // END: getters / setters

}
