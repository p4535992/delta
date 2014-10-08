package ee.webmedia.alfresco.workflow.web.evaluator;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.privilege.service.PrivilegeUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

/**
 * Evaluates to true if given workflow has status "teostamisel".
 */
public class WorkflowInprogressEvaluator extends AbstractFullAccessEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Object obj) {
        CompoundWorkflow compoundWorkflow = (CompoundWorkflow) obj;
        String currentUser = AuthenticationUtil.getRunAsUser();
        return obj != null && WorkflowUtil.isStatus(compoundWorkflow, Status.IN_PROGRESS) && hasFullAccess()
                && (!compoundWorkflow.isIndependentWorkflow() || isOwnerOrDocManager())
                && (!compoundWorkflow.isCaseFileWorkflow() || StringUtils.equals(compoundWorkflow.getOwnerId(), currentUser)
                        || BeanHelper.getDocumentDynamicService().isOwner(compoundWorkflow.getParent(), currentUser)
                        || PrivilegeUtil.isAdminOrDocmanagerWithPermission(compoundWorkflow.getParent(), Privilege.VIEW_CASE_FILE));
    }

}
