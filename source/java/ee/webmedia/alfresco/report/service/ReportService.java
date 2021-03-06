package ee.webmedia.alfresco.report.service;

import java.io.Serializable;
import java.util.List;

import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.TemplateReportType;
import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.report.model.ReportDataCollector;

public interface ReportService {

    String BEAN_NAME = "ReportService";

    ReportDataCollector getReportFileInMemory(ReportDataCollector reportDataCollector);

    NodeRef createReportResult(Node filter, TemplateReportType reportType, QName parentToChildAssoc);

    NodeRef createCsvReportResult(NodeRef nodeRef);

    List<NodeRef> getAllRunningReports();

    List<Pair<NodeRef, Serializable>> getAllInQueueReports();

    List<Pair<NodeRef, Serializable>> getAllInQueueReportsWithOrderNumbers();

    void markReportRunning(NodeRef reportRef);

    NodeRef completeReportResult(ReportDataCollector reportDataProvider);

    /**
     * Check if given file's parent is of type reportResult and if it is, changes parent's status if needed.
     * NB! This method is executed on every file download (also for files not stored under reportResult object)
     */
    void markReportDownloaded(NodeRef fileRef);

    List<ReportResult> getReportResultsForUser(String username);

    FileInfo getReportResultFileName(NodeRef reportResultRef);

    /**
     * Check if reportResult is still in general report queue (not under person's reports folder)
     * and if it is, set it's status CANCELLING_REQUESTED.
     * Otherwise set report's status to CANCELLED (this may happen if user requested cancelling,
     * but meanwhile report execution moved report under person's folder,
     * so execution job is not going to update status to CANCELLED)
     */
    void enqueueReportForCancelling(NodeRef reportRef);

    /**
     * If report is under person's report folder, delete it.
     * Otherwise (i.e. report is in general report execution queue) set report status to DELETING_REQUESTED.
     */
    void enqueueReportForDeleting(NodeRef reportRef);

    void deleteReportResult(NodeRef reportResultRef);

    boolean isReportGenerationEnabled();

    boolean isReportGenerationPaused();

    void setReportGenerationPaused(boolean reportGenerationPaused);

    /**
     * Calls Thread.sleep while reportGenerationPaused=true.
     */
    void doPauseReportGeneration();

    boolean isUsableByAdminDocManagerOnly();

    NodeRef getReportsSpaceRef();

    BulkLoadNodeService getBulkLoadNodeService();
}
