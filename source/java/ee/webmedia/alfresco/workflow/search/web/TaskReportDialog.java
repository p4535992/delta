package ee.webmedia.alfresco.workflow.search.web;

import java.util.List;
import java.util.Map;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.TemplateReportType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.workflow.search.model.TaskReportModel;
import ee.webmedia.alfresco.workflow.search.service.TaskSearchFilterService;

public class TaskReportDialog extends TaskSearchDialog {

    private static final long serialVersionUID = 1L;

    private List<SelectItem> reportTemplates;

    @Override
    protected TaskSearchFilterService getFilterService() {
        if (filterService == null) {
            filterService = BeanHelper.getTaskReportFilterService();
        }
        return filterService;
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        reportTemplates = BeanHelper.getDocumentTemplateService().getReportTemplates(TemplateReportType.TASKS_REPORT);
        reportTemplates.add(0, new SelectItem("", MessageUtil.getMessage("select_default_label")));
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (isValidFilter()) {
            try {
                BeanHelper.getReportService().createReportResult(filter, TemplateReportType.TASKS_REPORT, TaskReportModel.Assocs.FILTER);
                MessageUtil.addInfoMessage("report_created_success");
            } catch (UnableToPerformException e) {
                MessageUtil.addErrorMessage(e.getMessageKey());
            }
        }
        isFinished = false;
        return null;
    }

    private boolean isValidFilter() {
        Map<String, Object> filterProps = filter.getProperties();
        if (StringUtils.isBlank((String) filterProps.get(TaskReportModel.Props.REPORT_TEMPLATE))) {
            MessageUtil.addErrorMessage("report_error_template_not_selected");
            return false;
        }
        return true;
    }

    @Override
    public String getManageSavedBlockTitle() {
        return MessageUtil.getMessage("task_report_saved_manage");
    }

    @Override
    public String getSavedFilterSelectTitle() {
        return MessageUtil.getMessage("task_report_saved");
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage("task_search_execute_report");
    }

    @Override
    public String getFilterPanelTitle() {
        return MessageUtil.getMessage("task_report");
    }

    @Override
    protected QName getFilterType() {
        return TaskReportModel.Types.FILTER;
    }

    public List<SelectItem> getReportTemplates(@SuppressWarnings("unused") FacesContext context, @SuppressWarnings("unused") UIInput selectComponent) {
        return reportTemplates;
    }

    @Override
    public boolean isReportSearch() {
        return true;
    }

    @Override
    public boolean isShowManageSavedDialog() {
        return BeanHelper.getUserService().isAdministrator();
    }

    @Override
    protected boolean isPrivateFilter() {
        return false;
    }

}
