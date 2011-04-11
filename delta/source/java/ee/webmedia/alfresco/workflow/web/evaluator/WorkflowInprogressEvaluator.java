package ee.webmedia.alfresco.workflow.web.evaluator;

import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

/**
 * Evaluates to true if given workflow has status "teostamisel".
 * 
 * @author Erko Hansar
 */
public class WorkflowInprogressEvaluator extends AbstractFullAccessEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Object obj) {
        return obj != null && WorkflowUtil.isStatus((CompoundWorkflow) obj, Status.IN_PROGRESS) && hasFullAccess();
    }

}
