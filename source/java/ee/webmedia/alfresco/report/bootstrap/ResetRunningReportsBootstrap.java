<<<<<<< HEAD
package ee.webmedia.alfresco.report.bootstrap;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.report.model.ReportModel;
import ee.webmedia.alfresco.report.model.ReportStatus;
import ee.webmedia.alfresco.report.service.ReportService;

/**
 * Reset all reports with status RUNNING to status IN_QUEUE.
 * 
 * @author Riina Tens
 */
public class ResetRunningReportsBootstrap extends AbstractModuleComponent {
    private static final Log LOG = LogFactory.getLog(ResetRunningReportsBootstrap.class);
    private ReportService reportService;
    private NodeService nodeService;

    @Override
    protected void executeInternal() throws Throwable {
        List<NodeRef> reportRefs = reportService.getAllRunningReports();
        for (NodeRef reportRef : reportRefs) {
            Map<QName, Serializable> reportProps = nodeService.getProperties(reportRef);
            nodeService.setProperty(reportRef, ReportModel.Props.STATUS, ReportStatus.IN_QUEUE.toString());
            LOG.info("Resetting report status to IN_QUEUE for report nodeRef=" + reportRef + ", user=" + reportProps.get(ReportModel.Props.USERNAME) + ", reportName="
                    + reportProps.get(ReportModel.Props.REPORT_NAME));
        }
    }

    public void setReportService(ReportService reportService) {
        this.reportService = reportService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

}
=======
package ee.webmedia.alfresco.report.bootstrap;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.report.model.ReportModel;
import ee.webmedia.alfresco.report.model.ReportStatus;
import ee.webmedia.alfresco.report.service.ReportService;

/**
 * Reset all reports with status RUNNING to status IN_QUEUE.
 */
public class ResetRunningReportsBootstrap extends AbstractModuleComponent {
    private static final Log LOG = LogFactory.getLog(ResetRunningReportsBootstrap.class);
    private ReportService reportService;
    private NodeService nodeService;

    @Override
    protected void executeInternal() throws Throwable {
        List<NodeRef> reportRefs = reportService.getAllRunningReports();
        for (NodeRef reportRef : reportRefs) {
            Map<QName, Serializable> reportProps = nodeService.getProperties(reportRef);
            nodeService.setProperty(reportRef, ReportModel.Props.STATUS, ReportStatus.IN_QUEUE.toString());
            LOG.info("Resetting report status to IN_QUEUE for report nodeRef=" + reportRef + ", user=" + reportProps.get(ReportModel.Props.USERNAME) + ", reportName="
                    + reportProps.get(ReportModel.Props.REPORT_NAME));
        }
    }

    public void setReportService(ReportService reportService) {
        this.reportService = reportService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

}
>>>>>>> develop-5.1
