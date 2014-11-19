package ee.webmedia.alfresco.workflow.service.event;

import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public interface WorkflowModifications {

    /**
     * Finish task (it must be ... or UNFINISHED). Sets the outcome to the first one (outcomeIndex=0), so only supported when outcomes=1.
     */
    void setTaskFinished(WorkflowEventQueue queue, Task task);

    void addOtherCompundWorkflows(CompoundWorkflow compoundWorkflow);

    void changeInitiatingTaskDueDate(Task task, WorkflowEventQueue queue);

    void unfinishTasksByFinishingLetterResponsibleTask(Task task, WorkflowEventQueue queue);
<<<<<<< HEAD

    void rejectDueDateExtension(Task task);
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}
