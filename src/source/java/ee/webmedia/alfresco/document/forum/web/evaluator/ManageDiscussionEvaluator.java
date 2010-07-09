package ee.webmedia.alfresco.document.forum.web.evaluator;

import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.metadata.web.MetadataBlockBean;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.web.evaluator.IsAdminOrDocManagerEvaluator;
import ee.webmedia.alfresco.document.web.evaluator.IsOwnerEvaluator;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * @author Ats Uiboupin
 */
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
        FacesContext context = FacesContext.getCurrentInstance();
        MetadataBlockBean bean = (MetadataBlockBean) FacesHelper.getManagedBean(context, MetadataBlockBean.BEAN_NAME);
        final Node docNode = getDocNode(bean);
        return evaluateState(bean, docNode) && isAppropriateUser(docNode);
    }

    private boolean evaluateState(MetadataBlockBean bean, Node docNode) {
        return !bean.isInEditMode() //
                && DocumentStatus.WORKING.getValueName().equals(docNode.getProperties().get(DocumentCommonModel.Props.DOC_STATUS.toString()).toString());
    }

    private Node getDocNode(MetadataBlockBean bean) {
        return bean.getDocument();
    }

    private boolean isAppropriateUser(Node node) {
        boolean isOwner = new IsOwnerEvaluator().evaluate(node);
        boolean hasUserRights = isOwner || new IsAdminOrDocManagerEvaluator().evaluate(node);
        if (hasUserRights) {
            return true;
        }
        final NodeRef docRef = getNodeService().getPrimaryParent(node.getNodeRef()).getParentRef();
        final List<CompoundWorkflow> compoundWorkflows = getWorkflowService().getCompoundWorkflows(docRef);
        final List<Task> myTasks = getWorkflowService().getMyTasksInProgress(compoundWorkflows);
        if (myTasks.size() > 0) {
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
