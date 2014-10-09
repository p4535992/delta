package ee.webmedia.alfresco.document.sendout.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.evaluator.SharedResourceEvaluator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.web.evaluator.DocumentDynamicActionsGroupResources;
import ee.webmedia.alfresco.document.web.evaluator.DocumentNotInDraftsFunctionActionEvaluator;
import ee.webmedia.alfresco.privilege.model.Privilege;

/**
 * Evaluator, that evaluates to true if user is admin or document manager or document owner.
 */
public class SendOutActionEvaluator extends SharedResourceEvaluator {

    private static final long serialVersionUID = 2958297435415449179L;

    @Override
    public boolean evaluate(Node node) {
        NodeRef nodeRef = node.getNodeRef();
        if (!nodeRef.getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE) || BeanHelper.getDocumentDialogHelperBean().isInEditMode()) {
            return false;
        }
        boolean result = !BeanHelper.getDocumentDynamicService().isDraftOrImapOrDvk(nodeRef)
                && new DocumentNotInDraftsFunctionActionEvaluator().evaluate(node)
                && node.hasPermission(Privilege.EDIT_DOCUMENT);

        if (result) {
            final Map<String, Object> props = node.getProperties();
            final String regNumber = (String) props.get(DocumentCommonModel.Props.REG_NUMBER);
            if (regNumber == null) {
                result = BeanHelper.getWorkflowService().hasNoStoppedOrInprogressCompoundWorkflows(nodeRef);
            }
            if (result) {
                String docTypeId = (String) props.get(Props.OBJECT_TYPE_ID);
                if (!getDocumentAdminService().getDocumentTypeProperty(docTypeId, DocumentAdminModel.Props.SEND_UNREGISTRATED_DOC_ENABLED, Boolean.class)) {
                    result = regNumber != null;
                }
            }
        }
        return result;
    }

    @Override
    public boolean evaluate() {
        DocumentDynamicActionsGroupResources resource = (DocumentDynamicActionsGroupResources) sharedResource;
        if (!resource.isWorkspaceNode() || resource.isInEditMode()) {
            return false;
        }
        boolean result = !resource.isDraftOrImapOrDvk() && resource.isNotInDraftsFunction() && resource.hasEditPermission();
        if (result) {
            final String regNumber = resource.getRegNr();
            if (StringUtils.isBlank(regNumber)) {
                result = BeanHelper.getWorkflowService().hasNoStoppedOrInprogressCompoundWorkflows(resource.getObject().getNodeRef());
            }
            if (result) {
                if (!getDocumentAdminService().getDocumentTypeProperty(resource.getObjectTypeId(), DocumentAdminModel.Props.SEND_UNREGISTRATED_DOC_ENABLED, Boolean.class)) {
                    result = StringUtils.isNotEmpty(regNumber);
                }
            }
        }
        return result;
    }
}
