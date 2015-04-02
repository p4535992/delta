package ee.webmedia.alfresco.workflow.service.event;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.workflow.service.BaseWorkflowObject;

public class WorkflowEventQueue {

    private final List<WorkflowEvent> events = new ArrayList<>();
    private final Date now = new Date();

    private final Map<WorkflowQueueParameter, Object> parameters = new HashMap<>();
    private final Set<NodeRef> objectsWithEvent = new HashSet<>();

    public enum WorkflowQueueParameter {
        /** Boolean */
        TRIGGERED_BY_DOC_REGISTRATION,
        /** Boolean, true if finishing task is initiated by curren system, not by recieving finished task over dvk */
        TRIGGERED_BY_FINISHING_EXTERNAL_REVIEW_TASK_ON_CURRENT_SYSTEM,
        /** NodeRef */
        EXTERNAL_REVIEWER_TRIGGERING_TASK,
        /** List<NodeRef> */
        EXTERNAL_REVIEW_PROCESSED_DOCUMENTS,
        /** Map<NodeRef, List<String>> */
        ADDITIONAL_EXTERNAL_REVIEW_RECIPIENTS,
        /** Task, if set means that finishing was triggered by manually finishing given task */
        ORDER_ASSIGNMENT_FINISH_TRIGGERING_TASK,
        /** Workflow was cancelled with compound_workflow_finish button, Boolean. */
        WORKFLOW_CANCELLED_MANUALLY,
        /** Workflow was stopped with compound_workflow_stop button, Boolean. */
        WORKFLOW_STOPPED_MANUALLY,
        /** Inside continuing workflow process, Boolean. */
        WORKFLOW_CONTINUED,
        /** Finishing group task was initiated by finishing given task from user interface, NodeRef */
        INITIATING_GROUP_TASK,
        /** List<NodeRef>, tasks that are set finished when another group assignment task in the same workflow is finished by user */
        TASKS_FINISHED_BY_GROUP_TASK,
        /** New task is now in status "in progress" -> change workflow owner */
        ASSIGNEMNT_TASK_STARTED_WITH_RESPONSIBLE_ASPECT
    }

    public List<WorkflowEvent> getEvents() {
        return events;
    }

    private boolean isAlreadyAdded(WorkflowEvent event) {
        NodeRef objectRef = event.getObject().getNodeRef();
        if (objectsWithEvent.contains(objectRef)) { // check for duplicate
            BaseWorkflowObject object = event.getObject();
            WorkflowEventType type = event.getType();
            for (WorkflowEvent existingEvent : events) {
                if (existingEvent.getType() == type && existingEvent.getObject().equals(object)) {
                    return true;
                }
            }
        } else {
            objectsWithEvent.add(objectRef);
        }
        return false;
    }

    /** @return true if event was added to events list, false otherwise */
    public boolean add(WorkflowEvent event) {
        if (isAlreadyAdded(event)) {
            return false;
        }
        events.add(event);
        return true;
    }

    public void clear() {
        events.clear();
        objectsWithEvent.clear();
    }

    public Date getNow() {
        return now;
    }

    public List<NodeRef> getExternalReviewProcessedDocuments() {
        if (getParameter(WorkflowQueueParameter.EXTERNAL_REVIEW_PROCESSED_DOCUMENTS) == null) {
            setParameter(WorkflowQueueParameter.EXTERNAL_REVIEW_PROCESSED_DOCUMENTS, new ArrayList<String>());
        }
        return getParameter(WorkflowQueueParameter.EXTERNAL_REVIEW_PROCESSED_DOCUMENTS);
    }

    public Map<NodeRef, List<String>> getAdditionalExternalReviewRecipients() {
        if (getParameter(WorkflowQueueParameter.ADDITIONAL_EXTERNAL_REVIEW_RECIPIENTS) == null) {
            setParameter(WorkflowQueueParameter.ADDITIONAL_EXTERNAL_REVIEW_RECIPIENTS, new HashMap<NodeRef, List<String>>());
        }
        return getParameter(WorkflowQueueParameter.ADDITIONAL_EXTERNAL_REVIEW_RECIPIENTS);
    }

    public List<NodeRef> getTasksFinishedByGroupTask() {
        if (getParameter(WorkflowQueueParameter.TASKS_FINISHED_BY_GROUP_TASK) == null) {
            setParameter(WorkflowQueueParameter.TASKS_FINISHED_BY_GROUP_TASK, new ArrayList<String>());
        }
        return getParameter(WorkflowQueueParameter.TASKS_FINISHED_BY_GROUP_TASK);
    }

    public NodeRef getInitiatingGroupTask() {
        return getParameter(WorkflowQueueParameter.INITIATING_GROUP_TASK);
    }

    public Map<WorkflowQueueParameter, Object> getParameters() {
        return parameters;
    }

    public Boolean getBooleanParameter(WorkflowQueueParameter key) {
        return Boolean.TRUE.equals(getParameter(key));
    }

    public <T extends Object> T getParameter(WorkflowQueueParameter key) {
        @SuppressWarnings("unchecked")
        T value = (T) parameters.get(key);
        return value;
    }

    public void setParameter(WorkflowQueueParameter key, Object value) {
        parameters.put(key, value);
    }

}
