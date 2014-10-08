package ee.webmedia.alfresco.document.web.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import java.util.Date;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.evaluator.SharedResourceEvaluator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.privilege.model.Privilege;

/**
 * Evaluator, that is used to decide if we should show registrDoc button or not
 */
public class RegisterDocumentEvaluator extends SharedResourceEvaluator {
    private static final long serialVersionUID = 2958297435415449179L;

    @Override
    public boolean evaluate(Node docNode) {
        NodeRef docTypeRef = getDocTypeRef(docNode);
        return docNode.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)
                && new DocumentNotInDraftsFunctionActionEvaluator().evaluate(docNode)
                && !BeanHelper.getDocumentDialogHelperBean().isInEditMode() && canRegister(docNode, true, docTypeRef)
                && getDocumentAdminService().getTypeProperty(docTypeRef, DocumentAdminModel.Props.REGISTRATION_ON_DOC_FORM_ENABLED, Boolean.class);
    }

    public static boolean canRegister(Node docNode, boolean checkStoppedOrInprogressWorkflows) {
        return canRegister(docNode, checkStoppedOrInprogressWorkflows, getDocTypeRef(docNode));
    }

    private static boolean canRegister(Node docNode, boolean checkStoppedOrInprogressWorkflows, NodeRef docTypeRef) {
        if (!canRegister(docNode, docTypeRef)) {
            return false;
        }
        NodeRef docRef = docNode.getNodeRef();
        if (checkStoppedOrInprogressWorkflows && !BeanHelper.getWorkflowService().hasNoStoppedOrInprogressCompoundWorkflows(docRef)) {
            return false;
        }
        if (!BeanHelper.getDocumentService().isSaved(docRef)) {
            return false;
        }
        return docNode.hasPermission(Privilege.EDIT_DOCUMENT);
    }

    public static boolean canRegister(Node docNode) {
        return canRegister(docNode, getDocTypeRef(docNode));
    }

    private static boolean canRegister(Node docNode, NodeRef docTypeRef) {
        if (isRegistered(docNode)) {
            return false;
        }
        if (!getDocumentAdminService().getTypeProperty(docTypeRef, DocumentAdminModel.Props.REGISTRATION_ENABLED, Boolean.class)) {
            return false;
        }
        return true;
    }

    private static NodeRef getDocTypeRef(Node docNode) {
        NodeRef docTypeRef = getDocumentAdminService().getDocumentTypeRef((String) docNode.getProperties().get(Props.OBJECT_TYPE_ID));
        return docTypeRef;
    }

    public static boolean isNotRegistered(Node docNode) {
        return !isRegistered(docNode);
    }

    public static boolean isRegistered(Node docNode) {
        final Map<String, Object> props = docNode.getProperties();
        final String regNumber = (String) props.get(DocumentCommonModel.Props.REG_NUMBER);
        final Date regDateTime = (Date) props.get(DocumentCommonModel.Props.REG_DATE_TIME);
        if (StringUtils.isNotBlank(regNumber) && regDateTime != null) {
            return true;
        }
        return false;
    }

    public static boolean evaluateAdditionalButton(Node docNode) {
        if (!BeanHelper.getPermissionService().hasPermission(docNode.getNodeRef(), PermissionService.WRITE_PROPERTIES).equals(AccessStatus.ALLOWED)) {
            return false;
        }
        return isNotRegistered(docNode);
    }

    @Override
    public boolean evaluate() {
        DocumentDynamicActionsGroupResources resource = (DocumentDynamicActionsGroupResources) sharedResource;
        return resource.isWorkspaceNode() && !resource.isInEditMode()
                && resource.isNotInDraftsFunction()
                && canRegister(resource.getObject(), true, resource.getDocumentTypeRef())
                && getDocumentAdminService().getTypeProperty(resource.getDocumentTypeRef(), DocumentAdminModel.Props.REGISTRATION_ON_DOC_FORM_ENABLED, Boolean.class);
    }
}
