package ee.webmedia.alfresco.document.forum.web.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDialogHelperBean;

import java.util.Map;

import org.alfresco.model.ForumModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.util.EqualsHelper;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.evaluator.SharedResourceEvaluator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicDialog;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.web.evaluator.DocumentDynamicActionsGroupResources;
import ee.webmedia.alfresco.workflow.web.WorkflowBlockBean;

public class CreateForumNodeEvaluator extends SharedResourceEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        if (!node.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)
                || getDocumentDialogHelperBean().isInEditMode()
                || StringUtils.contains(node.getPath(), DocumentDynamicDialog.FORWARDED_DEC_DOCUMENTS)) {
            return false;
        }
        Map<String, Object> properties = node.getProperties();
        boolean statusWorking = properties.get(DocumentCommonModel.Props.DOC_STATUS.toString()).toString().equals(DocumentStatus.WORKING.getValueName());
        return statusWorking && !BeanHelper.getNodeService().hasAspect(node.getNodeRef(), ForumModel.ASPECT_DISCUSSABLE)
                && (isDocManagerOrOwner(properties) || isCWFOrWorkingTaskOwner());
    }

    private boolean isDocManagerOrOwner(Map<String, Object> properties) {
        return BeanHelper.getUserService().isDocumentManager()
                || EqualsHelper.nullSafeEquals(AuthenticationUtil.getRunAsUser(), properties.get(DocumentCommonModel.Props.OWNER_ID));
    }

    private boolean isCWFOrWorkingTaskOwner() {
        WorkflowBlockBean workflowBlock = BeanHelper.getWorkflowBlockBean();
        return workflowBlock.isCompoundWorkflowOwner() || workflowBlock.getMyTasks().size() > 0;
    }

    @Override
    public boolean evaluate() {
        DocumentDynamicActionsGroupResources resource = (DocumentDynamicActionsGroupResources) sharedResource;
        if (!resource.isWorkspaceNode() || resource.isInEditMode() || resource.isInForwardedDecDocuments()) {
            return false;
        }
        return resource.isInWorkingStatus()
                && !BeanHelper.getNodeService().hasAspect(resource.getObject().getNodeRef(), ForumModel.ASPECT_DISCUSSABLE)
                && (isDocManagerOrOwner(resource.getObject().getProperties()) || isCWFOrWorkingTaskOwner());
    }

}
