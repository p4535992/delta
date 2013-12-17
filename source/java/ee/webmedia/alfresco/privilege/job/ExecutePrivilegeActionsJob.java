package ee.webmedia.alfresco.privilege.job;

import java.util.List;

import org.alfresco.web.bean.repository.Node;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;

/**
 * @author Alar Kvell
 */
public class ExecutePrivilegeActionsJob implements StatefulJob {
    private static final Log LOG = LogFactory.getLog(ExecutePrivilegeActionsJob.class);

    private PrivilegeService privilegeService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOG.debug("Starting ExecutePrivilegeActionsJob");
        setServices(context);

        while (true) {
            privilegeService.doPausePrivilegeActions();
            List<Node> privilegeActions = privilegeService.getAllInQueuePrivilegeActions();
            if (privilegeActions.isEmpty()) {
                // if there are no privilege actions in queue, quit current execution cycle.
                // The job will be executed again after time stated in bean config (currently one minute)
                break;
            }
            for (Node privilegeAction : privilegeActions) {
                privilegeService.doPausePrivilegeActions();
                privilegeService.doPrivilegeAction(privilegeAction);
            }
        }
        LOG.debug("Finished ExecutePrivilegeActionsJob");
    }

    private void setServices(@SuppressWarnings("unused") JobExecutionContext context) {
        if (privilegeService == null) {
            privilegeService = BeanHelper.getPrivilegeService();
        }
    }

}
