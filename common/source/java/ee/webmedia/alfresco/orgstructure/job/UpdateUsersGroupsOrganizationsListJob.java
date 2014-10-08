package ee.webmedia.alfresco.orgstructure.job;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.sync.UserRegistrySynchronizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;

/**
 * Scheduled job to call a {@link UserRegistrySynchronizer#synchronize()} and {@link OrganizationStructureService#updateOrganisationStructures()}.
 * <p>
 * Job data is: <b>OrganizationStructureService</b>
 */
public class UpdateUsersGroupsOrganizationsListJob implements StatefulJob {
    private static Log log = LogFactory.getLog(UpdateUsersGroupsOrganizationsListJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.debug("Starting UpdateUsersGroupsOrganizationsListJob");
        JobDataMap jobData = context.getJobDetail().getJobDataMap();

        // Run the jobs
        updateOrganisationStructure(jobData);
        updateUsersAndGroups(jobData);
        updateOrganisationStructureBasedGroups(jobData);

        if (log.isDebugEnabled()) {
            log.debug("UpdateUsersGroupsOrganizationsListJob done");
        }
    }

    private void updateOrganisationStructureBasedGroups(JobDataMap jobData) {
        log.debug("Starting to update organization structure based groups");

        Object workerObj = jobData.get("organizationStructureService");
        if (workerObj == null || !(workerObj instanceof OrganizationStructureService)) {
            throw new AlfrescoRuntimeException("UpdateUsersGroupsOrganizationsListJob data must contain valid 'organizationStructureService' reference, but contained: "
                    + workerObj);
        }
        final OrganizationStructureService worker = (OrganizationStructureService) workerObj;

        // Run job as with systemUser privileges
        AuthenticationUtil.runAs(new RunAsWork<Integer>() {
            @Override
            public Integer doWork() throws Exception {
                return worker.updateOrganisationStructureBasedGroups();
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    private void updateOrganisationStructure(JobDataMap jobData) {
        log.debug("Starting to update organization structure");

        Object workerObj = jobData.get("organizationStructureService");
        if (workerObj == null || !(workerObj instanceof OrganizationStructureService)) {
            throw new AlfrescoRuntimeException("UpdateUsersGroupsOrganizationsListJob data must contain valid 'organizationStructureService' reference, but contained: "
                    + workerObj);
        }
        final OrganizationStructureService worker = (OrganizationStructureService) workerObj;

        // Run job as with systemUser privileges
        AuthenticationUtil.runAs(new RunAsWork<Integer>() {
            @Override
            public Integer doWork() throws Exception {
                return worker.updateOrganisationStructures();
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    private void updateUsersAndGroups(JobDataMap jobData) {
        log.debug("Starting to update users and usergroups");

        final UserRegistrySynchronizer userRegistrySynchronizer = (UserRegistrySynchronizer) jobData.get("userRegistrySynchronizer");
        final String synchronizeChangesOnly = (String) jobData.get("synchronizeChangesOnly");
        AuthenticationUtil.runAs(new RunAsWork<Object>()
                {
            @Override
            public Object doWork() throws Exception
            {
                userRegistrySynchronizer.synchronize(synchronizeChangesOnly == null
                        || !Boolean.parseBoolean(synchronizeChangesOnly));
                return null;
            }
                }, AuthenticationUtil.getSystemUserName());
    }
}
