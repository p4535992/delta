package ee.webmedia.alfresco.dvk.job;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

public abstract class AbstractDvkDocStatusesJob implements StatefulJob {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(AbstractDvkDocStatusesJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String className = getClass().getSimpleName();
        LOG.debug("Starting " + className);
        JobDataMap jobData = context.getJobDetail().getJobDataMap();
        final Integer statusesUpdated = doWork(jobData, className);
        // Done
        if (LOG.isDebugEnabled()) {
            LOG.debug(className + " done, updated " + statusesUpdated + " statuses of sent documents.");
        }
    }

    protected abstract Integer doWork(JobDataMap jobData, String className);

}