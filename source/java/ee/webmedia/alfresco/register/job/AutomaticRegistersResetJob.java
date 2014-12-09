package ee.webmedia.alfresco.register.job;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import ee.webmedia.alfresco.register.service.RegisterService;

public class AutomaticRegistersResetJob implements StatefulJob {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(AutomaticRegistersResetJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOG.info("Starting AutomaticRegistersResetJob");
        JobDataMap jobData = context.getJobDetail().getJobDataMap();

        String objectName = "registerService";
        Object workerObj = jobData.get(objectName);
        if (workerObj == null || !(workerObj instanceof RegisterService)) {
            throw new AlfrescoRuntimeException("AutomaticRegistersResetJob data must contain valid '" + objectName + "' reference, but contained: "
                    + workerObj);
        }
        final RegisterService worker = (RegisterService) workerObj;

        final Integer count = AuthenticationUtil.runAs(new RunAsWork<Integer>() {
            @Override
            public Integer doWork() throws Exception {
                return worker.resetAllAutoResetCounters();
            }
        }, AuthenticationUtil.getSystemUserName());
        LOG.info("AutomaticRegistersResetJob done, reset " + count + " registers");
    }

}
