<<<<<<< HEAD
package ee.webmedia.alfresco.workflow.web.evaluator;

import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

/**
 * Evaluates to true if given workflow has status "uus".
 * 
 * @author Erko Hansar
 */
public class WorkflowNewEvaluator extends AbstractFullAccessEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Object obj) {
        return obj != null && WorkflowUtil.isStatus((CompoundWorkflow) obj, Status.NEW) && hasFullAccess();
    }

}
=======
package ee.webmedia.alfresco.workflow.web.evaluator;

import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

/**
 * Evaluates to true if given workflow has status "uus".
 */
public class WorkflowNewEvaluator extends AbstractFullAccessEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Object obj) {
        return obj != null && WorkflowUtil.isStatus((CompoundWorkflow) obj, Status.NEW) && hasFullAccess();
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
