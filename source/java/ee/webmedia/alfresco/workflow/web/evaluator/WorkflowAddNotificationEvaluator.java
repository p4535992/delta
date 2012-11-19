package ee.webmedia.alfresco.workflow.web.evaluator;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.user.model.UserModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;

/**
 * @author Riina Tens
 */
public class WorkflowAddNotificationEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Object obj) {
        CompoundWorkflow compoundWorkflow = (CompoundWorkflow) obj;
        if (compoundWorkflow == null || !compoundWorkflow.isSaved() || compoundWorkflow.isDocumentWorkflow()) {
            return false;
        }
        NodeRef currentUserRef = BeanHelper.getUserService().getCurrentUser();
        if (compoundWorkflow.isIndependentWorkflow()) {
            return evaluateAssocs(compoundWorkflow, currentUserRef, UserModel.Assocs.INDEPENDENT_WORKFLOW_NOTIFICATION);
        } else if (compoundWorkflow.isCaseFileWorkflow()) {
            return evaluateAssocs(compoundWorkflow, currentUserRef, UserModel.Assocs.CASE_FILE_WORKFLOW_NOTIFICATION);
        }
        throw new RuntimeException("This code should never be reached!");
    }

    protected boolean evaluateAssocs(CompoundWorkflow compoundWorkflow, NodeRef userRef, QName assocType) {
        return !BeanHelper.getNotificationService().isNotificationAssocExists(userRef, compoundWorkflow.getNodeRef(), assocType);
    }

}
