package ee.webmedia.alfresco.document.forum.web.evaluator;

import javax.faces.context.FacesContext;

import org.alfresco.model.ForumModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.metadata.web.MetadataBlockBean;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.workflow.web.WorkflowBlockBean;

public class CreateForumNodeEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        MetadataBlockBean bean = (MetadataBlockBean) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), MetadataBlockBean.BEAN_NAME);
        UserService userService = (UserService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(UserService.BEAN_NAME);
        WorkflowBlockBean workflowBlock = (WorkflowBlockBean) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), WorkflowBlockBean.BEAN_NAME);
        String userName = AuthenticationUtil.getRunAsUser();
        GeneralService generalService = (GeneralService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(GeneralService.BEAN_NAME);
        node = generalService.fetchNode(node.getNodeRef()); // refresh the node, because dialog caches it, and sub dialogs change props/aspects
        
        boolean inEditMode = bean.isInEditMode();
        boolean statusWorking = bean.getDocument().getProperties().get(DocumentCommonModel.Props.DOC_STATUS.toString()).toString().equals(DocumentStatus.WORKING.getValueName());
        boolean isDiscussed = node.hasAspect(ForumModel.ASPECT_DISCUSSABLE);
        boolean isDocManager = userService.isDocumentManager();
        boolean isOwner = userName.equals(node.getProperties().get(DocumentCommonModel.Props.OWNER_ID)); // it appears that OWNER_ID is null if document is from IMAP
        boolean isCompoundWorkflowOwner = workflowBlock.isCompoundWorkflowOwner();
        boolean isWorkingTaskOwner =  workflowBlock.getMyTasks().size() > 0;
        
        return !inEditMode && statusWorking && !isDiscussed && (isDocManager || isOwner || isCompoundWorkflowOwner || isWorkingTaskOwner);
    }

}
