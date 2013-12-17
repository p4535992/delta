package ee.webmedia.alfresco.report.model;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Helper class to pass data between report creation phases
 * 
 * @author Riina Tens
 */
public class ReportDataCollector {

    private NodeRef reportResultNodeRef;
    private Workbook workbook;
    private String encoding;
    private ReportStatus resultStatus;
    private Map<QName, Serializable> reportResultProps;

    public void reset() {
        reportResultNodeRef = null;
        workbook = null;
        encoding = null;
        resultStatus = null;
        reportResultProps = null;
    }

    public NodeRef getReportResultNodeRef() {
        return reportResultNodeRef;
    }

    public void setReportResultNodeRef(NodeRef reportResultNodeRef) {
        this.reportResultNodeRef = reportResultNodeRef;
    }

    public void setWorkbook(Workbook workbook) {
        this.workbook = workbook;
    }

    public Workbook getWorkbook() {
        return workbook;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setResultStatus(ReportStatus resultStatus) {
        this.resultStatus = resultStatus;
    }

    public ReportStatus getResultStatus() {
        return resultStatus;
    }

    public void setReportResultProps(Map<QName, Serializable> reportResultProps) {
        this.reportResultProps = reportResultProps;
    }

    public Map<QName, Serializable> getReportResultProps() {
        return reportResultProps;
    }

    public void addReportResultProps(Map<QName, Serializable> reportResultProps) {
        if (this.reportResultProps != null && !this.reportResultProps.isEmpty()) {
            this.reportResultProps.putAll(reportResultProps);
        } else {
            setReportResultProps(reportResultProps);
        }
    }

}
