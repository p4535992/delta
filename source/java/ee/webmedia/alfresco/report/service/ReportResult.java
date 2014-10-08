<<<<<<< HEAD
package ee.webmedia.alfresco.report.service;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.classificator.enums.TemplateReportOutputType;
import ee.webmedia.alfresco.classificator.enums.TemplateReportType;
import ee.webmedia.alfresco.common.model.NodeBaseVO;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.report.model.ReportModel;
import ee.webmedia.alfresco.report.model.ReportStatus;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Riina Tens
 */
public class ReportResult extends NodeBaseVO {

    private static final long serialVersionUID = 1L;
    private String templateName;
    private String downloadUrl;

    public ReportResult(WmNode node) {
        Assert.notNull(node);
        this.node = node;
    }

    public String getReportName() {
        return getProp(ReportModel.Props.REPORT_NAME);
    }

    public Date getUserStartDateTime() {
        return (Date) getNode().getProperties().get(ReportModel.Props.USER_START_DATE_TIME);
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

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateName() {
        return templateName != null ? templateName : "";
    }

    public String getStatus() {
        return getProp(ReportModel.Props.STATUS);
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

}
=======
package ee.webmedia.alfresco.report.service;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.classificator.enums.TemplateReportOutputType;
import ee.webmedia.alfresco.classificator.enums.TemplateReportType;
import ee.webmedia.alfresco.common.model.NodeBaseVO;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.report.model.ReportModel;
import ee.webmedia.alfresco.report.model.ReportStatus;
import ee.webmedia.alfresco.utils.MessageUtil;

public class ReportResult extends NodeBaseVO {

    private static final long serialVersionUID = 1L;
    private String templateName;
    private String downloadUrl;

    public ReportResult(WmNode node) {
        Assert.notNull(node);
        this.node = node;
    }

    public String getReportName() {
        return getProp(ReportModel.Props.REPORT_NAME);
    }

    public Date getUserStartDateTime() {
        return (Date) getNode().getProperties().get(ReportModel.Props.USER_START_DATE_TIME);
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

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateName() {
        return templateName != null ? templateName : "";
    }

    public String getStatus() {
        return getProp(ReportModel.Props.STATUS);
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

}
>>>>>>> develop-5.1
