package ee.webmedia.alfresco.dvk.job;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.quartz.JobDataMap;

import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.dvk.service.DvkServiceImpl;

/**
 * Scheduled job to call a {@link DvkServiceImpl#XXXXXXXXXXXX}.
 * <p>
 * Job data is: <b>DvkService</b>
 */
public class DvkReceiveDocSendStatusesJob extends AbstractDvkDocStatusesJob {

    @Override
    protected Integer doWork(JobDataMap jobData, String className) {
        Object workerObj = jobData.get(DvkService.BEAN_NAME);
        if (workerObj == null || !(workerObj instanceof DvkService)) {
            throw new AlfrescoRuntimeException(className + " data must contain valid 'DvkService' reference");
        }
        final DvkService worker = (DvkService) workerObj;
        final Integer statusesUpdated = AuthenticationUtil.runAs(new RunAsWork<Integer>() {
            @Override
            public Integer doWork() throws Exception {
                return worker.updateDocAndTaskSendStatuses();
            }
        }, AuthenticationUtil.getSystemUserName());

        worker.deleteForwardedDecDocuments();
        return statusesUpdated;
    }
}
