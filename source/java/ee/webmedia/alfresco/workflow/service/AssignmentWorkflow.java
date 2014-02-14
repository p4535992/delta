package ee.webmedia.alfresco.workflow.service;

import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isActiveResponsible;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

/**
 * @author Erko Hansar
 */
public class AssignmentWorkflow extends Workflow {

    private static final long serialVersionUID = 1L;

    protected AssignmentWorkflow(WmNode node, CompoundWorkflow parent, WmNode newTaskTemplate, Class<? extends Task> newTaskClass, Integer newTaskOutcomes) {
        super(node, parent, newTaskTemplate, newTaskClass, newTaskOutcomes);
    }

    @Override
    protected Workflow copy(CompoundWorkflow parent) {
        // no need to copy newTaskTemplate, it is not changed ever
        return copyImpl(new AssignmentWorkflow(getNode().clone(), parent, newTaskTemplate, newTaskClass, newTaskOutcomes));
    }

    public Task addResponsibleTask() {
        // Passivate all current active responsible tasks
        for (Task task : getTasks()) {
            if (isActiveResponsible(task)) {
                task.setProp(WorkflowSpecificModel.Props.ACTIVE, Boolean.FALSE);
            }
        }
        // Create new active responsible task
        Task task = addTask();
        task.getNode().getAspects().add(WorkflowSpecificModel.Aspects.RESPONSIBLE);
        task.setProp(WorkflowSpecificModel.Props.ACTIVE, Boolean.TRUE);
        return task;
    }

}
