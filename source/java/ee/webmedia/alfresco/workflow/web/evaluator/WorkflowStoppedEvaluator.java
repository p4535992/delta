<<<<<<< HEAD
package ee.webmedia.alfresco.workflow.web.evaluator;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.privilege.service.PrivilegeUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

/**
 * Evaluates to true if given workflow has status "peatatud".
 * 
 * @author Erko Hansar
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
                        || PrivilegeUtil.isAdminOrDocmanagerWithPermission(compoundWorkflow.getParent(), DocumentCommonModel.Privileges.VIEW_CASE_FILE));
    }

}
=======
package ee.webmedia.alfresco.workflow.web.evaluator;

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
        return obj != null && WorkflowUtil.isStatus((CompoundWorkflow) obj, Status.STOPPED) && hasFullAccess();
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
