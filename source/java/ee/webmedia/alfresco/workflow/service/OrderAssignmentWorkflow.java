package ee.webmedia.alfresco.workflow.service;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

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

    @Override
    public String getCategory() {
        return getProp(WorkflowSpecificModel.Props.CATEGORY);
    }

}
