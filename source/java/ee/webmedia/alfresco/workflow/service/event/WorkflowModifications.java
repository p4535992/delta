package ee.webmedia.alfresco.workflow.service.event;

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

    void addOtherCompundWorkflows(CompoundWorkflow compoundWorkflow);

    void changeInitiatingTaskDueDate(Task task, WorkflowEventQueue queue);

    void unfinishTasksByFinishingLetterResponsibleTask(Task task, WorkflowEventQueue queue);

    void rejectDueDateExtension(Task task);
}
