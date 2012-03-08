package ee.webmedia.alfresco.report.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getReportService;

import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.report.service.ReportResult;
import ee.webmedia.alfresco.utils.ActionUtil;

/**
 * @author Riina Tens
 */
public class ReportListDialog extends BaseDialogBean {

    private static final String REPORT_RESULT_NODE_REF_PARAM = "reportResultNodeRef";
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "ReportListDialog";

    private List<ReportResult> reportResults;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        reload();
    }

    private void reload() {
        reportResults = BeanHelper.getReportService().getReportResultsForUser(AuthenticationUtil.getRunAsUser());
    }

    /**
     * Used in JSP pages.
     */
    public List<ReportResult> getReportResults() {
        return reportResults;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return null;
    }

    public void deleteReport(ActionEvent event) {
        NodeRef reportResultNodeRef = ActionUtil.getParam(event, REPORT_RESULT_NODE_REF_PARAM, NodeRef.class);
        BeanHelper.getReportService().enqueueReportForDeleting(reportResultNodeRef);
        reload();
    }

    public void cancelReport(ActionEvent event) {
        NodeRef reportResultNodeRef = ActionUtil.getParam(event, REPORT_RESULT_NODE_REF_PARAM, NodeRef.class);
        BeanHelper.getReportService().enqueueReportForCancelling(reportResultNodeRef);
        reload();
    }

    public void markReportDownloaded(ActionEvent event) {
        NodeRef reportResultNodeRef = ActionUtil.getParam(event, REPORT_RESULT_NODE_REF_PARAM, NodeRef.class);
        BeanHelper.getReportService().markReportDownloaded(reportResultNodeRef);
        reload();
    }

    // BEGIN Methods for generally pausing/continuing report generation from nodeBrowser
    public boolean isReportGenerationEnabled() {
        return getReportService().isReportGenerationEnabled();
    }

    public boolean isShowPauseReportGeneration() {
        return isReportGenerationEnabled() && !getReportService().isReportGenerationPaused();
    }

    public boolean isShowContinueReportGeneration() {
        return isReportGenerationEnabled() && getReportService().isReportGenerationPaused();
    }

    public void pauseReportGeneration(ActionEvent event) {
        getReportService().setReportGenerationPaused(true);
    }

    public void continueReportGeneration(ActionEvent event) {
        getReportService().setReportGenerationPaused(false);
    }

    // END Methods for generally pausing/continuing report generation from nodeBrowser

    @Override
    public String cancel() {
        reportResults = null;
        return super.cancel();
    }

    @Override
    public Object getActionsContext() {
        return null;
    }

}
