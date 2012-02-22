package ee.webmedia.alfresco.report.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.report.model.ReportDataCollector;
import ee.webmedia.alfresco.report.model.ReportType;

/**
 * @author Riina Tens
 */
public interface ReportService {

    String BEAN_NAME = "ReportService";

    ReportDataCollector getReportFileInMemory(ReportDataCollector reportDataCollector);

    NodeRef createReportResult(Node filter, ReportType reportType, QName parentToChildAssoc);

    List<NodeRef> getAllRunningReports();

    List<Node> getAllInQueueReports();

    void markReportRunning(NodeRef reportRef);

    NodeRef completeReportResult(ReportDataCollector reportDataProvider);

}
