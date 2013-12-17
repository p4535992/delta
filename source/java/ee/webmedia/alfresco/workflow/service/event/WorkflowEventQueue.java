package ee.webmedia.alfresco.workflow.service.event;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 * @author Alar Kvell
 */
public class WorkflowEventQueue {

    private final List<WorkflowEvent> events = new ArrayList<WorkflowEvent>();
    private final Date now = new Date();

    private final Map<WorkflowQueueParameter, Object> parameters = new HashMap<WorkflowQueueParameter, Object>();

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
        WORKFLOW_CANCELLED_MANUALLY
    }

    public List<WorkflowEvent> getEvents() {
        return events;
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

    public Map<WorkflowQueueParameter, Object> getParameters() {
        return parameters;
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
