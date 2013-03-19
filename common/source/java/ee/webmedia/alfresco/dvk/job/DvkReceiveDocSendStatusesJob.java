package ee.webmedia.alfresco.dvk.job;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.dvk.service.DvkServiceImpl;

/**
 * Scheduled job to call a {@link DvkServiceImpl#XXXXXXXXXXXX}.
 * <p>
 * Job data is: <b>DvkService</b>
 * 
 * @author Ats Uiboupin
 */
public class DvkReceiveDocSendStatusesJob implements StatefulJob {
    private static Log log = LogFactory.getLog(DvkReceiveDocSendStatusesJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.debug("Starting DvkReceiveDocSendStatusesJob");
        JobDataMap jobData = context.getJobDetail().getJobDataMap();
        Object workerObj = jobData.get(DvkService.BEAN_NAME);
        if (workerObj == null || !(workerObj instanceof DvkService)) {
            throw new AlfrescoRuntimeException("DvkReceiveDocSendStatusesJob data must contain valid 'DvkService' reference");
        }
        final DvkService worker = (DvkService) workerObj;

        // Run job as with systemUser privileges
        final Integer statusesUpdated = AuthenticationUtil.runAs(new RunAsWork<Integer>() {
            @Override
            public Integer doWork() throws Exception {
                return worker.updateDocAndTaskSendStatuses();
            }
        }, AuthenticationUtil.getSystemUserName());
        // Done
        if (log.isDebugEnabled()) {
            log.debug("DvkReceiveDocSendStatusesJob done, updated " + statusesUpdated + " statuses of sent documents.");
        }
    }
}
