package ee.webmedia.alfresco.maais.job;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import ee.webmedia.alfresco.maais.service.MaaisService;

/**
 * @author Keit Tehvan
 */
public class MaaisRenewCasesJob implements StatefulJob {
    private static Log log = LogFactory.getLog(MaaisRenewCasesJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("Starting UpdateMaaisRenewCasesJob");
        JobDataMap jobData = context.getJobDetail().getJobDataMap();
        Object workerObj = jobData.get("maaisService");
        if (workerObj == null || !(workerObj instanceof MaaisService)) {
            throw new AlfrescoRuntimeException("UpdateMaaisRenewCasesJob data must contain valid 'MaaisService' reference, but contained: "
                    + workerObj);
        }
        final MaaisService worker = (MaaisService) workerObj;
        if (!worker.isServiceAvailable()) {
            log.debug("Service not available, quitting");
            return;
        }

        // Run job as with systemUser privileges
        final Integer updateCount = AuthenticationUtil.runAs(new RunAsWork<Integer>() {
            @Override
            public Integer doWork() throws Exception {
                return worker.updateMaaisCases();
            }
        }, AuthenticationUtil.getSystemUserName());
        // Done
        log.info("UpdateMaaisRenewCasesJob done, got " + updateCount + "cases");
    }
}
