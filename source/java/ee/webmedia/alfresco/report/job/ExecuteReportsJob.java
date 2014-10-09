package ee.webmedia.alfresco.report.job;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.Pair;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.classificator.enums.TemplateReportType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.report.model.ReportDataCollector;
import ee.webmedia.alfresco.report.model.ReportModel;
import ee.webmedia.alfresco.report.model.ReportStatus;
import ee.webmedia.alfresco.report.service.ReportService;
import ee.webmedia.alfresco.utils.Transformer;
import ee.webmedia.alfresco.utils.UnableToPerformException;

public class ExecuteReportsJob implements StatefulJob {
    private static final Log LOG = LogFactory.getLog(ExecuteReportsJob.class);

    public static final Lock REORDER_LOCK = new ReentrantLock();

    private ReportService reportService;
    private TransactionService transactionService;
    private NodeService nodeService;

    @SuppressWarnings("unchecked")
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOG.debug("Starting ExecuteReportsJob");
        setServices(context);
        RetryingTransactionHelper retryingTransactionHelper = transactionService.getRetryingTransactionHelper();
        RetryingTransactionCallback<List<Pair<NodeRef, Serializable>>> getAllExecutableReportsCallback = new GetAllExecutableReportsCallback();
        ReportDataCollector reportDataProvider = new ReportDataCollector();
        RetryingTransactionCallback<Object> markReportRunningCallback = new MarkReportRunningCallback(reportDataProvider);
        RetryingTransactionCallback<Object> createReportInMemoryCallback = new CreateReportInMemoryCallback(reportDataProvider);
        RetryingTransactionCallback<Object> addReportResultFileCallback = new AddReportResultFileCallback(reportDataProvider);
        RetryingTransactionCallback<Void> reorderReportJobs = new ReorderReportsCallback();
        TransformingComparator comparator = new TransformingComparator(new Transformer<Pair<NodeRef, Serializable>, Date>() {
            @Override
            public Date tr(Pair<NodeRef, Serializable> pair) {
                return (Date) pair.getSecond();
            }
        }, new NullComparator());
        while (true) {
            reportService.doPauseReportGeneration();
            // retrieve all reports with status IN_QUEUE (read-only transaction)
            final List<Pair<NodeRef, Serializable>> reports = retryingTransactionHelper.doInTransaction(getAllExecutableReportsCallback, true, true);
            if (reports.isEmpty()) {
                // if there are no reports in queue, quit current execution cycle.
                // The job will be executed again after time stated in bean config (currently one minute)
                break;
            }
            Collections.sort(reports, comparator);
            for (Pair<NodeRef, Serializable> report : reports) {
                reportDataProvider.reset();
                final NodeRef reportResultRef = report.getFirst();
                try {
                    REORDER_LOCK.lock();
                    retryingTransactionHelper.doInTransaction(reorderReportJobs, false, true);
                } finally {
                    REORDER_LOCK.unlock();
                }
                reportDataProvider.setReportResultNodeRef(reportResultRef);
                reportDataProvider.setReportResultProps(nodeService.getProperties(reportResultRef));
                // check if user has marked current report for deleting or cancelling
                ReportStatus currentStatus = ReportStatus.valueOf((String) reportDataProvider.getReportResultProps().get(ReportModel.Props.STATUS));
                if (ReportStatus.DELETING_REQUESTED == currentStatus) {
                    reportDataProvider.setResultStatus(ReportStatus.DELETED);
                } else if (ReportStatus.CANCELLING_REQUESTED == currentStatus) {
                    reportDataProvider.setResultStatus(ReportStatus.CANCELLED);
                } else {
                    try {
                        // mark report as running (new rw transaction)
                        retryingTransactionHelper.doInTransaction(markReportRunningCallback, false, true);
                        // retrieve results in memory (new ro transaction)
                        retryingTransactionHelper.doInTransaction(createReportInMemoryCallback, false, true);
                        LOG.info("Successfully created file in memory for reportResult nodeRef=" + reportResultRef);
                    } catch (UnableToPerformException e) {
                        LOG.error("Unable to execute reportResult nodeRef=" + reportResultRef + ", setting status to FAILED.", e);
                        reportDataProvider.setResultStatus(ReportStatus.FAILED);
                    }
                }
                // if deleting is requested, delete repostResult,
                // otherwise write results to repo, move node to appropriate location and set required reportResult status (new rw transaction)
                Object result = retryingTransactionHelper.doInTransaction(addReportResultFileCallback, false, true);
                if (result != null) {
                    final NodeRef reportResultNodeRef = (NodeRef) result;
                    retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {

                        @Override
                        public Void execute() throws Throwable {
                            FileInfo fileInfo = BeanHelper.getReportService().getReportResultFileName(reportResultNodeRef);
                            if (fileInfo != null) {
                                Map<QName, Serializable> props = new HashMap<>();
                                props.put(ReportModel.Props.REPORT_RESULT_FILE_NAME, fileInfo.getName());
                                props.put(ReportModel.Props.REPORT_RESULT_FILE_REF, fileInfo.getNodeRef());
                                nodeService.addProperties(reportResultNodeRef, props);
                            }
                            return null;
                        }
                    });
                }
            }
        }
    }

    private void setServices(JobExecutionContext context) {
        reportService = BeanHelper.getReportService();
        transactionService = BeanHelper.getTransactionService();
        nodeService = BeanHelper.getNodeService();
    }

    private class ReorderReportsCallback implements RetryingTransactionCallback<Void> {

        @Override
        public Void execute() throws Throwable {
            List<Pair<NodeRef, Serializable>> reports = reportService.getAllInQueueReportsWithOrderNumbers();
            for (Pair<NodeRef, Serializable> report : reports) {
                NodeRef reportRef = report.getFirst();
                Integer orderInQueue = (Integer) report.getSecond();
                if (orderInQueue != null) {
                    if (orderInQueue > 1) {
                        nodeService.setProperty(reportRef, ReportModel.Props.ORDER_IN_QUEUE, orderInQueue - 1);
                    } else {
                        nodeService.removeProperty(reportRef, ReportModel.Props.ORDER_IN_QUEUE);
                    }
                }
            }
            return null;
        }

    }

    private class GetAllExecutableReportsCallback implements RetryingTransactionCallback<List<Pair<NodeRef, Serializable>>> {

        @Override
        public List<Pair<NodeRef, Serializable>> execute() throws Throwable {
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
            Map<QName, Serializable> props = reportDataProvider.getReportResultProps();
            boolean resultCancelled = ReportStatus.CANCELLED == resultStatus || ReportStatus.FAILED == resultStatus || ReportStatus.DELETED == resultStatus;
            boolean isConsolidatedList = (props != null) && (props.get(ReportModel.Props.REPORT_TYPE) != null)
                    && TemplateReportType.CONSOLIDATED_LIST.name().equals(props.get(ReportModel.Props.REPORT_TYPE));
            Assert.isTrue(resultCancelled || reportDataProvider.getWorkbook() != null || isConsolidatedList);
            Assert.isTrue(resultCancelled || reportDataProvider.getEncoding() != null);
            return reportService.completeReportResult(reportDataProvider);
        }
    }

}
