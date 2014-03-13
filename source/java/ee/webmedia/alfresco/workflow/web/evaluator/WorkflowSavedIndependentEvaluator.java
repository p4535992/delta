package ee.webmedia.alfresco.workflow.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;

import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;

public class WorkflowSavedIndependentEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Object obj) {
        CompoundWorkflow compoundWorkflow = (CompoundWorkflow) obj;
        return compoundWorkflow != null && compoundWorkflow.isSaved() && compoundWorkflow.isIndependentWorkflow();
    }
}
