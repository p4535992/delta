package ee.webmedia.alfresco.eventplan.web;

import java.util.Collections;

import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.log.web.LogBlockBean;
import ee.webmedia.alfresco.eventplan.model.EventPlan;
import ee.webmedia.alfresco.log.model.LogFilter;
import ee.webmedia.alfresco.log.web.LogEntryDataProvider;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;

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
            logs = new LogEntryDataProvider(getEventPlanLogFilter());
        } else {
            logs = new LogEntryDataProvider();
        }
    }

    @Override
    public void clean() {
        super.clean();
        eventPlan = null;
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
