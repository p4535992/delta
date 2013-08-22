package ee.webmedia.alfresco.workflow.service.event;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;

/**
 * @author Alar Kvell
 */
public interface WorkflowModifications {

    /**
     * Finish task (it must be ... or UNFINISHED). Sets the outcome to the first one (outcomeIndex=0), so only supported when outcomes=1.
     */
    void setTaskFinished(WorkflowEventQueue queue, Task task);

    void setWorkflowsAndTasksFinished(WorkflowEventQueue queue, CompoundWorkflow compoundWorkflow,
            String taskOutcomeLabelId, String userTaskComment,
            boolean finishOnRegisterDocument, List<NodeRef> excludedWorkflows);

    void addOtherCompundWorkflows(CompoundWorkflow compoundWorkflow);

}
