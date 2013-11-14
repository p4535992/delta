package ee.webmedia.alfresco.common.job;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.alfresco.repo.search.impl.lucene.AbstractLuceneIndexerAndSearcherFactory.LuceneIndexBackupJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import ee.webmedia.alfresco.common.bootstrap.InvalidNodeFixerBootstrap;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.privilege.bootstrap.FixAclInheritanceUpdater;

/**
 * Finds and fixes problems in data. Runs before {@link LuceneIndexBackupJob}, and prevents {@link LuceneIndexBackupJob} frmo starting before this job completes.
 * 
 * @author Alar Kvell
 */
public class NightlyDataFixJob implements StatefulJob {

    public static Lock nightlyMaintenanceJobLock = new ReentrantLock();

    @Override
    public void execute(JobExecutionContext arg0) throws JobExecutionException {
        nightlyMaintenanceJobLock.lock();
        try {
            InvalidNodeFixerBootstrap invalidNodeFixerBootstrap = BeanHelper.getSpringBean(InvalidNodeFixerBootstrap.class, "invalidNodeFixerBootstrap");
            if (!invalidNodeFixerBootstrap.isNewMaintenanceJobsEnabled()) {
                return;
            }
            invalidNodeFixerBootstrap.execute();

            FixAclInheritanceUpdater fixAclInheritanceUpdater = BeanHelper.getSpringBean(FixAclInheritanceUpdater.class, "fixAclInheritanceUpdater2");
            fixAclInheritanceUpdater.fixAllAclsThatInheritFromNonPrimaryParent();
        } finally {
            nightlyMaintenanceJobLock.unlock();
        }
    }

}
