package ee.webmedia.alfresco.common.listener;

import java.util.HashMap;
import java.util.Map;

import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.alfresco.util.Pair;
import org.alfresco.web.app.Application;
import org.apache.log4j.Logger;

import ee.webmedia.alfresco.common.filter.RequestControlFilter;

public class StatisticsPhaseListener implements PhaseListener {
    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(RequestControlFilter.class);

    private static ThreadLocal<Long> phaseStartTime = new ThreadLocal<Long>();
    private static ThreadLocal<Map<StatisticsPhaseListenerLogColumn, String>> stats = new ThreadLocal<Map<StatisticsPhaseListenerLogColumn, String>>() {
        @Override
        protected Map<StatisticsPhaseListenerLogColumn, String> initialValue() {
            return new HashMap<StatisticsPhaseListenerLogColumn, String>();
        }
    };
    private static ThreadLocal<Map<StatisticsPhaseListenerLogColumn, Pair<Long, Long>>> timings = new ThreadLocal<Map<StatisticsPhaseListenerLogColumn, Pair<Long, Long>>>() {
        @Override
        protected Map<StatisticsPhaseListenerLogColumn, Pair<Long, Long>> initialValue() {
            return new HashMap<StatisticsPhaseListenerLogColumn, Pair<Long, Long>>();
        }
    };

    @Override
    public void beforePhase(PhaseEvent event) {
        if (!log.isInfoEnabled()) {
            return;
        }
        phaseStartTime.set(new Long(System.currentTimeMillis()));
    }

    @Override
    public void afterPhase(PhaseEvent event) {
        if (!log.isInfoEnabled()) {
            return;
        }
        long duration = System.currentTimeMillis() - phaseStartTime.get().longValue();
        if (PhaseId.RESTORE_VIEW.equals(event.getPhaseId())) {
            add(StatisticsPhaseListenerLogColumn.PHASE_1RESTORE_VIEW, Long.toString(duration));
        } else if (PhaseId.APPLY_REQUEST_VALUES.equals(event.getPhaseId())) {
            add(StatisticsPhaseListenerLogColumn.PHASE_2APPLY_REQUEST_VALUES, Long.toString(duration));
        } else if (PhaseId.PROCESS_VALIDATIONS.equals(event.getPhaseId())) {
            add(StatisticsPhaseListenerLogColumn.PHASE_3PROCESS_VALIDATIONS, Long.toString(duration));
        } else if (PhaseId.UPDATE_MODEL_VALUES.equals(event.getPhaseId())) {
            add(StatisticsPhaseListenerLogColumn.PHASE_4UPDATE_MODEL_VALUES, Long.toString(duration));
        } else if (PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            add(StatisticsPhaseListenerLogColumn.PHASE_5INVOKE_APPLICATION, Long.toString(duration));
        } else if (PhaseId.RENDER_RESPONSE.equals(event.getPhaseId())) {
            add(StatisticsPhaseListenerLogColumn.PHASE_6RENDER_RESPONSE, Long.toString(duration));
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
            add(StatisticsPhaseListenerLogColumn.VIEWID, viewId);
        }
    }

    @Override
    public PhaseId getPhaseId() {
        return PhaseId.ANY_PHASE;
    }

    public static void clear() {
        if (!log.isInfoEnabled()) {
            return;
        }
        stats.get().clear();
        timings.get().clear();
    }

    public static void add(StatisticsPhaseListenerLogColumn logColumn, String value) {
        if (!log.isInfoEnabled()) {
            return;
        }
        Map<StatisticsPhaseListenerLogColumn, String> map = stats.get();
        if (map.containsKey(logColumn)) {
            map.put(logColumn, map.get(logColumn) + "!" + value);
        } else {
            map.put(logColumn, value);
        }
    }

    public static void addTimingNano(StatisticsPhaseListenerLogColumn logColumn, long startTimeNano) {
        addTiming(logColumn, System.nanoTime() - startTimeNano);
    }

    private static void addTiming(StatisticsPhaseListenerLogColumn logColumn, long value) {
        if (!log.isInfoEnabled()) {
            return;
        }
        Map<StatisticsPhaseListenerLogColumn, Pair<Long, Long>> map = timings.get();
        if (map.containsKey(logColumn)) {
            Pair<Long, Long> existing = map.get(logColumn);
            map.put(logColumn, new Pair<Long, Long>(existing.getFirst() + 1L, existing.getSecond() + value));
        } else {
            map.put(logColumn, new Pair<Long, Long>(1L, value));
        }
    }

    public static void log() {
        if (!log.isInfoEnabled()) {
            return;
        }
        StringBuilder s = new StringBuilder();
        Map<StatisticsPhaseListenerLogColumn, String> map = stats.get();
        Map<StatisticsPhaseListenerLogColumn, Pair<Long, Long>> map2 = timings.get();
        for (StatisticsPhaseListenerLogColumn logColumn : StatisticsPhaseListenerLogColumn.values()) {
            if (s.length() > 0) {
                s.append("|");
            }

            String value = map.get(logColumn);
            if (value != null) {
                s.append(logColumn.toString()).append(",").append(value);
            }

            Pair<Long, Long> timing = map2.get(logColumn);
            if (timing != null) {
                if (value == null) {
                    s.append(logColumn.toString());
                }
                s.append(",");
                s.append(timing.getFirst()).append(",").append(timing.getSecond() / 1000000L);
            }
        }
        log.info(s);
    }

}
