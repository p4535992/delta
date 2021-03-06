package ee.webmedia.alfresco.workflow.web.evaluator;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.evaluator.CompoundWorkflowActionGroupSharedResource;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.privilege.service.PrivilegeUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

/**
 * Evaluates to true if given workflow has status "peatatud".
 */
public class WorkflowStoppedEvaluator extends AbstractFullAccessEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Object obj) {
        CompoundWorkflow compoundWorkflow = (CompoundWorkflow) obj;
        String currentUser = AuthenticationUtil.getRunAsUser();
        return obj != null && WorkflowUtil.isStatus((CompoundWorkflow) obj, Status.STOPPED) && hasFullAccess()
                && (!compoundWorkflow.isIndependentWorkflow() || isOwnerOrDocManager())
                && (!compoundWorkflow.isCaseFileWorkflow() || StringUtils.equals(compoundWorkflow.getOwnerId(), currentUser)
                        || BeanHelper.getDocumentDynamicService().isOwner(compoundWorkflow.getParent(), currentUser)
                        || PrivilegeUtil.isAdminOrDocmanagerWithPermission(compoundWorkflow.getParent(), Privilege.VIEW_CASE_FILE));
    }

    @Override
    public boolean evaluate() {
        CompoundWorkflowActionGroupSharedResource resource = (CompoundWorkflowActionGroupSharedResource) sharedResource;
        CompoundWorkflow compoundWorkflow = resource.getObject();
        return compoundWorkflow != null && WorkflowUtil.isStatus(compoundWorkflow, Status.STOPPED) && hasFullAccess(resource)
                && (!compoundWorkflow.isIndependentWorkflow() || isOwnerOrDocManager(resource))
                && (!compoundWorkflow.isCaseFileWorkflow() || StringUtils.equals(compoundWorkflow.getOwnerId(), resource.getCurrentUser())
                        || resource.isParentOwner()
                        || resource.isAdminOrDocManagerWithPermission());
    }

}
