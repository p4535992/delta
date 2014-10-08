package ee.webmedia.alfresco.substitute.bootstrap;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import ee.webmedia.alfresco.common.web.BeanHelper;

public class ClearSubstituteCahceJob implements StatefulJob {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ClearSubstituteCahceJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Starting ClearSubstituteCahceJob");
        }
        try {
            final String message = AuthenticationUtil.runAs(new RunAsWork<String>() {
                @Override
                public String doWork() throws Exception {
                    return BeanHelper.getSubstituteService().clearCache();
                }
            }, AuthenticationUtil.getSystemUserName());
            if (LOG.isDebugEnabled()) {
                LOG.debug(message);
            }
        } catch (RuntimeException e) {
            LOG.error("ClearSubstituteCahceJob failed: " + e.getMessage(), e);
            throw e;
        }

    }

}