package ee.webmedia.alfresco.maais.job;

import java.util.HashSet;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import ee.webmedia.alfresco.common.listener.SessionListener;
import ee.webmedia.alfresco.maais.service.MaaisService;

/**
 * @author Keit Tehvan
 */
public class UpdateMaaisAuthJob implements StatefulJob {
    private static Log log = LogFactory.getLog(UpdateMaaisAuthJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.debug("Starting UpdateMaaisAuthJob");
        JobDataMap jobData = context.getJobDetail().getJobDataMap();
        Object workerObj = jobData.get("maaisService");
        if (workerObj == null || !(workerObj instanceof MaaisService)) {
            throw new AlfrescoRuntimeException("UpdateMaaisAuthJob data must contain valid 'MaaisService' reference, but contained: "
                    + workerObj);
        }
        final MaaisService worker = (MaaisService) workerObj;
        if (!worker.isServiceAvailable()) {
            log.debug("Service not available, quitting");
            return;
        }
        final Set<String> onlineUsers = new HashSet<String>(SessionListener.loggedInUsers);

        // Run job as with systemUser privileges
        final Integer updateCount = AuthenticationUtil.runAs(new RunAsWork<Integer>() {
            @Override
            public Integer doWork() throws Exception {
                for (String entry : onlineUsers) {
                    worker.updateAuth(entry);
                }
                return onlineUsers.size();
            }
        }, AuthenticationUtil.getSystemUserName());
        // Done
        if (log.isDebugEnabled()) {
            log.debug("UpdateMaaisAuthJob done, updateCount=" + updateCount);
        }
    }
}
