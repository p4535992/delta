<<<<<<< HEAD
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

/**
 * Triggers the notifications for contracts that are nearing their due date or have unspecified due date.
 * 
 * @author Kaarel Jõgeva
 */
public class ContractDueDateNotificationJob implements StatefulJob {
    private static Log log = LogFactory.getLog(ContractDueDateNotificationJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.debug("Starting ContractDueDateNotificationJob");
        JobDataMap jobData = context.getJobDetail().getJobDataMap();
        Object workerObj = jobData.get("notificationService");
        if (workerObj == null || !(workerObj instanceof NotificationService)) {
            throw new AlfrescoRuntimeException("ContractDueDateNotificationJob data must contain valid 'notificationService' reference, but contained: "
                    + workerObj);
        }
        final NotificationService worker = (NotificationService) workerObj;

        // Run job as with systemUser privileges
        final Integer notificationCount = AuthenticationUtil.runAs(new RunAsWork<Integer>() {
            @Override
            public Integer doWork() throws Exception {
                return worker.processContractDueDateNotifications();
            }
        }, AuthenticationUtil.getSystemUserName());
        // Done
        if (log.isDebugEnabled()) {
            log.debug("ContractDueDateNotificationJob done, sent " + notificationCount + " notifications");
        }
    }

}
=======
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

/**
 * Triggers the notifications for contracts that are nearing their due date or have unspecified due date.
 */
public class ContractDueDateNotificationJob implements StatefulJob {
    private static Log log = LogFactory.getLog(ContractDueDateNotificationJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.debug("Starting ContractDueDateNotificationJob");
        JobDataMap jobData = context.getJobDetail().getJobDataMap();
        Object workerObj = jobData.get("notificationService");
        if (workerObj == null || !(workerObj instanceof NotificationService)) {
            throw new AlfrescoRuntimeException("ContractDueDateNotificationJob data must contain valid 'notificationService' reference, but contained: "
                    + workerObj);
        }
        final NotificationService worker = (NotificationService) workerObj;

        // Run job as with systemUser privileges
        final Integer notificationCount = AuthenticationUtil.runAs(new RunAsWork<Integer>() {
            @Override
            public Integer doWork() throws Exception {
                return worker.processContractDueDateNotifications();
            }
        }, AuthenticationUtil.getSystemUserName());
        // Done
        if (log.isDebugEnabled()) {
            log.debug("ContractDueDateNotificationJob done, sent " + notificationCount + " notifications");
        }
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
