package ee.webmedia.alfresco.report.job;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.report.model.ReportDataCollector;
import ee.webmedia.alfresco.report.model.ReportModel;
import ee.webmedia.alfresco.report.model.ReportStatus;
import ee.webmedia.alfresco.report.service.ReportService;
import ee.webmedia.alfresco.utils.Transformer;
import ee.webmedia.alfresco.utils.UnableToPerformException;

/**
 * @author Riina Tens
 */
public class ExecuteReportsJob implements StatefulJob {
    private static final Log LOG = LogFactory.getLog(ExecuteReportsJob.class);

    private ReportService reportService;
    private TransactionService transactionService;

    @SuppressWarnings("unchecked")
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOG.debug("Starting ExecuteReportsJob");
        setServices(context);
        RetryingTransactionHelper retryingTransactionHelper = transactionService.getRetryingTransactionHelper();
        RetryingTransactionCallback<List<Node>> getAllRunningReportsCallback = new GetAllRunningReportsCallback();
        ReportDataCollector reportDataProvider = new ReportDataCollector();
        RetryingTransactionCallback<Object> markReportRunningCallback = new MarkReportRunningCallback(reportDataProvider);
        RetryingTransactionCallback<Object> createReportInMemoryCallback = new CreateReportInMemoryCallback(reportDataProvider);
        RetryingTransactionCallback<Object> addReportResultFileCallback = new AddReportResultFileCallback(reportDataProvider);
        TransformingComparator comparator = new TransformingComparator(new Transformer<Node, Date>() {
            @Override
            public Date tr(Node node) {
                return (Date) node.getProperties().get(ReportModel.Props.USER_START_DATE_TIME);
            }
        }, new NullComparator());
        while (true) {
            // TODO: Riina - add check if report generation has been globally paused (CL task 178352)

            // retrieve all reports with status IN_QUEUE (read-only transaction)
            List<Node> reports = retryingTransactionHelper.doInTransaction(getAllRunningReportsCallback, true, true);
            if (reports.isEmpty()) {
                // if there are no reports in queue, quit current execution cycle.
                // The job will be executed again after time stated in bean config (currently one minute)
                break;
            }
            Collections.sort(reports, comparator);
            for (Node report : reports) {
                // TODO: Riina - add check if current report has been marked for cancelling (CL task 178352)
                reportDataProvider.reset();
                NodeRef reportResultRef = report.getNodeRef();
                reportDataProvider.setReportResultNodeRef(reportResultRef);
                try {
                    // mark report as running (new rw transaction)
                    retryingTransactionHelper.doInTransaction(markReportRunningCallback, false, true);
                    // retrieve results in memory (new ro transaction)
                    retryingTransactionHelper.doInTransaction(createReportInMemoryCallback, true, true);
                    LOG.info("Successfully created file in memory for reportResult nodeRef=" + reportResultRef);
                } catch (UnableToPerformException e) {
                    LOG.error("Unable to execute reportResult nodeRef=" + reportResultRef + ", setting status to FAILED.", e);
                    reportDataProvider.setResultStatus(ReportStatus.FAILED);
                }
                // write results to repo, move node to appropriate location and set reportResult status (new rw transaction)
                retryingTransactionHelper.doInTransaction(addReportResultFileCallback, false, true);
            }
        }
    }

    private void setServices(JobExecutionContext context) {
        reportService = BeanHelper.getReportService();
        transactionService = BeanHelper.getTransactionService();
    }

    private class GetAllRunningReportsCallback implements RetryingTransactionCallback<List<Node>> {

        @Override
        public List<Node> execute() throws Throwable {
            return reportService.getAllInQueueReports();
        }

    }

    private abstract class ReportDataProviderCallback implements RetryingTransactionCallback<Object> {
        protected final ReportDataCollector reportDataProvider;

        public ReportDataProviderCallback(ReportDataCollector reportDataProvider) {
            Assert.notNull(reportDataProvider);
            this.reportDataProvider = reportDataProvider;
        }

        @Override
        public Object execute() throws Throwable {
            Assert.notNull(reportDataProvider.getReportResultNodeRef());
            return processNodeRef(reportDataProvider);
        }

        public abstract Object processNodeRef(ReportDataCollector reportDataProvider);

    }

    private class MarkReportRunningCallback extends ReportDataProviderCallback {

        public MarkReportRunningCallback(ReportDataCollector reportDataProvider) {
            super(reportDataProvider);
        }

        @Override
        public Object processNodeRef(ReportDataCollector reportDataProvider) {
            reportService.markReportRunning(reportDataProvider.getReportResultNodeRef());
            return null;
        }
    }

    private class CreateReportInMemoryCallback extends ReportDataProviderCallback {

        public CreateReportInMemoryCallback(ReportDataCollector reportDataProvider) {
            super(reportDataProvider);
        }

        @Override
        public Object processNodeRef(ReportDataCollector reportDataProvider) {
            // TODO: Riina - in following call, add check for cancelling (pass global list of cancelled reports?) (CL task 178352)
            return reportService.getReportFileInMemory(reportDataProvider);
        }
    }

    private class AddReportResultFileCallback extends ReportDataProviderCallback {

        public AddReportResultFileCallback(ReportDataCollector reportDataProvider) {
            super(reportDataProvider);
        }

        @Override
        public Object processNodeRef(ReportDataCollector reportDataProvider) {
            ReportStatus resultStatus = reportDataProvider.getResultStatus();
            Assert.notNull(resultStatus);
            boolean resultCancelled = ReportStatus.CANCELLED.equals(resultStatus) || ReportStatus.FAILED.equals(resultStatus);
            Assert.isTrue(resultCancelled || reportDataProvider.getWorkbook() != null);
            Assert.isTrue(resultCancelled || reportDataProvider.getEncoding() != null);
            return reportService.completeReportResult(reportDataProvider);
        }
    }

}
