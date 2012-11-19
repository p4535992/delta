package ee.webmedia.alfresco.dvk.job;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import ee.webmedia.alfresco.dvk.service.DvkService;

/**
 * Scheduled job to call a {@link DvkService#resendFailedSends()}.
 * 
 * @author Riina Tens
 */
public class DvkResendFailedSendsJob implements StatefulJob {

    private static Log log = LogFactory.getLog(DvkResendFailedSendsJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("Starting DvkResendFailedSendsJob");
        JobDataMap jobData = context.getJobDetail().getJobDataMap();
        Object workerObj = jobData.get(DvkService.BEAN_NAME);
        if (workerObj == null || !(workerObj instanceof DvkService)) {
            throw new AlfrescoRuntimeException("DvkResendFailedSendsJob data must contain valid 'DvkService' reference");
        }
        final DvkService worker = (DvkService) workerObj;

        // Run job as with systemUser privileges
        Pair<Integer, Integer> nrOfAllResendsAndSuccessfulResends = AuthenticationUtil.runAs(new RunAsWork<Pair<Integer, Integer>>() {
            @Override
            public Pair<Integer, Integer> doWork() throws Exception {
                return worker.resendFailedSends();
            }
        }, AuthenticationUtil.getSystemUserName());
        if (log.isInfoEnabled()) {
            log.info("DvkResendFailedSendsJob done, tried to resend " + nrOfAllResendsAndSuccessfulResends.getFirst()
                    + " tasks, successfully resent " + nrOfAllResendsAndSuccessfulResends.getSecond() + " tasks.");
        }
    }

}
