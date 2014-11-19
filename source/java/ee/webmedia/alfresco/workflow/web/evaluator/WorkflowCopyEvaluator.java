package ee.webmedia.alfresco.workflow.web.evaluator;

import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Workflow;

/**
 * Evaluates to true if the workflow has been saved to the repository and doesn't contain any {@link WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK} tasks
 */
public class WorkflowCopyEvaluator extends WorkflowSavedEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Object obj) {
        if (!super.evaluate(obj)) {
            return false;
        }

        // Check for due date extension tasks
        CompoundWorkflow compoundWorkflow = (CompoundWorkflow) obj;
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            if (WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_WORKFLOW.equals(workflow.getType())) {
                return false;
            }
        }

        return true;
    }
}