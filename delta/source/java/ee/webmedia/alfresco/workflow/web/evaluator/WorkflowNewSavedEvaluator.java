package ee.webmedia.alfresco.workflow.web.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isStatus;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;

/**
 * Evaluates to true if given workflow has status "uus" and it has been saved into the repository.
 * 
 * @author Erko Hansar
 */
public class WorkflowNewSavedEvaluator extends AbstractFullAccessEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Object obj) {
        CompoundWorkflow workflow = (CompoundWorkflow) obj;
        return workflow != null && workflow.isSaved() && (isStatus(workflow, Status.NEW) || isStatus(workflow, Status.FINISHED) && getUserService().isAdministrator())
                && hasFullAccess();
    }

}
