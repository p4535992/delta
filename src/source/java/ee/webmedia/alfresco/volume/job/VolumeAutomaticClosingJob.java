package ee.webmedia.alfresco.volume.job;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import ee.webmedia.alfresco.functions.service.FunctionsService;

public class VolumeAutomaticClosingJob implements StatefulJob {
    private static Log log = LogFactory.getLog(VolumeAutomaticClosingJob.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (log.isDebugEnabled())  log.debug("Starting VolumeAutomaticClosingJob");
        JobDataMap jobData = jobExecutionContext.getJobDetail().getJobDataMap();
        Object workerObj = jobData.get("functionsService");

        if (workerObj == null || !(workerObj instanceof FunctionsService)) {
            throw new AlfrescoRuntimeException("VolumeAutomaticClosingJob data must contain valid 'functionsService' reference, but contained: "
                    + workerObj);
        }
        final FunctionsService worker = (FunctionsService) workerObj;

        // Run job as with systemUser privileges
        final Long closedCount = AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Long>() {
            @Override
            public Long doWork() throws Exception {
                return worker.closeAllOpenExpiredVolumes();
            }
        }, AuthenticationUtil.getSystemUserName());

        if (log.isDebugEnabled())  log.debug("VolumeAutomaticClosingJob done, closedCount=" + closedCount);
    }
}
