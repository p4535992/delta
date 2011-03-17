package ee.webmedia.alfresco.workflow.web.evaluator;

import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;

/**
 * Evaluates to true if given workflow has been saved into the repository.
 *
 * @author Erko Hansar
 */
public class WorkflowSavedEvaluator extends AbstractFullAccessEvaluator {

    private static final long serialVersionUID = 1L;
    
    @Override
    public boolean evaluate(Object obj) {
        return obj != null && ((CompoundWorkflow)obj).getNode().getNodeRef() != null && hasFullAccess();
    }
    
}
