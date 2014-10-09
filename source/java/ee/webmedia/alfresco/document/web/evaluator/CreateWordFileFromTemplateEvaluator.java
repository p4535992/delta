package ee.webmedia.alfresco.document.web.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentTemplateService;

import java.util.Map;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.util.EqualsHelper;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.evaluator.SharedResourceEvaluator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.privilege.model.Privilege;

public class CreateWordFileFromTemplateEvaluator extends SharedResourceEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        if (!node.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE) || BeanHelper.getDocumentDialogHelperBean().isInEditMode()) {
            return false;
        }
        Map<String, Object> properties = node.getProperties();
        Object docStatus = properties.get(DocumentCommonModel.Props.DOC_STATUS.toString());
        String objectTypeId = (String) properties.get(Props.OBJECT_TYPE_ID);
        if (!(EqualsHelper.nullSafeEquals(DocumentStatus.WORKING.getValueName(), docStatus)
                && getDocumentTemplateService().hasDocumentsTemplate(objectTypeId)
                && node.hasPermission(Privilege.EDIT_DOCUMENT))) {
            return false;
        }
        return BeanHelper.getWorkflowDbService().hasNoInProgressOrOnlyActiveResponsibleAssignmentTasks(BeanHelper.getWorkflowBlockBean().getCompoundWorkflows());
    }

    @Override
    public boolean evaluate() {
        DocumentDynamicActionsGroupResources resource = (DocumentDynamicActionsGroupResources) sharedResource;
        if (!resource.isWorkspaceNode() || resource.isInEditMode()) {
            return false;
        }
        if (!resource.isInStatus(DocumentStatus.WORKING)
                || !getDocumentTemplateService().hasDocumentsTemplate(resource.getObjectTypeId())
                || !resource.hasEditPermission()) {
            return false;
        }
        return BeanHelper.getWorkflowDbService().hasNoInProgressOrOnlyActiveResponsibleAssignmentTasks(BeanHelper.getWorkflowBlockBean().getCompoundWorkflows());
    }
}