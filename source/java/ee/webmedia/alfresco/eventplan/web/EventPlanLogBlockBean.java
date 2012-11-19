package ee.webmedia.alfresco.eventplan.web;

import java.util.ArrayList;
import java.util.Collections;

import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.log.web.LogBlockBean;
import ee.webmedia.alfresco.eventplan.model.EventPlan;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogFilter;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * @author Martti Tamm
 */
public class EventPlanLogBlockBean extends LogBlockBean {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "EventPlanLogBlockBean";

    private Node eventPlan;

    public void init(EventPlan plan) {
        if (plan != null) {
            eventPlan = plan.getNode();
            init(plan.getNode());
        } else {
            reset();
        }
    }

    @Override
    public void restore() {
        if (RepoUtil.isSaved(eventPlan)) {
            logs = BeanHelper.getLogService().getLogEntries(getEventPlanLogFilter());
        } else {
            logs = new ArrayList<LogEntry>();
        }
    }

    private LogFilter getEventPlanLogFilter() {
        LogFilter logFilter = new LogFilter();
        logFilter.setObjectId(Collections.singletonList(eventPlan.getNodeRef().toString()));
        return logFilter;
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage("eventplan_log_title");
    }
}
