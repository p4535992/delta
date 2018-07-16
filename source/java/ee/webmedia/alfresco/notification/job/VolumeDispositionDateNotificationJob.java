package ee.webmedia.alfresco.notification.job;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import ee.webmedia.alfresco.notification.service.NotificationService;

public class VolumeDispositionDateNotificationJob implements StatefulJob {
    private static Log log = LogFactory.getLog(VolumeDispositionDateNotificationJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.debug("Starting VolumeDispositionDateNotificationJob");
        JobDataMap jobData = context.getJobDetail().getJobDataMap();
        Object workerObj = jobData.get("notificationService");
        if (workerObj == null || !(workerObj instanceof NotificationService)) {
            throw new AlfrescoRuntimeException("VolumeDispositionDateNotificationJob data must contain valid 'notificationService' reference, but contained: "
                    + workerObj);
        }
        final NotificationService worker = (NotificationService) workerObj;

        // Run job as with systemUser privileges
        final Integer notificationCount = AuthenticationUtil.runAs(new RunAsWork<Integer>() {
            @Override
            public Integer doWork() throws Exception {
                return worker.processVolumeDispositionDateNotifications();
            }
        }, AuthenticationUtil.getSystemUserName());
        // Done
        if (log.isDebugEnabled()) {
            log.debug("VolumeDispositionNotificationJob done, sent " + notificationCount + " notifications");
        }
    }

}
