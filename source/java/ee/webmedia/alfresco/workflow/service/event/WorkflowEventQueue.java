package ee.webmedia.alfresco.workflow.service.event;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventQueue.WorkflowQueueParameter;

/**
 * @author Alar Kvell
 */
public class WorkflowEventQueue {

    private List<WorkflowEvent> events = new ArrayList<WorkflowEvent>();
    private Date now = new Date();
    private Map<WorkflowQueueParameter, Object> parameters = new HashMap<WorkflowQueueParameter, Object>();
    
    public enum WorkflowQueueParameter{
        TRIGGERED_BY_DOC_REGISTRATION
    }
    
    public List<WorkflowEvent> getEvents() {
        return events;
    }

    public Date getNow() {
        return now;
    }

    public Map<WorkflowQueueParameter, Object> getParameters() {
        return parameters;
    }
    
    public <T extends Object> T getParameter(WorkflowQueueParameter key) {
        @SuppressWarnings("unchecked")
        T value = (T) parameters.get(key);
        return value;
    }
    

}
