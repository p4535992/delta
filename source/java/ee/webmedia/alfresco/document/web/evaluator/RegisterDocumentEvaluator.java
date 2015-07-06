package ee.webmedia.alfresco.document.web.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import java.util.Date;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;

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
                && !BeanHelper.getDocumentDialogHelperBean().isInEditMode() && canRegister(docNode, true, docTypeRef, null)
                && getDocumentAdminService().getTypeProperty(docTypeRef, DocumentAdminModel.Props.REGISTRATION_ON_DOC_FORM_ENABLED, Boolean.class);
    }

    public static boolean canRegister(Node docNode, boolean checkStoppedOrInprogressWorkflows) {
        return canRegister(docNode, checkStoppedOrInprogressWorkflows, getDocTypeRef(docNode), null);
    }

    public static boolean canRegisterWithLog(Node docNode, boolean checkStoppedOrInprogressWorkflows, Log log) {
        return canRegister(docNode, checkStoppedOrInprogressWorkflows, getDocTypeRef(docNode), log);
    }

    private static boolean canRegister(Node docNode, boolean checkStoppedOrInprogressWorkflows, NodeRef docTypeRef, Log log) {
        if (!canRegister(docNode, docTypeRef, log)) {
            return false;
        }
        NodeRef docRef = docNode.getNodeRef();
        boolean doLog = log != null;
        if (checkStoppedOrInprogressWorkflows && !BeanHelper.getWorkflowService().hasNoStoppedOrInprogressCompoundWorkflows(docRef)) {
            if (doLog) {
                log.info(getCannotRegisterLogPrefix(docNode) + "document has compound workflows in in progress or stopped status.");
            }
            return false;
        }
        if (!BeanHelper.getDocumentService().isSaved(docRef)) {
            if (doLog) {
                log.info(getCannotRegisterLogPrefix(docNode) + "document is not saved (parent with type volume, caseFile or case cannot be found)");
            }
            return false;
        }
        boolean hasPermission = docNode.hasPermission(Privilege.EDIT_DOCUMENT);
        if (!hasPermission && doLog) {
            String username = AuthenticationUtil.getRunAsUser();
            log.info(getCannotRegisterLogPrefix(docNode) + "no permission to edit document. Document loaded permissions: "
                    + docNode.printLoadedPermissions());
        }
        return hasPermission;
    }

    public static boolean canRegister(Node docNode) {
        return canRegister(docNode, getDocTypeRef(docNode), null);
    }

    private static boolean canRegister(Node docNode, NodeRef docTypeRef, Log log) {
        if (isRegistered(docNode)) {
            if (log != null) {
                Map<String, Object> props = docNode.getProperties();
                log.info(getCannotRegisterLogPrefix(docNode) + "document is already registered, number: "
                        + (String) props.get(DocumentCommonModel.Props.REG_NUMBER) + ", date: " + props.get(DocumentCommonModel.Props.REG_DATE_TIME));
            }
            return false;
        }
        if (!getDocumentAdminService().getTypeProperty(docTypeRef, DocumentAdminModel.Props.REGISTRATION_ENABLED, Boolean.class)) {
            if (log != null) {
                log.info(getCannotRegisterLogPrefix(docNode) + " registration is not enabled on document type nodeRef=" + docTypeRef);
            }
            return false;
        }
        return true;
    }

    private static String getCannotRegisterLogPrefix(Node docNode) {
        return "Cannot register document nodeRef = " + docNode.getNodeRef() + ", reason: ";
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
                && canRegister(resource.getObject(), true, resource.getDocumentTypeRef(), null)
                && getDocumentAdminService().getTypeProperty(resource.getDocumentTypeRef(), DocumentAdminModel.Props.REGISTRATION_ON_DOC_FORM_ENABLED, Boolean.class);
    }
}
