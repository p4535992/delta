package ee.webmedia.alfresco.document.forum.web.evaluator;

import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.web.DocumentDialogHelperBean;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

public class ManageDiscussionEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;
    private transient WorkflowService workflowService;
    private transient NodeService nodeService;
    private transient GeneralService generalService;

    @Override
    public boolean evaluate(Object obj) {
        throw new RuntimeException("test");
    }

    @Override
    public boolean evaluate(Node _notUsed) {
        DocumentDialogHelperBean bean = BeanHelper.getDocumentDialogHelperBean();
        final Node docNode = bean.getNode();
        return evaluateState(bean, docNode) && isAppropriateUser(docNode);
    }

    private boolean evaluateState(DocumentDialogHelperBean bean, Node docNode) {
        return !bean.isInEditMode() //
                && DocumentStatus.WORKING.getValueName().equals(docNode.getProperties().get(DocumentCommonModel.Props.DOC_STATUS.toString()).toString());
    }

    private boolean isAppropriateUser(Node node) {
        String runAsUser = AuthenticationUtil.getRunAsUser();
        boolean isOwner = runAsUser.equals(node.getProperties().get(DocumentCommonModel.Props.OWNER_ID.toString()));
        boolean hasUserRights = isOwner || BeanHelper.getUserService().isDocumentManager();
        if (hasUserRights) {
            return true;
        }
        final List<NodeRef> compoundWorkflows = getWorkflowService().getCompoundWorkflowNodeRefs(node.getNodeRef());
        // Check if owner is compoundWorkflow owner
        final String currentUser = AuthenticationUtil.getRunAsUser();
        for (NodeRef compoundWorkflowRef : compoundWorkflows) {
            if (StringUtils.equals(currentUser, (String) nodeService.getProperty(compoundWorkflowRef, WorkflowCommonModel.Props.OWNER_ID))) {
                return true;
            }
        }
        // Check if user has any tasks in workflows
        if (CollectionUtils.isNotEmpty(compoundWorkflows) && BeanHelper.getWorkflowDbService().hasInProgressTasks(compoundWorkflows, currentUser)) {
            return true;
        }
        return false;
    }

    protected WorkflowService getWorkflowService() {
        if (workflowService == null) {
            workflowService = (WorkflowService) FacesContextUtils.getRequiredWebApplicationContext(//
                    FacesContext.getCurrentInstance()).getBean(WorkflowService.BEAN_NAME);
        }
        return workflowService;
    }

    protected NodeService getNodeService() {
        if (nodeService == null) {
            nodeService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getNodeService();
        }
        return nodeService;
    }

    protected GeneralService getGeneralService() {
        if (generalService == null) {
            generalService = (GeneralService) FacesContextUtils.getRequiredWebApplicationContext(//
                    FacesContext.getCurrentInstance()).getBean(GeneralService.BEAN_NAME);
        }
        return generalService;
    }
}
