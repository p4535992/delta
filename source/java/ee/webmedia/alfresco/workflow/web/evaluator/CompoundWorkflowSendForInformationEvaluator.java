package ee.webmedia.alfresco.workflow.web.evaluator;

import org.alfresco.repo.security.authentication.AuthenticationUtil;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

public class CompoundWorkflowSendForInformationEvaluator extends AbstractFullAccessEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Object obj) {
        CompoundWorkflow workflow = (CompoundWorkflow) obj;
        if (workflow == null || !workflow.isSaved() || !workflow.isIndependentWorkflow()) {
            return false;
        }
        if (BeanHelper.getWorkflowService().containsDocumentsWithLimitedActivities(workflow.getNodeRef())) {
            return false;
        }
        return isOwnerOrDocManager() || WorkflowUtil.isOwnerOfInProgressTask(workflow, AuthenticationUtil.getRunAsUser());
    }

}
