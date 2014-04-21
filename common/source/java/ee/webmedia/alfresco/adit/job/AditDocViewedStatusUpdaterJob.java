package ee.webmedia.alfresco.adit.job;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.quartz.JobDataMap;

import ee.webmedia.alfresco.adit.service.AditService;
import ee.webmedia.alfresco.dvk.job.AbstractDvkDocStatusesJob;

public class AditDocViewedStatusUpdaterJob extends AbstractDvkDocStatusesJob {

    @Override
    protected Integer doWork(JobDataMap jobData, String className) {
        Object workerObj = jobData.get(AditService.BEAN_NAME);
        if (workerObj == null || !(workerObj instanceof AditService)) {
            throw new AlfrescoRuntimeException(className + " data must contain valid 'AditService' reference");
        }
        final AditService worker = (AditService) workerObj;
        final Integer statusesUpdated = AuthenticationUtil.runAs(new RunAsWork<Integer>() {
            @Override
            public Integer doWork() throws Exception {
                return worker.updateAditDocViewedStatuses();
            }
        }, AuthenticationUtil.getSystemUserName());
        return statusesUpdated;
    }
}
