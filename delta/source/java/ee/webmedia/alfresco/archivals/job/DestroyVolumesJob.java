package ee.webmedia.alfresco.archivals.job;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import ee.webmedia.alfresco.archivals.service.ArchivalsService;

/**
 * Scheduled job to call a {@link ee.webmedia.alfresco.archivals.service.ArchivalsService#destroyArchivedVolumes()}.
 * <p>
 * Job data is: <b>archivalsService</b>
 * 
 * @author Romet Aidla
 */
public class DestroyVolumesJob implements Job {
    private static Log log = LogFactory.getLog(DestroyVolumesJob.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (log.isDebugEnabled()) {
            log.debug("Starting DestroyVolumesJob");
        }
        JobDataMap jobData = jobExecutionContext.getJobDetail().getJobDataMap();
        Object workerObj = jobData.get("archivalsService");

        if (workerObj == null || !(workerObj instanceof ArchivalsService)) {
            throw new AlfrescoRuntimeException("DestroyVolumesJob data must contain valid 'archivalsService' reference, but contained: "
                    + workerObj);
        }
        final ArchivalsService worker = (ArchivalsService) workerObj;

        // Run job as with systemUser privileges
        final Integer updateCount = AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Integer>() {
            @Override
            public Integer doWork() throws Exception {
                return worker.destroyArchivedVolumes();
            }
        }, AuthenticationUtil.getSystemUserName());

        if (log.isDebugEnabled()) {
            log.debug("DestroyVolumesJob done, updateCount=" + updateCount);
        }
    }
}
