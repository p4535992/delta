package ee.webmedia.alfresco.workflow.service;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/** Kooskõlastamiseks töövoog - review workflow */
public class ReviewWorkflow extends Workflow {

    private static final long serialVersionUID = 1L;

    protected ReviewWorkflow(WmNode node, CompoundWorkflow parent, WmNode newTaskTemplate, Class<? extends Task> newTaskClass, Integer newTaskOutcomes) {
        super(node, parent, newTaskTemplate, newTaskClass, newTaskOutcomes);
    }

    @Override
    protected Workflow copy(CompoundWorkflow parent) {
        // no need to copy newTaskTemplate, it is not changed ever
        return copyImpl(new ReviewWorkflow(getNode().clone(), parent, newTaskTemplate, newTaskClass, newTaskOutcomes));
    }

    @Override
    protected void postCreate() {
        setProp(WorkflowCommonModel.Props.PARALLEL_TASKS, null);
    }

}
