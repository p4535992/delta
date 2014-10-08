package ee.webmedia.alfresco.parameters.job;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleTrigger;

import ee.webmedia.alfresco.parameters.model.Parameter;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;

/**
 * SimpleTrigger that stores next fire time into repository,
 * so that trigger state can be restored between server restarts.
<<<<<<< HEAD
 * 
 * @author Romet Aidla
=======
>>>>>>> develop-5.1
 */
public class PersistentTrigger extends SimpleTrigger {
    private static final long serialVersionUID = 1L;

    private final ParametersService parametersService;
    private final String parameterName;

    public PersistentTrigger(String name, String group, String parameterName, ParametersService parametersService) {
        super(name, group);
        this.parameterName = parameterName;
        this.parametersService = parametersService;
    }

    @Override
    public int executionComplete(JobExecutionContext context, JobExecutionException result) {
        AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Object>() {
            @Override
            public Object doWork() throws Exception {
                Parameter param = parametersService.getParameter(Parameters.get(parameterName));
                persistNextFireTime(param);
                return null;
            }
        }, AuthenticationUtil.getSystemUserName());

        return super.executionComplete(context, result);
    }

    private void persistNextFireTime(Parameter param) {
        param.setNextFireTime(getNextFireTime());
        parametersService.setParameterNextFireTime(param);
    }
}
