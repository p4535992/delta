package ee.webmedia.alfresco.workflow.web.evaluator;

import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;

/**
 * Evaluates to true if given workflow has been saved into the repository.
 */
public class WorkflowSavedEvaluator extends AbstractFullAccessEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Object obj) {
        CompoundWorkflow compoundWorkflow = (CompoundWorkflow) obj;
        return compoundWorkflow != null && compoundWorkflow.isSaved() && hasFullAccess();
    }

}
