package ee.webmedia.alfresco.dvk.job;

import java.util.Collection;

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
 * Scheduled job to call a {@link DvkServiceImpl#receiveDocuments()}.
 * <p>
 * Job data is: <b>DvkService</b>
 */
public class DvkReceiveDocumentsJob implements StatefulJob {
    private static Log log = LogFactory.getLog(DvkReceiveDocumentsJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.debug("Starting DvkReceiveDocumentsJob");
        JobDataMap jobData = context.getJobDetail().getJobDataMap();
        Object workerObj = jobData.get(DvkService.BEAN_NAME);
        if (workerObj == null || !(workerObj instanceof DvkService)) {
            throw new AlfrescoRuntimeException("DvkReceiveDocumentsJob data must contain valid 'DvkService' reference");
        }
        final DvkService worker = (DvkService) workerObj;

        // Run job as with systemUser privileges
        final Collection<String> receivedDocumentDhlIds = AuthenticationUtil.runAs(new RunAsWork<Collection<String>>() {
            @Override
            public Collection<String> doWork() throws Exception {
                Collection<String> receiveDocuments = null;
                try {
                    receiveDocuments = worker.receiveDocuments();
                } catch (Throwable e) { // Catch-all for possible exceptions that may slip out
                    String message = "Worker job threw an exception! " + e.getMessage();
                    log.error(message, e);
                    log.error("DvkReceiveDocumentsJob failed!");
                    throw new JobExecutionException(message, e);
                }
                return receiveDocuments;
            }
        }, AuthenticationUtil.getSystemUserName());
        // Done
        if (log.isDebugEnabled()) {
            log.debug("DvkReceiveDocumentsJob done, received " + receivedDocumentDhlIds.size() + " documents: " + receivedDocumentDhlIds);
        }
    }
}
