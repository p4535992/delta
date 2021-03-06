package ee.webmedia.alfresco.report.service;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.classificator.enums.TemplateReportOutputType;
import ee.webmedia.alfresco.classificator.enums.TemplateReportType;
import ee.webmedia.alfresco.report.model.ReportModel;
import ee.webmedia.alfresco.report.model.ReportStatus;
import ee.webmedia.alfresco.utils.MessageUtil;

public class ReportResult implements Serializable {

    private static final long serialVersionUID = 1L;
    private String templateName;
    private String downloadUrl;
    private final NodeRef nodeRef;
    private final Map<QName, Serializable> props;

    public ReportResult(Map<QName, Serializable> props, NodeRef nodeRef) {
        Assert.notNull(props);
        this.props = props;
        this.nodeRef = nodeRef;
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public String getReportName() {
        return getProp(ReportModel.Props.REPORT_NAME);
    }

    public Date getUserStartDateTime() {
        return getProp(ReportModel.Props.USER_START_DATE_TIME);
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getReportTypeText() {
        return MessageUtil.getMessage(TemplateReportType.valueOf((String) getProp(ReportModel.Props.REPORT_TYPE)));
    }

    public String getReportOutputTypeText() {
        String reportOutputStr = (String) getProp(ReportModel.Props.REPORT_OUTPUT_TYPE);
        return reportOutputStr != null ? MessageUtil.getMessage(TemplateReportOutputType.valueOf(reportOutputStr)) : "";
    }

    public String getTemplateName() {
        String templateName = getProp(ReportModel.Props.REPORT_TEMPLATE);
        return templateName != null ? templateName : "";
    }

    public String getStatus() {
        return getProp(ReportModel.Props.STATUS);
    }

    public String getOrderInQueue() {
        Integer order = getProp(ReportModel.Props.ORDER_IN_QUEUE);
        return order == null ? "" : String.valueOf(order);
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public boolean isShowDownloadLink() {
        return StringUtils.isNotBlank(downloadUrl);
    }

    public String getConfirmDeleteMessage() {
        if (ReportStatus.IN_QUEUE == getReportStatus()) {
            return MessageUtil.getMessage("report_confirm_delete_in_queue", getReportName(), getUserStartDateTime());
        } else if (ReportStatus.RUNNING == getReportStatus()) {
            return MessageUtil.getMessage("report_confirm_delete_running", getReportName(), getUserStartDateTime());
        }
        return MessageUtil.getMessage("report_confirm_delete", getReportName(), getUserStartDateTime());
    }

    public String getConfirmCancelMessage() {
        return templateName != null ? templateName : "";
    }

    public boolean isCancellingEnabled() {
        ReportStatus reportStatus = getReportStatus();
        return ReportStatus.IN_QUEUE == reportStatus || ReportStatus.RUNNING == reportStatus || ReportStatus.CANCELLING_REQUESTED == reportStatus;
    }

    private ReportStatus getReportStatus() {
        return ReportStatus.valueOf(getStatus());
    }

    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getProp(QName propName) {
        return (T) props.get(propName);
    }

}
