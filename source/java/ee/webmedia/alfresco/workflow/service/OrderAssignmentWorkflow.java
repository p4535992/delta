package ee.webmedia.alfresco.workflow.service;

import ee.webmedia.alfresco.common.web.WmNode;
<<<<<<< HEAD
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

/**
 * @author Riina Tens
 */
=======
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

>>>>>>> develop-5.1
public class OrderAssignmentWorkflow extends Workflow {

    private static final long serialVersionUID = 1L;

    protected OrderAssignmentWorkflow(WmNode node, CompoundWorkflow parent, WmNode newTaskTemplate, Class<? extends Task> newTaskClass, Integer newTaskOutcomes) {
        super(node, parent, newTaskTemplate, newTaskClass, newTaskOutcomes);
    }

    @Override
    protected Workflow copy(CompoundWorkflow parent) {
        // no need to copy newTaskTemplate, it is not changed ever
        return copyImpl(new OrderAssignmentWorkflow(getNode().clone(), parent, newTaskTemplate, newTaskClass, newTaskOutcomes));
    }

<<<<<<< HEAD
    @Override
    protected void preSave() {
        super.preSave();
        WorkflowUtil.setWorkflowResolution(getTasks(), getProp(WorkflowSpecificModel.Props.RESOLUTION), Status.NEW, Status.IN_PROGRESS);
    }

=======
>>>>>>> develop-5.1
    public Task addResponsibleTask() {
        Task task = addTask();
        setActiveResponsible(task);
        return task;
    }

    public Task addResponsibleTask(int taskIndex) {
        Task task = addTask(taskIndex);
        setActiveResponsible(task);
        return task;
    }

    private void setActiveResponsible(Task task) {
        task.getNode().getAspects().add(WorkflowSpecificModel.Aspects.RESPONSIBLE);
        task.setProp(WorkflowSpecificModel.Props.ACTIVE, Boolean.TRUE);
    }

<<<<<<< HEAD
=======
    @Override
>>>>>>> develop-5.1
    public String getCategory() {
        return getProp(WorkflowSpecificModel.Props.CATEGORY);
    }

}
