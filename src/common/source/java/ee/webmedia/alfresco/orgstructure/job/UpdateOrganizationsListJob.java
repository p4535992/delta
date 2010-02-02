package ee.webmedia.alfresco.orgstructure.job;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;

/**
 * Scheduled job to call a {@link OrganizationStructureService#updateOrganisationStructures()}.
 * <p>
 * Job data is: <b>OrganizationStructureService</b>
 * 
 * @author Ats Uiboupin
 */
public class UpdateOrganizationsListJob implements StatefulJob {
    private static Log log = LogFactory.getLog(UpdateOrganizationsListJob.class);

    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.debug("Starting UpdateOrganizationsListJob");
        JobDataMap jobData = context.getJobDetail().getJobDataMap();
        Object workerObj = jobData.get("organizationStructureService");
        if (workerObj == null || !(workerObj instanceof OrganizationStructureService)) {
            throw new AlfrescoRuntimeException("UpdateOrganizationsListJob data must contain valid 'organizationStructureService' reference, but contained: "
                    + workerObj);
        }
        final OrganizationStructureService worker = (OrganizationStructureService) workerObj;

        // Run job as with systemUser privileges
        final Integer updateCount = AuthenticationUtil.runAs(new RunAsWork<Integer>() {
            @Override
            public Integer doWork() throws Exception {
                return worker.updateOrganisationStructures();
            }
        }, AuthenticationUtil.getSystemUserName());
        // Done
        if (log.isDebugEnabled()) {
            log.debug("UpdateOrganizationsListJob done, updateCount=" + updateCount);
        }
    }
}
