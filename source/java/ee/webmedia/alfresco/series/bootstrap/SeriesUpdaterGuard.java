package ee.webmedia.alfresco.series.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.common.web.BeanHelper;

/**
 * Updater finishes when seriesUpdater (running in separate tread) has finished.
 * 
 * @author Riina Tens
 */
public class SeriesUpdaterGuard extends AbstractModuleComponent {

    private static final Log LOG = LogFactory.getLog(SeriesUpdaterGuard.class);
    private boolean enabled;
    private SeriesUpdater seriesUpdater;

    @Override
    protected void executeInternal() throws Throwable {
        if (enabled) {
            LOG.info("Waiting for SeriesUpdater to finish.");
            while (seriesUpdater.isUpdaterRunning()) {
                LOG.info("SeriesUpdater is still running, waiting another minute...");
                Thread.sleep(60000);
            }
            if (seriesUpdater.isErrorExecutingUpdaterInBackground()) {
                BeanHelper.getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {

                    @Override
                    public Void execute() throws Throwable {
                        NodeRef seriesBootstrapRef = BeanHelper.getGeneralService().deleteBootstrapNodeRef("simdhs", "seriesBootstrap2");
                        if (seriesBootstrapRef == null) {
                            LOG.info("Could not delete module 'seriesBootstrap2'");
                        } else {
                            LOG.info("Deleted module 'seriesBootstrap2'");
                        }
                        return null;
                    }
                }, false, true);
                throw new RuntimeException("Error occured while executing seriesUpdater, cancelling application startup.");
            }
            LOG.info("SeriesUpdater finished, continuing main thread.");
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setSeriesUpdater(SeriesUpdater seriesUpdater) {
        this.seriesUpdater = seriesUpdater;
    }

}
