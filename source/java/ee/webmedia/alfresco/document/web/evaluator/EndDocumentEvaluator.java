package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.evaluator.SharedResourceEvaluator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * UI action evaluator for validating whether user can end current document.
 */
public class EndDocumentEvaluator extends SharedResourceEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Node node) {
        if (!node.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE) || !new DocumentNotInDraftsFunctionActionEvaluator().evaluate(node)) {
            return false;
        }
        String regNumber = (String) node.getProperties().get(DocumentCommonModel.Props.REG_NUMBER.toString());
        if (StringUtils.isBlank(regNumber) && !BeanHelper.getApplicationConstantsBean().isFinishUnregisteredDocumentEnabled()) {
            return false;
        }
        boolean isWorking = DocumentStatus.WORKING.getValueName().equals(node.getProperties().get(DocumentCommonModel.Props.DOC_STATUS.toString()));
        return isWorking && !BeanHelper.getDocumentDialogHelperBean().isInEditMode()
                && (ReopenDocumentEvaluator.hasUserRights(node) || BeanHelper.getWorkflowService().isOwnerOfInProgressActiveResponsibleAssignmentTask(node.getNodeRef()));
    }

    @Override
    public boolean evaluate() {
        DocumentDynamicActionsGroupResources resource = (DocumentDynamicActionsGroupResources) sharedResource;
        if (!resource.isWorkspaceNode() || resource.isInEditMode() || !resource.isInWorkingStatus() || !resource.isNotInDraftsFunction()) {
            return false;
        }
        if (StringUtils.isBlank(resource.getRegNr()) && !BeanHelper.getApplicationConstantsBean().isFinishUnregisteredDocumentEnabled()) {
            return false;
        }
        Node node = resource.getObject();
        return ReopenDocumentEvaluator.hasUserRights(node) || BeanHelper.getWorkflowService().isOwnerOfInProgressActiveResponsibleAssignmentTask(node.getNodeRef());

    }

}
