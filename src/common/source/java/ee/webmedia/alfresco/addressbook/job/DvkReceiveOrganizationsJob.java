package ee.webmedia.alfresco.addressbook.job;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import ee.webmedia.alfresco.addressbook.service.AddressbookService;

/**
 * Scheduled job to call a {@link AddressbookService#updateOrganizationsDvkCapability()}.
 * <p>
 * Job data is: <b>AddressbookService</b>
 * 
 * @author Ats Uiboupin
 */
public class DvkReceiveOrganizationsJob implements Job {
    private static Log log = LogFactory.getLog(DvkReceiveOrganizationsJob.class);

    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.debug("Starting DvkReceiveOrganizationsJob");
        JobDataMap jobData = context.getJobDetail().getJobDataMap();
        Object workerObj = jobData.get(AddressbookService.BEAN_NAME);
        if (workerObj == null || !(workerObj instanceof AddressbookService)) {
            throw new AlfrescoRuntimeException("DvkReceiveOrganizationsJob data must contain valid 'AddressbookService' reference");
        }
        final AddressbookService worker = (AddressbookService) workerObj;

        // Run job as with systeUser privileges
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
