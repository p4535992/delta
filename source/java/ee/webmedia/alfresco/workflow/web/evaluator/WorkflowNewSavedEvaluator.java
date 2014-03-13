package ee.webmedia.alfresco.workflow.web.evaluator;

import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

/**
 * Evaluates to true if given workflow has status "uus" and it has been saved into the repository.
 */
public class WorkflowNewSavedEvaluator extends AbstractFullAccessEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Object obj) {
        CompoundWorkflow workflow = (CompoundWorkflow) obj;
        return workflow != null && workflow.getNode().getNodeRef() != null && WorkflowUtil.isStatus(workflow, Status.NEW) && hasFullAccess();
    }

}
