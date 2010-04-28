package ee.webmedia.alfresco.common.listener;

import java.util.HashMap;
import java.util.Map;

import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.alfresco.web.app.Application;
import org.apache.log4j.Logger;

import ee.webmedia.alfresco.common.filter.RequestControlFilter;

public class StatisticsPhaseListener implements PhaseListener {
    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(StatisticsPhaseListener.class);

    private static ThreadLocal<Long> phaseStartTime = new ThreadLocal<Long>();
    private static ThreadLocal<Map<LogColumn, String>> stats = new ThreadLocal<Map<LogColumn, String>>() {
        @Override
        protected Map<LogColumn,String> initialValue() {
            return new HashMap<LogColumn, String>();
        }
    };

    @Override
    public void beforePhase(PhaseEvent event) {
        phaseStartTime.set(new Long(System.currentTimeMillis()));
    }

    @Override
    public void afterPhase(PhaseEvent event) {
        long duration = System.currentTimeMillis() - phaseStartTime.get().longValue();
        if (PhaseId.RESTORE_VIEW.equals(event.getPhaseId())) {
            add(LogColumn.PHASE_1RESTORE_VIEW, Long.toString(duration));
        } else if (PhaseId.APPLY_REQUEST_VALUES.equals(event.getPhaseId())) {
            add(LogColumn.PHASE_2APPLY_REQUEST_VALUES, Long.toString(duration));
        } else if (PhaseId.PROCESS_VALIDATIONS.equals(event.getPhaseId())) {
            add(LogColumn.PHASE_3PROCESS_VALIDATIONS, Long.toString(duration));
        } else if (PhaseId.UPDATE_MODEL_VALUES.equals(event.getPhaseId())) {
            add(LogColumn.PHASE_4UPDATE_MODEL_VALUES, Long.toString(duration));
        } else if (PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            add(LogColumn.PHASE_5INVOKE_APPLICATION, Long.toString(duration));
        } else if (PhaseId.RENDER_RESPONSE.equals(event.getPhaseId())) {
            add(LogColumn.PHASE_6RENDER_RESPONSE, Long.toString(duration));
        }

        if (event.getPhaseId().equals(PhaseId.RENDER_RESPONSE)) {
            String viewId = event.getFacesContext().getViewRoot().getViewId();
            if ("/jsp/dialog/container.jsp".equals(viewId)) {
                if (Application.getDialogManager().getState() != null) {
                    viewId = "dialog:" + Application.getDialogManager().getCurrentDialog().getName();
                } else {
                    viewId = "null";
                }
            } else if ("/jsp/wizard/container.jsp".equals(viewId)) {
                if (Application.getWizardManager().getState() != null) {
                    viewId = "wizard:" + Application.getWizardManager().getCurrentWizard().getName();
                } else {
                    viewId = "null";
                }
            }
            add(LogColumn.VIEWID, viewId);
        }
    }

    @Override
    public PhaseId getPhaseId() {
        return PhaseId.ANY_PHASE;
    }

    public static enum LogColumn {
        REQUEST_END,
        REQUEST_CANCEL,
        PHASE_1RESTORE_VIEW,
        PHASE_2APPLY_REQUEST_VALUES,
        PHASE_3PROCESS_VALIDATIONS,
        PHASE_4UPDATE_MODEL_VALUES,
        PHASE_5INVOKE_APPLICATION,
        PHASE_6RENDER_RESPONSE,
        ACTION_LISTENER,
        ACTION,
        OUTCOME,
        VIEWID,
        EVENT,
    }
    
    public static void clear() {
        stats.get().clear();
    }

    public static void add(LogColumn logColumn, String value) {
        Map<LogColumn, String> map = stats.get();
        if (map.containsKey(logColumn)) {
            map.put(logColumn, map.get(logColumn) + "!" + value);
        } else {
            map.put(logColumn, value);
        }
    }

    public static void log() {
        StringBuilder s = new StringBuilder();
        Map<LogColumn, String> map = stats.get();
        for (LogColumn logColumn : LogColumn.values()) {
            String value = map.get(logColumn);
            if (s.length() > 0) {
                s.append("|");
            }
            if (value != null) {
                s.append(logColumn.toString() + "," + value);
            }
        }
        log.info(s);
    }

}
