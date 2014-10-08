<<<<<<< HEAD
package ee.webmedia.alfresco.workflow.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;

/**
 * @author Riina Tens
 */
public class WorkflowReopenEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Object obj) {
        CompoundWorkflow compoundWorkflow = (CompoundWorkflow) obj;
        return compoundWorkflow != null && compoundWorkflow.isIndependentWorkflow() && compoundWorkflow.isStatus(Status.FINISHED) && BeanHelper.getUserService().isAdministrator();
    }

}
=======
package ee.webmedia.alfresco.workflow.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;

public class WorkflowReopenEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Object obj) {
        CompoundWorkflow compoundWorkflow = (CompoundWorkflow) obj;
        return compoundWorkflow != null && compoundWorkflow.isIndependentWorkflow() && compoundWorkflow.isStatus(Status.FINISHED) && BeanHelper.getUserService().isAdministrator();
    }

}
>>>>>>> develop-5.1
