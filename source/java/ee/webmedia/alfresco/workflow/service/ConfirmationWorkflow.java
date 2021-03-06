package ee.webmedia.alfresco.workflow.service;

import ee.webmedia.alfresco.common.web.WmNode;

public class ConfirmationWorkflow extends Workflow {

    private static final long serialVersionUID = 1L;

    protected ConfirmationWorkflow(WmNode node, CompoundWorkflow parent, WmNode newTaskTemplate, Class<? extends Task> newTaskClass, Integer newTaskOutcomes) {
        super(node, parent, newTaskTemplate, newTaskClass, newTaskOutcomes);
    }

    @Override
    protected Workflow copy(CompoundWorkflow parent) {
        // no need to copy newTaskTemplate, it is not changed ever
        return copyImpl(new ConfirmationWorkflow(getNode().clone(), parent, newTaskTemplate, newTaskClass, newTaskOutcomes));
    }

}
