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

import ee.webmedia.alfresco.addressbook.service.AddressbookService;
import ee.webmedia.alfresco.dvk.service.DvkService;

/**
 * Scheduled job to call a {@link AddressbookService#updateOrganizationsDvkCapability()}.
 * <p>
 * Job data is: <b>AddressbookService</b>
 */
public class DvkReceiveOrganizationsJob implements StatefulJob {
    private static Log log = LogFactory.getLog(DvkReceiveOrganizationsJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.debug("Starting DvkReceiveOrganizationsJob");
        JobDataMap jobData = context.getJobDetail().getJobDataMap();
        Object workerObj = jobData.get(DvkService.BEAN_NAME);
        if (workerObj == null || !(workerObj instanceof DvkService)) {
            throw new AlfrescoRuntimeException("DvkReceiveOrganizationsJob data must contain valid 'DvkService' reference");
        }
        final DvkService worker = (DvkService) workerObj;

        // Run job as with systemUser privileges
        final Integer nrOfDvkCapableOrgs = AuthenticationUtil.runAs(new RunAsWork<Integer>() {
            @Override
            public Integer doWork() throws Exception {
                return worker.updateOrganizationsDvkCapability();
            }
        }, AuthenticationUtil.getSystemUserName());
        if (log.isDebugEnabled()) {
            log.debug("DvkReceiveOrganizationsJob done, " + nrOfDvkCapableOrgs
                    + " organizations in the addressbook are capable of receiving documents using DVK");
        }
    }
}
