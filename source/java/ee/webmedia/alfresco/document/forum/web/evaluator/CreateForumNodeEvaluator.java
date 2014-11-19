<<<<<<< HEAD
package ee.webmedia.alfresco.document.forum.web.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDialogHelperBean;

import org.alfresco.model.ForumModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.workflow.web.WorkflowBlockBean;

public class CreateForumNodeEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        if (!node.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)) {
            return false;
        }
        WorkflowBlockBean workflowBlock = BeanHelper.getWorkflowBlockBean();
        String userName = AuthenticationUtil.getRunAsUser();
        node = BeanHelper.getGeneralService().fetchNode(node.getNodeRef()); // refresh the node, because dialog caches it, and sub dialogs change props/aspects

        boolean inEditMode = getDocumentDialogHelperBean().isInEditMode();
        boolean statusWorking = node.getProperties().get(DocumentCommonModel.Props.DOC_STATUS.toString()).toString().equals(DocumentStatus.WORKING.getValueName());
        boolean isDiscussed = node.hasAspect(ForumModel.ASPECT_DISCUSSABLE);
        boolean isDocManager = BeanHelper.getUserService().isDocumentManager();
        boolean isOwner = userName.equals(node.getProperties().get(DocumentCommonModel.Props.OWNER_ID)); // it appears that OWNER_ID is null if document is from IMAP
        boolean isCompoundWorkflowOwner = workflowBlock.isCompoundWorkflowOwner();
        boolean isWorkingTaskOwner = workflowBlock.getMyTasks().size() > 0;

        return !inEditMode && statusWorking && !isDiscussed && (isDocManager || isOwner || isCompoundWorkflowOwner || isWorkingTaskOwner);
    }

}
=======
package ee.webmedia.alfresco.document.forum.web.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDialogHelperBean;

import org.alfresco.model.ForumModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.workflow.web.WorkflowBlockBean;

public class CreateForumNodeEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        if (!node.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)) {
            return false;
        }
        WorkflowBlockBean workflowBlock = BeanHelper.getWorkflowBlockBean();
        String userName = AuthenticationUtil.getRunAsUser();
        node = BeanHelper.getGeneralService().fetchNode(node.getNodeRef()); // refresh the node, because dialog caches it, and sub dialogs change props/aspects

        boolean inEditMode = getDocumentDialogHelperBean().isInEditMode();
        boolean statusWorking = node.getProperties().get(DocumentCommonModel.Props.DOC_STATUS.toString()).toString().equals(DocumentStatus.WORKING.getValueName());
        boolean isDiscussed = node.hasAspect(ForumModel.ASPECT_DISCUSSABLE);
        boolean isDocManager = BeanHelper.getUserService().isDocumentManager();
        boolean isOwner = userName.equals(node.getProperties().get(DocumentCommonModel.Props.OWNER_ID)); // it appears that OWNER_ID is null if document is from IMAP
        boolean isCompoundWorkflowOwner = workflowBlock.isCompoundWorkflowOwner();
        boolean isWorkingTaskOwner = workflowBlock.getMyTasks().size() > 0;

        return !inEditMode && statusWorking && !isDiscussed && (isDocManager || isOwner || isCompoundWorkflowOwner || isWorkingTaskOwner);
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
