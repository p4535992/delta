package ee.webmedia.alfresco.workflow.web.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isStatus;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;

/**
 * Evaluates to true if given workflow has status "uus" and it has been saved into the repository.
 */
public class WorkflowNewSavedEvaluator extends AbstractFullAccessEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Object obj) {
        CompoundWorkflow workflow = (CompoundWorkflow) obj;
        if (workflow == null || !workflow.isSaved()) {
            return false;
        }
        boolean isAdmin = getUserService().isAdministrator();
        boolean isValidFinished = isStatus(workflow, Status.FINISHED) && isAdmin;
        boolean isNew = isStatus(workflow, Status.NEW);
        if (workflow.isDocumentWorkflow()) {
            return (isValidFinished || isNew) && hasFullAccess();
        } else if (workflow.isIndependentWorkflow()) {
            return isValidFinished || (isNew && (isAdmin || StringUtils.equals(AuthenticationUtil.getRunAsUser(), workflow.getOwnerId())));
        } else {
            // conditions for case file workflow should be checked here
            return true;
        }
    }
}
