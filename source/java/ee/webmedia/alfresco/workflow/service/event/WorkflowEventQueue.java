package ee.webmedia.alfresco.workflow.service.event;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;

/**
 * @author Alar Kvell
 */
public class WorkflowEventQueue {

    private List<WorkflowEvent> events = new ArrayList<WorkflowEvent>();
    private Date now = new Date();

    public List<WorkflowEvent> getEvents() {
        return events;
    }

    public Date getNow() {
        return now;
    }

}
