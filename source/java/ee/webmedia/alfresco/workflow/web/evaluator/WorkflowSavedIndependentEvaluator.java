package ee.webmedia.alfresco.workflow.web.evaluator;

import ee.webmedia.alfresco.common.evaluator.CompoundWorkflowActionGroupSharedResource;
import ee.webmedia.alfresco.common.evaluator.SharedResourceEvaluator;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;

public class WorkflowSavedIndependentEvaluator extends SharedResourceEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Object obj) {
        CompoundWorkflow compoundWorkflow = (CompoundWorkflow) obj;
        return compoundWorkflow != null && compoundWorkflow.isSaved() && compoundWorkflow.isIndependentWorkflow();
    }

    @Override
    public boolean evaluate() {
        CompoundWorkflowActionGroupSharedResource resource = (CompoundWorkflowActionGroupSharedResource) sharedResource;
        CompoundWorkflow compoundWorkflow = resource.getObject();
        return compoundWorkflow != null && resource.isSaved() && resource.isIndependentWorkflow();
    }
}
