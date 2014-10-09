package ee.webmedia.alfresco.workflow.web;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.log.web.LogBlockBean;
import ee.webmedia.alfresco.log.model.LogFilter;
import ee.webmedia.alfresco.log.web.LogEntryDataProvider;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;

public class CompoundWorkflowLogBlockBean extends LogBlockBean {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "CompoundWorkflowLogBlockBean";

    private CompoundWorkflow compoundWorkflow;

    public void init(CompoundWorkflow compoundWorkflow) {
        if (compoundWorkflow != null) {
            this.compoundWorkflow = compoundWorkflow;
            init(compoundWorkflow.getNode());
        } else {
            reset();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void restore() {
        if (compoundWorkflow.isSaved()) {
            logs = new LogEntryDataProvider(getCompoundWorkflowLogFilter());
        } else {
            logs = new LogEntryDataProvider();
        }
    }

    @Override
    public void clean() {
        super.clean();
        compoundWorkflow = null;
    }

    private LogFilter getCompoundWorkflowLogFilter() {
        LogFilter logFilter = new LogFilter();
        Set<String> excludedDescriptions = new HashSet<String>(1);
        excludedDescriptions.add(MessageUtil.getMessage("applog_compoundWorkflow_view"));
        logFilter.setExcludedDescriptions(excludedDescriptions);
        List<String> objectIds = new ArrayList<String>();
        objectIds.add(compoundWorkflow.getNodeRef().toString());
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            if (!workflow.isSaved()) {
                continue;
            }
            for (Task task : workflow.getTasks()) {
                if (!task.isSaved()) {
                    continue;
                }
                objectIds.add(task.getNodeRef().toString());
            }
        }
        logFilter.setObjectId(objectIds);
        logFilter.setExactObjectId(true);
        return logFilter;
    }

    @Override
    public boolean isRendered() {
        return true;
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage("compoundWorkflow_log_title");
    }

    @Override
    public String getCreatedDateColumnTitle() {
        return MessageUtil.getMessage("compoundWorkflow_log_date");
    }

    @Override
    public String getEventColumnTitle() {
        return MessageUtil.getMessage("compoundWorkflow_log_event");
    }

    @Override
    public boolean isShowLogDetailsLink() {
        return BeanHelper.getUserService().isSupervisor();
    }

}
