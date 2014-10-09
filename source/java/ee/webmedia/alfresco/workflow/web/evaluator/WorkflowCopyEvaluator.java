package ee.webmedia.alfresco.workflow.web.evaluator;

import ee.webmedia.alfresco.common.evaluator.CompoundWorkflowActionGroupSharedResource;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;

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
        return !BeanHelper.getBulkLoadNodeService().hasChildNodeOfType(compoundWorkflow.getNodeRef(), WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_WORKFLOW);
    }

    @Override
    public boolean evaluate() {
        if (!super.evaluate()) {
            return false;
        }
        CompoundWorkflowActionGroupSharedResource resource = (CompoundWorkflowActionGroupSharedResource) sharedResource;
        return !BeanHelper.getBulkLoadNodeService().hasChildNodeOfType(resource.getObject().getNodeRef(), WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_WORKFLOW);
    }
}